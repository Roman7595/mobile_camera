package com.example.mobile_course.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.mobile_course.MediaFile
import com.example.mobile_course.databinding.ImageFragmentBinding
import kotlin.getValue

class ImageFragment : Fragment() {

    private var _binding: ImageFragmentBinding ? = null

    private val binding: ImageFragmentBinding
        get() = _binding ?: throw RuntimeException()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ImageFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args : ImageFragmentArgs by navArgs()
        val mediaFile : MediaFile? = args.MEDIA

        if (mediaFile !=null){

            if (mediaFile.type == "image"){
                binding.localImg.visibility = View.VISIBLE
                binding.videoView.visibility = View.GONE

                binding.localImg.setImageURI(mediaFile.uri.toUri())
            }else if (mediaFile.type == "video"){
                binding.localImg.visibility = View.GONE
                binding.videoView.visibility = View.VISIBLE

                binding.videoView.setMediaController(MediaController(requireContext()))
                binding.videoView.setVideoURI(mediaFile.uri.toUri())
                binding.videoView.requestFocus()
                binding.videoView.start()

            }

            binding.deleteButton.setOnClickListener {
                requireContext().contentResolver.delete(mediaFile.uri.toUri(),null,null)
                findNavController().popBackStack()

            }

            val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {

                    val delta = (detector.scaleFactor)

                    binding.localImg.scaleX *= delta
                    binding.localImg.scaleY *= delta

                    return true
                }
            }


            val scaleGestureDetector = ScaleGestureDetector(requireContext(), listener)
            binding.localImg.setOnTouchListener { _, event ->
                scaleGestureDetector.onTouchEvent(event)
                true
            }
        }

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

    }

    override fun onStart() {
        super.onStart()



    }
}