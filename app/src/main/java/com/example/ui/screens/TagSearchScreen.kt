package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InventoryLocation
import com.example.data.model.InventoryTagEntity
import com.example.ui.components.LocationBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagSearchScreen(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    selectedLocation: String,
    onLocationChange: (String) -> Unit,
    selectedClass: String,
    onClassChange: (String) -> Unit,
    discoveredClasses: List<String>,
    searchResults: List<InventoryTagEntity>,
    onSelectTag: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("tag_search_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "TAG SEARCH & AUDIT",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Search Input Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            placeholder = { Text("Search by Tag Serial Number or Subagent...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_search_tag"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Location Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedLocation.isEmpty(),
                onClick = { onLocationChange("") },
                label = { Text("All Locations") }
            )
            listOf(
                InventoryLocation.CENTRAL.name,
                InventoryLocation.MASTER.name,
                InventoryLocation.EXECUTIVE.name,
                InventoryLocation.ASSIGNED.name
            ).forEach { loc ->
                FilterChip(
                    selected = selectedLocation == loc,
                    onClick = { onLocationChange(if (selectedLocation == loc) "" else loc) },
                    label = { Text(loc) }
                )
            }
        }

        // Class Filter Chips if available
        if (discoveredClasses.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedClass.isEmpty(),
                    onClick = { onClassChange("") },
                    label = { Text("All Classes") }
                )
                discoveredClasses.forEach { cls ->
                    FilterChip(
                        selected = selectedClass == cls,
                        onClick = { onClassChange(if (selectedClass == cls) "" else cls) },
                        label = { Text(cls) }
                    )
                }
            }
        }

        // Results count
        Text(
            text = "Found ${searchResults.size} matching tags",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Results list
        if (searchResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No matching tags found for criteria.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults, key = { it.tagSerialNumber }) { tag ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTag(tag.tagSerialNumber) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = tag.tagSerialNumber,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = tag.tagClass,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Subagent: ${tag.subagentName} (${tag.subagentId})" +
                                            if (tag.currentExecutiveId != null) " • Holder: ${tag.currentExecutiveId}" else "",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LocationBadge(locationName = tag.currentLocation)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
