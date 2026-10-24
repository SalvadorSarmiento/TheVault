package com.example.thevault.data.remote

data class EncryptedPayloadDto(
    val algorithm: String,
    val kdf: String,
    val iterations: Int,
    val saltB64: String,
    val nonceB64: String,
    val ciphertextB64: String
)