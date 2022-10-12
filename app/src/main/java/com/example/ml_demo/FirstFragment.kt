package com.example.ml_demo

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.ml_demo.databinding.FragmentFirstBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.*


class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val pickImage = 100
    private val captureImage = 200
    private val readExt = 300
    private val writeExt = 400
    private var imageUri: Uri? = null
    private var textFromPickedImage: String? = null
    private lateinit var tts : TextToSpeech

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imageviewToBeProcessed.setImageResource(R.drawable.sneha_profile)

        setupPermissions()

        binding.buttonGallery.setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            @Suppress("DEPRECATION") startActivityForResult(gallery, pickImage)
        }

        binding.buttonCamera.setOnClickListener {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE,"CAP_IMG")
            values.put(MediaStore.Images.Media.DESCRIPTION,"Captured Image From Camera.")
            imageUri = context?.contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            val camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            camera.putExtra(MediaStore.EXTRA_OUTPUT,imageUri)
            @Suppress("DEPRECATION") startActivityForResult(camera, captureImage)
        }
    }

    private fun setupPermissions() {
        val pick = context?.let { ContextCompat.checkSelfPermission(it, Manifest.permission.READ_MEDIA_IMAGES) }
        val capture = context?.let { ContextCompat.checkSelfPermission(it, Manifest.permission.CAMERA) }
        val readExternal = context?.let { ContextCompat.checkSelfPermission(it, Manifest.permission.READ_EXTERNAL_STORAGE) }
        val writeExternal = context?.let { ContextCompat.checkSelfPermission(it, Manifest.permission.WRITE_EXTERNAL_STORAGE) }
        if ((pick != PackageManager.PERMISSION_GRANTED) || (capture != PackageManager.PERMISSION_GRANTED) || (readExternal != PackageManager.PERMISSION_GRANTED) || (writeExternal != PackageManager.PERMISSION_GRANTED)) {
            makeRequest()
        }
    }

    private fun makeRequest() {
        ActivityCompat.requestPermissions(
            context as Activity, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), pickImage
        )
        ActivityCompat.requestPermissions(
            context as Activity, arrayOf(Manifest.permission.CAMERA), captureImage
        )
        ActivityCompat.requestPermissions(
            context as Activity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), readExt
        )
        ActivityCompat.requestPermissions(
            context as Activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), writeExt
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            pickImage -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        context,
                        "Image access permission has been denied by user",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "Image access permission has been granted by user",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            captureImage -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        context,
                        "Image capture permission has been denied by user",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "Image capture permission has been granted by user",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION") super.onActivityResult(requestCode, resultCode, data)
        if ((resultCode == RESULT_OK) && (requestCode == pickImage)) {
            imageUri = data?.data
            binding.imageviewToBeProcessed.setImageURI(imageUri)
            val imageForMLModel =
                imageUri?.let { it1 -> InputImage.fromFilePath(requireContext(), it1) }
            if (imageForMLModel != null) {
                recognizeText(imageForMLModel)
            } else {
                error("Null image.")
            }
        }
        if ((resultCode == RESULT_OK) && (requestCode == captureImage)) {
            binding.imageviewToBeProcessed.setImageURI(imageUri)
            val imageForMLModel =
                imageUri?.let { it1 -> InputImage.fromFilePath(requireContext(), it1) }
            if (imageForMLModel != null) {
                recognizeText(imageForMLModel)
            } else {
                error("Null image.")
            }
        }
    }

    private fun recognizeText(image: InputImage) {
        textFromPickedImage = ""
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image).addOnSuccessListener { visionText ->
            for (block in visionText.textBlocks) {
                val text = block.text
                textFromPickedImage += text
            }
            if ((textFromPickedImage == null) || (textFromPickedImage == "")) {
                binding.textViewResult.text = getString(R.string.text_view_result_text)
            } else {
                binding.textViewResult.text = textFromPickedImage
                speakOut(binding.textViewResult.text.toString())
            }
        }.addOnFailureListener { e ->
            Toast.makeText(
                context,
                "Something went wrong while processing image.",
                Toast.LENGTH_SHORT
            ).show()
            error(e.message.toString())
        }
    }

    private fun speakOut(textForSpeak: String) {
        tts = TextToSpeech(context) {
            if (it == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                @Suppress("DEPRECATION")
                tts.speak(textForSpeak, TextToSpeech.QUEUE_FLUSH, null)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tts.stop()
        tts.shutdown()
        _binding = null
    }

}