package com.example.thevault.data.auth

import com.google.firebase.auth.FirebaseAuth

interface ApiTokenProvider {
    suspend fun idToken(): String
}

class DevelopmentTokenProvider : ApiTokenProvider {
    override suspend fun idToken(): String = "dev:demo"
}

class FirebaseIdTokenProvider(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : ApiTokenProvider {
    override suspend fun idToken(): String {
        val user = firebaseAuth.currentUser
            ?: error("No hay una sesión activa en Firebase")
        return user.getIdToken(false).awaitResult().token
            ?: error("Firebase no devolvió un ID token")
    }
}
