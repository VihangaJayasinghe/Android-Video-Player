package com.example.videosystem

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.videosystem.databinding.ActivityPlayerBinding
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.TrackGroup
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.util.MimeTypes

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var player: ExoPlayer
    private lateinit var trackSelector: DefaultTrackSelector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val videoPath = intent.getStringExtra("VIDEO_PATH")

        if (videoPath != null) {
            setupPlayer(videoPath)
        } else {
            finish()
        }
    }

    private fun setupPlayer(videoPath: String) {
        // Create track selector for quality control
        trackSelector = DefaultTrackSelector(this)
        
        // Create player instance with track selector
        player = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .build()

        // Bind player to view
        binding.playerView.player = player
        binding.playerView.keepScreenOn = true
        
        // Show/Hide controller settings for seeking
        binding.playerView.setShowBuffering(com.google.android.exoplayer2.ui.StyledPlayerView.SHOW_BUFFERING_ALWAYS)

        val uri = Uri.parse(videoPath)
        val mediaItem: MediaItem

        // Check if it's an HLS stream (m3u8)
        val isHls = videoPath.contains(".m3u8", ignoreCase = true)
        if (isHls) {
            mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build()
            
            // Show quality text for HLS streams
            binding.qualityTextView.visibility = View.VISIBLE
            binding.qualityTextView.setOnClickListener {
                showQualitySelectionDialog()
            }
        } else {
            mediaItem = MediaItem.fromUri(uri)
            binding.qualityTextView.visibility = View.GONE
        }

        // Set media item and prepare player
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true

        // Show simple back button
        binding.backButton.setOnClickListener {
            finish()
        }
    }
    
    private fun showQualitySelectionDialog() {
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo ?: return
        
        // Find video renderer index
        var videoRendererIndex = -1
        for (i in 0 until mappedTrackInfo.rendererCount) {
            if (player.getRendererType(i) == C.TRACK_TYPE_VIDEO) {
                videoRendererIndex = i
                break
            }
        }
        
        if (videoRendererIndex == -1) {
            Toast.makeText(this, "No video tracks found", Toast.LENGTH_SHORT).show()
            return
        }

        val trackGroups = mappedTrackInfo.getTrackGroups(videoRendererIndex)
        val qualityList = mutableListOf<String>()
        val trackIndices = mutableListOf<Pair<Int, Int>>() // GroupIndex, TrackIndex

        // Add "Auto" option
        qualityList.add("Auto")
        trackIndices.add(Pair(-1, -1))

        for (groupIndex in 0 until trackGroups.length) {
            val group = trackGroups.get(groupIndex)
            for (trackIndex in 0 until group.length) {
                if (mappedTrackInfo.getTrackSupport(videoRendererIndex, groupIndex, trackIndex) == C.FORMAT_HANDLED) {
                    val format = group.getFormat(trackIndex)
                    val height = format.height
                    val bitrate = format.bitrate / 1000
                    if (height != -1) {
                        qualityList.add("${height}p")
                        trackIndices.add(Pair(groupIndex, trackIndex))
                    }
                }
            }
        }

        if (qualityList.size <= 1) {
            Toast.makeText(this, "No quality options available", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Select Quality")
            .setItems(qualityList.toTypedArray()) { _, which ->
                val (groupIndex, trackIndex) = trackIndices[which]
                val selectedQuality = qualityList[which]
                
                binding.qualityTextView.text = selectedQuality
                
                if (groupIndex == -1) {
                    // Auto
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .clearSelectionOverrides(videoRendererIndex)
                    )
                } else {
                    // Specific resolution
                    val override = DefaultTrackSelector.SelectionOverride(groupIndex, trackIndex)
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setSelectionOverride(videoRendererIndex, trackGroups, override)
                    )
                }
            }
            .show()
    }

    override fun onPause() {
        super.onPause()
        if (::player.isInitialized) {
            player.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::player.isInitialized) {
            player.play()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::player.isInitialized) {
            player.release()
        }
    }
}