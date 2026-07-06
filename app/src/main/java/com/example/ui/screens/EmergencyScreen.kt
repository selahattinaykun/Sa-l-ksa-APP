package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var currentLocation by remember { mutableStateOf<android.location.Location?>(null) }
    var isPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isFetchingLocation by remember { mutableStateOf(false) }

    // Helper to fetch/refresh the location
    val updateLocation = {
        if (isPermissionGranted) {
            isFetchingLocation = true
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    currentLocation = location
                    isFetchingLocation = false
                }.addOnFailureListener {
                    isFetchingLocation = false
                }
            } catch (e: SecurityException) {
                isFetchingLocation = false
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        isPermissionGranted = granted
        if (granted) {
            isFetchingLocation = true
            try {
                LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener { location ->
                    currentLocation = location
                    isFetchingLocation = false
                }.addOnFailureListener {
                    isFetchingLocation = false
                }
            } catch (e: SecurityException) {
                isFetchingLocation = false
            }
        }
    }

    // Auto-fetch location on load if permission is granted
    LaunchedEffect(isPermissionGranted) {
        if (isPermissionGranted) {
            updateLocation()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sağlık & Acil Durum") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Anlık Konum
            Text(
                "Anlık Konum Bilginiz",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Mevcut Koordinatlar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (isPermissionGranted) {
                            IconButton(onClick = updateLocation) {
                                if (isFetchingLocation) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Konumu Yenile",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    if (!isPermissionGranted) {
                        Text(
                            text = "Konumunuzu görmek ve acil mesajlarda paylaşmak için lütfen konum izni verin.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            modifier = Modifier.align(Alignment.End),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Konum İzni Ver")
                        }
                    } else {
                        val loc = currentLocation
                        if (loc != null) {
                            Text(
                                text = "Enlem (Lat): ${String.format(Locale.US, "%.5f", loc.latitude)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Boylam (Lng): ${String.format(Locale.US, "%.5f", loc.longitude)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            TextButton(
                                onClick = {
                                    val uri = Uri.parse("geo:${loc.latitude},${loc.longitude}?q=${loc.latitude},${loc.longitude}(Konumum)")
                                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                        setPackage("com.google.android.apps.maps")
                                    }
                                    if (intent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(intent)
                                    } else {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${loc.latitude},${loc.longitude}"))
                                        context.startActivity(browserIntent)
                                    }
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Haritada Göster", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(
                                text = "Konum alınıyor veya servis kapalı...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Section 2: Yakındaki Sağlık Kuruluşları
            Text(
                "Yakındaki Sağlık Kuruluşları",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            ActionCard(
                title = "Yakındaki Hastaneler",
                icon = Icons.Default.LocalHospital,
                onClick = { openMap(context, "Hastaneler") }
            )
            
            ActionCard(
                title = "Nöbetçi Eczaneler",
                icon = Icons.Default.LocalPharmacy,
                onClick = { openMap(context, "Eczaneler") }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Section 3: Acil İletişim
            Text(
                "Acil İletişim",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            ActionCard(
                title = "112'yi Ara",
                icon = Icons.Default.Phone,
                onClick = { callEmergency(context) },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
            
            ActionCard(
                title = "Acil Durum SMS Gönder",
                icon = Icons.Default.Message,
                onClick = {
                    if (isPermissionGranted) {
                        sendEmergencySMSWithLocation(context, currentLocation)
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )

            ActionCard(
                title = "WhatsApp ile Acil Durum Gönder",
                icon = Icons.Default.Send,
                onClick = {
                    if (isPermissionGranted) {
                        sendEmergencyWhatsAppWithLocation(context, currentLocation)
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                containerColor = androidx.compose.ui.graphics.Color(0xFF25D366), // Beautiful authentic WhatsApp Green
                contentColor = androidx.compose.ui.graphics.Color.White
            )
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = if (containerColor == MaterialTheme.colorScheme.errorContainer || containerColor == androidx.compose.ui.graphics.Color(0xFF25D366)) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

private fun openMap(context: Context, query: String) {
    val uri = Uri.parse("geo:0,0?q=$query")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    intent.setPackage("com.google.android.apps.maps")
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$query"))
        context.startActivity(browserIntent)
    }
}

private fun callEmergency(context: Context) {
    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:112")
    }
    context.startActivity(dialIntent)
}

private fun sendEmergencySMSWithLocation(context: Context, location: android.location.Location?) {
    val message = if (location != null) {
        "Acil durum! Lütfen bana ulaşın. Konumum: https://maps.google.com/?q=${location.latitude},${location.longitude}"
    } else {
        "Acil durum! Lütfen bana ulaşın, sağlık durumumla ilgili yardıma ihtiyacım var."
    }
    openSmsApp(context, message)
}

private fun sendEmergencyWhatsAppWithLocation(context: Context, location: android.location.Location?) {
    val message = if (location != null) {
        "Acil durum! Lütfen bana ulaşın. Konumum: https://maps.google.com/?q=${location.latitude},${location.longitude}"
    } else {
        "Acil durum! Lütfen bana ulaşın, sağlık durumumla ilgili yardıma ihtiyacım var."
    }
    openWhatsAppApp(context, message)
}

private fun openSmsApp(context: Context, message: String) {
    val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("smsto:")
        putExtra("sms_body", message)
    }
    context.startActivity(smsIntent)
}

private fun openWhatsAppApp(context: Context, message: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback message via generic share intent if URL is unresolvable
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Paylaş"))
    }
}
