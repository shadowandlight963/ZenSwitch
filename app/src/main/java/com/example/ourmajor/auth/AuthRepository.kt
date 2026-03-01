package com.example.ourmajor.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository {

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    fun currentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun signIn(
        email: String,
        password: String,
        onResult: (Result<FirebaseUser>) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    onResult(Result.success(user))
                } else {
                    onResult(Result.failure(IllegalStateException("Authentication succeeded but user is null")))
                }
            }
            .addOnFailureListener { e ->
                onResult(Result.failure(e))
            }
    }

    fun sendPasswordReset(
        email: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                onResult(Result.failure(e))
            }
    }

    fun signUp(
        email: String,
        password: String,
        displayName: String,
        onResult: (Result<FirebaseUser>) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    onResult(Result.failure(IllegalStateException("Sign up succeeded but user is null")))
                    return@addOnSuccessListener
                }

                val name = displayName.trim()
                val profileTask = if (name.isNotBlank()) {
                    user.updateProfile(
                        UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                    )
                } else {
                    null
                }

                val writeDoc: () -> Unit = {
                    firestore.collection("users")
                        .document(user.uid)
                        .set(
                            mapOf(
                                "email" to user.email,
                                "uid" to user.uid,
                                "displayName" to (user.displayName ?: name)
                            )
                        )
                    onResult(Result.success(user))
                }

                if (profileTask != null) {
                    profileTask
                        .addOnSuccessListener { writeDoc() }
                        .addOnFailureListener { e -> onResult(Result.failure(e)) }
                } else {
                    writeDoc()
                }
            }
            .addOnFailureListener { e ->
                onResult(Result.failure(e))
            }
    }

    fun signOut() {
        auth.signOut()
    }
}
