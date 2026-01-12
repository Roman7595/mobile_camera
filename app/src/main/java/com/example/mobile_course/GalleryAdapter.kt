package com.example.mobile_course

import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mobile_course.databinding.GridItemBinding
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone
import kotlin.time.Instant

class GalleryAdapter(private val list: List<MediaFile>,
                     private val onItemClicked: (MediaFile) -> Unit) :
    RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {
    class ViewHolder(private val binding: GridItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val info : TextView = binding.textView
        val image : ImageView = binding.imageView

        fun bind(uri: Uri) {
            Glide.with(binding.root).load(uri).into(binding.imageView)
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(GridItemBinding.inflate(layoutInflater, parent, false))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mediaFile = list.get(position)
        val uri = mediaFile.uri.toUri()

        val creationTime = mediaFile.creationDate

        val calendar = GregorianCalendar.getInstance(TimeZone.getDefault())
        calendar.timeInMillis = mediaFile.creationDate.toLong()*1000
        val year = calendar.get(Calendar.YEAR)
        val month = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val day = calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')

        val info = "type: ${mediaFile.type} \n date: ${day}.${month}.${year}"
        holder.info.text = info

        holder.bind(uri)
        holder.image.setOnClickListener {
            onItemClicked(mediaFile)
        }

    }

    override fun getItemCount(): Int {
        return list.size
    }

}