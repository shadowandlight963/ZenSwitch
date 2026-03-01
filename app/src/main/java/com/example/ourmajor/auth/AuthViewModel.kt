package com.example.ourmajor.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private var pendingUnverifiedUser: FirebaseUser? = null

    private val unverifiedLoginMessage = "Email not verified. Please check your inbox."

    private val _uiStateLiveData = MutableLiveData(AuthUiState())

    val uiStateLiveData: LiveData<AuthUiState> = _uiStateLiveData

    init {
        val user = repository.currentUser()
        if (user == null) {
            _uiStateLiveData.value = AuthUiState(isAuthenticated = false)
        } else if (user.isEmailVerified) {
            _uiStateLiveData.value = AuthUiState(
                isAuthenticated = true,
                email = user.email,
                displayName = user.displayName
            )
        } else {
            pendingUnverifiedUser = user
            repository.signOut()
            _uiStateLiveData.value = AuthUiState(
                isAuthenticated = false,
                email = user.email,
                needsEmailVerification = true,
                errorMessage = unverifiedLoginMessage
            )
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiStateLiveData.value = _uiStateLiveData.value?.copy(errorMessage = "Email and password are required")
                ?: AuthUiState(errorMessage = "Email and password are required")
            return
        }

        _uiStateLiveData.value = _uiStateLiveData.value?.copy(isLoading = true, errorMessage = null)
            ?: AuthUiState(isLoading = true)

        repository.signIn(email.trim(), password) { result ->
            result
                .onSuccess { user ->
                    user.reload()
                        .addOnCompleteListener {
                            if (user.isEmailVerified) {
                                pendingUnverifiedUser = null
                                updateUser(user)
                            } else {
                                pendingUnverifiedUser = user
                                repository.signOut()
                                _uiStateLiveData.postValue(
                                    (_uiStateLiveData.value ?: AuthUiState()).copy(
                                        isLoading = false,
                                        isAuthenticated = false,
                                        email = user.email,
                                        needsEmailVerification = true,
                                        errorMessage = unverifiedLoginMessage
                                    )
                                )
                            }
                        }
                        .addOnFailureListener { e ->
                            repository.signOut()
                            _uiStateLiveData.postValue(
                                (_uiStateLiveData.value ?: AuthUiState()).copy(
                                    isLoading = false,
                                    isAuthenticated = false,
                                    errorMessage = e.message ?: "Login failed"
                                )
                            )
                        }
                }
                .onFailure { e ->
                    _uiStateLiveData.postValue(
                        (_uiStateLiveData.value ?: AuthUiState()).copy(
                            isLoading = false,
                            isAuthenticated = false,
                            errorMessage = e.message ?: "Login failed"
                        )
                    )
                }
        }
    }

    fun signUp(email: String, password: String) {
        signUp(email = email, password = password, displayName = "")
    }

    fun signUp(email: String, password: String, displayName: String = "") {
        if (email.isBlank() || password.isBlank()) {
            _uiStateLiveData.value = _uiStateLiveData.value?.copy(errorMessage = "Email and password are required")
                ?: AuthUiState(errorMessage = "Email and password are required")
            return
        }

        if (password.length < 6) {
            _uiStateLiveData.value = _uiStateLiveData.value?.copy(errorMessage = "Password must be at least 6 characters")
                ?: AuthUiState(errorMessage = "Password must be at least 6 characters")
            return
        }

        _uiStateLiveData.value = _uiStateLiveData.value?.copy(isLoading = true, errorMessage = null)
            ?: AuthUiState(isLoading = true)

        repository.signUp(email.trim(), password, displayName.trim()) { result ->
            result
                .onSuccess { user ->
                    user.sendEmailVerification()
                        .addOnSuccessListener {
                            repository.signOut()
                            pendingUnverifiedUser = user
                            _uiStateLiveData.postValue(
                                (_uiStateLiveData.value ?: AuthUiState()).copy(
                                    isLoading = false,
                                    isAuthenticated = false,
                                    email = user.email,
                                    verificationEmailSentTo = user.email,
                                    needsEmailVerification = false,
                                    errorMessage = null
                                )
                            )
                        }
                        .addOnFailureListener { e ->
                            repository.signOut()
                            pendingUnverifiedUser = user
                            _uiStateLiveData.postValue(
                                (_uiStateLiveData.value ?: AuthUiState()).copy(
                                    isLoading = false,
                                    isAuthenticated = false,
                                    email = user.email,
                                    needsEmailVerification = true,
                                    errorMessage = e.message ?: "Failed to send verification email"
                                )
                            )
                        }
                }
                .onFailure { e ->
                    _uiStateLiveData.postValue(
                        (_uiStateLiveData.value ?: AuthUiState()).copy(
                            isLoading = false,
                            isAuthenticated = false,
                            errorMessage = e.message ?: "Sign up failed"
                        )
                    )
                }
        }
    }

    fun resendVerificationEmail() {
        val user = pendingUnverifiedUser ?: repository.currentUser()
        if (user == null) {
            _uiStateLiveData.value = (_uiStateLiveData.value ?: AuthUiState()).copy(
                errorMessage = "No user session available to resend verification"
            )
            return
        }

        _uiStateLiveData.value = (_uiStateLiveData.value ?: AuthUiState()).copy(isLoading = true, errorMessage = null)

        user.sendEmailVerification()
            .addOnSuccessListener {
                repository.signOut()
                pendingUnverifiedUser = user
                _uiStateLiveData.postValue(
                    (_uiStateLiveData.value ?: AuthUiState()).copy(
                        isLoading = false,
                        isAuthenticated = false,
                        email = user.email,
                        verificationEmailSentTo = user.email,
                        needsEmailVerification = false,
                        errorMessage = null
                    )
                )
            }
            .addOnFailureListener { e ->
                repository.signOut()
                pendingUnverifiedUser = user
                _uiStateLiveData.postValue(
                    (_uiStateLiveData.value ?: AuthUiState()).copy(
                        isLoading = false,
                        isAuthenticated = false,
                        email = user.email,
                        needsEmailVerification = true,
                        errorMessage = e.message ?: "Failed to resend verification email"
                    )
                )
            }
    }

    fun signOut() {
        repository.signOut()
        pendingUnverifiedUser = null
        _uiStateLiveData.value = AuthUiState()
    }

    fun clearError() {
        _uiStateLiveData.value = _uiStateLiveData.value?.copy(errorMessage = null)
    }

    fun clearVerificationEmailSent() {
        _uiStateLiveData.value = _uiStateLiveData.value?.copy(verificationEmailSentTo = null)
    }

    fun clearNeedsEmailVerification() {
        _uiStateLiveData.value = _uiStateLiveData.value?.copy(needsEmailVerification = false)
    }

    fun sendPasswordReset(email: String, onResult: (Result<Unit>) -> Unit) {
        if (email.isBlank()) {
            _uiStateLiveData.value = _uiStateLiveData.value?.copy(errorMessage = "Enter your email to reset password")
                ?: AuthUiState(errorMessage = "Enter your email to reset password")
            onResult(Result.failure(IllegalArgumentException("Email is required")))
            return
        }

        _uiStateLiveData.value = _uiStateLiveData.value?.copy(isLoading = true, errorMessage = null)
            ?: AuthUiState(isLoading = true)

        repository.sendPasswordReset(email.trim()) { result ->
            result
                .onSuccess {
                    _uiStateLiveData.postValue(
                        (_uiStateLiveData.value ?: AuthUiState()).copy(
                            isLoading = false,
                            errorMessage = null
                        )
                    )
                    onResult(Result.success(Unit))
                }
                .onFailure { e ->
                    _uiStateLiveData.postValue(
                        (_uiStateLiveData.value ?: AuthUiState()).copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Password reset failed"
                        )
                    )
                    onResult(Result.failure(e))
                }
        }
    }

    private fun updateUser(user: FirebaseUser) {
        _uiStateLiveData.postValue(
            AuthUiState(
            isAuthenticated = true,
            email = user.email,
            displayName = user.displayName,
            isLoading = false
            )
        )
    }
}
