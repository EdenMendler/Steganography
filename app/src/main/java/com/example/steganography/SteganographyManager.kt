package com.example.steganography

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class SteganographyManager(private val context: Context) {

    private val imageSteganography = ImageSteganography()
    private val audioSteganography = AudioSteganography()

    interface SteganographyCallback {
        fun onSuccess(message: String, resultPath: String? = null)
        fun onError(error: String)
        fun onProgress(message: String)
    }

    enum class MediaType { IMAGE, AUDIO }
    enum class DataType { TEXT, IMAGE, AUDIO }

    var coverImagePath: String? = null
    var coverAudioPath: String? = null
    var resultImagePath: String? = null
    var resultAudioPath: String? = null
    var extractedAudioPath: String? = null
    var extractedImagePath: String? = null
    var extractedSecondaryAudioPath: String? = null

    companion object {
        private const val TAG = "SteganographyManager"
    }

    fun copyUriToInternalStorage(uri: Uri, fileName: String): String {
        val inputStream: InputStream = context.contentResolver.openInputStream(uri)!!
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)

        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()

        return file.absolutePath
    }

    private fun hideTextInMedia(
        mediaPath: String?,
        mediaType: MediaType,
        secretText: String,
        callback: SteganographyCallback
    ) {
        if (mediaPath == null) {
            val mediaName = if (mediaType == MediaType.IMAGE) "host image" else "host audio file"
            callback.onError("❌ Please select $mediaName first")
            return
        }

        if (secretText.isEmpty()) {
            callback.onError("❌ Please enter secret text")
            return
        }

        val mediaName = if (mediaType == MediaType.IMAGE) "image" else "audio"
        callback.onProgress("🔐 Hiding text in $mediaName...")
        Log.d(TAG, "Starting text hiding in $mediaName")

        if (mediaType == MediaType.AUDIO) {
            callback.onProgress("📋 Works with all audio types: MP3, M4A, WAV and more!")
        }

        try {
            val success = when (mediaType) {
                MediaType.IMAGE -> {
                    resultImagePath = "${context.filesDir}/image_with_text.png"
                    imageSteganography.hideTextInImage(mediaPath, secretText, resultImagePath!!)
                }
                MediaType.AUDIO -> {
                    val originalFile = File(mediaPath)
                    val fileExtension = originalFile.extension.lowercase()
                    val outputExtension = if (fileExtension.isNotEmpty()) fileExtension else "mp3"
                    resultAudioPath = "${context.filesDir}/audio_with_text.$outputExtension"
                    audioSteganography.hideTextInAudio(mediaPath, secretText, resultAudioPath!!)
                }
            }

            if (success) {
                val successMessage = when (mediaType) {
                    MediaType.IMAGE -> "✅ Text hidden successfully!\n💾 The image with hidden data is displayed on the right\n💾 Click 'Save Image to Gallery' to access from phone"
                    MediaType.AUDIO -> {
                        val ext = File(resultAudioPath!!).extension.uppercase()
                        "✅ Text hidden in audio successfully!\n🎵 Audio saved in original format ($ext)\n📁 Saved as: audio_with_text.$ext\n💾 Click 'Save Audio to Gallery' to access from phone\n🎯 Original audio preserved and playable!"
                    }
                }
                val resultPath = if (mediaType == MediaType.IMAGE) resultImagePath else null
                callback.onSuccess(successMessage, resultPath)
                Log.d(TAG, "Text hiding completed successfully")
            } else {
                callback.onError("❌ Error hiding text in $mediaName\n💡 Try with another file")
                Log.e(TAG, "Failed to hide text in $mediaName")
            }
        } catch (e: Exception) {
            callback.onError("❌ Error: ${e.message}\n💡 Try with another file")
            Log.e(TAG, "Exception during text hiding", e)
        }
    }

    private fun hideDataInMedia(
        coverPath: String?,
        coverType: MediaType,
        secretPath: String,
        secretType: DataType,
        callback: SteganographyCallback
    ) {
        if (coverPath == null) {
            val mediaName = if (coverType == MediaType.IMAGE) "host image" else "host audio file"
            callback.onError("❌ Please select $mediaName first")
            return
        }

        val secretTypeName = when (secretType) {
            DataType.IMAGE -> "image"
            DataType.AUDIO -> "audio"
            else -> "data"
        }
        val coverTypeName = if (coverType == MediaType.IMAGE) "image" else "audio"

        callback.onProgress("🔐 Hiding $secretTypeName in $coverTypeName...")
        Log.d(TAG, "Starting $secretTypeName hiding in $coverTypeName")

        try {
            val success = when (coverType) {
                MediaType.IMAGE -> {
                    resultImagePath = "${context.filesDir}/image_with_${secretTypeName.lowercase()}.png"
                    when (secretType) {
                        DataType.IMAGE -> imageSteganography.hideImageInImage(coverPath, secretPath, resultImagePath!!)
                        DataType.AUDIO -> imageSteganography.hideAudioInImage(coverPath, secretPath, resultImagePath!!)
                        else -> false
                    }
                }
                MediaType.AUDIO -> {
                    val originalFile = File(coverPath)
                    val fileExtension = originalFile.extension.lowercase()
                    val outputExtension = if (fileExtension.isNotEmpty()) fileExtension else "mp3"
                    resultAudioPath = "${context.filesDir}/audio_with_${secretTypeName.lowercase()}.$outputExtension"
                    when (secretType) {
                        DataType.IMAGE -> audioSteganography.hideImageInAudio(coverPath, secretPath, resultAudioPath!!)
                        DataType.AUDIO -> audioSteganography.hideAudioInAudio(coverPath, secretPath, resultAudioPath!!)
                        else -> false
                    }
                }
            }

            if (success) {
                val successMessage = when (coverType) {
                    MediaType.IMAGE -> "✅ The $secretTypeName hidden successfully!\n💾 The image with hidden data is displayed on the right\n💾 Click 'Save Image to Gallery' to access from phone"
                    MediaType.AUDIO -> {
                        val ext = File(resultAudioPath!!).extension.uppercase()
                        "✅ The $secretTypeName hidden in audio successfully!\n🎵 Audio saved in original format ($ext)\n📁 Saved as: audio_with_${secretTypeName.lowercase()}.$ext\n💾 Click 'Save Audio to Gallery' to access from phone\n🎯 Original audio preserved and playable!"
                    }
                }
                val resultPath = if (coverType == MediaType.IMAGE) resultImagePath else null
                callback.onSuccess(successMessage, resultPath)
                Log.d(TAG, "$secretTypeName hiding completed successfully")
            } else {
                callback.onError("❌ Error hiding $secretTypeName in $coverTypeName\n💡 Try with other files")
                Log.e(TAG, "Failed to hide $secretTypeName in $coverTypeName")
            }
        } catch (e: Exception) {
            callback.onError("❌ Error: ${e.message}\n💡 Try with other files")
            Log.e(TAG, "Exception during data hiding", e)
        }
    }

    private fun extractFromMedia(mediaPath: String, mediaType: MediaType, callback: SteganographyCallback) {
        val mediaName = if (mediaType == MediaType.IMAGE) "image" else "audio"
        callback.onProgress("🔍 Trying to extract data from $mediaName...")
        Log.d(TAG, "Starting data extraction from $mediaName")

        if (mediaType == MediaType.AUDIO) {
            callback.onProgress("📋 Works with all audio types!")
        }

        try {
            val results = mutableListOf<String>()

            val extractedText = when (mediaType) {
                MediaType.IMAGE -> imageSteganography.extractTextFromImage(mediaPath)
                MediaType.AUDIO -> audioSteganography.extractTextFromAudio(mediaPath)
            }

            if (!extractedText.isNullOrEmpty()) {
                results.add("✅ Text found!")
                val textLabel = if (mediaType == MediaType.AUDIO) "Hidden text" else "Text"
                results.add("📝 $textLabel: \"$extractedText\"")
                Log.d(TAG, "Text extracted successfully")
            }

            val extractedImagePath = "${context.filesDir}/extracted_image_from_${mediaName.lowercase()}.jpg"
            val imageSuccess = when (mediaType) {
                MediaType.IMAGE -> imageSteganography.extractImageFromImage(mediaPath, extractedImagePath)
                MediaType.AUDIO -> audioSteganography.extractImageFromAudio(mediaPath, extractedImagePath)
            }

            if (imageSuccess && File(extractedImagePath).exists()) {
                results.add("✅ Image extracted!")
                results.add("💾 Click 'Save Image to Gallery' to view it")
                resultImagePath = extractedImagePath
                this.extractedImagePath = extractedImagePath
                Log.d(TAG, "Image extracted successfully")
            }

            val extractedAudioPath = "${context.filesDir}/extracted_audio_from_${mediaName.lowercase()}.wav"
            val audioSuccess = when (mediaType) {
                MediaType.IMAGE -> imageSteganography.extractAudioFromImage(mediaPath, extractedAudioPath)
                MediaType.AUDIO -> audioSteganography.extractAudioFromAudio(mediaPath, extractedAudioPath)
            }

            if (audioSuccess && File(extractedAudioPath).exists()) {
                results.add("✅ Audio extracted!")
                results.add("🎵 Click 'Play Extracted Audio' button to listen")
                this.extractedAudioPath = extractedAudioPath
                if (mediaType == MediaType.AUDIO) {
                    this.extractedSecondaryAudioPath = extractedAudioPath
                }
                Log.d(TAG, "Audio extracted successfully")
            }

            if (results.isEmpty()) {
                if (mediaType == MediaType.AUDIO) {
                    val hasHidden = audioSteganography.hasHiddenData(mediaPath)
                    if (hasHidden) {
                        callback.onError("⚠️ Hidden data found but cannot extract it\n💡 File might be corrupted or data damaged")
                        Log.w(TAG, "Hidden data detected but extraction failed")
                    } else {
                        callback.onError("❌ No hidden data found in this file\nℹ️ Make sure you selected a file with hidden data\n💡 Try another file")
                        Log.d(TAG, "No hidden data found in audio file")
                    }
                } else {
                    callback.onError("❌ No hidden information found in $mediaName")
                    Log.d(TAG, "No hidden data found in image file")
                }
            } else {
                val message = results.joinToString("\n") +
                        if (mediaType == MediaType.AUDIO) "\n🎯 Extraction completed successfully!" else ""
                callback.onSuccess(message, if (imageSuccess) extractedImagePath else null)
                Log.d(TAG, "Data extraction completed successfully")
            }
        } catch (e: Exception) {
            val errorMsg = if (mediaType == MediaType.AUDIO) {
                "❌ Extraction error: ${e.message}\n💡 Try another audio file or check if file is valid"
            } else {
                "❌ Extraction error: ${e.message}"
            }
            callback.onError(errorMsg)
            Log.e(TAG, "Exception during data extraction", e)
        }
    }

    fun hideTextInImage(secretText: String, callback: SteganographyCallback) {
        hideTextInMedia(coverImagePath, MediaType.IMAGE, secretText, callback)
    }

    fun hideTextInAudio(secretText: String, callback: SteganographyCallback) {
        hideTextInMedia(coverAudioPath, MediaType.AUDIO, secretText, callback)
    }

    fun hideImageInImage(secretImagePath: String, callback: SteganographyCallback) {
        hideDataInMedia(coverImagePath, MediaType.IMAGE, secretImagePath, DataType.IMAGE, callback)
    }

    fun hideImageInAudio(secretImagePath: String, callback: SteganographyCallback) {
        hideDataInMedia(coverAudioPath, MediaType.AUDIO, secretImagePath, DataType.IMAGE, callback)
    }

    fun hideAudioInImage(secretAudioPath: String, callback: SteganographyCallback) {
        hideDataInMedia(coverImagePath, MediaType.IMAGE, secretAudioPath, DataType.AUDIO, callback)
    }

    fun hideAudioInAudio(secretAudioPath: String, callback: SteganographyCallback) {
        hideDataInMedia(coverAudioPath, MediaType.AUDIO, secretAudioPath, DataType.AUDIO, callback)
    }

    fun extractFromImage(imagePath: String, callback: SteganographyCallback) {
        extractFromMedia(imagePath, MediaType.IMAGE, callback)
    }

    fun extractFromAudio(audioPath: String, callback: SteganographyCallback) {
        extractFromMedia(audioPath, MediaType.AUDIO, callback)
    }
}