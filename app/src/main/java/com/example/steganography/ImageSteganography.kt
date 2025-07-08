package com.example.steganography

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class ImageSteganography {

    companion object {
        private const val TAG = "ImageSteganography"
        private const val DELIMITER = "###END###"
        private const val HEADER_SIZE = 32 // Header size in bits
    }

    enum class DataType {
        TEXT, IMAGE, AUDIO
    }

    fun hideTextInImage(imagePath: String, text: String, outputPath: String): Boolean {
        return try {
            Log.d(TAG, "Starting text hiding")
            Log.d(TAG, "Source file: $imagePath")
            Log.d(TAG, "Text: '$text'")
            Log.d(TAG, "Output file: $outputPath")

            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap == null) {
                Log.e(TAG, "Cannot load image")
                return false
            }

            Log.d(TAG, "Image size: ${bitmap.width}x${bitmap.height}")

            val messageWithDelimiter = text + DELIMITER
            val binaryMessage = stringToBinary(messageWithDelimiter)

            Log.d(TAG, "Binary message size: ${binaryMessage.length} bits")
            Log.d(TAG, "Available pixels: ${bitmap.width * bitmap.height}")

            if (binaryMessage.length > bitmap.width * bitmap.height) {
                Log.e(TAG, "Text too large for image")
                return false
            }

            val modifiedBitmap = embedBinaryDataInImage(bitmap, binaryMessage)
            val success = saveBitmapToFile(modifiedBitmap, outputPath)

            Log.d(TAG, "Text hiding completed: $success")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding text", e)
            false
        }
    }

    fun extractTextFromImage(imagePath: String): String? {
        return try {
            Log.d(TAG, "Starting text extraction")
            Log.d(TAG, "File: $imagePath")

            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap == null) {
                Log.e(TAG, "Cannot load image for extraction")
                return null
            }

            Log.d(TAG, "Image size for extraction: ${bitmap.width}x${bitmap.height}")

            val binaryData = extractBinaryDataFromImage(bitmap)
            Log.d(TAG, "Extracted ${binaryData.length} bits")
            Log.d(TAG, "Binary data start: ${binaryData.take(64)}")

            val text = binaryToString(binaryData)
            Log.d(TAG, "Raw text: '${text.take(50)}...'")

            if (text.contains(DELIMITER)) {
                val result = text.substring(0, text.indexOf(DELIMITER))
                Log.d(TAG, "Text found: '$result'")
                result
            } else {
                Log.w(TAG, "Delimiter not found in text")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text", e)
            null
        }
    }

    private fun hideBinaryDataInImage(
        coverImagePath: String,
        dataBytes: ByteArray,
        outputPath: String,
        dataType: DataType,
        logContext: String
    ): Boolean {
        return try {
            Log.d(TAG, "Starting $logContext hiding in image")
            Log.d(TAG, "Cover image: $coverImagePath")
            Log.d(TAG, "Output file: $outputPath")

            val coverBitmap = BitmapFactory.decodeFile(coverImagePath)
            if (coverBitmap == null) {
                Log.e(TAG, "Cannot load cover image")
                return false
            }

            Log.d(TAG, "Cover image: ${coverBitmap.width}x${coverBitmap.height}")
            Log.d(TAG, "Data size: ${dataBytes.size} bytes")

            val availablePixels = coverBitmap.width * coverBitmap.height
            val maxBitsForData = (availablePixels * 0.9).toInt() - HEADER_SIZE
            val maxBytesForData = maxBitsForData / 8

            Log.d(TAG, "Available pixels: $availablePixels")
            Log.d(TAG, "Maximum bytes for data: $maxBytesForData")

            var finalDataBytes = dataBytes

            if (dataType == DataType.IMAGE && dataBytes.size > maxBytesForData) {
                finalDataBytes = compressImageIfNeeded(dataBytes, maxBytesForData)
                if (finalDataBytes.size > maxBytesForData) {
                    Log.e(TAG, "Cannot compress enough - data too large")
                    return false
                }
            } else if (dataBytes.size > maxBytesForData) {
                Log.e(TAG, "Data too large for image")
                return false
            }

            val binaryData = byteArrayToBinary(finalDataBytes)
            Log.d(TAG, "Binary size: ${binaryData.length} bits")


            val headerBinary = createBinaryHeader(binaryData.length)
            val dataWithHeader = headerBinary + binaryData
            Log.d(TAG, "Total size with header: ${dataWithHeader.length} bits")

            if (dataWithHeader.length > availablePixels) {
                Log.e(TAG, "Data still too large after compression")
                return false
            }

            val modifiedBitmap = embedBinaryDataInImage(coverBitmap, dataWithHeader)
            val success = saveBitmapToFile(modifiedBitmap, outputPath)

            Log.d(TAG, "$logContext hiding completed: $success")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error hiding $logContext", e)
            false
        }
    }

    private fun extractBinaryDataFromImageGeneric(
        imagePath: String,
        outputPath: String,
        dataType: DataType,
        logContext: String
    ): Boolean {
        return try {
            Log.d(TAG, "Starting $logContext extraction")
            Log.d(TAG, "Source file: $imagePath")
            Log.d(TAG, "Output file: $outputPath")

            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap == null) {
                Log.e(TAG, "Cannot load image for extraction")
                return false
            }

            Log.d(TAG, "Image size: ${bitmap.width}x${bitmap.height}")

            // Extract binary header
            val headerBinary = extractBinaryDataFromImage(bitmap, HEADER_SIZE)
            val dataSize = parseBinaryHeader(headerBinary)

            if (dataSize <= 0 || dataSize > bitmap.width * bitmap.height - HEADER_SIZE) {
                Log.e(TAG, "Invalid data size: $dataSize")
                return false
            }

            Log.d(TAG, "Expected data size: $dataSize bits")

            // Extract data
            val binaryData = extractBinaryDataFromImage(bitmap, dataSize, HEADER_SIZE)
            val dataBytes = binaryToByteArray(binaryData)

            Log.d(TAG, "Converted to ${dataBytes.size} bytes")

            // Handle by data type
            when (dataType) {
                DataType.IMAGE -> {
                    val extractedBitmap = BitmapFactory.decodeByteArray(dataBytes, 0, dataBytes.size)
                    if (extractedBitmap == null) {
                        Log.e(TAG, "Cannot create bitmap from extracted data")
                        return false
                    }
                    Log.d(TAG, "Image extracted: ${extractedBitmap.width}x${extractedBitmap.height}")
                    saveBitmapToFile(extractedBitmap, outputPath)
                }
                DataType.AUDIO -> {
                    File(outputPath).writeBytes(dataBytes)
                    Log.d(TAG, "Audio file saved")
                }
                else -> {
                    File(outputPath).writeBytes(dataBytes)
                }
            }

            Log.d(TAG, "$logContext extraction completed")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting $logContext", e)
            false
        }
    }

    fun hideImageInImage(coverImagePath: String, secretImagePath: String, outputPath: String): Boolean {
        val imageBytes = File(secretImagePath).readBytes()
        return hideBinaryDataInImage(coverImagePath, imageBytes, outputPath, DataType.IMAGE, "image")
    }

    fun extractImageFromImage(imagePath: String, outputPath: String): Boolean {
        return extractBinaryDataFromImageGeneric(imagePath, outputPath, DataType.IMAGE, "image")
    }

    fun hideAudioInImage(imagePath: String, audioPath: String, outputPath: String): Boolean {
        val audioBytes = File(audioPath).readBytes()
        return hideBinaryDataInImage(imagePath, audioBytes, outputPath, DataType.AUDIO, "audio")
    }

    fun extractAudioFromImage(imagePath: String, outputPath: String): Boolean {
        return extractBinaryDataFromImageGeneric(imagePath, outputPath, DataType.AUDIO, "audio")
    }

    private fun stringToBinary(text: String): String {
        return text.toByteArray().joinToString("") { byte ->
            String.format("%8s", Integer.toBinaryString(byte.toInt() and 0xFF)).replace(' ', '0')
        }
    }

    private fun binaryToString(binary: String): String {
        val bytes = mutableListOf<Byte>()
        for (i in binary.indices step 8) {
            if (i + 8 <= binary.length) {
                val byteString = binary.substring(i, i + 8)
                val byte = Integer.parseInt(byteString, 2).toByte()
                bytes.add(byte)
            }
        }
        return String(bytes.toByteArray(), Charsets.UTF_8)
    }

    private fun byteArrayToBinary(bytes: ByteArray): String {
        return bytes.joinToString("") { byte ->
            String.format("%8s", Integer.toBinaryString(byte.toInt() and 0xFF)).replace(' ', '0')
        }
    }

    private fun binaryToByteArray(binary: String): ByteArray {
        val bytes = mutableListOf<Byte>()
        for (i in binary.indices step 8) {
            if (i + 8 <= binary.length) {
                val byteString = binary.substring(i, i + 8)
                val byte = Integer.parseInt(byteString, 2).toByte()
                bytes.add(byte)
            }
        }
        return bytes.toByteArray()
    }

    private fun createBinaryHeader(dataSize: Int): String {
        return String.format("%32s", Integer.toBinaryString(dataSize)).replace(' ', '0')
    }

    private fun parseBinaryHeader(headerBinary: String): Int {
        return try {
            Integer.parseInt(headerBinary, 2)
        } catch (e: NumberFormatException) {
            Log.e(TAG, "Cannot convert binary header to number: '$headerBinary'")
            -1
        }
    }

    private fun compressImageIfNeeded(imageBytes: ByteArray, maxSize: Int): ByteArray {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            if (bitmap == null) return imageBytes

            var quality = 70
            var compressedBytes = imageBytes

            while (compressedBytes.size > maxSize && quality > 10) {
                val stream = java.io.ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                compressedBytes = stream.toByteArray()
                stream.close()
                quality -= 20
                Log.d(TAG, "Compressed at quality $quality: ${compressedBytes.size} bytes")
            }

            compressedBytes
        } catch (e: Exception) {
            Log.e(TAG, "Compression error", e)
            imageBytes
        }
    }

    private fun embedBinaryDataInImage(bitmap: Bitmap, binaryData: String): Bitmap {
        Log.d(TAG, "Starting data embedding in image")
        Log.d(TAG, "Data size to embed: ${binaryData.length} bits")

        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        var dataIndex = 0

        outer@ for (y in 0 until mutableBitmap.height) {
            for (x in 0 until mutableBitmap.width) {
                if (dataIndex >= binaryData.length) break@outer

                val pixel = mutableBitmap.getPixel(x, y)
                val red = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val blue = pixel and 0xFF
                val alpha = (pixel shr 24) and 0xFF

                val newBlue = if (binaryData[dataIndex] == '1') {
                    blue or 1
                } else {
                    blue and 0xFE
                }

                val newPixel = (alpha shl 24) or (red shl 16) or (green shl 8) or newBlue
                mutableBitmap.setPixel(x, y, newPixel)
                dataIndex++
            }
        }

        Log.d(TAG, "Embedding completed, embedded $dataIndex bits")
        return mutableBitmap
    }

    private fun extractBinaryDataFromImage(bitmap: Bitmap, maxLength: Int = -1, offset: Int = 0): String {
        val binaryData = StringBuilder()
        var extractedBits = 0
        val targetLength = if (maxLength == -1) bitmap.width * bitmap.height else maxLength

        outer@ for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                if (extractedBits >= targetLength + offset) break@outer

                val pixel = bitmap.getPixel(x, y)
                val blue = pixel and 0xFF

                if (extractedBits >= offset) {
                    binaryData.append(blue and 1)
                }
                extractedBits++
            }
        }

        Log.d(TAG, "Extraction completed, extracted ${binaryData.length} bits")
        return binaryData.toString()
    }

    private fun saveBitmapToFile(bitmap: Bitmap, filePath: String): Boolean {
        return try {
            val file = File(filePath)
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
            Log.d(TAG, "File saved: $filePath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving file", e)
            false
        }
    }
}