package com.example.videosystem

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.videosystem.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: VideoDatabaseHelper
    private lateinit var videoAdapter: VideoAdapter
    private var pendingVideoPath: String? = null

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = VideoDatabaseHelper(this)

        setupRecyclerView()

        binding.addVideoButton.setOnClickListener {
            val intent = Intent(this, AddVideoActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadVideos()
    }

    private fun setupRecyclerView() {
        videoAdapter = VideoAdapter(emptyList()) { video ->
            checkPermissionsAndPlay(video.source)
        }
        binding.videoRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.videoRecyclerView.adapter = videoAdapter
    }

    private fun loadVideos() {
        val videos = dbHelper.getAllVideos()
        if (videos.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            binding.videoRecyclerView.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.videoRecyclerView.visibility = View.VISIBLE
            videoAdapter.updateData(videos)
        }
    }

    private fun checkPermissionsAndPlay(videoPath: String) {
        pendingVideoPath = videoPath

        // Check for URL (http/https) - no storage permissions needed for internet
        if (videoPath.startsWith("http://") || videoPath.startsWith("https://")) {
            playVideo(videoPath)
            return
        }

        // Check for storage permissions for local files
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED) {
                playVideo(videoPath)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_VIDEO), PERMISSION_REQUEST_CODE)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                playVideo(videoPath)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pendingVideoPath?.let { playVideo(it) }
            } else {
                Toast.makeText(this, "Permission denied. Cannot play local file.", Toast.LENGTH_SHORT).show()
            }
            pendingVideoPath = null
        }
    }

    private fun playVideo(videoPath: String) {
        val intent = Intent(this, PlayerActivity::class.java)
        intent.putExtra("VIDEO_PATH", videoPath)
        startActivity(intent)
    }
}