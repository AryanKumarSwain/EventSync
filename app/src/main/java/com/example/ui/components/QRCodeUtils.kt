package com.example.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import java.util.EnumMap

object QRCodeUtils {

    /**
     * Builds standard public URL for an event.
     */
    fun buildPublicEventUrl(eventSlug: String): String {
        val cleanSlug = if (eventSlug.isBlank()) "event-live" else eventSlug.trim()
        return "https://eventsync.app/public/events/$cleanSlug"
    }

    /**
     * Generates a 100% genuine, standard, scannable QR Code Bitmap.
     */
    fun generateQRCodeBitmap(
        content: String,
        sizePx: Int = 512,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                put(EncodeHintType.MARGIN, 1) // Clean compact border
            }

            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )

            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) foregroundColor else backgroundColor
                }
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Shares the public event link with rich details using Android Native Share Sheet.
     */
    fun shareEventViaApps(
        context: Context,
        eventName: String,
        eventDate: String = "",
        venue: String = "",
        publicUrl: String
    ) {
        try {
            val shareBody = buildString {
                append("🎪 *${eventName.ifBlank { "School Event" }}*\n")
                if (eventDate.isNotBlank()) append("📅 Date: $eventDate\n")
                if (venue.isNotBlank()) append("📍 Venue: $venue\n")
                append("\n✨ *Live Program Schedule & Stage Tracker:*\n")
                append("👉 $publicUrl\n\n")
                append("📱 *How to View / Track Live:*\n")
                append("• **In App (Instant Live Sync):** If you have EventSync installed, tap the link or scan the QR to open this event directly.\n")
                append("• **No App yet?** Download/Open EventSync app -> Go to 'Scan QR' -> Paste the link or pick QR from photos to access live schedule, stage timers & winners in real-time!")
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Live Event: $eventName")
                putExtra(Intent.EXTRA_TEXT, shareBody)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Public Event Link & Schedule via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Could not open share dialog: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shares the actual generated QR Code image to WhatsApp, Telegram, Email, etc.
     */
    fun shareQRCodeImage(
        context: Context,
        eventName: String,
        bitmap: Bitmap,
        publicUrl: String
    ) {
        try {
            val cacheFolder = File(context.cacheDir, "qr_codes")
            if (!cacheFolder.exists()) cacheFolder.mkdirs()

            val sanitizedName = eventName.replace("[^a-zA-Z0-9]".toRegex(), "_").take(25)
            val file = File(cacheFolder, "QR_${sanitizedName}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val authority = "${context.packageName}.provider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val shareBody = buildString {
                append("🎪 *${eventName}* - Live Event QR Code\n\n")
                append("✨ Event Link: $publicUrl\n\n")
                append("📱 Scan this QR with your camera or open EventSync App -> Scan from Photos / Gallery to track live stage performances, delays, and results!")
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, shareBody)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share QR Code Image")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to share QR image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Saves the QR Code image to Gallery / Downloads folder.
     */
    fun saveQRCodeToGallery(
        context: Context,
        eventName: String,
        bitmap: Bitmap
    ) {
        val sanitizedName = eventName.replace("[^a-zA-Z0-9]".toRegex(), "_").take(25)
        val fileName = "EventSync_QR_${sanitizedName}_${System.currentTimeMillis()}.png"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/EventSync")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val uri = context.contentResolver.insert(collection, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                    Toast.makeText(context, "✅ QR Code saved to Pictures/EventSync!", Toast.LENGTH_LONG).show()
                    return
                }
            }

            // Fallback for older versions or if MediaStore insert failed
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "EventSync")
            if (!appDir.exists()) appDir.mkdirs()
            val file = File(appDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Toast.makeText(context, "✅ QR Code saved to Pictures/EventSync/$fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "⚠️ Could not save to gallery: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
