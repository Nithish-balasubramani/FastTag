package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClassStockSummary
import com.example.data.model.ExecutiveStockSummary
import com.example.data.model.OverallStockMetrics
import com.example.ui.theme.*

@Composable
fun ReportsScreen(
    metrics: OverallStockMetrics,
    classSummaries: List<ClassStockSummary>,
    executiveSummaries: List<ExecutiveStockSummary>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("reports_screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "INVENTORY AUDIT & REPORTS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "All totals derived dynamically from verified individual RFID tag records.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Summary Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("LOCATION-WISE TOTALS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Divider()
                    ReportMetricRow("Central Warehouse", metrics.centralStock, LocationCentralColor)
                    ReportMetricRow("Master Inventory", metrics.masterStock, LocationMasterColor)
                    ReportMetricRow("Executive / Subagent Stock", metrics.executiveStock, LocationExecutiveColor)
                    ReportMetricRow("Assigned / Installed Tags", metrics.assignedStock, LocationAssignedColor)
                    Divider()
                    ReportMetricRow("Total Active Tag Inventory", metrics.totalActive, BrandBluePrimary, isBold = true)
                }
            }
        }

        // Class-wise Report Matrix
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CLASS-WISE INVENTORY AUDIT", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        Row(
                            modifier = Modifier
                                .background(Slate100, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Text("Tag Class", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(110.dp))
                            Text("Central", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(60.dp))
                            Text("Master", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(60.dp))
                            Text("Executive", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(70.dp))
                            Text("Assigned", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(70.dp))
                            Text("Total", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(60.dp))
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        classSummaries.forEach { cls ->
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cls.tagClass, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.width(110.dp))
                                Text("${cls.centralCount}", fontSize = 12.sp, modifier = Modifier.width(60.dp))
                                Text("${cls.masterCount}", fontSize = 12.sp, modifier = Modifier.width(60.dp), fontWeight = FontWeight.Bold, color = LocationMasterColor)
                                Text("${cls.executiveCount}", fontSize = 12.sp, modifier = Modifier.width(70.dp))
                                Text("${cls.assignedCount}", fontSize = 12.sp, modifier = Modifier.width(70.dp))
                                Text("${cls.totalCount}", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp))
                            }
                        }
                    }
                }
            }
        }

        // Executive Breakdown Report
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("EXECUTIVE STOCK SUMMARY", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(10.dp))

                    executiveSummaries.forEach { exec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${exec.executiveId} - ${exec.executiveName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(exec.region, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "${exec.currentStock} tags",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (exec.currentStock > 0) LocationExecutiveColor else Slate600
                            )
                        }
                        Divider(color = Slate100)
                    }
                }
            }
        }
    }
}

@Composable
fun ReportMetricRow(label: String, value: Int, color: androidx.compose.ui.graphics.Color, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = "%,d".format(value),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
