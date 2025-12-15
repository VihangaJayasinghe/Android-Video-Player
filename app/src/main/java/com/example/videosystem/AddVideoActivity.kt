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
import com.example.videosystem.databinding.ActivityAddVideoBinding

class AddVideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddVideoBinding
    private lateinit var dbHelper: VideoDatabaseHelper
    private var selectedVideoUri: Uri? = null

    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                selectedVideoUri = uri
                
                // Try to get file name from URI
                val fileName = getFileName(uri)
                binding.selectedPathTextView.text = "Selected: $fileName"
                
                // Auto-fill name if empty
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

        setupInputTypeToggle()

        binding.selectFileButton.setOnClickListener {
            openFilePicker()
        }

        binding.addButton.setOnClickListener {
            addVideoToDb()
        }
    }

    private fun setupInputTypeToggle() {
        binding.sourceTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radioLocal) {
                binding.localFileContainer.visibility = View.VISIBLE
                binding.onlineUrlContainer.visibility = View.GONE
            } else {
                binding.localFileContainer.visibility = View.GONE
                binding.onlineUrlContainer.visibility = View.VISIBLE
            }
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
            // Persist permission to read this file later
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        }
        pickVideoLauncher.launch(intent)
    }

    private fun addVideoToDb() {
        val name = binding.nameEditText.text.toString().trim()
        
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
            return
        }

        val isLocal = binding.radioLocal.isChecked
        var finalSource = ""

        if (isLocal) {
            if (selectedVideoUri == null) {
                Toast.makeText(this, "Please select a video file", Toast.LENGTH_SHORT).show()
                return
            }

            // We need to persist permissions for this URI so we can access it later after restart
            try {
                contentResolver.takePersistableUriPermission(
                    selectedVideoUri!!,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
            finalSource = selectedVideoUri.toString()
        } else {
            val url = binding.urlEditText.text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(this, "Please enter a video URL", Toast.LENGTH_SHORT).show()
                return
            }
            finalSource = url
        }

        val result = dbHelper.addVideo(name, finalSource)
        
        if (result != -1L) {
            Toast.makeText(this, "Video added successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Failed to add video", Toast.LENGTH_SHORT).show()
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