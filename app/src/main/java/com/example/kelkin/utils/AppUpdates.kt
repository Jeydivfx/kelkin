package com.example.kelkin.utils

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.example.kelkin.BuildConfig
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.security.MessageDigest

internal data class AppRelease(val code: Long, val name: String, val url: String, val sha256: String, val notes: String)
internal sealed class UpdateDownload {
    data class Progress(val percent: Int?) : UpdateDownload()
    data class Ready(val file: File) : UpdateDownload()
    data class Failed(val reason: String) : UpdateDownload()
}

/** Release metadata is public; only administrators should be able to publish it. */
internal class AppUpdates(context: Context) {
    private val context = context.applicationContext
    private val prefs = this.context.getSharedPreferences("app_updates", Context.MODE_PRIVATE)
    private val downloads = this.context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val flavor = if (BuildConfig.IS_TV) "tv" else "mobile"
    private val localCode get() = PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0))
    val hasDownload get() = prefs.getLong("downloadId", -1) >= 0
    val versionName get() = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    init {
        if (hasDownload && prefs.getLong("versionCode", -1) <= localCode) discard()
    }

    suspend fun check(): AppRelease? {
        val snapshot = withTimeout(15_000) {
            FirebaseDatabase.getInstance().getReference("app_config/updates/$flavor").get().await()
        }
        if (!snapshot.exists()) error("هنوز نسخه‌ای برای به‌روزرسانی منتشر نشده است.")
        val code = snapshot.child("versionCode").getValue(Long::class.java) ?: error("اطلاعات نسخه کامل نیست.")
        if (code <= localCode) return null
        val release = AppRelease(code,
            snapshot.child("versionName").getValue(String::class.java).orEmpty(),
            snapshot.child("apkUrl").getValue(String::class.java).orEmpty(),
            snapshot.child("sha256").getValue(String::class.java).orEmpty().lowercase(),
            snapshot.child("releaseNotes").getValue(String::class.java).orEmpty())
        val uri = Uri.parse(release.url)
        require(uri.scheme == "https" && uri.host == "github.com" &&
            uri.path.orEmpty().startsWith("/Jeydivfx/kelkin/releases/download/") &&
            uri.lastPathSegment.orEmpty().endsWith("-$flavor.apk")) { "لینک APK این نسخه معتبر نیست." }
        require(release.sha256.matches(Regex("[0-9a-f]{64}"))) { "کد بررسی صحت فایل وارد نشده است." }
        return release
    }

    fun download(release: AppRelease) {
        check(!hasDownload) { "دانلود دیگری در حال انجام است." }
        val path = "updates/kelkin-$flavor-${release.code}-${System.currentTimeMillis()}.apk"
        val request = DownloadManager.Request(Uri.parse(release.url))
            .setTitle("کلکین ${release.name}").setDescription("دانلود به‌روزرسانی برنامه")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, path)
        val id = downloads.enqueue(request)
        prefs.edit().putLong("downloadId", id).putLong("versionCode", release.code)
            .putString("sha256", release.sha256).putString("path", path).apply()
    }

    fun status(): UpdateDownload {
        val id = prefs.getLong("downloadId", -1)
        downloads.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
            if (!cursor.moveToFirst()) return UpdateDownload.Failed("دانلود پیدا نشد؛ دوباره تلاش کنید.")
            when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    val base = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        ?: return UpdateDownload.Failed("فضای ذخیره‌سازی در دسترس نیست.")
                    val file = File(base, prefs.getString("path", "").orEmpty())
                    return if (file.isFile) UpdateDownload.Ready(file) else UpdateDownload.Failed("فایل دانلود‌شده پیدا نشد.")
                }
                DownloadManager.STATUS_FAILED -> return UpdateDownload.Failed("دانلود انجام نشد؛ اینترنت و فضای دستگاه را بررسی کنید.")
                else -> {
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val loaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    return UpdateDownload.Progress(if (total > 0) (loaded * 100 / total).toInt().coerceIn(0, 100) else null)
                }
            }
        }
    }

    suspend fun verify(file: File) = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
        }
        val hash = digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
        require(hash == prefs.getString("sha256", "")) { "فایل ناقص یا متفاوت است؛ دوباره دانلود کنید." }
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val installed = context.packageManager.getPackageInfo(context.packageName, flags)
        val archive = context.packageManager.getPackageArchiveInfo(file.absolutePath, flags)
            ?: error("فایل APK معتبر نیست.")
        require(archive.packageName == context.packageName &&
            PackageInfoCompat.getLongVersionCode(archive) == prefs.getLong("versionCode", -1) &&
            PackageInfoCompat.getLongVersionCode(archive) > localCode) { "این فایل به‌روزرسانیِ نسخهٔ نصب‌شده نیست." }
        @Suppress("DEPRECATION")
        fun certificates(info: android.content.pm.PackageInfo) =
            (if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures)
                ?.map { it.toCharsString() }?.toSet().orEmpty()
        require(certificates(installed).isNotEmpty() && certificates(installed) == certificates(archive)) {
            "امضای فایل با اپ نصب‌شده مطابقت ندارد. از APK امضاشده با کلید اصلی استفاده کنید."
        }
    }

    fun canInstall() = Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()
    fun permissionIntent() = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
    fun installIntent(file: File): Intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(FileProvider.getUriForFile(context, "${context.packageName}.updates", file), "application/vnd.android.package-archive")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    fun discard() {
        val id = prefs.getLong("downloadId", -1)
        if (id >= 0) downloads.remove(id)
        prefs.edit().clear().apply()
    }
}
