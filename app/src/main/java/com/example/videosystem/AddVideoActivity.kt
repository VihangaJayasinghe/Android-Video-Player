package com.example.videosystem

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.videosystem.databinding.ActivityAddVideoBinding

class AddVideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddVideoBinding
    private lateinit var dbHelper: VideoDatabaseHelper
    private var selectedVideoUri: Uri? = null
    private var isUploading = false

    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                selectedVideoUri = uri
                val fileName = getFileName(uri)
                binding.selectedPathTextView.text = "Selected: $fileName"
                if (binding.nameEditText.text.isNullOrEmpty()) {
                    binding.nameEditText.setText(fileName)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = VideoDatabaseHelper(this)
        
        initCloudinary()

        binding.selectFileButton.setOnClickListener {
            if (!isUploading) openFilePicker()
        }

        binding.uploadButton.setOnClickListener {
            if (!isUploading) uploadToCloudinary()
        }
    }
    
    private fun initCloudinary() {
        try {
            MediaManager.get()
        } catch (e: Exception) {
            try {
                val config = HashMap<String, String>()
                config["cloud_name"] = BuildConfig.CLOUDINARY_CLOUD_NAME
                config["api_key"] = BuildConfig.CLOUDINARY_API_KEY
                config["api_secret"] = BuildConfig.CLOUDINARY_API_SECRET
                MediaManager.init(this, config)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
        }
        pickVideoLauncher.launch(intent)
    }

    private fun uploadToCloudinary() {
        val name = binding.nameEditText.text.toString().trim()
        
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedVideoUri == null) {
            Toast.makeText(this, "Please select a video file", Toast.LENGTH_SHORT).show()
            return
        }
        
        isUploading = true
        binding.progressContainer.visibility = View.VISIBLE
        binding.uploadButton.isEnabled = false
        binding.selectFileButton.isEnabled = false
        binding.uploadStatusText.text = "Starting upload..."
        binding.uploadProgressBar.progress = 0

        val uploadPreset = BuildConfig.CLOUDINARY_UPLOAD_PRESET
        if (uploadPreset.isEmpty() || uploadPreset == "ml_default") {
             Toast.makeText(this, "Warning: Using default upload preset 'ml_default'. Make sure this exists in Cloudinary.", Toast.LENGTH_LONG).show()
        }
        
        MediaManager.get().upload(selectedVideoUri)
            .unsigned(uploadPreset)
            .option("resource_type", "video")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    runOnUiThread {
                         binding.uploadStatusText.text = "Uploading... 0%"
                    }
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    val progress = (bytes.toDouble() / totalBytes * 100).toInt()
                    runOnUiThread {
                        binding.uploadProgressBar.progress = progress
                        binding.uploadStatusText.text = "Uploading... $progress%"
                    }
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    runOnUiThread {
                        binding.uploadStatusText.text = "Processing..."
                        binding.uploadProgressBar.progress = 100
                        
                        val secureUrl = resultData["secure_url"] as String
                        
                        // Generate HLS URL manually (just like your Spring Boot service)
                        val hlsUrl = generateSimpleHLSURL(secureUrl)
                        
                        saveVideoToDb(name, hlsUrl)
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    runOnUiThread {
                        isUploading = false
                        binding.uploadButton.isEnabled = true
                        binding.selectFileButton.isEnabled = true
                        binding.uploadStatusText.text = "Error: ${error.description}"
                        Toast.makeText(this@AddVideoActivity, "Upload failed: ${error.description}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                }
            })
            .dispatch()
    }

    private fun generateSimpleHLSURL(secureUrl: String): String {
        // Replace /upload/ with /upload/sp_auto/ and .mp4 with .m3u8
        return secureUrl.replace("/upload/", "/upload/sp_auto/").replace(".mp4", ".m3u8")
    }
    
    private fun saveVideoToDb(name: String, url: String) {
        val result = dbHelper.addVideo(name, url)
        
        if (result != -1L) {
            Toast.makeText(this, "Video added successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        } else {
            isUploading = false
            binding.uploadButton.isEnabled = true
            binding.selectFileButton.isEnabled = true
            Toast.makeText(this, "Failed to save video to database", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "unknown_video"
    }
}