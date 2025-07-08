# 🔒 Steganography

## Project Overview 📱

Advanced Android application for hiding and extracting data using steganography techniques. The app allows users to hide text, images, and audio files within other images and audio files, making the hidden data completely invisible to the naked eye.

## Key Features 🎯

* **Multi-format data hiding**: Hide text, images, and audio in both image and audio files
* **Smart extraction**: Automatically detect and extract hidden data from files
* **File management**: Save processed files to device gallery with proper organization
* **User-friendly interface**: Simple, intuitive design with real-time feedback
* **Format preservation**: Maintain original file quality and format
* **Local processing**: All operations performed on-device for maximum privacy

## Architecture Structure 🏗️

The project consists of 6 main files:

### Core Components:
1. **MainActivity.kt** - Main UI controller and user interaction handler
2. **SteganographyManager.kt** - Central coordinator for all steganography operations
3. **ImageSteganography.kt** - Image file manipulation and data embedding
4. **AudioSteganography.kt** - Audio file manipulation and data embedding
5. **FileManager.kt** - File operations and Android storage integration
6. **activity_main.xml** - UI layout

## How to Use 📖

### Hiding Data:

1. **Select Host File**
   - Tap "Host Image" or "Host Audio" 
   - Choose your cover file from device storage

2. **Choose What to Hide**
   - **Text**: Enter message in text field, tap "Hide Text"
   - **Image**: Tap "Hide Image", select secret image file
   - **Audio**: Tap "Hide Audio", select secret audio file

3. **Save Result**
   - Tap "Save Image to Gallery" or "Save Audio to Gallery"
   - File saved with hidden data embedded

### Extracting Hidden Data:

1. **Select File with Hidden Data**
   - Tap "Extract from Image" or "Extract from Audio"
   - Choose file containing hidden information

2. **View Results**
   - Text appears in results log
   - Images display automatically 
   - Audio files can be played with "Play Extracted Audio"

3. **Save Extracted Files**
   - Use save buttons to store extracted content

## File Format Support 📁

### Input Formats:
* **Images**: JPG, PNG, BMP, WEBP
* **Audio**: MP3, M4A, WAV, AAC

### Output Formats:
* **Images**: PNG (lossless quality)
* **Audio**: Original format preserved

## How It Works 🔬

### Image Steganography (LSB Method):
The app hides data in the **least significant bits** of image pixels. Since the human eye can't detect small changes in color values, we can store information without visible changes.

```
Original pixel: RGB(120, 200, 150) = Binary: 01111000, 11001000, 10010110
Hidden bit: 1
Modified pixel: RGB(120, 200, 151) = Binary: 01111000, 11001000, 10010111
                                                                        ↑
                                                               Hidden bit stored here
```

### Audio Steganography (Metadata Method):
For audio files, hidden data is appended after the original audio content with special boundary markers, keeping the audio playable and unmodified.

```
[Original Audio Data] → [Boundary Marker] → [Hidden Data] → [End Marker]
```

### Data Extraction Process:
1. **Detection**: App scans for boundary markers or LSB (Least Significant Bit) patterns
2. **Reading**: Extracts hidden bits/data from their storage locations  
3. **Reconstruction**: Converts binary data back to original format (text/image/audio)
4. **Validation**: Verifies data integrity using end markers

## Privacy & Security 🛡️

* ✅ **100% Local Processing** - No internet connection required
* ✅ **No Data Transmission** - Everything stays on your device
* ✅ **Invisible Hiding** - Hidden data undetectable by standard viewers
* ✅ **Format Integrity** - Original file structure maintained
* ✅ **Temporary File Cleanup** - No traces left on device

## Performance Optimization 🚀

* **Background Processing**: UI remains responsive during operations
* **Memory Management**: Efficient bitmap handling and recycling
* **File Size Optimization**: Automatic compression for large images
* **Stream Processing**: Efficient handling of large audio files

## 📚 Project Architecture

The project follows a component-based architecture for better maintainability:

```
com.example.steganography/
├── MainActivity.kt                   # Main activity and UI controller
├── SteganographyManager.kt           # Central coordinator for operations
├── FileManager.kt                    # File operations and storage handling
├── ImageSteganography.kt             # Image processing and LSB algorithms
├── AudioSteganography.kt             # Audio file manipulation and embedding
└── res/
    └── layout/
        └── activity_main.xml         # Main UI layout and components
```

---