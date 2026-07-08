package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.util.UpdateManager
import com.example.util.UpdateState

@Composable
fun UpdateDialog() {
    val context = LocalContext.current
    val updateState by UpdateManager.updateState.collectAsState()
    val info = UpdateManager.updateInfo

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val currentState = updateState
                if (currentState is UpdateState.PermissionRequired) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        if (context.packageManager.canRequestPackageInstalls()) {
                            UpdateManager.installApk(context, currentState.apkFile)
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (UpdateManager.showDialog && info != null) {
        Dialog(
            onDismissRequest = {
                if (!info.isForceUpdate && updateState !is UpdateState.Downloading && updateState !is UpdateState.Installing) {
                    UpdateManager.showDialog = false
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = !info.isForceUpdate && updateState !is UpdateState.Downloading && updateState !is UpdateState.Installing,
                dismissOnClickOutside = !info.isForceUpdate && updateState !is UpdateState.Downloading && updateState !is UpdateState.Installing
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Güncelleme",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )

                    Text(
                        text = "Yeni Sürüm Mevcut!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Sürüm: v${info.versionName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Yenilikler:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = info.releaseNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when (val state = updateState) {
                        is UpdateState.Downloading -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val animatedProgress by animateFloatAsState(targetValue = state.progress / 100f)
                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                Text(
                                    text = "Yeni sürüm indiriliyor: %${state.progress}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        is UpdateState.Installing -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Yükleyici başlatılıyor...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        is UpdateState.Error -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            UpdateManager.showDialog = false
                                            UpdateManager.resetState()
                                        },
                                        modifier = Modifier.weight(1.5f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Kapat")
                                    }
                                    Button(
                                        onClick = {
                                            try {
                                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/selahattinaykun/Sa-l-ksa-APP/releases"))
                                                context.startActivity(browserIntent)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        },
                                        modifier = Modifier.weight(2f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("GitHub'ı Aç")
                                    }
                                }
                            }
                        }
                        is UpdateState.PermissionRequired -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Uygulamanın güncellenebilmesi için bilinmeyen kaynaklardan yükleme yapılmasına izin vermelisiniz. Lütfen yönlendirilen ekrandan izin verip geri dönün.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            UpdateManager.showDialog = false
                                            UpdateManager.resetState()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("İptal")
                                    }
                                    Button(
                                        onClick = {
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                                try {
                                                    val settingsIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                                        data = Uri.parse("package:${context.packageName}")
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(settingsIntent)
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1.5f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("İzin Ayarlarını Aç")
                                    }
                                }
                            }
                        }
                        else -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (!info.isForceUpdate) {
                                    OutlinedButton(
                                        onClick = { UpdateManager.showDialog = false },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Daha Sonra")
                                    }
                                }
                                Button(
                                    onClick = {
                                        UpdateManager.startDownloadAndInstall(context, info.updateUrl)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Güncelle")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
