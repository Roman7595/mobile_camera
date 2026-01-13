package com.example.mobile_course.fragments

import android.content.ContentUris
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.lab1.extensions.debugging
import com.example.mobile_course.GalleryAdapter
import com.example.mobile_course.MediaFile
import com.example.mobile_course.databinding.GalleryFragmentBinding


class GalleryFragment : Fragment() {
    private var _binding: GalleryFragmentBinding ? = null

    private val binding: GalleryFragmentBinding
        get() = _binding ?: throw RuntimeException()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = GalleryFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val files = mutableListOf<MediaFile>()
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED)

        requireContext().contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            imageProjection,
            null, null, null
        )?.use { cursor ->

            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)

            while (cursor.moveToNext()) {
                val name =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME
                    ))
                debugging("MediaStore: Found image: $name")
                val mediaId = cursor.getLong(idColumn)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    mediaId
                )
                val creationDate = cursor.getString((cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED
                )))
                files.add(MediaFile(uri.toString(), "image", creationDate))
            }
        }

        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED)

        requireContext().contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            videoProjection,
            null, null, null
        )?.use { cursor ->

            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)

            while (cursor.moveToNext()) {
                val name =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME
                    ))
                debugging("MediaStore: Found image: $name")
                val mediaId = cursor.getLong(idColumn)

                val uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    mediaId
                )
                val creationDate = cursor.getString((cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED
                )))


                files.add(MediaFile(uri.toString(), "video", creationDate))
            }
        }


        files.reverse()
        val adapter = GalleryAdapter(files){
            clickedItem ->
            val action = GalleryFragmentDirections.actionGalleryFragmentToImageFragment(
                MEDIA = clickedItem)
            findNavController().navigate(action)
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}