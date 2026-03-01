package com.example.ourmajor

import com.example.ourmajor.common.Result
import com.example.ourmajor.data.profile.ProfileRepository
import com.example.ourmajor.data.profile.UserDocument
import com.example.ourmajor.data.profile.UserPreferences
import com.example.ourmajor.data.profile.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProfileRepositoryTest {

    @Mock
    private lateinit var mockAuth: FirebaseAuth

    @Mock
    private lateinit var mockFirestore: FirebaseFirestore

    private lateinit var repository: ProfileRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = ProfileRepository(mockAuth, mockFirestore)
    }

    @Test
    fun testEnsureUserDocument_NoUser_ReturnsFailure() = runBlocking {
        // Given
        `when`(mockAuth.currentUser).thenReturn(null)

        // When
        val result = repository.ensureUserDocument()

        // Then
        assertTrue(result is Result.Failure)
        assertEquals("User not authenticated", (result as Result.Failure).exception.message)
    }

    @Test
    fun testUpdateProfile_NoUser_ReturnsFailure() = runBlocking {
        // Given
        `when`(mockAuth.currentUser).thenReturn(null)
        val profile = UserProfile(displayName = "Test")

        // When
        val result = repository.updateProfile(profile)

        // Then
        assertTrue(result is Result.Failure)
        assertEquals("User not authenticated", (result as Result.Failure).exception.message)
    }

    @Test
    fun testUpdatePreferences_NoUser_ReturnsFailure() = runBlocking {
        // Given
        `when`(mockAuth.currentUser).thenReturn(null)
        val preferences = UserPreferences(dailyGoalMinutes = 45)

        // When
        val result = repository.updatePreferences(preferences)

        // Then
        assertTrue(result is Result.Failure)
        assertEquals("User not authenticated", (result as Result.Failure).exception.message)
    }
}
