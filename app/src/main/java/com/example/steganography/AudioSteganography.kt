package com.example.steganography

import android.util.Log
import java.io.File
import java.io.FileOutputStream

class AudioSteganography {

    companion object {
        private const val TAG = "AudioSteganography"
        private const val TEXT_DELIMITER = "###END###"
        private const val IMAGE_DELIMITER = "###IMG_END###"
        private const val AUDIO_DELIMITER = "###AUD_END###"
        private const val TEXT_MARKER = "STEG_TEXT:"
        private const val IMAGE_MARKER = "STEG_IMAGE:"
        private const val AUDIO_MARKER = "STEG_AUDIO:"
        private const val BOUNDARY_SEPARATOR = "\n---STEGANOGRAPHY_BOUNDARY---\n"
    }

    enum class DataType(val marker: String, val delimiter: String) {
        TEXT(TEXT_MARKER, TEXT_DELIMITER),
        IMAGE(IMAGE_MARKER, IMAGE_DELIMITER),
        AUDIO(AUDIO_MARKER, AUDIO_DELIMITER)
    }

    private fun hideDataInAudio(
        audioPath: String,
        dataToHide: ByteArray,
        outputPath: String,
        dataType: DataType,
        logContext: String
    ): Boolean {
        return try {
            Log.d(TAG, "Starting $logContext hiding in audio")
            Log.d(TAG, "Source file: $audioPath")
            Log.d(TAG, "Output file: $outputPath")

            val originalFile = File(audioPath)
            if (!originalFile.exists()) {
                Log.e(TAG, "Source file does not exist")
                return false
            }

            val originalBytes = originalFile.readBytes()
            Log.d(TAG, "Original file size: ${originalBytes.size} bytes")

            // Create hidden data
            val dataWithDelimiter = dataToHide + dataType.delimiter.toByteArray(Charsets.UTF_8)
            val hiddenData = dataType.marker.toByteArray(Charsets.UTF_8) + dataWithDelimiter

            Log.d(TAG, "Additional data size: ${hiddenData.size} bytes")

            // Create new file
            writeAudioWithHiddenData(originalBytes, hiddenData, outputPath)
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error hiding $logContext in audio", e)
            false
        }
    }

    private fun writeAudioWithHiddenData(originalBytes: ByteArray, hiddenData: ByteArray, outputPath: String) {
        val outputFile = File(outputPath)
        val outputStream = FileOutputStream(outputFile)

        try {
            outputStream.write(originalBytes)
            val separator = BOUNDARY_SEPARATOR.toByteArray(Charsets.UTF_8)
            outputStream.write(separator)
            outputStream.write(hiddenData)
            outputStream.flush()

            Log.d(TAG, "File created successfully: ${outputFile.length()} bytes")

        } finally {
            outputStream.close()
        }
    }

    fun hideTextInAudio(audioPath: String, text: String, outputPath: String): Boolean {
        Log.d(TAG, "Text: '$text'")
        val textBytes = text.toByteArray(Charsets.UTF_8)
        return hideDataInAudio(audioPath, textBytes, outputPath, DataType.TEXT, "text")
    }

    fun hideImageInAudio(audioPath: String, imagePath: String, outputPath: String): Boolean {
        Log.d(TAG, "Image to hide: $imagePath")

        val imageFile = File(imagePath)
        if (!imageFile.exists()) {
            Log.e(TAG, "Image file does not exist")
            return false
        }

        val imageBytes = imageFile.readBytes()
        Log.d(TAG, "Image size: ${imageBytes.size} bytes")

        return hideDataInAudio(audioPath, imageBytes, outputPath, DataType.IMAGE, "image")
    }

    fun hideAudioInAudio(coverAudioPath: String, secretAudioPath: String, outputPath: String): Boolean {
        Log.d(TAG, "Audio to hide: $secretAudioPath")

        val secretFile = File(secretAudioPath)
        if (!secretFile.exists()) {
            Log.e(TAG, "Secret audio file does not exist")
            return false
        }

        val secretBytes = secretFile.readBytes()
        Log.d(TAG, "Secret audio size: ${secretBytes.size} bytes")

        return hideDataInAudio(coverAudioPath, secretBytes, outputPath, DataType.AUDIO, "audio")
    }

    private fun extractDataFromAudio(audioPath: String, expectedType: DataType): ByteArray? {
        return try {
            Log.d(TAG, "Starting ${expectedType.name} extraction from audio")
            Log.d(TAG, "File: $audioPath")

            val file = File(audioPath)
            if (!file.exists()) {
                Log.e(TAG, "File does not exist")
                return null
            }

            val fileBytes = file.readBytes()
            Log.d(TAG, "File size: ${fileBytes.size} bytes")


            val fileString = String(fileBytes, Charsets.ISO_8859_1)
            val boundaryIndex = fileString.lastIndexOf(BOUNDARY_SEPARATOR)

            if (boundaryIndex == -1) {
                Log.w(TAG, "Steganography boundary not found")
                return null
            }

            Log.d(TAG, "Boundary found at index: $boundaryIndex")

            // Extract hidden data
            val hiddenDataStart = boundaryIndex + BOUNDARY_SEPARATOR.toByteArray(Charsets.UTF_8).size
            if (hiddenDataStart >= fileBytes.size) {
                Log.w(TAG, "No data after boundary")
                return null
            }

            val hiddenBytes = fileBytes.sliceArray(hiddenDataStart until fileBytes.size)

            val markerBytes = expectedType.marker.toByteArray(Charsets.UTF_8)
            if (hiddenBytes.size < markerBytes.size) {
                Log.w(TAG, "Hidden data too small")
                return null
            }

            val markerMatch = markerBytes.indices.all { i ->
                hiddenBytes[i] == markerBytes[i]
            }

            if (!markerMatch) {
                Log.w(TAG, "${expectedType.name} marker not found")
                return null
            }

            Log.d(TAG, "${expectedType.name} marker found")

            val dataWithDelimiter = hiddenBytes.sliceArray(markerBytes.size until hiddenBytes.size)
            val delimiterBytes = expectedType.delimiter.toByteArray(Charsets.UTF_8)

            var delimiterIndex = -1
            for (i in 0..dataWithDelimiter.size - delimiterBytes.size) {
                var found = true
                for (j in delimiterBytes.indices) {
                    if (dataWithDelimiter[i + j] != delimiterBytes[j]) {
                        found = false
                        break
                    }
                }
                if (found) {
                    delimiterIndex = i
                    break
                }
            }

            if (delimiterIndex == -1) {
                Log.w(TAG, "${expectedType.name} delimiter not found")
                return null
            }

            val extractedData = dataWithDelimiter.sliceArray(0 until delimiterIndex)
            Log.d(TAG, "Extracted data size: ${extractedData.size} bytes")
            Log.d(TAG, "${expectedType.name} found!")

            extractedData

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting ${expectedType.name} from audio", e)
            null
        }
    }

    fun extractTextFromAudio(audioPath: String): String? {
        val textBytes = extractDataFromAudio(audioPath, DataType.TEXT)
        return textBytes?.let {
            val result = String(it, Charsets.UTF_8)
            Log.d(TAG, "Text found: '$result'")
            result
        }
    }

    fun extractImageFromAudio(audioPath: String, outputImagePath: String): Boolean {
        val imageBytes = extractDataFromAudio(audioPath, DataType.IMAGE)
        return imageBytes?.let {
            try {
                val outputFile = File(outputImagePath)
                outputFile.writeBytes(it)
                Log.d(TAG, "Image saved successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error saving image", e)
                false
            }
        } ?: false
    }

    fun extractAudioFromAudio(audioPath: String, outputAudioPath: String): Boolean {
        val audioBytes = extractDataFromAudio(audioPath, DataType.AUDIO)
        return audioBytes?.let {
            try {
                val outputFile = File(outputAudioPath)
                outputFile.writeBytes(it)
                Log.d(TAG, "Audio saved successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error saving audio", e)
                false
            }
        } ?: false
    }

    fun hasHiddenData(audioPath: String): Boolean {
        return try {
            val file = File(audioPath)
            if (!file.exists()) return false

            val fileString = String(file.readBytes(), Charsets.ISO_8859_1)
            val hasData = fileString.contains(BOUNDARY_SEPARATOR) &&
                    (fileString.contains(TEXT_MARKER) || fileString.contains(IMAGE_MARKER) || fileString.contains(AUDIO_MARKER))

            Log.d(TAG, "Hidden data check for $audioPath: $hasData")
            hasData
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for hidden data", e)
            false
        }
    }
}