package com.example.steganography

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import java.io.File
import java.io.FileOutputStream

class FileManager(private val context: Context) {

    companion object {
        private const val TAG = "FileManager"
    }

    interface FileCallback {
        fun onSuccess(message: String)
        fun onError(error: String)
        fun onProgress(message: String)
    }

    fun saveImageToGallery(imagePath: String, callback: FileCallback) {
        callback.onProgress("💾 Saving image to gallery...")
        Log.d(TAG, "Starting image save to gallery: $imagePath")

        try {
            val sourceFile = File(imagePath)
            if (!sourceFile.exists()) {
                callback.onError("❌ Image file not found")
                Log.e(TAG, "Source image file does not exist: $imagePath")
                return
            }

            val bitmap = android.graphics.BitmapFactory.decodeFile(imagePath)
            if (bitmap == null) {
                callback.onError("❌ Cannot load image")
                Log.e(TAG, "Cannot decode bitmap from file: $imagePath")
                return
            }

            val timestamp = System.currentTimeMillis()
            val fileName = "steganography_hidden_$timestamp.png"

            val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveImageToMediaStore(bitmap, fileName)
            } else {
                saveImageToExternalStorage(bitmap, fileName)
            }

            bitmap.recycle()

            if (success) {
                callback.onSuccess("✅ Image saved to gallery successfully!\n📱 You can now access it from gallery\n🔍 Data can be extracted from saved image\n📂 Look in 'Pictures/Steganography' folder")
                Log.d(TAG, "Image saved successfully to gallery")
            } else {
                callback.onError("❌ Error saving to gallery")
                Log.e(TAG, "Failed to save image to gallery")
            }

        } catch (e: Exception) {
            callback.onError("❌ Save error: ${e.message}")
            Log.e(TAG, "Exception during image save", e)
        }
    }

    fun saveAudioToDownloads(audioPath: String, callback: FileCallback) {
        callback.onProgress("🎵 Saving audio to downloads folder...")
        Log.d(TAG, "Starting audio save to downloads: $audioPath")

        try {
            val sourceFile = File(audioPath)
            if (!sourceFile.exists()) {
                callback.onError("❌ Audio file not found")
                Log.e(TAG, "Source audio file does not exist: $audioPath")
                return
            }

            val timestamp = System.currentTimeMillis()
            val fileExtension = sourceFile.extension.lowercase()
            val extension = if (fileExtension.isNotEmpty()) fileExtension else "mp3"
            val fileName = "steganography_audio_$timestamp.$extension"

            var success = false
            var savedLocation = ""

            // Option 1: MediaStore (works well on Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                success = saveAudioToMediaStore(sourceFile, fileName, callback)
                if (success) {
                    savedLocation = "MediaStore - Downloads/Steganography"
                }
            }

            // Option 2: Direct access (Android 9 and below)
            if (!success && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                success = saveAudioToExternalDownloads(sourceFile, fileName, callback)
                if (success) {
                    savedLocation = "Downloads/Steganography"
                }
            }

            // Option 3: Music folder (alternative)
            if (!success) {
                success = saveAudioToMusic(sourceFile, fileName, callback)
                if (success) {
                    savedLocation = "Music/Steganography"
                }
            }

            // Option 4: App accessible folder
            if (!success) {
                success = saveToAppExternalFiles(sourceFile, fileName, callback)
                if (success) {
                    savedLocation = "App accessible folder"
                }
            }

            if (success) {
                callback.onSuccess("✅ Audio saved successfully!\n📱 Location: $savedLocation\n🔍 Look for file named: $fileName\n🎵 File saved in $extension format")
                Log.d(TAG, "Audio saved successfully to: $savedLocation")
            } else {
                callback.onError("❌ Error saving audio in all locations")
                Log.e(TAG, "Failed to save audio in all attempted locations")
            }

        } catch (e: Exception) {
            callback.onError("❌ Save error: ${e.message}")
            Log.e(TAG, "Exception during audio save", e)
        }
    }

    private fun saveAudioToMediaStore(sourceFile: File, fileName: String, callback: FileCallback): Boolean {
        return try {
            val mimeType = when (sourceFile.extension.lowercase()) {
                "mp3" -> "audio/mpeg"
                "m4a" -> "audio/mp4"
                "wav" -> "audio/wav"
                "aac" -> "audio/aac"
                else -> "audio/mpeg" // default
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Download/Steganography")
                put(MediaStore.Audio.Media.IS_MUSIC, 0) // not regular music
            }

            val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    sourceFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                callback.onProgress("✅ Saved to MediaStore Downloads (${sourceFile.extension.uppercase()})")
                Log.d(TAG, "Audio saved to MediaStore successfully")
                true
            } else {
                callback.onProgress("❌ Failed to save to MediaStore Downloads")
                Log.w(TAG, "MediaStore URI creation failed")
                false
            }
        } catch (e: Exception) {
            callback.onProgress("❌ MediaStore error: ${e.message}")
            Log.e(TAG, "MediaStore save error", e)
            false
        }
    }

    private fun saveAudioToExternalDownloads(sourceFile: File, fileName: String, callback: FileCallback): Boolean {
        return try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                callback.onProgress("❌ No write permission to external storage")
                Log.w(TAG, "Write external storage permission not granted")
                return false
            }

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val steganographyDir = File(downloadsDir, "Steganography")

            if (!steganographyDir.exists()) {
                val created = steganographyDir.mkdirs()
                callback.onProgress("📁 Folder creation: $created")
                Log.d(TAG, "Created steganography directory: $created")
            }

            val destinationFile = File(steganographyDir, fileName)
            sourceFile.copyTo(destinationFile, overwrite = true)

            if (destinationFile.exists() && destinationFile.length() > 0) {
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                mediaScanIntent.data = Uri.fromFile(destinationFile)
                context.sendBroadcast(mediaScanIntent)

                callback.onProgress("✅ Saved to ${destinationFile.absolutePath}")
                Log.d(TAG, "Audio saved to external downloads successfully")
                true
            } else {
                callback.onProgress("❌ File not created in Downloads")
                Log.e(TAG, "Destination file was not created or is empty")
                false
            }

        } catch (e: Exception) {
            callback.onProgress("❌ External Downloads error: ${e.message}")
            Log.e(TAG, "External downloads save error", e)
            false
        }
    }

    private fun saveAudioToMusic(sourceFile: File, fileName: String, callback: FileCallback): Boolean {
        return try {
            val mimeType = when (sourceFile.extension.lowercase()) {
                "mp3" -> "audio/mpeg"
                "m4a" -> "audio/mp4"
                "wav" -> "audio/wav"
                "aac" -> "audio/aac"
                else -> "audio/mpeg" // default
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Steganography")
                put(MediaStore.Audio.Media.IS_MUSIC, 1)
            }

            val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    sourceFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                callback.onProgress("✅ Saved to Music/Steganography (${sourceFile.extension.uppercase()})")
                Log.d(TAG, "Audio saved to Music folder successfully")
                true
            } else {
                callback.onProgress("❌ Failed to save to Music")
                Log.w(TAG, "Music folder URI creation failed")
                false
            }
        } catch (e: Exception) {
            callback.onProgress("❌ Music folder error: ${e.message}")
            Log.e(TAG, "Music folder save error", e)
            false
        }
    }

    private fun saveToAppExternalFiles(sourceFile: File, fileName: String, callback: FileCallback): Boolean {
        return try {
            val externalFilesDir = context.getExternalFilesDir(null)
            if (externalFilesDir != null) {
                val audioDir = File(externalFilesDir, "SteganographyAudio")
                if (!audioDir.exists()) {
                    audioDir.mkdirs()
                }

                val destinationFile = File(audioDir, fileName)
                sourceFile.copyTo(destinationFile, overwrite = true)

                if (destinationFile.exists() && destinationFile.length() > 0) {
                    callback.onProgress("✅ Saved to accessible folder: ${destinationFile.absolutePath}")
                    Log.d(TAG, "Audio saved to app external files successfully")
                    true
                } else {
                    Log.e(TAG, "App external files destination not created")
                    false
                }
            } else {
                Log.e(TAG, "External files directory is null")
                false
            }
        } catch (e: Exception) {
            callback.onProgress("❌ Accessible folder error: ${e.message}")
            Log.e(TAG, "App external files save error", e)
            false
        }
    }

    private fun saveImageToMediaStore(bitmap: Bitmap, fileName: String): Boolean {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Steganography")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val compressed = bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    if (!compressed) {
                        Log.e(TAG, "Bitmap compression failed")
                        return false
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)

                Log.d(TAG, "Image saved to MediaStore successfully")
                true
            } else {
                Log.e(TAG, "MediaStore image URI creation failed")
                false
            }

        } catch (e: Exception) {
            Log.e(TAG, "MediaStore image save error", e)
            false
        }
    }

    private fun saveImageToExternalStorage(bitmap: Bitmap, fileName: String): Boolean {
        return try {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val steganographyDir = File(picturesDir, "Steganography")

            if (!steganographyDir.exists()) {
                steganographyDir.mkdirs()
            }

            val imageFile = File(steganographyDir, fileName)
            val outputStream = FileOutputStream(imageFile)

            val compressed = bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            if (compressed) {
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                mediaScanIntent.data = Uri.fromFile(imageFile)
                context.sendBroadcast(mediaScanIntent)

                Log.d(TAG, "Image saved to external storage successfully")
                true
            } else {
                Log.e(TAG, "External storage image compression failed")
                false
            }

        } catch (e: Exception) {
            Log.e(TAG, "External storage image save error", e)
            false
        }
    }
}