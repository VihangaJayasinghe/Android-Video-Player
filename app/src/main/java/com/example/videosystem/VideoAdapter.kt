package com.example.videosystem

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VideoAdapter(
    private var videoList: List<Video>,
    private val onItemClick: (Video) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.videoNameTextView)
        val sourceTextView: TextView = view.findViewById(R.id.videoSourceTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videoList[position]
        holder.nameTextView.text = video.name
        holder.sourceTextView.text = video.source
        
        holder.itemView.setOnClickListener {
            onItemClick(video)
        }
    }

    override fun getItemCount() = videoList.size
    
    fun updateData(newVideos: List<Video>) {
        videoList = newVideos
        notifyDataSetChanged()
    }
}