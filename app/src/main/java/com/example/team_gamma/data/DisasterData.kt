package com.example.team_gamma.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.FireTruck
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Water
import androidx.compose.ui.graphics.vector.ImageVector

// Data classes to structure our safety information
data class ChecklistItem(val text: String, val icon: ImageVector)
data class ChecklistSection(val title: String, val items: List<ChecklistItem>)
data class DisasterGuide(val name: String, val icon: ImageVector, val sections: List<ChecklistSection>)

// Hardcoded data for the Do's & Don'ts screen.
// In a real app, this might come from a server or a local database.
object DisasterDataProvider {
    val guides = listOf(
        DisasterGuide(
            name = "Floods",
            icon = Icons.Default.Water,
            sections = listOf(
                ChecklistSection("Before a Flood", listOf(
                    ChecklistItem("Build an emergency kit", Icons.Default.Build),
                    ChecklistItem("Know your evacuation routes", Icons.Default.EvStation),
                    ChecklistItem("Secure your home and move essential items to higher ground", Icons.Default.DoorFront),
                    ChecklistItem("Turn off utilities like electricity and gas if instructed", Icons.Default.ElectricalServices)
                )),
                ChecklistSection("During a Flood", listOf(
                    ChecklistItem("Evacuate immediately if told to do so", Icons.Default.FamilyRestroom),
                    ChecklistItem("Do not walk or drive through floodwaters", Icons.Default.Water),
                    ChecklistItem("Stay informed through official alerts", Icons.Default.NotificationsActive),
                    ChecklistItem("Move to higher ground or a roof if trapped", Icons.Default.Cloud)
                )),
                ChecklistSection("After a Flood", listOf(
                    ChecklistItem("Return home only when authorities say it is safe", Icons.Default.Check),
                    ChecklistItem("Avoid contact with floodwater as it may be contaminated", Icons.Default.Healing),
                    ChecklistItem("Check for damage to your home before entering", Icons.Default.Build),
                    ChecklistItem("Document damage with photos for insurance purposes", Icons.Default.Phone)
                ))
            )
        ),
        DisasterGuide(
            name = "Earthquake",
            icon = Icons.Default.Build, // Placeholder icon
            sections = listOf(
                ChecklistSection("Before an Earthquake", listOf(
                    ChecklistItem("Secure heavy furniture to walls", Icons.Default.Build),
                    ChecklistItem("Create a family emergency plan", Icons.Default.FamilyRestroom),
                    ChecklistItem("Identify safe spots in each room (under sturdy tables)", Icons.Default.Check)
                )),
                ChecklistSection("During an Earthquake", listOf(
                    ChecklistItem("Drop, Cover, and Hold On", Icons.Default.NotificationsActive),
                    ChecklistItem("Stay indoors until shaking stops", Icons.Default.DoorFront),
                    ChecklistItem("Stay away from windows and heavy objects", Icons.Default.Warning)
                )),
                ChecklistSection("After an Earthquake", listOf(
                    ChecklistItem("Check for injuries and provide first aid", Icons.Default.LocalHospital),
                    ChecklistItem("Be prepared for aftershocks", Icons.Default.NotificationsActive),
                    ChecklistItem("Check for gas leaks and fire hazards", Icons.Default.FireTruck)
                ))
            )
        ),
        DisasterGuide(
            name = "Fire",
            icon = Icons.Default.FireTruck,
            sections = listOf(
                ChecklistSection("Before a Fire", listOf(
                    ChecklistItem("Install and test smoke alarms regularly", Icons.Default.NotificationsActive),
                    ChecklistItem("Plan and practice a home fire escape plan", Icons.Default.EvStation),
                    ChecklistItem("Keep flammable materials away from heat sources", Icons.Default.Warning)
                )),
                ChecklistSection("During a Fire", listOf(
                    ChecklistItem("Evacuate immediately; stay low to avoid smoke", Icons.Default.FamilyRestroom),
                    ChecklistItem("Once you are out, stay out. Do not go back inside.", Icons.Default.DoorFront),
                    ChecklistItem("Call emergency services from a safe location", Icons.Default.Phone)
                )),
                ChecklistSection("After a Fire", listOf(
                    ChecklistItem("Wait for official clearance before re-entering", Icons.Default.Check),
                    ChecklistItem("Contact your insurance agent", Icons.Default.Edit),
                    ChecklistItem("Seek professional help for cleaning and restoration", Icons.Default.Healing)
                ))
            )
        )
    )
}
