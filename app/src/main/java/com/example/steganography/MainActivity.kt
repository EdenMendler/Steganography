package com.example.steganography

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    // System managers
    private lateinit var steganographyManager: SteganographyManager
    private lateinit var fileManager: FileManager

    // Background thread executor
    private val backgroundExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    // UI Elements
    private lateinit var btnSelectCoverImage: Button
    private lateinit var btnSelectCoverAudio: Button
    private lateinit var etSecretText: EditText

    // Image hiding buttons
    private lateinit var btnHideTextInImage: Button
    private lateinit var btnHideImageInImage: Button
    private lateinit var btnHideAudioInImage: Button

    // Audio hiding buttons
    private lateinit var btnHideTextInAudio: Button
    private lateinit var btnHideImageInAudio: Button
    private lateinit var btnHideAudioInAudio: Button

    // Extraction buttons
    private lateinit var btnExtractFromImage: Button
    private lateinit var btnExtractFromAudio: Button

    private lateinit var tvResults: TextView
    private lateinit var ivOriginalImage: ImageView
    private lateinit var ivResultImage: ImageView
    private lateinit var scrollResults: ScrollView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSaveImage: Button
    private lateinit var btnSaveAudio: Button
    private lateinit var btnPlayAudio: Button
    private lateinit var btnClearResults: Button
    private lateinit var btnScrollToBottom: Button

    // Layout groups
    private lateinit var tvHidingTitle: TextView
    private lateinit var layoutImageHiding: LinearLayout
    private lateinit var layoutAudioHiding: LinearLayout
    private lateinit var tvExtractionTitle: TextView
    private lateinit var layoutExtraction: LinearLayout
    private lateinit var tvSaveTitle: TextView
    private lateinit var layoutSaveButtons: LinearLayout
    private var mediaPlayer: MediaPlayer? = null

    private var autoScrollEnabled = true

    enum class MediaType { IMAGE, AUDIO }
    enum class DataType { TEXT, IMAGE, AUDIO }

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 100
        private const val SELECT_COVER_IMAGE = 1001
        private const val SELECT_COVER_AUDIO = 1002
        private const val SELECT_SECRET_IMAGE_FOR_HIDING = 1003
        private const val SELECT_SECRET_AUDIO_FOR_HIDING = 1004
        private const val SELECT_SECRET_IMAGE_FOR_AUDIO = 1005
        private const val SELECT_SECRET_AUDIO_FOR_AUDIO = 1006
        private const val SELECT_RESULT_IMAGE = 1007
        private const val SELECT_RESULT_AUDIO = 1008
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        steganographyManager = SteganographyManager(this)
        fileManager = FileManager(this)

        initViews()
        setupClickListeners()
        setupScrollBehavior()

        if (checkAndRequestPermissions()) {
            createDemoFiles()
        }

        Log.d(TAG, "MainActivity created and initialized")
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissionsNeeded = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            // Android 12 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            // Write permission - required only until Android 9
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                    permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }

        return if (permissionsNeeded.isNotEmpty()) {
            Log.d(TAG, "Requesting permissions: $permissionsNeeded")
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), PERMISSION_REQUEST_CODE)
            false
        } else {
            Log.d(TAG, "All permissions already granted")
            true
        }
    }

    private fun initViews() {
        btnSelectCoverImage = findViewById(R.id.btnSelectCoverImage)
        btnSelectCoverAudio = findViewById(R.id.btnSelectCoverAudio)
        etSecretText = findViewById(R.id.etSecretText)

        // Image hiding buttons
        btnHideTextInImage = findViewById(R.id.btnHideTextInImage)
        btnHideImageInImage = findViewById(R.id.btnHideImageInImage)
        btnHideAudioInImage = findViewById(R.id.btnHideAudioInImage)

        // Audio hiding buttons
        btnHideTextInAudio = findViewById(R.id.btnHideTextInAudio)
        btnHideImageInAudio = findViewById(R.id.btnHideImageInAudio)
        btnHideAudioInAudio = findViewById(R.id.btnHideAudioInAudio)

        // Extraction buttons
        btnExtractFromImage = findViewById(R.id.btnExtractFromImage)
        btnExtractFromAudio = findViewById(R.id.btnExtractFromAudio)

        tvResults = findViewById(R.id.tvResults)
        ivOriginalImage = findViewById(R.id.ivOriginalImage)
        ivResultImage = findViewById(R.id.ivResultImage)
        scrollResults = findViewById(R.id.scrollResults)
        btnSaveImage = findViewById(R.id.btnSaveImage)
        btnSaveAudio = findViewById(R.id.btnSaveAudio)
        btnPlayAudio = findViewById(R.id.btnPlayAudio)
        btnClearResults = findViewById(R.id.btnClearResults)
        btnScrollToBottom = findViewById(R.id.btnScrollToBottom)

        tvHidingTitle = findViewById(R.id.tvHidingTitle)
        layoutImageHiding = findViewById(R.id.layoutImageHiding)
        layoutAudioHiding = findViewById(R.id.layoutAudioHiding)
        tvExtractionTitle = findViewById(R.id.tvExtractionTitle)
        layoutExtraction = findViewById(R.id.layoutExtraction)
        tvSaveTitle = findViewById(R.id.tvSaveTitle)
        layoutSaveButtons = findViewById(R.id.layoutSaveButtons)

        progressBar = ProgressBar(this).apply {
            visibility = ProgressBar.GONE
        }

        Log.d(TAG, "All views initialized")
    }

    private fun setupScrollBehavior() {
        // Scroll to bottom button
        btnScrollToBottom.setOnClickListener {
            autoScrollEnabled = true
            scrollResults.post {
                scrollResults.fullScroll(ScrollView.FOCUS_DOWN)
            }
            Log.d(TAG, "Scrolled to bottom")
        }

        btnClearResults.setOnClickListener {
            tvResults.text = ""
            addToResults("🔄 Results cleared")
            Log.d(TAG, "Results cleared")
        }

        scrollResults.viewTreeObserver.addOnScrollChangedListener {
            val view = scrollResults
            val child = view.getChildAt(0)

            if (child != null) {
                val isAtBottom = (view.scrollY + view.height) >= child.height - 10
                autoScrollEnabled = isAtBottom
            }
        }
    }

    private fun setupClickListeners() {
        btnSelectCoverImage.setOnClickListener { selectFile(SELECT_COVER_IMAGE, "image/*") }
        btnSelectCoverAudio.setOnClickListener { selectFile(SELECT_COVER_AUDIO, "audio/*") }

        // Image hiding buttons
        btnHideTextInImage.setOnClickListener { hideTextAsync(MediaType.IMAGE) }
        btnHideImageInImage.setOnClickListener { selectSecretFileAndHide(DataType.IMAGE, MediaType.IMAGE) }
        btnHideAudioInImage.setOnClickListener { selectSecretFileAndHide(DataType.AUDIO, MediaType.IMAGE) }

        // Audio hiding buttons
        btnHideTextInAudio.setOnClickListener { hideTextAsync(MediaType.AUDIO) }
        btnHideImageInAudio.setOnClickListener { selectSecretFileAndHide(DataType.IMAGE, MediaType.AUDIO) }
        btnHideAudioInAudio.setOnClickListener { selectSecretFileAndHide(DataType.AUDIO, MediaType.AUDIO) }

        // Extraction buttons
        btnExtractFromImage.setOnClickListener { selectFile(SELECT_RESULT_IMAGE, "image/*") }
        btnExtractFromAudio.setOnClickListener { selectFile(SELECT_RESULT_AUDIO, "audio/*") }

        btnSaveImage.setOnClickListener { saveFileToStorage(MediaType.IMAGE) }
        btnSaveAudio.setOnClickListener { saveFileToStorage(MediaType.AUDIO) }
        btnPlayAudio.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                stopAudio()
            } else {
                playExtractedAudio()
            }
        }

        Log.d(TAG, "Click listeners setup completed")
    }

    private fun selectFile(requestCode: Int, mimeType: String) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = mimeType
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, "Select File"), requestCode)
        Log.d(TAG, "File selection started with requestCode: $requestCode, mimeType: $mimeType")
    }

    private fun selectSecretFileAndHide(secretType: DataType, coverType: MediaType) {
        val coverPath = if (coverType == MediaType.IMAGE) steganographyManager.coverImagePath else steganographyManager.coverAudioPath

        if (coverPath == null) {
            val mediaName = if (coverType == MediaType.IMAGE) "host image" else "host audio file"
            addToResults("❌ Please select $mediaName first")
            Log.w(TAG, "No cover media selected for hiding operation")
            return
        }

        val secretTypeName = when (secretType) {
            DataType.IMAGE -> "image"
            DataType.AUDIO -> "audio"
            else -> "file"
        }
        val coverTypeName = if (coverType == MediaType.IMAGE) "image" else "audio"
        val mimeType = if (secretType == DataType.IMAGE) "image/*" else "audio/*"

        addToResults("📋 Select secret $secretTypeName to hide in $coverTypeName")

        val requestCode = when {
            coverType == MediaType.IMAGE && secretType == DataType.IMAGE -> SELECT_SECRET_IMAGE_FOR_HIDING
            coverType == MediaType.IMAGE && secretType == DataType.AUDIO -> SELECT_SECRET_AUDIO_FOR_HIDING
            coverType == MediaType.AUDIO && secretType == DataType.IMAGE -> SELECT_SECRET_IMAGE_FOR_AUDIO
            coverType == MediaType.AUDIO && secretType == DataType.AUDIO -> SELECT_SECRET_AUDIO_FOR_AUDIO
            else -> SELECT_SECRET_IMAGE_FOR_HIDING
        }

        selectFile(requestCode, mimeType)
        Log.d(TAG, "Secret file selection initiated: $secretTypeName in $coverTypeName")
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data?.data != null) {
            val uri = data.data!!
            Log.d(TAG, "File selected successfully for requestCode: $requestCode")

            when (requestCode) {
                SELECT_COVER_IMAGE -> handleCoverSelection(uri, MediaType.IMAGE)
                SELECT_COVER_AUDIO -> handleCoverSelection(uri, MediaType.AUDIO)
                SELECT_SECRET_IMAGE_FOR_HIDING -> handleSecretSelection(uri, "secret_image.jpg", DataType.IMAGE, MediaType.IMAGE)
                SELECT_SECRET_AUDIO_FOR_HIDING -> handleSecretSelection(uri, "secret_audio.wav", DataType.AUDIO, MediaType.IMAGE)
                SELECT_SECRET_IMAGE_FOR_AUDIO -> handleSecretSelection(uri, "secret_image_for_audio.jpg", DataType.IMAGE, MediaType.AUDIO)
                SELECT_SECRET_AUDIO_FOR_AUDIO -> handleSecretSelection(uri, "secret_audio_for_audio.wav", DataType.AUDIO, MediaType.AUDIO)
                SELECT_RESULT_IMAGE -> {
                    val imagePath = steganographyManager.copyUriToInternalStorage(uri, "result_for_extraction.png")
                    extractDataAsync(imagePath, MediaType.IMAGE)
                }
                SELECT_RESULT_AUDIO -> {
                    val audioPath = steganographyManager.copyUriToInternalStorage(uri, "result_for_extraction.wav")
                    extractDataAsync(audioPath, MediaType.AUDIO)
                }
            }
        } else {
            Log.w(TAG, "File selection cancelled or failed")
        }
    }

    private fun handleCoverSelection(uri: android.net.Uri, mediaType: MediaType) {
        if (mediaType == MediaType.IMAGE) {
            showProgress(true)
            backgroundExecutor.execute {
                try {
                    val path = steganographyManager.copyUriToInternalStorage(uri, "cover_image.jpg")
                    mainHandler.post {
                        steganographyManager.coverImagePath = path
                        steganographyManager.coverAudioPath = null // Reset host audio
                        displayImage(ivOriginalImage, steganographyManager.coverImagePath)
                        addToResults("✅ Host image selected")
                        btnSelectCoverImage.text = "Host Image ✓"
                        btnSelectCoverAudio.text = "Host Audio"
                        updateButtonsVisibility()
                        showProgress(false)
                    }
                    Log.d(TAG, "Cover image loaded successfully")
                } catch (e: Exception) {
                    mainHandler.post {
                        addToResults("❌ Error loading image: ${e.message}")
                        showProgress(false)
                    }
                    Log.e(TAG, "Error loading cover image", e)
                }
            }
        } else {
            steganographyManager.coverAudioPath = steganographyManager.copyUriToInternalStorage(uri, "cover_audio.wav")
            steganographyManager.coverImagePath = null // Reset host image
            addToResults("✅ Host audio file selected")
            btnSelectCoverAudio.text = "Host Audio ✓"
            btnSelectCoverImage.text = "Host Image"
            ivOriginalImage.setImageBitmap(null)
            updateButtonsVisibility()
            Log.d(TAG, "Cover audio loaded successfully")
        }
    }

    private fun handleSecretSelection(uri: android.net.Uri, fileName: String, secretType: DataType, coverType: MediaType) {
        showProgress(true)
        backgroundExecutor.execute {
            try {
                val secretPath = steganographyManager.copyUriToInternalStorage(uri, fileName)
                val secretTypeName = when (secretType) {
                    DataType.IMAGE -> "image"
                    DataType.AUDIO -> "audio"
                    else -> "file"
                }

                mainHandler.post {
                    addToResults("📋 Secret $secretTypeName selected, performing hiding...")
                }
                hideDataAsync(secretPath, coverType, secretType)
                Log.d(TAG, "Secret file processed: $secretTypeName")
            } catch (e: Exception) {
                mainHandler.post {
                    addToResults("❌ Error: ${e.message}")
                    showProgress(false)
                }
                Log.e(TAG, "Error processing secret file", e)
            }
        }
    }

    private fun updateButtonsVisibility() {
        when {
            steganographyManager.coverImagePath != null -> showImageMode()
            steganographyManager.coverAudioPath != null -> showAudioMode()
            else -> hideAllModes()
        }
    }

    private fun showImageMode() {
        // Show image hiding buttons
        tvHidingTitle.visibility = TextView.VISIBLE
        layoutImageHiding.visibility = LinearLayout.VISIBLE
        layoutAudioHiding.visibility = LinearLayout.GONE

        // Show extraction buttons
        tvExtractionTitle.visibility = TextView.VISIBLE
        layoutExtraction.visibility = LinearLayout.VISIBLE

        // Show image save button
        tvSaveTitle.visibility = TextView.VISIBLE
        layoutSaveButtons.visibility = LinearLayout.VISIBLE
        btnSaveImage.visibility = Button.VISIBLE
        btnSaveAudio.visibility = Button.GONE

        addToResults("📋 Mode: Host image - can hide text, image and audio")
        Log.d(TAG, "Switched to image mode")
    }

    private fun showAudioMode() {
        // Show audio hiding buttons
        tvHidingTitle.visibility = TextView.VISIBLE
        layoutImageHiding.visibility = LinearLayout.GONE
        layoutAudioHiding.visibility = LinearLayout.VISIBLE

        // Show extraction buttons
        tvExtractionTitle.visibility = TextView.VISIBLE
        layoutExtraction.visibility = LinearLayout.VISIBLE

        // Show audio save button
        tvSaveTitle.visibility = TextView.VISIBLE
        layoutSaveButtons.visibility = LinearLayout.VISIBLE
        btnSaveImage.visibility = Button.GONE
        btnSaveAudio.visibility = Button.VISIBLE

        addToResults("📋 Mode: Host audio - can hide text, image and audio")
        Log.d(TAG, "Switched to audio mode")
    }

    private fun hideAllModes() {
        tvHidingTitle.visibility = TextView.GONE
        layoutImageHiding.visibility = LinearLayout.GONE
        layoutAudioHiding.visibility = LinearLayout.GONE
        tvExtractionTitle.visibility = TextView.GONE
        layoutExtraction.visibility = LinearLayout.GONE
        tvSaveTitle.visibility = TextView.GONE
        layoutSaveButtons.visibility = LinearLayout.GONE
        btnSaveImage.visibility = Button.GONE
        btnSaveAudio.visibility = Button.GONE
        Log.d(TAG, "All modes hidden")
    }

    private fun displayImage(imageView: ImageView, imagePath: String?) {
        if (imagePath != null && File(imagePath).exists()) {
            try {
                val options = BitmapFactory.Options()
                options.inSampleSize = 2
                val bitmap = BitmapFactory.decodeFile(imagePath, options)
                imageView.setImageBitmap(bitmap)
                Log.d(TAG, "Image displayed successfully")
            } catch (e: Exception) {
                addToResults("⚠️ Error displaying image: ${e.message}")
                Log.e(TAG, "Error displaying image", e)
            }
        }
    }

    private fun showProgress(show: Boolean) {
        runOnUiThread {
            setButtonsEnabled(!show)
            if (show) {
                addToResults("⏳ Processing...")
            }
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        btnHideTextInImage.isEnabled = enabled
        btnHideImageInImage.isEnabled = enabled
        btnHideAudioInImage.isEnabled = enabled
        btnHideTextInAudio.isEnabled = enabled
        btnHideImageInAudio.isEnabled = enabled
        btnHideAudioInAudio.isEnabled = enabled
        btnExtractFromImage.isEnabled = enabled
        btnExtractFromAudio.isEnabled = enabled
        btnSaveImage.isEnabled = enabled
        btnSaveAudio.isEnabled = enabled
        btnPlayAudio.isEnabled = enabled
    }

    private fun hideTextAsync(mediaType: MediaType) {
        val secretText = etSecretText.text.toString()
        showProgress(true)
        Log.d(TAG, "Starting text hiding in ${mediaType.name}")

        backgroundExecutor.execute {
            val callback = createSteganographyCallback(mediaType == MediaType.IMAGE)

            when (mediaType) {
                MediaType.IMAGE -> steganographyManager.hideTextInImage(secretText, callback)
                MediaType.AUDIO -> steganographyManager.hideTextInAudio(secretText, callback)
            }
        }
    }

    private fun hideDataAsync(secretPath: String, coverType: MediaType, secretType: DataType) {
        val callback = createSteganographyCallback(coverType == MediaType.IMAGE)
        Log.d(TAG, "Starting data hiding: ${secretType.name} in ${coverType.name}")

        when (coverType to secretType) {
            MediaType.IMAGE to DataType.IMAGE -> steganographyManager.hideImageInImage(secretPath, callback)
            MediaType.IMAGE to DataType.AUDIO -> steganographyManager.hideAudioInImage(secretPath, callback)
            MediaType.AUDIO to DataType.IMAGE -> steganographyManager.hideImageInAudio(secretPath, callback)
            MediaType.AUDIO to DataType.AUDIO -> steganographyManager.hideAudioInAudio(secretPath, callback)
        }
    }

    private fun extractDataAsync(filePath: String?, mediaType: MediaType) {
        if (filePath == null) return

        showProgress(true)
        btnPlayAudio.visibility = Button.GONE
        Log.d(TAG, "Starting data extraction from ${mediaType.name}")

        backgroundExecutor.execute {
            val callback = object : SteganographyManager.SteganographyCallback {
                override fun onSuccess(message: String, resultPath: String?) {
                    mainHandler.post {
                        addToResults(message)
                        resultPath?.let { displayImage(ivResultImage, it) }

                        // If audio extracted, show play button
                        if (steganographyManager.extractedAudioPath != null) {
                            btnPlayAudio.visibility = Button.VISIBLE
                        }

                        showProgress(false)
                    }
                }

                override fun onError(error: String) {
                    mainHandler.post {
                        addToResults(error)
                        showProgress(false)
                    }
                }

                override fun onProgress(message: String) {
                    mainHandler.post { addToResults(message) }
                }
            }

            when (mediaType) {
                MediaType.IMAGE -> steganographyManager.extractFromImage(filePath, callback)
                MediaType.AUDIO -> steganographyManager.extractFromAudio(filePath, callback)
            }
        }
    }

    private fun saveFileToStorage(mediaType: MediaType) {
        val fileToSave = when (mediaType) {
            MediaType.IMAGE -> steganographyManager.resultImagePath ?: steganographyManager.extractedImagePath
            MediaType.AUDIO -> steganographyManager.resultAudioPath ?: steganographyManager.extractedAudioPath
        }

        if (fileToSave == null || !File(fileToSave).exists()) {
            val fileTypeName = if (mediaType == MediaType.IMAGE) "image" else "audio file"
            addToResults("❌ No $fileTypeName to save - perform hiding or extraction first")
            Log.w(TAG, "No file to save for media type: ${mediaType.name}")
            return
        }

        showProgress(true)
        Log.d(TAG, "Starting file save for ${mediaType.name}")

        backgroundExecutor.execute {
            val callback = object : FileManager.FileCallback {
                override fun onSuccess(message: String) {
                    mainHandler.post {
                        addToResults(message)
                        if (mediaType == MediaType.IMAGE && fileToSave.contains("extracted")) {
                            addToResults("🎯 This is the image extracted from hiding!")
                        } else if (mediaType == MediaType.IMAGE) {
                            addToResults("🔐 This is the image with hidden data")
                        }
                        showProgress(false)
                    }
                }

                override fun onError(error: String) {
                    mainHandler.post {
                        addToResults(error)
                        showProgress(false)
                    }
                }

                override fun onProgress(message: String) {
                    mainHandler.post { addToResults(message) }
                }
            }

            when (mediaType) {
                MediaType.IMAGE -> fileManager.saveImageToGallery(fileToSave, callback)
                MediaType.AUDIO -> fileManager.saveAudioToDownloads(fileToSave, callback)
            }
        }
    }

    private fun createSteganographyCallback(isImageResult: Boolean): SteganographyManager.SteganographyCallback {
        return object : SteganographyManager.SteganographyCallback {
            override fun onSuccess(message: String, resultPath: String?) {
                mainHandler.post {
                    addToResults(message)
                    if (isImageResult) {
                        resultPath?.let { displayImage(ivResultImage, it) }
                    }
                    showProgress(false)
                }
            }

            override fun onError(error: String) {
                mainHandler.post {
                    addToResults(error)
                    showProgress(false)
                }
            }

            override fun onProgress(message: String) {
                mainHandler.post { addToResults(message) }
            }
        }
    }

    private fun playExtractedAudio() {
        if (steganographyManager.extractedAudioPath == null || !File(steganographyManager.extractedAudioPath!!).exists()) {
            addToResults("❌ No audio file to play - extract audio first")
            Log.w(TAG, "No extracted audio file available for playback")
            return
        }

        try {
            stopAudio()
            addToResults("🎵 Starting audio playback...")
            Log.d(TAG, "Starting audio playback")

            mediaPlayer = MediaPlayer().apply {
                setDataSource(steganographyManager.extractedAudioPath!!)
                prepareAsync()

                setOnPreparedListener {
                    start()
                    addToResults("▶️ Audio is now playing...")
                    btnPlayAudio.text = "⏹️ Stop Audio"
                    Log.d(TAG, "Audio playback started")
                }

                setOnCompletionListener {
                    addToResults("✅ Audio finished")
                    btnPlayAudio.text = "🎵 Play Extracted Audio"
                    releaseMediaPlayer()
                    Log.d(TAG, "Audio playback completed")
                }

                setOnErrorListener { _, what, extra ->
                    addToResults("❌ Audio playback error: $what, $extra")
                    btnPlayAudio.text = "🎵 Play Extracted Audio"
                    releaseMediaPlayer()
                    Log.e(TAG, "Audio playback error: what=$what, extra=$extra")
                    true
                }
            }

        } catch (e: Exception) {
            addToResults("❌ Audio playback error: ${e.message}")
            btnPlayAudio.text = "🎵 Play Extracted Audio"
            releaseMediaPlayer()
            Log.e(TAG, "Exception during audio playback", e)
        }
    }

    private fun stopAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
                addToResults("⏹️ Audio playback stopped")
                Log.d(TAG, "Audio playback stopped")
            }
            releaseMediaPlayer()
        }
        btnPlayAudio.text = "🎵 Play Extracted Audio"
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
        Log.d(TAG, "MediaPlayer released")
    }

    private fun addToResults(text: String) {
        runOnUiThread {
            tvResults.append("$text\n")
            if (autoScrollEnabled) {
                scrollResults.post {
                    scrollResults.fullScroll(ScrollView.FOCUS_DOWN)
                }
            }
        }
    }

    private fun createDemoFiles() {
        addToResults("🎯 Welcome to Advanced Steganography System!")
        addToResults("📋 Instructions:")
        addToResults("1. Select image or audio host")
        addToResults("2. Buttons will appear according to your choice:")
        addToResults("   📸 Image host: hide text/image/audio")
        addToResults("   🎵 Audio host: hide text/image/audio")
        addToResults("3. Choose what you want to hide:")
        addToResults("   🔤 Text: Enter text in field and click 'Hide Text'")
        addToResults("   🖼️ Image: Click 'Hide Image' and select file")
        addToResults("   🎵 Audio: Click 'Hide Audio' and select file")
        addToResults("4. Extract data from existing files:")
        addToResults("   • Click 'Extract from Image' or 'Extract from Audio'")
        addToResults("   • Select file containing hidden data")
        addToResults("5. Save results to gallery or system folders")
        addToResults("💡 Scroll tips:")
        addToResults("   • ⬇️ Down arrow button returns to bottom")
        addToResults("   • 🗑️ Trash button clears results")
        addToResults("════════════════════════")
        Log.d(TAG, "Demo instructions displayed")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                addToResults("✅ Permissions granted")
                createDemoFiles()
                Log.d(TAG, "All permissions granted")
            } else {
                Toast.makeText(this, "Permissions required for app operation", Toast.LENGTH_LONG).show()
                addToResults("❌ Permissions not granted - some functions may not work")
                Log.w(TAG, "Permissions denied")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundExecutor.shutdown()
        releaseMediaPlayer()
        Log.d(TAG, "MainActivity destroyed, resources cleaned up")
    }
}