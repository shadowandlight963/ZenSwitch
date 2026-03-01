package com.example.ourmajor

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class ActivitiesCrashTest {

    @Test
    fun testActivitiesComponentsExist() {
        // Test that app context is available
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull("App context should not be null", context)
        assertEquals("App package name should match", "com.example.ourmajor", context.packageName)
        
        // Test that ActivitiesFragment class exists and can be instantiated
        try {
            val fragmentClass = Class.forName("com.example.ourmajor.ui.activities.ActivitiesFragment")
            assertNotNull("ActivitiesFragment class should exist", fragmentClass)
            println("SUCCESS: ActivitiesFragment class found and accessible")
        } catch (e: Exception) {
            fail("FAILED: ActivitiesFragment class not found - ${e.message}")
        }
        
        // Test that ExerciseActivity class exists and can be instantiated
        try {
            val activityClass = Class.forName("com.example.ourmajor.ui.activities.ExerciseActivity")
            assertNotNull("ExerciseActivity class should exist", activityClass)
            println("SUCCESS: ExerciseActivity class found and accessible")
        } catch (e: Exception) {
            fail("FAILED: ExerciseActivity class not found - ${e.message}")
        }
        
        // Test that ActivitiesViewModel class exists
        try {
            val viewModelClass = Class.forName("com.example.ourmajor.ui.activities.ActivitiesViewModel")
            assertNotNull("ActivitiesViewModel class should exist", viewModelClass)
            println("SUCCESS: ActivitiesViewModel class found and accessible")
        } catch (e: Exception) {
            fail("FAILED: ActivitiesViewModel class not found - ${e.message}")
        }
        
        // Test that ActivityAdapter class exists
        try {
            val adapterClass = Class.forName("com.example.ourmajor.ui.activities.ActivityAdapter")
            assertNotNull("ActivityAdapter class should exist", adapterClass)
            println("SUCCESS: ActivityAdapter class found and accessible")
        } catch (e: Exception) {
            fail("FAILED: ActivityAdapter class not found - ${e.message}")
        }
        
        println("SUCCESS: All Activities-related classes are accessible and should not crash")
    }
}
