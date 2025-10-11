package com.example.team_gamma.screens



import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.team_gamma.data.ChecklistSection
import com.example.team_gamma.data.DisasterDataProvider
import com.example.team_gamma.data.DisasterGuide
import com.example.team_gamma.ui.theme.TeamGammaTheme
import com.example.team_gamma.data.ChecklistItem


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DosAndDontsScreen(onNavigateBack: () -> Unit) {
    val disasterGuides = DisasterDataProvider.guides
    var selectedTabIndex by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preparedness Guides") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                disasterGuides.forEachIndexed { index, guide ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(guide.name) },
                        icon = { Icon(guide.icon, contentDescription = guide.name) }
                    )
                }
            }

            // Display content for the selected tab
            DisasterGuideContent(guide = disasterGuides[selectedTabIndex])
        }
    }
}

@Composable
fun DisasterGuideContent(guide: DisasterGuide) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(guide.sections) { section ->
            ExpandableSectionCard(section = section)
        }
    }
}

@Composable
fun ExpandableSectionCard(section: ChecklistSection) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }

            // Animated visibility for the checklist items
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Divider()
                    section.items.forEach { item ->
                        ChecklistItemRow(item = item)
                    }
                }
            }
        }
    }
}

@Composable
fun ChecklistItemRow(item: ChecklistItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = item.text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(showBackground = true)
@Composable
fun DosAndDontsScreenPreview() {
    TeamGammaTheme {
        DosAndDontsScreen(onNavigateBack = {})
    }
}
