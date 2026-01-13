package com.example.mobile_course.fragments

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat

import androidx.fragment.app.Fragment
import com.example.mobile_course.databinding.CameraFragmentBinding
import androidx.navigation.fragment.findNavController
import com.example.mobile_course.R
import com.example.mobile_course.databinding.VideoFragmentBinding
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.Locale


class VideoFragment : Fragment() {

    companion object {
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()


    }
    private fun allPermissionGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext().applicationContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }
    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (allPermissionGranted()) {
            startCamera()
        } else {
            Toast.makeText(
                requireContext(),
                "Permissions not granted by the user.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    private val FILENAME_FORMAT = "yyyy.mm.dd"
    private var currentRecording: Recording? = null
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraSelector: CameraSelector
    private var videoCapture: VideoCapture<Recorder>? = null
    private var _binding: VideoFragmentBinding? = null
    private val binding: VideoFragmentBinding
        get() = _binding ?: throw RuntimeException()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = VideoFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        _binding = VideoFragmentBinding.bind(view)
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        binding.takeVideoButton.setOnClickListener {
                startRecording()
        }
        binding.goToCamera.setOnClickListener {
            findNavController().navigate(VideoFragmentDirections.actionVideoFragmentToCameraFragment())
        }

        binding.galleryButton.setOnClickListener { view -> findNavController().navigate(
            VideoFragmentDirections.actionVideoFragmentToGalleryFragment()) }

        binding.changeCameraButton.setOnClickListener {
            switchCamera()
        }

        if (allPermissionGranted()) {
            startCamera()
        } else {
            requestPermissions.launch(REQUIRED_PERMISSIONS)
        }

    }


    private fun switchCamera(){
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        startCamera()
    }
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.preview.surfaceProvider)
            }

            if (videoCapture == null) {
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                    .build()
                videoCapture = VideoCapture.withOutput(recorder)
            }

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture
                )

                binding.preview.setOnTouchListener { _, event ->
                    val meteringPoint = binding.preview.meteringPointFactory
                        .createPoint(event.x, event.y)
                    val action = FocusMeteringAction.Builder(meteringPoint).build()
                    camera.cameraControl.startFocusAndMetering(action)
                    true
                }


                val cameraInfo = camera.cameraInfo
                val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        val currentZoomRatio = cameraInfo.zoomState.value?.zoomRatio ?: 0F

                        val delta = detector.scaleFactor

                        camera.cameraControl.setZoomRatio(currentZoomRatio * delta)
                        return true
                    }
                }

                val scaleGestureDetector = ScaleGestureDetector(requireContext(), listener)
                binding.preview.setOnTouchListener { _, event ->
                    scaleGestureDetector.onTouchEvent(event)
                    true
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun startRecording(){
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val name = "Video-" + SimpleDateFormat(FILENAME_FORMAT, Locale.ENGLISH)
            .format(System.currentTimeMillis()) + ".mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH,
                Environment.DIRECTORY_MOVIES + "/MyVideos")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val mediaStoreOutput = MediaStoreOutputOptions
            .Builder(requireContext().contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()


        videoCapture?.let  {
            currentRecording = it.output
                .prepareRecording(requireContext(), mediaStoreOutput)
                .withAudioEnabled()
                .asPersistentRecording()
                .start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
                    val chronometer = binding.recordingTime
                    when(recordEvent) {
                        is VideoRecordEvent.Start -> {

                            binding.takeVideoButton.setOnClickListener {
                                currentRecording?.stop()
                            }
                            binding.takeVideoButton.icon = context?.let {
                            ResourcesCompat.getDrawable(it.resources, R.drawable.stop_recording, it.theme)
                            }

                            binding.takeVideoButton.iconSize = requireContext().resources.getDimensionPixelSize(R.dimen.stop_size)

                            chronometer.base = SystemClock.elapsedRealtime()
                            chronometer.start()
                            chronometer.visibility = View.VISIBLE
                        }
                        is VideoRecordEvent.Finalize -> {
                            chronometer.stop()
                            chronometer.visibility = View.INVISIBLE

                            binding.takeVideoButton.setOnClickListener {
                                startRecording()
                            }
                            binding.takeVideoButton.icon = context?.let {
                            ResourcesCompat.getDrawable(it.resources, R.drawable.start_recording, it.theme)
                            }
                            binding.takeVideoButton.iconSize = requireContext().resources.getDimensionPixelSize(R.dimen.start_size)

                        }
                    }
                }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
    }


}