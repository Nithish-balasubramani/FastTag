package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.AppDatabase
import com.example.data.model.ExecutiveClassCountRaw
import com.example.data.model.ExecutiveEntity
import com.example.data.model.InventoryTagEntity
import com.example.data.model.MovementWithProof
import com.example.ui.theme.*
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExecutiveDetailDialog(
    executiveId: String,
    db: AppDatabase,
    onDismiss: () -> Unit,
    onViewProof: (Long) -> Unit
) {
    var executive by remember { mutableStateOf<ExecutiveEntity?>(null) }
    var classCounts by remember { mutableStateOf<List<ExecutiveClassCountRaw>>(emptyList()) }
    var heldTags by remember { mutableStateOf<List<InventoryTagEntity>>(emptyList()) }
    var movements by remember { mutableStateOf<List<MovementWithProof>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Class Stock, 1: Held Tags, 2: History

    LaunchedEffect(executiveId) {
        executive = db.executiveDao().getExecutiveByExecId(executiveId).firstOrNull()
        classCounts = db.inventoryTagDao().getExecutiveClassCounts(executiveId).firstOrNull() ?: emptyList()
        heldTags = db.inventoryTagDao().getTagsByExecutive(executiveId).firstOrNull() ?: emptyList()
        movements = db.stockMovementLedgerDao().getMovementsForExecutive(executiveId).firstOrNull() ?: emptyList()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = BrandPurple,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = executive?.executiveId ?: executiveId,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${executive?.executiveName ?: "Executive"} • ${executive?.region ?: "Region"}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_close_exec_details")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Summary Chip Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = LocationExecutiveColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Current Stock", fontSize = 10.sp, color = LocationExecutiveColor, fontWeight = FontWeight.SemiBold)
                            Text("${heldTags.size} tags", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LocationExecutiveColor)
                        }
                    }

                    Surface(
                        color = if (executive?.active == true) BrandEmerald.copy(alpha = 0.12f) else Slate200,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Status", fontSize = 10.sp, color = if (executive?.active == true) BrandEmerald else Slate600, fontWeight = FontWeight.SemiBold)
                            Text(if (executive?.active == true) "Active" else "Inactive", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (executive?.active == true) BrandEmerald else Slate600)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tabs
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Class Stock", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Tags (${heldTags.size})", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("History", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Content
                when (selectedTab) {
                    0 -> {
                        // Class-wise Stock
                        if (classCounts.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No tags currently held by this executive.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(classCounts) { item ->
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(item.tag_class, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("${item.count} tags", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LocationExecutiveColor)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Held Tag List
                        if (heldTags.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No tags in hand.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(heldTags) { tag ->
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(tag.tagSerialNumber, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                                Text(tag.tagClass, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            LocationBadge(locationName = tag.currentLocation)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Executive Movements
                        if (movements.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No recorded transfers.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(movements) { m ->
                                    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(m.createdAt))
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("${m.previousLocation ?: "BASELINE"} → ${m.newLocation}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                                Text("Tag: ${m.tagSerialNumber} (${m.tagClass})", fontSize = 11.sp)
                                                Text(dateStr, fontSize = 10.sp, color = Slate600)
                                            }
                                            TextButton(onClick = { onViewProof(m.proofDocumentId) }) {
                                                Text("Proof", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
