# ZenSwitch (OurMajor) — Full Codebase Audit Report

**Scope:** AndroidManifest, Gradle, Firebase, permissions, architecture, UI/UX, accessibility, background services, lifecycle, app logic, security, performance, OEM/version compatibility.  
**Standards:** Play Store readiness, 2024–2026 Android best practices.

---

## 1. LIFECYCLE & APPLICATION

### 1.1 ZenSwitchApp — super.onCreate() order and prefs before init

**What is wrong:**  
`ZenSwitchApp.onCreate()` reads `SharedPreferences` and sets night mode **before** calling `super.onCreate()`, and only then calls `FirebaseApp.initializeApp(this)`.

**Why it is a problem:**  
Application context and component initialization are not fully ready until `super.onCreate()` has run. Reading prefs and applying theme before that can behave inconsistently across devices. Firebase must be initialized as early as possible; doing it after theme can delay or affect dependent code.

**Severity:** Medium

**How to fix:**
```kotlin
override fun onCreate() {
    super.onCreate()
    FirebaseApp.initializeApp(this)
    val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)
    val darkEnabled = prefs.getBoolean("dark_mode", false)
    AppCompatDelegate.setDefaultNightMode(
        if (darkEnabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
    )
    val firestore = FirebaseFirestore.getInstance()
    firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
        .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
        .build()
}
```

**Better alternative:** Use `AppCompatDelegate.setDefaultNightMode()` in a dedicated splash or after the first frame, or migrate to `EdgeToEdge` and system bar handling per Activity.

---

### 1.2 ZenSwitchApp — Firestore settings overwritten every process start

**What is wrong:**  
Firestore cache settings are set in `Application.onCreate()` every time. No check for existing/default settings.

**Why it is a problem:**  
Redundant work and possible re-initialization of cache; in edge cases could reset or conflict with Firestore’s own defaults.

**Severity:** Low

**How to fix:**  
Set once and/or guard: e.g. only call `setLocalCacheSettings` if you need to override defaults; otherwise remove and rely on Firestore defaults.

**Better alternative:** Configure Firestore in a single place (e.g. a `Firebase` or `Di` module) and ensure it’s only applied once.

---

## 2. MAIN ACTIVITY & PERMISSION FLOW

### 2.1 MainActivity — checkAndRequestPermissions on every onResume

**What is wrong:**  
`PermissionManager.checkAndRequestPermissions(this)` is called in `onResume()` with no guard. Every return from Settings (or any other app) triggers the full permission flow again.

**Why it is a problem:**  
If the user has already granted everything, dialogs don’t show and the method just returns true. If one permission is still missing, a dialog is shown every time the app is resumed (e.g. after switching app or returning from Settings), which is repetitive and annoying.

**Severity:** Medium (UX)

**How to fix:**  
Track “permission gate” state (e.g. only run the gate until all critical permissions are granted once). Example:

```kotlin
override fun onResume() {
    super.onResume()
    if (!permissionGatePassed && !PermissionManager.checkAndRequestPermissions(this)) return
    permissionGatePassed = true
}
```

Where `permissionGatePassed` is set to true when `checkAndRequestPermissions` returns true, and optionally reset when you want to re-check (e.g. after a long time or app update).

**Better alternative:** Run the permission gate only on first launch (or first launch after login) and after returning from a specific Settings screen (e.g. via `ActivityResultLauncher` or a known `requestCode`), not on every generic `onResume`.

---

### 2.2 MainActivity — observeAuthState with finish() and LiveData

**What is wrong:**  
When `state.isAuthenticated` is false, the activity starts `AuthActivity` and calls `finish()`. The observer is registered on `this` (activity) and never removed.

**Why it is a problem:**  
If the ViewModel or repository emits a second event (e.g. token expired) after `finish()` is called but before the activity is destroyed, you can get a call into a finishing activity. LiveData may also deliver to an activity in an invalid state.

**Severity:** Low–Medium

**How to fix:**  
Use `lifecycleScope` or remove the observer when navigating away, or use a one-shot event (SingleLiveEvent / Event wrapper) so “navigate to auth” happens only once per sign-out.

**Better alternative:** Use a single source of truth for “auth required” (e.g. NavGraph with conditional start destination, or AuthStateViewModel with one-time events) so that only one component decides the transition to AuthActivity.

---

### 2.3 MainActivity — crash trap exposes stack traces to UI

**What is wrong:**  
In the `catch` block of `onCreate()`, a `TextView` is filled with `Log.getStackTraceString(e)` and shown in a `ScrollView` with `setContentView(sv)`.

**Why it is a problem:**  
In debug or if the build is misconfigured, users (or testers) could see full stack traces. That leaks internal paths and logic and is not acceptable for production.

**Severity:** High (security / policy)

**How to fix:**  
Remove the debug UI in release, or show a generic “Something went wrong” message and optionally report the crash to a non-log endpoint (e.g. Crashlytics) without putting the stack trace on screen.

```kotlin
} catch (e: Exception) {
    Log.e("CRASH_TRAP", "Startup Error", e)
    if (BuildConfig.DEBUG) {
        // optional: show stack trace only in debug
    } else {
        Toast.makeText(applicationContext, getString(R.string.error_generic), Toast.LENGTH_LONG).show()
    }
    // Do not setContentView with stack trace in release
}
```

**Better alternative:** Use `Thread.setDefaultUncaughtExceptionHandler` and Crashlytics (or similar) for crash reporting; keep the UI to a simple error state or finish.

---

## 3. FOCUS MONITOR SERVICE & ENGINE

### 3.1 FocusMonitorService — CoroutineScope never cancelled

**What is wrong:**  
`serviceScope = CoroutineScope(Dispatchers.IO + Job())` is never cancelled. Only `monitorJob` is cancelled in `onDestroy()`.

**Why it is a problem:**  
If any other coroutine is ever launched on `serviceScope`, it will outlive the service. The scope’s Job is not tied to the service lifecycle, so you can get leaks and work continuing after the service is destroyed.

**Severity:** Medium

**How to fix:**  
Cancel the scope in `onDestroy()`:

```kotlin
override fun onDestroy() {
    serviceScope.coroutineContext[Job]?.cancel()
    // ... rest
}
```

Or use `serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())` and hold the `Job` in a property, then `job.cancel()` in `onDestroy()`.

**Better alternative:** Use `lifecycleScope` equivalent for services (e.g. a scope that cancels when the service is destroyed), or at minimum a single `Job` that is the parent of all work and cancel that in `onDestroy()`.

---

### 3.2 FocusMonitorService — startForegroundService can throw on O+

**What is wrong:**  
`RestartReceiver` and other callers use `context.startForegroundService(serviceIntent)`. If the app was in background long enough, the system may not allow starting a foreground service and can throw `ForegroundServiceStartNotAllowedException` (Android 12+).

**Why it is a problem:**  
Unhandled exception kills the receiver (or the process), and the service is not restarted. Users then see Focus Guard “die” after the app was in background.

**Severity:** High

**How to fix:**  
Wrap the start in try/catch and fall back to `startService` on older APIs, or schedule a job to start the service when constraints allow:

```kotlin
try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(serviceIntent)
    } else {
        context.startService(serviceIntent)
    }
} catch (e: ForegroundServiceStartNotAllowedException) {
    // Schedule WorkManager one-time job to start service when app can
}
```

**Better alternative:** Use WorkManager to “restart” the monitor when the app is in foreground or when constraints are met, and have the worker start the foreground service. That respects background limits and avoids crashes.

---

### 3.3 FocusMonitorService — direct startActivity from service without FLAG_ACTIVITY_NEW_TASK

**What is wrong:**  
In `triggerHighPriorityNudge`, when overlay is allowed you call `startActivity(intent)`. The intent already has `FLAG_ACTIVITY_NEW_TASK` (and others). Starting an activity from a non-activity context without `FLAG_ACTIVITY_NEW_TASK` would be wrong; here the flags are set, so this is correct. No change needed for this point; only noting for completeness.

(If anywhere you started an activity from the service without NEW_TASK, that would crash. Current code sets the flags on the intent.)

---

### 3.4 FocusMonitorService — PendingIntent request code from currentTimeMillis().toInt()

**What is wrong:**  
`val requestCode = System.currentTimeMillis().toInt()` is used for `PendingIntent.getActivity`. On 32-bit devices, `toInt()` truncates the long; multiple nudges in the same second can theoretically get the same request code (and PendingIntent reuse).

**Why it is a problem:**  
Android may reuse a PendingIntent when request code and intent match. If two nudges get the same request code, the second could update the first and the “unique” PendingIntent guarantee is weakened.

**Severity:** Low

**How to fix:**  
Use a counter or a mix of time and nudge index, and keep the value in Int range:

```kotlin
private var nudgeRequestCode = 0
// ...
val requestCode = (System.currentTimeMillis().toInt() and 0x7FFF) or (++nudgeRequestCode shl 15)
```

Or use `NUDGE_NOTIFICATION_ID + (nudgeCount % 10000)` if you only need uniqueness per process.

**Better alternative:** Use `PendingIntent.getActivity(..., PendingIntent.FLAG_ONE_SHOT)` if each nudge should be one-shot, and still use a sufficiently unique request code to avoid collisions.

---

### 3.5 FocusMonitorService — nudge channel recreated every time

**What is wrong:**  
`ensureNudgeChannel()` is called on every nudge and calls `nm.createNotificationChannel(channel)`. The API is idempotent (existing channel is updated), but the channel object is recreated every time.

**Why it is a problem:**  
Minor unnecessary work; user-facing channel name/sound/vibration can be changed on the fly (Android 8.0+) so repeated creation is usually harmless but redundant.

**Severity:** Low

**How to fix:**  
Create the channel once (e.g. in `onCreate()` or the first time you need it) and guard:

```kotlin
private var nudgeChannelCreated = false
private fun ensureNudgeChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || nudgeChannelCreated) return
    nudgeChannelCreated = true
    // ...
}
```

**Better alternative:** Create all notification channels at app startup (e.g. in Application or a dedicated “notifications” init) so behavior is predictable and you avoid any first-nudge delay.

---

### 3.6 FocusRepository — init(context) not thread-safe

**What is wrong:**  
`FocusRepository.init(context)` does `if (prefs == null) { prefs = ... }`. No synchronization.

**Why it is a problem:**  
If two threads call `init` concurrently, both can see `prefs == null` and assign different `SharedPreferences` instances (or the same, depending on getSharedPreferences implementation). At minimum you have a data race; in theory you could end up with inconsistent prefs references.

**Severity:** Medium

**How to fix:**  
Synchronize on a lock object:

```kotlin
private val initLock = Any()
fun init(context: Context) {
    if (prefs != null) return
    synchronized(initLock) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
```

**Better alternative:** Initialize in `Application.onCreate()` on the main thread and never call `init` from multiple threads; then the repository can assume prefs is set after app start.

---

### 3.7 FocusRepository — getRealTimeUsage and forceUpdateUsageNow duplicate logic

**What is wrong:**  
`getRealTimeUsage` and `forceUpdateUsageNow` both call `UsageCalculator.calculateUsage`, update the same in-memory and persisted state (saveUsage, setCurrentUsageMs). Logic is duplicated.

**Why it is a problem:**  
Future changes (e.g. rounding, filtering) might be applied in one place and forgotten in the other, leading to inconsistent behavior between “loop” and “pull-to-refresh”.

**Severity:** Low

**How to fix:**  
Have one internal function that performs “recalculate and persist”, and expose it for both:

```kotlin
private fun recalculateAndPersistUsage(context: Context): Map<String, Long> {
    init(context)
    val appContext = context.applicationContext
    val map = UsageCalculator.calculateUsage(appContext, targetPackages)
    map.forEach { (pkg, ms) -> saveUsage(pkg, ms) }
    setCurrentUsageMs(map.values.sum())
    return map
}
fun getRealTimeUsage(context: Context): Long = recalculateAndPersistUsage(context).values.sum()
fun forceUpdateUsageNow(context: Context): Map<String, Long> = recalculateAndPersistUsage(context)
```

---

### 3.8 RestartReceiver — exported=true and no input validation

**What is wrong:**  
RestartReceiver has `android:exported="true"` (needed for SCREEN_ON/USER_PRESENT). It does not validate that the broadcast is from the system or from the same app for RESTART_FOCUS_MONITOR.

**Why it is a problem:**  
Any app can send an explicit intent with action `ACTION_RESTART_FOCUS_MONITOR` to your package. That could start FocusMonitorService repeatedly (e.g. DoS or battery drain) or at inconvenient times.

**Severity:** Medium (security / abuse)

**How to fix:**  
For `ACTION_RESTART_FOCUS_MONITOR`, only react if the intent is from your app (e.g. setPackage in sender is not enough for receiver to trust it). You cannot reliably “verify” the sender of a broadcast. So: either keep the receiver not exported and use a different mechanism for SCREEN_ON (e.g. a foreground service that subscribes to screen state), or accept that RESTART_FOCUS_MONITOR can be sent by others and add rate limiting / checks (e.g. only restart if last start was > X seconds ago).

**Better alternative:** Do not export the receiver for RESTART_FOCUS_MONITOR. Send the restart broadcast with an explicit component (Intent.setComponent) so only the system delivers it to you. For SCREEN_ON/USER_PRESENT, the system sends implicit broadcasts; on Android 8+ many of these are restricted. Use a foreground service or WorkManager to “wake” when the app is used again instead of relying on SCREEN_ON for critical logic.

---

### 3.9 UsageCalculator — queryEvents window and event reuse

**What is wrong:**  
`UsageEvents.Event()` is created once and reused in `while (usageEvents.getNextEvent(event))`. The API reuses the same object; that’s correct. But `queryEvents(startTime, endTime)` can return a large number of events if the device has been used for many days without reboot (startTime is midnight today).

**Why it is a problem:**  
On a device with lots of usage, the loop can run for a long time on the main thread if ever called from the main thread, or block the IO dispatcher for a long time, causing ANR or delayed nudges.

**Severity:** Medium (performance)

**How to fix:**  
Ensure `calculateUsage` is only ever called from a background thread (FocusMonitorService and HomeFragment already use Dispatchers.IO or lifecycleScope+withContext(IO)). Add a safety limit (e.g. max events to process) or cap `endTime - startTime` to 24 hours and document that “today” is from midnight.

**Better alternative:** Keep the 24-hour window but add a timeout or max-iterations in the loop so that one bad device cannot hang the app.

---

### 3.10 ActivityRegistry — getRandomActivity() and nudge intent

**What is wrong:**  
`getRandomActivity()` returns a random `Class<out Activity>`. The nudge builds an `Intent(this, activityClass)`. If that activity has `android:launchMode` or required extras, the intent might not be sufficient.

**Why it is a problem:**  
Some activities might assume they are started from the app’s own UI with certain extras; starting them without extras could show incomplete or wrong UI.

**Severity:** Low

**How to fix:**  
Document that all activities in the registry must be launchable with a simple startActivity(Intent(context, Clazz)) with no required extras. Or add a small “nudge launch” contract (e.g. optional extras) and set them in FocusMonitorService.

---

## 4. PERMISSIONS & PERMISSION MANAGER

### 4.1 PermissionManager — checkAndRequestPermissions shows dialogs with setCancelable(false)

**What is wrong:**  
All permission dialogs use `setCancelable(false)`. The user must tap “Open Settings”; there is no “Cancel” or “Later”.

**Why it is a problem:**  
If the user does not want to grant a permission (e.g. overlay), they cannot dismiss the dialog and use the rest of the app. That can feel coercive and may lead to bad store reviews or uninstalls.

**Severity:** Medium (UX)

**How to fix:**  
For non-critical permissions (e.g. overlay, battery optimization), allow cancel and proceed with limited functionality:

```kotlin
.setCancelable(true)
.setNegativeButton(activity.getString(android.R.string.cancel)) { dialog, _ -> dialog.dismiss() }
```

And return true from `checkAndRequestPermissions` when only “optional” permissions are missing, or introduce a “Skip” that sets a “don’t ask again for this session” flag.

**Better alternative:** Request only the strict minimum to use the app (e.g. usage stats) in a blocking way; request notification, battery, and overlay later in context (e.g. when the user enables Focus Guard) with a clear explanation and a “Not now” option.

---

### 4.2 PermissionManager — context can be an Activity that is finishing

**What is wrong:**  
If the user taps “Open Settings” and the activity finishes (e.g. due to low memory or the system), the passed `activity` might be finishing when the callback runs.

**Why it is a problem:**  
Calling `activity.startActivity(...)` from a callback when the activity is finishing can throw or behave oddly.

**Severity:** Low

**How to fix:**  
In the positive button listener, check `if (!activity.isFinishing && !activity.isDestroyed) activity.startActivity(...)`.

---

### 4.3 AndroidManifest — REQUEST_IGNORE_BATTERY_OPTIMIZATIONS and store policy

**What is wrong:**  
The app declares `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. Google Play policy restricts use of this permission to a short list of acceptable use cases (e.g. backup, device finder). “Focus Guard” might not be accepted without justification.

**Why it is a problem:**  
App could be rejected or removed if the use case is not clearly allowed.

**Severity:** High (policy)

**How to fix:**  
Check current Play policy for “Request to disable battery optimization”. If your use case is not listed, remove the permission and the flow that opens the “ignore battery optimizations” screen, and rely on “Unrestricted” battery setting guidance in-app instead of programmatic request.

**Better alternative:** Rely on foreground service + “Unrestricted” in battery settings (user manually sets it) and document it in the app; avoid requesting the exemption in code unless policy explicitly allows it.

---

### 4.4 AndroidManifest — SYSTEM_ALERT_WINDOW and store policy

**What is wrong:**  
SYSTEM_ALERT_WINDOW (draw over other apps) is a sensitive permission. Play policy requires that the use case is clearly necessary and explained.

**Why it is a problem:**  
Overlay can be abused (e.g. phishing). If the app doesn’t justify it clearly, it may be rejected or flagged.

**Severity:** Medium (policy)

**How to fix:**  
In the store listing and in-app, clearly explain that overlay is used only to bring the user back to a mindful activity when the daily limit is reached (no overlay content for ads or extra UI). Consider making overlay optional and degrading to notification-only when not granted.

---

## 5. UI / FRAGMENTS / LIFECYCLE

### 5.1 HomeFragment — refreshStats() uses requireContext() inside coroutine

**What is wrong:**  
In `refreshStats()` you do `lifecycleScope.launch { withContext(Dispatchers.IO) { FocusRepository.forceUpdateUsageNow(requireContext().applicationContext) } ... }`. If the fragment is destroyed while the IO block is running, `requireContext()` will throw when the block runs.

**Why it is a problem:**  
Crash when the user leaves the screen during pull-to-refresh (e.g. back press or rotation).

**Severity:** High

**How to fix:**  
Capture context before launching and use it in IO, and guard post-IO UI updates:

```kotlin
private fun refreshStats() {
    val ctx = context?.applicationContext ?: return
    lifecycleScope.launch {
        withContext(Dispatchers.IO) {
            FocusRepository.forceUpdateUsageNow(ctx)
        }
        if (!isAdded) return@launch
        syncFocusGuardUI()
        binding.swipeRefresh.isRefreshing = false
    }
}
```

**Better alternative:** Use `viewLifecycleOwner.lifecycleScope` and cancel the job when the view is destroyed; use `context?.applicationContext` and skip UI updates if the fragment is not added.

---

### 5.2 HomeFragment — syncFocusGuardUI and binding after destroy

**What is wrong:**  
`syncFocusGuardUI()` uses `binding` (which is `_binding!!`). If called after `onDestroyView()`, `_binding` is null and the getter throws.

**Why it is a problem:**  
If any callback (e.g. from a bottom sheet `onDismissed`) calls `syncFocusGuardUI()` after the fragment’s view has been destroyed, the app will crash.

**Severity:** High

**How to fix:**  
At the start of `syncFocusGuardUI()`:

```kotlin
val card = _binding?.focusGuardCardInclude ?: return
```

Use safe calls for all binding access in that method, or early-return if `_binding == null`.

**Better alternative:** Ensure all callbacks that can run after destroy (e.g. sheet `onDismissed`) check `isAdded` or use a lifecycle-aware observer so that `syncFocusGuardUI` is never invoked when the view is gone.

---

### 5.3 HomeFragment — ValueAnimator not cancelled in onDestroyView

**What is wrong:**  
`animateProgress()` starts a `ValueAnimator` and never stores a reference or cancels it in `onDestroyView()`.

**Why it is a problem:**  
The animator holds a reference to the update listener, which references `_binding?.todayMinutesValue`. If the view is destroyed, the listener can run and access a detached view or null binding, or leak the fragment.

**Severity:** Medium

**How to fix:**  
Keep a reference to the animator and cancel it in `onDestroyView()`:

```kotlin
private var progressAnimator: ValueAnimator? = null
private fun animateProgress(current: Int, goal: Int) {
    progressAnimator?.cancel()
    // ...
    progressAnimator = textAnimator
    textAnimator.start()
}
override fun onDestroyView() {
    progressAnimator?.cancel()
    progressAnimator = null
    // ...
}
```

---

### 5.4 HomeFragment — runEntryAnimation() and prepareEntryAnimation() on every onViewCreated

**What is wrong:**  
Entry animations run every time the view is created (e.g. every time the user navigates back to Home). No check for “already ran once” or saved state.

**Why it is a problem:**  
Repetitive animation on every return to Home can feel unnecessary and slightly expensive.

**Severity:** Low

**How to fix:**  
Run the entry animation only when `savedInstanceState == null` (first time in this lifecycle), or use a shared ViewModel flag “hasAnimated” so it runs only once per process.

---

### 5.5 FocusConfigBottomSheet — Test nudge broadcast when service not running

**What is wrong:**  
“Test Notification” sends a broadcast `ACTION_TEST_NUDGE`. The receiver is registered only inside `FocusMonitorService.onCreate()`. If the service is not running (e.g. Focus Guard off), nothing receives the broadcast and the user sees no nudge.

**Why it is a problem:**  
Confusing UX: user taps “Test Notification”, sees “Firing test notification…”, but no notification appears and there is no explanation.

**Severity:** Medium (UX)

**How to fix:**  
Before sending the broadcast, either start the service (if Focus Guard is enabled) or show a message: “Turn on Focus Guard first to test the nudge,” or start the service temporarily for the test (start service, send broadcast with a short delay, then stop if guard was off). Prefer in-app message if guard is off.

---

### 5.6 FocusConfigBottomSheet — onDismissed called in onDestroyView

**What is wrong:**  
`onDestroyView()` calls `onDismissed?.invoke()` then `super.onDestroyView()`. The callback is invoked when the sheet is being destroyed (e.g. after dismiss or configuration change).

**Why it is a problem:**  
The callback (e.g. `syncFocusGuardUI()` in HomeFragment) may run when the host fragment’s view is already destroyed if the destruction order is unfavorable, leading to the crash in 5.2.

**Severity:** High (when combined with 5.2)

**How to fix:**  
Invoke `onDismissed` when the sheet is actually dismissed by the user (e.g. in `onDismiss` or when the positive button is clicked), not in `onDestroyView`. If you need to refresh when the sheet is gone, use `dialog?.setOnDismissListener { onDismissed?.invoke() }` and in the listener ensure the host is still added before calling `syncFocusGuardUI()`.

---

## 6. DATA LAYER & ROOM

### 6.1 AppDatabase (history) — fallbackToDestructiveMigration()

**What is wrong:**  
`Room.databaseBuilder(...).fallbackToDestructiveMigration().build()` is used. No migrations are defined.

**Why it is a problem:**  
On any schema change (new column, new table, etc.), the database is recreated and all local session history is lost. Users lose data after an update.

**Severity:** High

**How to fix:**  
Add proper migrations and remove `fallbackToDestructiveMigration()` for production:

```kotlin
.addMigrations(MIGRATION_1_2)  // example
.build()
```

**Better alternative:** Use `autoMigrations` (Room 2.4+) for additive changes where possible, and always provide a migration for any breaking schema change.

---

### 6.2 SessionEntity — default updatedAtMillis = timestamp

**What is wrong:**  
`updatedAtMillis: Long = timestamp` uses the constructor parameter `timestamp` as default. In Kotlin this is valid, but if someone creates the entity with default values only, `timestamp` might be 0 and then `updatedAtMillis` is 0.

**Why it is a problem:**  
Minor: dirty/sync logic might behave oddly if entities are ever created with default timestamp. Most usages pass explicit values.

**Severity:** Low

**How to fix:**  
Ensure all creation sites pass explicit `timestamp` and, if needed, `updatedAtMillis`. Or use `System.currentTimeMillis()` in a factory method instead of in the data class default.

---

### 6.3 SessionDao — getDirty and markSynced used but sync path unclear

**What is wrong:**  
DAO has `getDirty` and `markSynced` for sync. It’s unclear if any worker or repository actually calls them and performs sync; if not, `isDirty` and related columns are dead code.

**Why it is a problem:**  
Dead code and possible confusion; if sync is added later, the schema might already have changed.

**Severity:** Low

**How to fix:**  
Either implement sync (e.g. SessionSyncWorker) that uses getDirty/markSynced, or remove these methods and columns and add them when sync is implemented.

---

## 7. FIREBASE & AUTH

### 7.1 AuthViewModel — user.reload() and race with state

**What is wrong:**  
After sign-in success, you call `user.reload().addOnCompleteListener { if (user.isEmailVerified) ... }`. The `user` reference is the same as the one returned by the sign-in result. If the user object is updated asynchronously, `user.isEmailVerified` in the listener might still be the old value on some devices.

**Why it is a problem:**  
Rarely, a just-verified user might still be treated as unverified, forcing an extra reload or confusing state.

**Severity:** Low

**How to fix:**  
In the listener, use the result of the reload: `task.result?.user?.isEmailVerified` (or the current FirebaseAuth.getInstance().currentUser) instead of the captured `user` reference.

---

### 7.2 Firestore rules — catalog read with only signedIn()

**What is wrong:**  
`match /catalog/static/activities/{activityId} { allow read: if signedIn(); allow write: if false; }` allows any signed-in user to read all catalog activities. There is no check that the user is “allowed” to see certain content (e.g. by region or feature flags).

**Why it is a problem:**  
If you later add paid or restricted activities in the same collection, the current rules would allow read for all. Not a current bug if the catalog is fully public for all users.

**Severity:** Low

**How to fix:**  
When you add restricted content, scope the rule (e.g. allow read only if `get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role in ['subscriber', 'admin']` or similar). For now, document that the catalog is intentionally public to all signed-in users.

---

## 8. NOTIFICATIONS & RECEIVERS

### 8.1 ReminderReceiver — REQUEST_CODE 9001 and FocusMonitorService NOTIFICATION_ID 9001

**What is wrong:**  
ReminderReceiver uses `REQUEST_CODE = 9001` for its PendingIntent; FocusMonitorService uses `NOTIFICATION_ID = 9001` for the foreground notification. They are different namespaces (PendingIntent request code vs notification id), so they don’t collide, but the same number in two components can cause confusion or future collision if someone reuses the constant.

**Why it is a problem:**  
Low risk of actual bug; mainly maintainability.

**Severity:** Low

**How to fix:**  
Use distinct namespaces: e.g. reminder notification id 10001 (already used in notify(10001, ...)), and reminder PendingIntent request code 8001; keep focus monitor IDs in the 900x range and document.

---

### 8.2 ReminderReceiver — re-scheduling alarm every time the receiver runs

**What is wrong:**  
Every time the reminder fires, the receiver schedules the next day’s alarm with `alarmManager.set(RTC_WAKEUP, cal.timeInMillis, pi)`. If the device was in Doze or the alarm was delayed, you might schedule multiple “next day” alarms or miss a day.

**Why it is a problem:**  
Duplicate or missed reminders; battery impact if many alarms are set.

**Severity:** Medium

**How to fix:**  
Use `setExactAndAllowWhileIdle` or `setAlarmClock` for user-facing reminders (within policy), and cancel any previous PendingIntent for this reminder before setting a new one. Use a single PendingIntent (same request code and intent) so that only one next alarm is active.

**Better alternative:** Use WorkManager with a periodic work or a single delayed work that reschedules itself, so that scheduling is centralized and survives reboots.

---

### 8.3 ReminderReceiver — hardcoded channel name "Daily Reminders"

**What is wrong:**  
The channel name is a literal string, not from `strings.xml`.

**Why it is a problem:**  
Not localizable; inconsistent with the rest of the app.

**Severity:** Low

**How to fix:**  
Use `context.getString(R.string.channel_daily_reminders)` and add the string resource.

---

## 9. PROGUARD & BUILD

### 9.1 ProGuard — engine and Focus classes not kept

**What is wrong:**  
ProGuard keeps `com.example.ourmajor.data.**` and `com.example.ourmajor.ui.**` but there is no explicit keep rule for `com.example.ourmajor.engine.**`. FocusMonitorService, RestartReceiver, FocusRepository, etc. are in `engine` and might be obfuscated or stripped in release.

**Why it is a problem:**  
Services and receivers must be keep by name so the system can find them. If the class or its parameterless constructor is renamed or removed, the app can crash on start or when the system tries to start the service/receiver.

**Severity:** Critical

**How to fix:**  
Add to proguard-rules.pro:

```proguard
-keep class com.example.ourmajor.engine.FocusMonitorService { *; }
-keep class com.example.ourmajor.engine.RestartReceiver { *; }
-keep class com.example.ourmajor.engine.** { *; }
```

Or at minimum keep the service and receiver classes and their no-arg constructors.

---

### 9.2 build.gradle.kts — keystore properties not closed

**What is wrong:**  
`keystoreProperties.load(FileInputStream(keystorePropertiesFile))` is called but the `FileInputStream` is never closed. In Gradle this is usually short-lived, but it’s a resource leak.

**Why it is a problem:**  
Minor resource leak during config; on repeated builds it’s usually negligible.

**Severity:** Low

**How to fix:**  
Use `keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }`.

---

### 9.3 compileSdk 36 and targetSdk 36

**What is wrong:**  
Using SDK 36 (likely a preview or very new) can lead to compatibility issues with older devices and libraries that haven’t been updated.

**Why it is a problem:**  
Some devices or OEM builds might behave differently; 36 might not be the default for all developers.

**Severity:** Low

**How to fix:**  
Align with the current stable SDK (e.g. 35 or 34) unless you explicitly need 36. Document the choice.

---

## 10. ACCESSIBILITY

### 10.1 Layouts — contentDescription="@null" on meaningful images

**What is wrong:**  
Many ImageViews (e.g. icons for Instagram, YouTube, header image, decorative icons) use `android:contentDescription="@null"`. That marks them as unimportant for accessibility, which is correct only for purely decorative images.

**Why it is a problem:**  
If the image conveys meaning (e.g. “Instagram”, “YouTube”, “Focus Guard”), screen reader users get no announcement. If it’s decorative, @null is correct.

**Severity:** Medium (accessibility)

**How to fix:**  
For meaningful images, set a short, localized content description (e.g. “Instagram”, “YouTube”, “Focus Guard”). For decorative images, keep `contentDescription="@null"` or use `tools:ignore="ContentDescription"` and ensure the surrounding text gives the same information.

Example for layout_focus_card.xml:
```xml
android:contentDescription="@string/instagram_label"
android:contentDescription="@string/youtube_label"
```

---

### 10.2 Focus Guard pill and card — no contentDescription or state announcement

**What is wrong:**  
The Focus Guard status pill (ON/OFF) and the card are interactive but may not announce their state (e.g. “Focus Guard, ON, double-tap to pause” or “Focus Guard, OFF, double-tap to activate”).

**Why it is a problem:**  
Screen reader users may not know the current state or what the control does.

**Severity:** Medium (accessibility)

**How to fix:**  
Set `contentDescription` on the pill that includes the state and action, e.g. from code: `pill.contentDescription = getString(R.string.focus_guard_pill_content_description, pill.text, ...)`. Or use `android:contentDescription` with a template string. For the card container, ensure the card has a description that includes “Focus Guard” and “limit” when relevant.

---

### 10.3 SwipeRefreshLayout — no accessibility label

**What is wrong:**  
SwipeRefreshLayout might not announce “Pull to refresh” or “Refreshing” for TalkBack.

**Why it is a problem:**  
Users may not discover the gesture or understand that a refresh is in progress.

**Severity:** Low

**How to fix:**  
Set `android:contentDescription` on the SwipeRefreshLayout (e.g. “Pull down to refresh usage”) or use `setAccessibilityDelegate` to announce “Refreshing” when `isRefreshing` is true.

---

## 11. SECURITY & LEAKS

### 11.1 MainActivity — crash trap leaks exception details

Already covered in 2.3 (crash trap). Same severity and fix.

---

### 11.2 AuthActivity — potential leak of AuthViewModel and repository

**What is wrong:**  
AuthActivity holds AuthViewModel and AuthRepository. If the repository holds a reference to FirebaseAuth or listeners, and the activity is not destroyed properly (e.g. leaked), those can live longer than needed.

**Why it is a problem:**  
Usually the activity is destroyed on navigate away; in edge cases (e.g. not finishing) you could retain the activity and its dependencies.

**Severity:** Low

**How to fix:**  
Ensure AuthActivity always finishes when navigating to MainActivity and that no static or long-lived reference holds the activity. ViewModel is scoped to the activity, so it’s fine; just avoid static references to the activity or repository.

---

## 12. OEM & VERSION COMPATIBILITY

### 12.1 SCREEN_ON and USER_PRESENT on Android 8+

**What is wrong:**  
From Android 8.0, many implicit broadcasts (e.g. SCREEN_ON, USER_PRESENT) are restricted. Manifest-declared receivers may not receive them on all devices or OS versions.

**Why it is a problem:**  
RestartReceiver might not get SCREEN_ON/USER_PRESENT on some OEMs or Android versions, so the “watchdog” behavior would be unreliable.

**Severity:** Medium

**How to fix:**  
Don’t rely solely on these broadcasts for critical “restart service” logic. Prefer the explicit RESTART_FOCUS_MONITOR from the service’s onDestroy, and use WorkManager or “start when app is opened” as a fallback. Document that Focus Guard may need to be re-enabled by the user after a long sleep on some devices.

---

### 12.2 Foreground service type dataSync

**What is wrong:**  
FocusMonitorService uses `foregroundServiceType="dataSync"`. Google’s guidance says dataSync is for syncing data from the server to the device. Using it for “usage monitoring” could be questioned in a policy review.

**Why it is a problem:**  
If the store interprets “dataSync” strictly, they might require a different type (e.g. “specialUse” with a declared reason) or reject the app.

**Severity:** Medium (policy)

**How to fix:**  
Check current Play policy for foreground service types. If “usage monitoring for digital wellbeing” is better expressed as `specialUse` with a user-visible justification, switch back to `specialUse` and the corresponding permission, and declare the reason in the manifest property.

---

## 13. SILENT FAILURES & EDGE CASES

### 13.1 FocusRepository.getDailyLimitMs() when prefs is null

**What is wrong:**  
If `init(context)` is never called (e.g. in a test or a rare code path), `prefs` is null and `getDailyLimitMs()` returns `DEFAULT_DAILY_LIMIT_MS`. That’s safe but silent.

**Why it is a problem:**  
In production, init is called from multiple places, so prefs is usually set. If a new code path calls getDailyLimitMs() before any init, you get the default without any log, which can make “wrong limit” bugs hard to trace.

**Severity:** Low

**How to fix:**  
Call `FocusRepository.init(applicationContext)` in Application.onCreate() so prefs is always set. Optionally in getDailyLimitMs(), if prefs == null, log a warning and return default.

---

### 13.2 PermissionManager — activity can be null in theory

**What is wrong:**  
checkAndRequestPermissions(activity: Activity) is called with `this` from MainActivity. If ever called with a null or finishing activity, startActivity could throw or behave oddly.

**Why it is a problem:**  
Defensive coding: public API should validate inputs.

**Severity:** Low

**How to fix:**  
At the start of checkAndRequestPermissions: `if (activity.isFinishing || activity.isDestroyed) return false`. That also avoids showing dialogs on a dead activity.

---

### 13.3 HomeViewModel — ProfileRepository() created inside ViewModel

**What is wrong:**  
HomeViewModel constructor has `profileRepo: ProfileRepository = ProfileRepository()`. So each HomeViewModel creates its own ProfileRepository (or uses the one passed by the factory). SessionHistoryRepository and GamificationManager are passed from the factory; ProfileRepository is default-constructed.

**Why it is a problem:**  
If ProfileRepository is a singleton or holds global state, this is fine. If it’s not, you might have multiple instances and inconsistent state. Depends on ProfileRepository implementation.

**Severity:** Low

**How to fix:**  
Inject ProfileRepository via the ViewModel factory (like SessionHistoryRepository and GamificationManager) so lifecycle and scope are explicit and testable.

---

## 14. SUMMARY TABLE

| #   | Area              | Issue                                             | Severity  |
|-----|-------------------|---------------------------------------------------|-----------|
| 1.1 | ZenSwitchApp      | super.onCreate() and prefs order                  | Medium    |
| 1.2 | ZenSwitchApp      | Firestore settings every start                    | Low       |
| 2.1 | MainActivity      | Permission check every onResume                   | Medium    |
| 2.2 | MainActivity      | observeAuthState + finish()                       | Low–Medium|
| 2.3 | MainActivity      | Crash trap exposes stack trace                    | High     |
| 3.1 | FocusMonitorService | serviceScope never cancelled                    | Medium    |
| 3.2 | FocusMonitorService | startForegroundService can throw (O+)           | High     |
| 3.4 | FocusMonitorService | PendingIntent request code truncation            | Low       |
| 3.5 | FocusMonitorService | Nudge channel recreated every time              | Low       |
| 3.6 | FocusRepository   | init() not thread-safe                            | Medium    |
| 3.7 | FocusRepository   | Duplicate logic getRealTimeUsage/forceUpdate      | Low       |
| 3.8 | RestartReceiver   | exported + no validation                          | Medium    |
| 3.9 | UsageCalculator   | Large event window / no timeout                    | Medium    |
| 4.1 | PermissionManager | setCancelable(false) for all                      | Medium    |
| 4.2 | PermissionManager | Activity finishing when starting Settings         | Low       |
| 4.3 | Manifest          | Battery optimizations permission policy           | High     |
| 4.4 | Manifest          | SYSTEM_ALERT_WINDOW policy                        | Medium    |
| 5.1 | HomeFragment      | requireContext() in coroutine                     | High     |
| 5.2 | HomeFragment      | syncFocusGuardUI after destroy                    | High     |
| 5.3 | HomeFragment      | ValueAnimator not cancelled                       | Medium    |
| 5.4 | HomeFragment      | Entry animation every time                        | Low       |
| 5.5 | FocusConfigBottomSheet | Test nudge when service off                  | Medium    |
| 5.6 | FocusConfigBottomSheet | onDismissed in onDestroyView                   | High     |
| 6.1 | AppDatabase       | fallbackToDestructiveMigration                    | High     |
| 8.2 | ReminderReceiver | Alarm re-scheduling every run                      | Medium    |
| 8.3 | ReminderReceiver | Hardcoded channel name                            | Low       |
| 9.1 | ProGuard          | engine package not kept                           | Critical  |
| 9.2 | Gradle            | FileInputStream not closed                        | Low       |
| 10.1| Accessibility     | contentDescription null on meaningful images       | Medium    |
| 10.2| Accessibility     | Focus Guard state not announced                   | Medium    |
| 12.1| OEM               | SCREEN_ON/USER_PRESENT restrictions               | Medium    |
| 12.2| Manifest          | dataSync foreground type policy                   | Medium    |

---

### 15. ADDITIONAL ITEMS

### 15.1 HomeFragment — refreshStats() sets isRefreshing = false after IO without isAdded check

**What is wrong:**  
After `withContext(Dispatchers.IO)` you call `syncFocusGuardUI()` and `binding.swipeRefresh.isRefreshing = false`. If the fragment was destroyed during the IO block, `binding` will be null and accessing it throws.

**Why it is a problem:**  
Same as 5.1: crash when user navigates away during refresh.

**Severity:** High (same fix as 5.1)

**How to fix:**  
After withContext, check `if (!isAdded) return@launch` before any binding access. Use safe call: `_binding?.swipeRefresh?.isRefreshing = false`.

---

### 15.2 ReminderReceiver — PendingIntent for alarm uses same Intent instance

**What is wrong:**  
`PendingIntent.getBroadcast(context, REQUEST_CODE, Intent(context, ReminderReceiver::class.java), ...)` is called with a new Intent each time. For alarm rescheduling this is correct. The REQUEST_CODE (9001) is the same for every reminder PendingIntent, so updating the alarm replaces the previous PendingIntent. That’s intended, but if another part of the app used request code 9001 for a different PendingIntent (e.g. activity), the system could treat them as the same in some cases (same request code + same target). ReminderReceiver uses 9001 only for its own alarm; FocusMonitorService uses 9001 as notification id. So no direct collision.

**Why it is a problem:**  
Document so future code doesn’t reuse 9001 for a conflicting PendingIntent.

**Severity:** Low

**How to fix:**  
Use a dedicated constant namespace for reminder (e.g. REMINDER_REQUEST_CODE = 7001) and document in code that 900x is for Focus Monitor.

---

### 15.3 SessionEntity — updatedAtMillis default uses `timestamp`

**What is wrong:**  
In SessionEntity, `updatedAtMillis: Long = timestamp` references the constructor parameter. In Kotlin data classes, the order of parameters matters: `timestamp` is the 5th parameter. So when you create `SessionEntity(id, name, category, durationSeconds, timestamp, pointsEarned)`, the default for `updatedAtMillis` is that `timestamp` value. But if you use named parameters and omit `updatedAtMillis`, you get `updatedAtMillis = timestamp`. However, `updatedAtMillis` is the 8th parameter and `timestamp` is the 5th; in the default expression, `timestamp` refers to the constructor parameter. So when the entity is created with an explicit timestamp, updatedAtMillis is correctly that timestamp. When the entity is created for “now”, the caller typically passes System.currentTimeMillis() as timestamp, so it’s fine. The only subtle issue is that “updatedAtMillis” semantically should be “last updated”, but here it’s initialized to “session timestamp”. For a new insert that’s acceptable (creation time = update time).

**Why it is a problem:**  
Minimal; just clarifying semantics.

**Severity:** Low

**How to fix:**  
Document that for new sessions, updatedAtMillis is set to session timestamp. If you later add “edit session” support, set updatedAtMillis to System.currentTimeMillis() when updating.

---

### 15.4 FocusMonitorService — startActivity(intent) from service

**What is wrong:**  
When overlay is granted, you call `startActivity(intent)`. The intent has FLAG_ACTIVITY_NEW_TASK. Starting an activity from a service without FLAG_ACTIVITY_NEW_TASK would throw; you have the flag, so this is correct. On Android 10+, background activity starts are restricted; starting an activity from a foreground service is generally allowed. So the code is correct.

**Why it is a problem:**  
None for current code. Only a reminder: if the nudge fires while the app is in background and the user is in another app, some OEMs might still not bring your activity to the foreground. Overlay permission is what mitigates that.

**Severity:** N/A (informational)

---

### 15.5 ProGuard — engine keep rule

**What is wrong:**  
Without `-keep` for engine, in release build the class names might be obfuscated. Android starts services and receivers by class name from the manifest. If the class is renamed, the system would not find it.

**Why it is a problem:**  
Critical: release build can crash on service/receiver start.

**Severity:** Critical (duplicate of 9.1; ensure fix is applied)

---

**End of audit.** Address Critical and High items first, then Medium, then Low. Re-run static analysis and manual testing after changes.
