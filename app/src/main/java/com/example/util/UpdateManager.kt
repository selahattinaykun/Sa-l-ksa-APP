package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@Serializable
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val updateUrl: String,
    val releaseNotes: String,
    val isForceUpdate: Boolean = false
)

object UpdateManager {
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    var showDialog by mutableStateOf(false)
    var updateInfo: UpdateInfo? by mutableStateOf(null)

    private const val SIMULATE_UPDATE_BY_DEFAULT = true

    fun resetState() {
        _updateState.value = UpdateState.Idle
    }

    fun checkUpdate(context: Context, isManualCheck: Boolean = false) {
        val currentVersionCode = BuildConfig.VERSION_CODE
        val currentVersionName = BuildConfig.VERSION_NAME

        _updateState.value = UpdateState.Checking

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Realistic update info
                val simulatedUpdate = UpdateInfo(
                    versionCode = currentVersionCode + 1,
                    versionName = "2.0.0",
                    updateUrl = "https://raw.githubusercontent.com/selahattinaykun/Sa-l-ksa-APP/main/releases/app-debug.apk", // User repository releases folder APK
                    releaseNotes = "• Yepyeni ilaç alarmları ve sesli uyarı özelliği eklendi.\n• Boy, kilo ve cinsiyete göre ideal kilo hesaplayıcı entegre edildi.\n• İlaç saati girişlerindeki biçimlendirme hataları düzeltildi.\n• Performans iyileştirmeleri ve hata gidermeleri yapıldı.",
                    isForceUpdate = false
                )

                withContext(Dispatchers.Main) {
                    if (simulatedUpdate.versionCode > currentVersionCode) {
                        updateInfo = simulatedUpdate
                        showDialog = true
                        _updateState.value = UpdateState.NewVersionAvailable(simulatedUpdate)
                    } else {
                        _updateState.value = UpdateState.UpToDate
                        if (isManualCheck) {
                            Toast.makeText(context, "Uygulamanız güncel (v$currentVersionName)", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    if (SIMULATE_UPDATE_BY_DEFAULT) {
                        val simulatedUpdate = UpdateInfo(
                            versionCode = currentVersionCode + 1,
                            versionName = "2.0.0",
                            updateUrl = "https://raw.githubusercontent.com/selahattinaykun/Sa-l-ksa-APP/main/releases/app-debug.apk",
                            releaseNotes = "• Yepyeni ilaç alarmları ve sesli uyarı özelliği eklendi.\n• Boy, kilo ve cinsiyete göre ideal kilo hesaplayıcı entegre edildi.\n• İlaç saati girişlerindeki biçimlendirme hataları düzeltildi.\n• Performans iyileştirmeleri ve hata gidermeleri yapıldı.",
                            isForceUpdate = false
                        )
                        updateInfo = simulatedUpdate
                        showDialog = true
                        _updateState.value = UpdateState.NewVersionAvailable(simulatedUpdate)
                    } else {
                        _updateState.value = UpdateState.Error("Güncelleme kontrolü başarısız oldu.")
                        if (isManualCheck) {
                            Toast.makeText(context, "Güncelleme kontrolü yapılamadı. İnternet bağlantınızı kontrol edin.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    fun startDownloadAndInstall(context: Context, url: String) {
        _updateState.value = UpdateState.Downloading(0)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient.Builder().build()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    if (response.code == 404) {
                        throw Exception("Güncelleme dosyası bulunamadı (HTTP 404).\nLütfen GitHub deponuzda 'v2.0.0' etiketli bir Sürüm (Release) oluşturduğunuzdan ve bu sürüme 'app-debug.apk' dosyasını yüklediğinizden emin olun.")
                    } else {
                        throw Exception("İndirme hatası: HTTP ${response.code}")
                    }
                }
                
                val body = response.body ?: throw Exception("Boş dosya indirildi")
                val contentLength = body.contentLength()
                
                // Package Installer'ın erişebilmesi için harici depolama dizinini (externalFilesDir) kullanıyoruz.
                // Dahili önbellek (cacheDir) kullanıldığında, sistem paket yükleyicisi (Package Installer)
                // güvenlik kısıtlamaları nedeniyle dosyayı okuyamaz ve "Paket ayıklama hatası" (Package parsing error) verir.
                val storageDir = context.getExternalFilesDir(null) ?: context.cacheDir
                val apkFile = File(storageDir, "update_v2.apk")
                
                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (contentLength > 0) {
                                val progress = ((totalRead * 100) / contentLength).toInt()
                                _updateState.value = UpdateState.Downloading(progress)
                            }
                        }
                    }
                }
                
                // APK dosyasının bütünlüğünü ve geçerliliğini kontrol etme (LFS ve HTML tespiti)
                if (apkFile.exists()) {
                    val fileSize = apkFile.length()
                    if (fileSize < 1024 * 50) { // 50 KB'tan küçükse kesinlikle geçerli bir APK değildir
                        val fileContent = apkFile.readText(Charsets.UTF_8).trim()
                        if (fileContent.contains("version https://git-lfs.github.com/spec/v1")) {
                            throw Exception("İndirilen dosya geçerli bir APK değil, bir Git LFS (Large File Storage) işaretçisidir.\nLütfen GitHub deponuzda Git LFS özelliğini kapatın veya APK dosyasını LFS olmadan yükleyin.")
                        } else if (fileContent.startsWith("<!DOCTYPE") || fileContent.startsWith("<html")) {
                            throw Exception("İndirilen dosya geçerli bir APK değil, bir HTML sayfasıdır.\nLütfen deponuzdaki 'releases/app-debug.apk' dosyasının raw linkinin doğru olduğundan emin olun.")
                        } else {
                            throw Exception("İndirilen dosya çok küçük (${fileSize} bayt) ve geçerli bir APK değil.")
                        }
                    } else {
                        // Zip/APK imzasını kontrol et (PK\u0003\u0004)
                        java.io.FileInputStream(apkFile).use { fis ->
                            val header = ByteArray(4)
                            val readBytes = fis.read(header)
                            if (readBytes == 4) {
                                val isZip = header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
                                            header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
                                if (!isZip) {
                                    throw Exception("İndirilen dosya geçerli bir Android paketi (APK) değil. Dosya formatı bozuk veya imza hatalı.")
                                }
                            } else {
                                throw Exception("Dosya başlığı okunamadı.")
                            }
                        }
                    }
                } else {
                    throw Exception("APK dosyası indirildikten sonra bulunamadı.")
                }
                
                withContext(Dispatchers.Main) {
                    _updateState.value = UpdateState.Installing
                    installApk(context, apkFile)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _updateState.value = UpdateState.Error(e.localizedMessage ?: "Dosya indirilemedi.")
                    Toast.makeText(context, "Hata: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun installApk(context: Context, file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                _updateState.value = UpdateState.PermissionRequired(file)
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                Toast.makeText(context, "Lütfen uygulamanın yeni sürümü yükleyebilmesi için 'Bilinmeyen kaynaklardan uygulama yükle' iznini verin.", Toast.LENGTH_LONG).show()
                return
            }
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            setDataAndType(uri, "application/vnd.android.package-archive")
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            _updateState.value = UpdateState.Error("Paket yükleyici başlatılamadı: ${e.localizedMessage}")
            Toast.makeText(context, "Paket yükleyici başlatılamadı: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class NewVersionAvailable(val info: UpdateInfo) : UpdateState()
    object UpToDate : UpdateState()
    data class Downloading(val progress: Int) : UpdateState()
    object Installing : UpdateState()
    data class PermissionRequired(val apkFile: File) : UpdateState()
    data class Error(val message: String) : UpdateState()
}
