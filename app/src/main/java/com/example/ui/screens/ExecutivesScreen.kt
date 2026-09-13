package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ExecutiveStockSummary
import com.example.ui.theme.*

@Composable
fun ExecutivesScreen(
    executives: List<ExecutiveStockSummary>,
    onToggleActive: (String, Boolean) -> Unit,
    onAddExecutive: (String, String, String, (Boolean, String) -> Unit) -> Unit,
    onEditExecutiveDetails: (String, String, String, Boolean, (Boolean, String) -> Unit) -> Unit,
    onViewExecutiveDetails: (String) -> Unit,
    onClearInventoryData: ((Boolean, String) -> Unit) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var executiveToEdit by remember { mutableStateOf<ExecutiveStockSummary?>(null) }
    var showClearDataConfirm by remember { mutableStateOf(false) }
    var adminNoticeMessage by remember { mutableStateOf<String?>(null) }

    val filtered = remember(executives, searchQuery) {
        if (searchQuery.isBlank()) executives
        else executives.filter {
            it.executiveId.contains(searchQuery, ignoreCase = true) ||
            it.executiveName.contains(searchQuery, ignoreCase = true) ||
            it.region.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("executives_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SUBAGENTS & EXECUTIVES",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "ADMIN ONLY",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "${executives.size} registered executives (${executives.count { it.active }} active)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Fresh Database Clean Button
                OutlinedButton(
                    onClick = { showClearDataConfirm = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.testTag("btn_admin_fresh_reset")
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fresh DB", fontSize = 12.sp)
                }

                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn_add_executive")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add", fontSize = 13.sp)
                }
            }
        }

        if (adminNoticeMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = adminNoticeMessage ?: "",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { adminNoticeMessage = null }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Search Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by ID, name, or region...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_search_executives")
        )

        // Subagents List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered) { exec ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (exec.active) MaterialTheme.colorScheme.surface else Slate100
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_exec_${exec.executiveId}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (exec.active) BrandPurple.copy(alpha = 0.15f) else Slate200),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = exec.executiveId.takeLast(2),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (exec.active) BrandPurple else Slate600
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = exec.executiveId,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "• ${exec.executiveName}",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = exec.region,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        color = if (exec.currentStock > 0) LocationExecutiveColor.copy(alpha = 0.12f) else Slate100,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${exec.currentStock} tags in hand",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (exec.currentStock > 0) LocationExecutiveColor else Slate600,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (exec.active) "Active" else "Inactive",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (exec.active) BrandEmerald else Slate600
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Switch(
                                        checked = exec.active,
                                        onCheckedChange = { onToggleActive(exec.executiveId, exec.active) },
                                        modifier = Modifier.testTag("switch_active_${exec.executiveId}")
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Action Buttons: Edit Details & View Stock
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Admin Edit Details button
                            OutlinedButton(
                                onClick = { executiveToEdit = exec },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("btn_edit_exec_${exec.executiveId}")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Subagent Details", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            }

                            TextButton(
                                onClick = { onViewExecutiveDetails(exec.executiveId) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("View Inventory", fontSize = 11.5.sp)
                                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Executive Dialog
    if (showAddDialog) {
        AddExecutiveDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { id, name, region, cb ->
                onAddExecutive(id, name, region) { success, msg ->
                    cb(success, msg)
                    if (success) {
                        showAddDialog = false
                        adminNoticeMessage = msg
                    }
                }
            }
        )
    }

    // Edit Executive Dialog
    executiveToEdit?.let { exec ->
        EditExecutiveDialog(
            executive = exec,
            onDismiss = { executiveToEdit = null },
            onConfirm = { id, name, region, active, cb ->
                onEditExecutiveDetails(id, name, region, active) { success, msg ->
                    cb(success, msg)
                    if (success) {
                        executiveToEdit = null
                        adminNoticeMessage = "Subagent '$id' details updated successfully."
                    }
                }
            }
        )
    }

    // Fresh Database Confirmation Dialog
    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Reset to Fresh Inventory?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will wipe all existing tags, uploaded proofs, and movement ledgers to provide a 100% clean and fresh app. Registered subagent accounts will remain intact.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDataConfirm = false
                        onClearInventoryData { success, msg ->
                            adminNoticeMessage = msg
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_fresh_reset")
                ) {
                    Text("Clear All & Start Fresh")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EditExecutiveDialog(
    executive: ExecutiveStockSummary,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Boolean, (Boolean, String) -> Unit) -> Unit
) {
    var name by remember { mutableStateOf(executive.executiveName) }
    var region by remember { mutableStateOf(executive.region) }
    var active by remember { mutableStateOf(executive.active) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Edit Subagent Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Admin Governance Console",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = executive.executiveId,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subagent Full Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_exec_name"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = region,
                    onValueChange = { region = it },
                    label = { Text("Region / Assigned Territory") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_exec_region"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Active Status",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (active) "Can receive courier & upload tags" else "Suspended / Inactive",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = active,
                        onCheckedChange = { active = it },
                        modifier = Modifier.testTag("switch_edit_exec_active")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Subagent Name cannot be blank."
                                return@Button
                            }
                            isSaving = true
                            errorMessage = null
                            onConfirm(executive.executiveId, name, region, active) { success, msg ->
                                isSaving = false
                                if (!success) errorMessage = msg
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier.testTag("btn_save_edit_exec")
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

@Composable
fun AddExecutiveDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, (Boolean, String) -> Unit) -> Unit
) {
    var id by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Add New Executive / Subagent",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedTextField(
                    value = id,
                    onValueChange = { id = it },
                    label = { Text("Executive ID (e.g. EXEC-10)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_exec_id"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Executive Name") },
                    modifier = Modifier.fillMaxWidth().testTag("input_exec_name"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = region,
                    onValueChange = { region = it },
                    label = { Text("Region / Territory Hub") },
                    modifier = Modifier.fillMaxWidth().testTag("input_exec_region"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (id.isBlank() || name.isBlank()) {
                                errorMessage = "Executive ID and Name cannot be blank."
                                return@Button
                            }
                            isSaving = true
                            errorMessage = null
                            onConfirm(id, name, region) { success, msg ->
                                isSaving = false
                                if (!success) errorMessage = msg
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier.testTag("btn_save_exec")
                    ) {
                        Text("Save Executive")
                    }
                }
            }
        }
    }
}
