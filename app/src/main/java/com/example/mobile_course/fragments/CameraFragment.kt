package com.example.mobile_course.fragments

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat

import androidx.fragment.app.Fragment
import com.example.mobile_course.databinding.CameraFragmentBinding
import androidx.core.graphics.drawable.toDrawable
import androidx.navigation.fragment.findNavController
import com.example.lab1.extensions.debugging
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraFragment : Fragment() {

    private val REQUIRED_PERMISSIONS = mutableListOf(
        Manifest.permission.CAMERA
    ).apply {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }.toTypedArray()


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

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraSelector: CameraSelector
    private var imageCapture: ImageCapture? = null
    private lateinit var imageCaptureExecutor: ExecutorService


    private var _binding: CameraFragmentBinding? = null
    private val binding: CameraFragmentBinding
        get() = _binding ?: throw RuntimeException()

    val projection = arrayOf(MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATE_ADDED)


    private fun allPermissionGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext().applicationContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CameraFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        _binding = CameraFragmentBinding.bind(view)
        imageCaptureExecutor = Executors.newSingleThreadExecutor()
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        binding.takeImageButton.setOnClickListener {
            takePhoto()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                animateFlash()
            }
        }

        binding.galleryButton.setOnClickListener { view -> findNavController().navigate(
            CameraFragmentDirections.actionCameraFragmentToGalleryFragment()) }

        binding.goToVideo.setOnClickListener { view -> findNavController().navigate(
            CameraFragmentDirections.actionCameraFragmentToVideoFragment()) }

        binding.changeCameraButton.setOnClickListener {
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            startCamera()
        }

        if (allPermissionGranted()) {
            startCamera()
        } else {
            requestPermissions.launch(REQUIRED_PERMISSIONS)
        }


    }
    @RequiresApi(Build.VERSION_CODES.M)
    private fun animateFlash() {
        binding.root.postDelayed({
            binding.root.foreground = Color.WHITE.toDrawable()
            binding.root.postDelayed({
                binding.root.foreground = null
            }, 50)
        }, 100)
    }

    private fun startCamera() {

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.preview.surfaceProvider)
        }
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            imageCapture = ImageCapture.Builder().build()


            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                val cameraControl = camera.cameraControl

                binding.preview.setOnTouchListener { _, event ->
                    val meteringPoint = binding.preview.meteringPointFactory
                        .createPoint(event.x, event.y)
                    val action = FocusMeteringAction.Builder(meteringPoint).build()
                    cameraControl.startFocusAndMetering(action)
                    true
                }


                val cameraInfo = camera.cameraInfo
                val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        val currentZoomRatio = cameraInfo.zoomState.value?.zoomRatio ?: 0F

                        val delta = detector.scaleFactor

                        binding.zoomText.text = "%.2f".format(currentZoomRatio)
                        cameraControl.setZoomRatio(currentZoomRatio * delta)
                        return true
                    }
                }


                val scaleGestureDetector = ScaleGestureDetector(requireContext(), listener)
                binding.preview.setOnTouchListener { _, event ->
                    scaleGestureDetector.onTouchEvent(event)
                    true
                }

            } catch (e: Exception) {
                debugging("Use case binding failed")
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        imageCapture?.let {
            val fileName = "JPEG_${System.currentTimeMillis()}.jpg"


            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/MyPictures")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }



            val resolver = requireContext().contentResolver


            val outputFileOptions = ImageCapture.OutputFileOptions
                .Builder(resolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                .build()


            it.takePicture(
                outputFileOptions,
                imageCaptureExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val savedUri = outputFileResults.savedUri

                        debugging("The image has been saved in $savedUri")
                    }

                    override fun onError(exception: ImageCaptureException) {
                        debugging("Error taking photo:$exception")
                    }

                })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        imageCaptureExecutor.shutdown()
    }

}