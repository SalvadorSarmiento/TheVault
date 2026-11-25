package com.example.thevault.data.transfer

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_import_receipts")
data class VaultImportReceipt(@PrimaryKey val id: String)
