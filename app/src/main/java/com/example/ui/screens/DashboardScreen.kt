package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.LocationBadge
import com.example.ui.components.MetricCard
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    metrics: OverallStockMetrics,
    executiveSummaries: List<ExecutiveStockSummary>,
    classSummaries: List<ClassStockSummary>,
    recentMovements: List<MovementWithProof>,
    onStartWorkflow: (ProofType) -> Unit,
    onViewExecutive: (String) -> Unit,
    onViewTag: (String) -> Unit,
    onViewProof: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top Inventory Summary Cards
        item {
            Text(
                text = "INVENTORY SNAPSHOT",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // 5 KPI Cards in responsive grid/row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Central",
                    value = metrics.centralStock,
                    subtitle = "Warehouse stock",
                    icon = Icons.Default.Cloud,
                    accentColor = LocationCentralColor,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Master",
                    value = metrics.masterStock,
                    subtitle = "Ready for courier",
                    icon = Icons.Default.Warehouse,
                    accentColor = LocationMasterColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Executive",
                    value = metrics.executiveStock,
                    subtitle = "With field agents",
                    icon = Icons.Default.Person,
                    accentColor = LocationExecutiveColor,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Assigned",
                    value = metrics.assignedStock,
                    subtitle = "Sold & installed",
                    icon = Icons.Default.CheckCircle,
                    accentColor = LocationAssignedColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "TOTAL ACTIVE INVENTORY",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "%,d RFID Tags".format(metrics.totalActive),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Quick Actions Section
        item {
            Text(
                text = "INVENTORY OPERATIONS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionCard(
                        title = "Existing Stock",
                        subtitle = "Initialize Baseline",
                        icon = Icons.Default.FileUpload,
                        color = BrandBluePrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { onStartWorkflow(ProofType.EXISTING_STOCK) }
                    )
                    QuickActionCard(
                        title = "Central Received",
                        subtitle = "CENTRAL → MASTER",
                        icon = Icons.Default.MoveToInbox,
                        color = LocationMasterColor,
                        modifier = Modifier.weight(1f),
                        onClick = { onStartWorkflow(ProofType.CENTRAL_RECEIVED) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionCard(
                        title = "Courier Transfer",
                        subtitle = "MASTER → EXECUTIVE",
                        icon = Icons.Default.LocalShipping,
                        color = LocationExecutiveColor,
                        modifier = Modifier.weight(1f),
                        onClick = { onStartWorkflow(ProofType.COURIER_TO_EXECUTIVE) }
                    )
                    QuickActionCard(
                        title = "Tag Assigned",
                        subtitle = "EXECUTIVE → ASSIGNED",
                        icon = Icons.Default.AssignmentTurnedIn,
                        color = LocationAssignedColor,
                        modifier = Modifier.weight(1f),
                        onClick = { onStartWorkflow(ProofType.TAG_ASSIGNED) }
                    )
                }
            }
        }

        // Class-Wise Dynamic Matrix Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "CLASS-WISE INVENTORY MATRIX",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Auto-Discovered",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (classSummaries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tags recorded yet. Run an upload workflow to initialize inventory.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        // Header row
                        Row(
                            modifier = Modifier
                                .background(Slate100, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("Tag Class", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(110.dp))
                            Text("Central", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(64.dp))
                            Text("Master", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(64.dp))
                            Text("Executive", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(74.dp))
                            Text("Assigned", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(70.dp))
                            Text("Total", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(64.dp))
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        // Data rows
                        classSummaries.forEach { cls ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(cls.tagClass, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.width(110.dp))
                                Text("${cls.centralCount}", fontSize = 12.sp, modifier = Modifier.width(64.dp), color = LocationCentralColor)
                                Text("${cls.masterCount}", fontSize = 12.sp, modifier = Modifier.width(64.dp), color = LocationMasterColor, fontWeight = FontWeight.Bold)
                                Text("${cls.executiveCount}", fontSize = 12.sp, modifier = Modifier.width(74.dp), color = LocationExecutiveColor)
                                Text("${cls.assignedCount}", fontSize = 12.sp, modifier = Modifier.width(70.dp), color = LocationAssignedColor)
                                Text("${cls.totalCount}", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(64.dp))
                            }
                        }
                    }
                }
            }
        }

        // Executive Stock Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "EXECUTIVE STOCK DASHBOARD",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${executiveSummaries.size} Subagents",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    executiveSummaries.take(5).forEachIndexed { idx, exec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onViewExecutive(exec.executiveId) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (exec.active) BrandPurple.copy(alpha = 0.15f) else Slate200),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = exec.executiveId.takeLast(2),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (exec.active) BrandPurple else Slate600
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${exec.executiveId} • ${exec.executiveName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = exec.region,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = if (exec.currentStock > 0) LocationExecutiveColor.copy(alpha = 0.12f) else Slate100,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${exec.currentStock} tags",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (exec.currentStock > 0) LocationExecutiveColor else Slate600,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (idx < executiveSummaries.take(5).lastIndex) {
                            Divider(color = Slate200.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        // Recent Movements Audit Trail
        item {
            Text(
                text = "RECENT MOVEMENTS LEDGER",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (recentMovements.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Movement ledger is empty. Complete a transfer to log verified events.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        recentMovements.take(6).forEachIndexed { idx, m ->
                            val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(m.createdAt))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onViewTag(m.tagSerialNumber) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = m.tagSerialNumber,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = m.tagClass,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${m.previousLocation ?: "BASELINE"} → ${m.newLocation} • ${m.subagentName}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = dateStr,
                                        fontSize = 10.sp,
                                        color = Slate600
                                    )
                                }

                                TextButton(
                                    onClick = { onViewProof(m.proofDocumentId) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Proof", fontSize = 11.sp)
                                }
                            }
                            if (idx < recentMovements.take(6).lastIndex) {
                                Divider(color = Slate200.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .testTag("btn_${title.lowercase().replace(" ", "_")}")
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}
