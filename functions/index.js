/**
 * Server checks for Kannada Kids subscriptions (Firebase project kannadakids-fd92d).
 *
 * The app sends the purchase proof it got from the store:
 *   - Android: the Google Play purchase token
 *   - iPhone:  the App Store signed transaction (JWS)
 * This function asks Google Play / checks Apple's signature, and only then writes the family's
 * record at subscriptions/{uid}. The Firestore rules stop the app from writing that record itself,
 * so nobody can switch Gold on without paying.
 *
 * Settings live in functions/.env (ANDROID_PACKAGE, IOS_BUNDLE_ID, APPLE_APP_ID).
 */
const crypto = require("crypto");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const { GoogleAuth } = require("google-auth-library");
const { SignedDataVerifier, Environment } = require("@apple/app-store-server-library");

admin.initializeApp();
const db = admin.firestore();

const ANDROID_PACKAGE = process.env.ANDROID_PACKAGE || "com.funlearn.studio";
const IOS_BUNDLE_ID = process.env.IOS_BUNDLE_ID || "com.aistudio.kannadakids.ios";
// The app's numeric Apple ID (App Store Connect > App Information). Needed for real App Store
// purchases; without it only TestFlight / sandbox purchases can be checked.
const APPLE_APP_ID = process.env.APPLE_APP_ID ? Number(process.env.APPLE_APP_ID) : undefined;

// Same names and prices as PlanPeriod in the app (composeApp/.../platform/Billing.kt).
const PLANS = {
  premium_monthly: { name: "Monthly", amount: "₹99" },
  premium_halfyearly: { name: "HalfYearly", amount: "₹499" },
  premium_yearly: { name: "Yearly", amount: "₹899" },
};

const MAX_PURCHASES = 10;
const MAX_PROOF_LENGTH = 20000;

/** SHA-256 of the user id, as hex. The Android app sends this as the Play "obfuscated account id". */
function accountHash(uid) {
  return crypto.createHash("sha256").update(uid, "utf8").digest("hex");
}

/** The App Store "appAccountToken" the iPhone app sets: the first 16 bytes of SHA-256(uid) as a UUID. */
function accountUuid(uid) {
  const h = crypto.createHash("sha256").update(uid, "utf8").digest("hex").slice(0, 32);
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20, 32)}`;
}

// ---------------------------------------------------------------- Google Play

let playClient = null;
async function play() {
  if (!playClient) {
    const auth = new GoogleAuth({ scopes: ["https://www.googleapis.com/auth/androidpublisher"] });
    playClient = await auth.getClient();
  }
  return playClient;
}

const PLAY_ENTITLED = new Set([
  "SUBSCRIPTION_STATE_ACTIVE",
  "SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
  "SUBSCRIPTION_STATE_CANCELED", // cancelled but paid until the expiry time
]);

/** Checks a Google Play purchase token. Returns an entitlement, or null when it gives no access. */
async function checkAndroid(uid, token) {
  const url =
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/` +
    `${encodeURIComponent(ANDROID_PACKAGE)}/purchases/subscriptionsv2/tokens/${encodeURIComponent(token)}`;
  let sub;
  try {
    sub = (await (await play()).request({ url })).data;
  } catch (e) {
    const code = e.response?.status;
    if (code === 400 || code === 404 || code === 410) return null; // not a real / expired token
    throw e;
  }
  const owner = sub.externalAccountIdentifiers?.obfuscatedExternalAccountId;
  if (owner !== accountHash(uid)) {
    logger.warn("Play purchase belongs to another account", { uid });
    return null;
  }
  if (!PLAY_ENTITLED.has(sub.subscriptionState)) return null;
  const item = (sub.lineItems || [])
    .filter((l) => PLANS[l.productId])
    .sort((a, b) => Date.parse(b.expiryTime) - Date.parse(a.expiryTime))[0];
  if (!item) return null;
  const expiresAt = Date.parse(item.expiryTime);
  if (!(expiresAt > Date.now())) return null;
  return {
    platform: "android",
    productId: item.productId,
    expiresAt,
    autoRenew: item.autoRenewingPlan?.autoRenewEnabled === true,
    orderId: sub.latestOrderId || token.slice(0, 24),
    startedAt: sub.startTime ? Date.parse(sub.startTime) : Date.now(),
    keep: { token },
  };
}

// ---------------------------------------------------------------- App Store

let appleRoots = null;
async function appleRootCerts() {
  if (!appleRoots) {
    const urls = [
      "https://www.apple.com/certificateauthority/AppleRootCA-G3.cer",
      "https://www.apple.com/certificateauthority/AppleRootCA-G2.cer",
      "https://www.apple.com/appleca/AppleIncRootCertificate.cer",
    ];
    appleRoots = await Promise.all(
      urls.map(async (u) => {
        const r = await fetch(u);
        if (!r.ok) throw new Error(`Couldn't download ${u} (${r.status})`);
        return Buffer.from(await r.arrayBuffer());
      })
    );
  }
  return appleRoots;
}

const verifiers = {};
async function appleVerifier(environment) {
  if (!verifiers[environment]) {
    if (environment === Environment.PRODUCTION && !APPLE_APP_ID) {
      throw new HttpsError("failed-precondition", "APPLE_APP_ID isn't set in functions/.env yet.");
    }
    verifiers[environment] = new SignedDataVerifier(
      await appleRootCerts(),
      true, // check the certificates haven't been revoked
      environment,
      IOS_BUNDLE_ID,
      environment === Environment.PRODUCTION ? APPLE_APP_ID : undefined
    );
  }
  return verifiers[environment];
}

/** Which Apple environment a JWS says it is from (checked properly by the verifier afterwards). */
function claimedEnvironment(jws) {
  try {
    const payload = JSON.parse(Buffer.from(jws.split(".")[1], "base64url").toString("utf8"));
    return payload.environment === "Production" ? Environment.PRODUCTION : Environment.SANDBOX;
  } catch (e) {
    return null;
  }
}

/** Checks an App Store signed transaction. Returns an entitlement, or null when it gives no access. */
async function checkIos(uid, jws, autoRenewHint) {
  const env = claimedEnvironment(jws);
  if (!env) return null;
  let tx;
  try {
    tx = await (await appleVerifier(env)).verifyAndDecodeTransaction(jws);
  } catch (e) {
    if (e instanceof HttpsError) throw e;
    logger.warn("App Store transaction didn't verify", { uid, error: String(e) });
    return null;
  }
  if (!PLANS[tx.productId] || tx.revocationDate) return null;
  if (tx.appAccountToken && tx.appAccountToken.toLowerCase() !== accountUuid(uid)) {
    logger.warn("App Store purchase belongs to another account", { uid });
    return null;
  }
  const expiresAt = Number(tx.expiresDate || 0);
  if (!(expiresAt > Date.now())) return null;

  // One App Store subscription can only unlock one account in the app.
  const original = String(tx.originalTransactionId || tx.transactionId);
  const claimRef = db.doc(`purchaseClaims/ios_${original}`);
  const ok = await db.runTransaction(async (t) => {
    const claim = await t.get(claimRef);
    if (claim.exists && claim.get("uid") !== uid) return false;
    if (!claim.exists) t.set(claimRef, { uid, at: Date.now() });
    return true;
  });
  if (!ok) {
    logger.warn("App Store purchase already used by another account", { uid });
    return null;
  }

  return {
    platform: "ios",
    productId: tx.productId,
    expiresAt,
    autoRenew: autoRenewHint !== false, // only used for display
    orderId: String(tx.transactionId),
    startedAt: Number(tx.originalPurchaseDate || tx.purchaseDate || Date.now()),
    keep: { jws },
  };
}

// ---------------------------------------------------------------- The function the app calls

/**
 * verifyPurchases({ purchases: [{ platform: "android"|"ios", proof: "...", autoRenew: bool }] })
 * Checks the purchases sent by the app plus the ones saved from earlier calls (so a phone without
 * the subscription still sees it when it was bought on the other platform), then updates
 * subscriptions/{uid} and returns it.
 */
exports.verifyPurchases = onCall({ region: "us-central1", maxInstances: 10 }, async (request) => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Please sign in first.");

  const sent = Array.isArray(request.data?.purchases) ? request.data.purchases.slice(0, MAX_PURCHASES) : [];
  const recordRef = db.doc(`purchaseRecords/${uid}`);
  const subRef = db.doc(`subscriptions/${uid}`);
  const [recordSnap, subSnap] = await Promise.all([recordRef.get(), subRef.get()]);
  const saved = recordSnap.exists ? recordSnap.data() : {};
  const current = subSnap.exists ? subSnap.data() : null;

  // Everything to check: what the app sent + what we kept from before.
  const toCheck = [];
  for (const p of sent) {
    if (!p || typeof p.proof !== "string" || !p.proof || p.proof.length > MAX_PROOF_LENGTH) continue;
    if (p.platform === "android" || p.platform === "ios") {
      toCheck.push({ platform: p.platform, proof: p.proof, autoRenew: p.autoRenew });
    }
  }
  if (saved.android?.token) toCheck.push({ platform: "android", proof: saved.android.token });
  if (saved.ios?.jws) toCheck.push({ platform: "ios", proof: saved.ios.jws, autoRenew: saved.ios.autoRenew });
  const unique = [...new Map(toCheck.map((p) => [`${p.platform}:${p.proof}`, p])).values()];

  const found = [];
  for (const p of unique) {
    try {
      const e = p.platform === "android"
        ? await checkAndroid(uid, p.proof)
        : await checkIos(uid, p.proof, p.autoRenew);
      if (e) found.push(e);
    } catch (e) {
      if (e instanceof HttpsError) throw e;
      logger.error("Couldn't check a purchase", { uid, platform: p.platform, error: String(e) });
      throw new HttpsError("unavailable", "Couldn't reach the store to check the subscription. Please try again.");
    }
  }

  const now = Date.now();
  const best = found.sort((a, b) => b.expiresAt - a.expiresAt)[0];

  // Keep the newest valid proof per platform, so the other platform's app can be checked later.
  const keep = {};
  for (const platform of ["android", "ios"]) {
    const e = found.filter((x) => x.platform === platform).sort((a, b) => b.expiresAt - a.expiresAt)[0];
    keep[platform] = e ? { ...e.keep, autoRenew: e.autoRenew, checkedAt: now } : admin.firestore.FieldValue.delete();
  }
  await recordRef.set(keep, { merge: true });

  const wasPaid = current && (current.status === "active" || current.status === "autorenew_off");
  const fields = {
    uid,
    email: request.auth.token.email || "",
    name: request.auth.token.name || (request.auth.token.email || "").split("@")[0],
    updatedAt: now,
    verifiedAt: now,
  };
  if (!current) fields.joinedAt = now;

  if (best) {
    const payments = Array.isArray(current?.payments) ? current.payments.slice() : [];
    if (!payments.some((x) => x && x.orderId === best.orderId)) {
      payments.push({ orderId: best.orderId, date: now, plan: PLANS[best.productId].name, amount: PLANS[best.productId].amount });
    }
    Object.assign(fields, {
      plan: PLANS[best.productId].name,
      status: best.autoRenew ? "active" : "autorenew_off",
      autoRenew: best.autoRenew,
      renewsAt: best.expiresAt,
      platform: best.platform,
      payments: payments.slice(-60),
    });
    if (!current?.startedAt) fields.startedAt = best.startedAt;
  } else {
    Object.assign(fields, {
      status: wasPaid ? "expired" : current?.status || "none",
      autoRenew: false,
    });
  }

  await subRef.set(fields, { merge: true });
  const result = { ...(current || {}), ...fields };
  return {
    plan: result.plan || "",
    status: result.status || "none",
    autoRenew: !!result.autoRenew,
    renewsAt: Number(result.renewsAt || 0),
    platform: result.platform || "",
    bonusUntil: Number(result.bonusUntil || 0),
  };
});
