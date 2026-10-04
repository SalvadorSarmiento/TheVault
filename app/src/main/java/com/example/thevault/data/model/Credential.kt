package com.example.thevault.data.model

enum class CredentialCategory(val label: String) {
    PERSONAL("Personal"),
    WORK("Trabajo"),
    STUDIES("Estudios"),
    SHOPPING("Compras")
}

data class Credential(
    val id: Long,
    val name: String,
    val username: String,
    val password: String,
    val website: String = "",
    val notes: String = "",
    val category: CredentialCategory = CredentialCategory.PERSONAL,
    val isFavorite: Boolean = false
)
