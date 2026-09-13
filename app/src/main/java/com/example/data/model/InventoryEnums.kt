package com.example.data.model

enum class InventoryLocation(val displayName: String) {
    CENTRAL("Central"),
    MASTER("Master"),
    EXECUTIVE("Executive"),
    ASSIGNED("Assigned")
}

enum class ProofType(val displayName: String) {
    EXISTING_STOCK("Existing Stock Baseline"),
    CENTRAL_RECEIVED("Central Received"),
    COURIER_TO_EXECUTIVE("Courier to Executive"),
    TAG_ASSIGNED("Tag Assigned")
}
