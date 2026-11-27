# Classroom homework module

Teachers make a class and share its code. Parents join with the code, and the teacher lets each child in.
The teacher gives homework (videos, letters to trace, games, in order). The child does it from the
"Homework" button on Home, and the teacher sees each child's progress.

Everything lives in this folder:

| File | What it holds |
|---|---|
| `ClassroomData.kt` | Firestore data: classes, students, homework, results |
| `ClassroomUi.kt` | Shared colours, buttons, chips |
| `StudentHomework.kt` | The child's homework list, the tracing task, the Home button |
| `TeacherScreens.kt` | Classroom hub (join / create), class page, progress grid, homework builder |
| `ClassroomNav.kt` | The screens' routes and the Home button wrapper |

## Switch it off (keeps the code)

In `ui/data/FeatureFlags.kt`, set `CLASSROOM = false`. Nothing shows, and the compiler leaves the
screens out of the build.

## Remove it completely

1. Delete this folder (`ui/classroom/`).
2. In `ui/navigation/AppNavigation.kt`, delete:
   - the line `import com.example.ui.classroom.classroomRoutes`
   - the line `if (… CLASSROOM) classroomRoutes(navController)`
   - in `composable("home")`, replace the `WithHomeworkButton(…)` line with `home()`
3. In `ui/data/FeatureFlags.kt`, delete `CLASSROOM`.
4. In `firestore.rules`, delete the "Classroom homework" block and publish the rules.

Two small general hooks outside this folder can stay. They do nothing unless something listens:

- `GameProgress.lastLevel`: the last level finished in any game
- `TraceExercise(onChecked = …)`: the accuracy of each checked trace

## Firestore

Paste `firestore.rules` into Firebase console › Firestore › Rules and publish. Until you do,
classroom screens say "Not allowed".
