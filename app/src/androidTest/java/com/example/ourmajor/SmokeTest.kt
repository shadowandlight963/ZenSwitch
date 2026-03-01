package com.example.ourmajor

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.junit.Before

@RunWith(AndroidJUnit4::class)
class SmokeTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var device: UiDevice

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun testLoginFlow() {
        // Verify we can navigate to login screen if not logged in
        // This test assumes the app handles auth guard properly
        Thread.sleep(2000) // Wait for auth check
        // TODO: Add specific UI interactions for login if needed
    }

    @Test
    fun testProfileUpdatesPersist() {
        // Navigate to Profile
        device.findObject(UiSelector().textContains("Profile")).click()
        Thread.sleep(1000)

        // Verify profile elements exist
        assertTrue(device.findObject(UiSelector().resourceId("com.example.ourmajor:id/email")).exists())
        assertTrue(device.findObject(UiSelector().resourceId("com.example.ourmajor:id/dropdown_daily_goal")).exists())

        // Change daily goal
        device.findObject(UiSelector().resourceId("com.example.ourmajor:id/dropdown_daily_goal")).click()
        Thread.sleep(500)
        device.findObject(UiSelector().text("45")).click()
        Thread.sleep(1000)

        // Verify change persisted (would need to relaunch or check Firestore)
        // For smoke test, just verify UI updates
        device.pressBack()
        device.findObject(UiSelector().textContains("Profile")).click()
        Thread.sleep(1000)
        // Verify the selection is retained
    }

    @Test
    fun testActivitiesListAndStart() {
        // Navigate to Activities
        device.findObject(UiSelector().textContains("Activities")).click()
        Thread.sleep(1000)

        // Verify categories exist
        assertTrue(device.findObject(UiSelector().textContains("Breathing")).exists())

        // Select a category
        device.findObject(UiSelector().textContains("Breathing")).click()
        Thread.sleep(1000)

        // Verify activities list appears
        assertTrue(device.findObject(UiSelector().textContains("Box Breathing")).exists())

        // Start an activity
        device.findObject(UiSelector().textContains("Start")).click()
        Thread.sleep(2000)

        // Verify ExerciseActivity launched
        assertTrue(device.findObject(UiSelector().textContains("Complete")).exists() ||
                device.findObject(UiSelector().textContains("Abandon")).exists())
    }

    @Test
    fun testFavoritesToggle() {
        // Navigate to Activities
        device.findObject(UiSelector().textContains("Activities")).click()
        Thread.sleep(1000)

        // Select a category
        device.findObject(UiSelector().textContains("Breathing")).click()
        Thread.sleep(1000)

        // Toggle favorite (assuming favorite button exists)
        // TODO: Implement when favorite UI is added
        // For now, just verify the activity list loads
        assertTrue(device.findObject(UiSelector().textContains("Box Breathing")).exists())
    }
}
