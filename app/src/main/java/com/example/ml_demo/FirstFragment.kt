package com.example.ml_demo

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.ProgressDialog
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
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.*


class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val pickImageCode = 100
    private val textDetectionCode = 200
    private val objectDetectionCode = 300
    private val readExt = 300
    private val writeExt = 400
    private var imageUri: Uri? = null
    private var textFromPickedImage: String? = null
    private lateinit var tts: TextToSpeech
    private lateinit var processDialog: ProgressDialog

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPermissions()

        processDialog = ProgressDialog(context)
        processDialog.setTitle("Please Wait")
        processDialog.setCanceledOnTouchOutside(false)

        binding.textViewResult.text = "Please select any of the available option."

        binding.buttonTextDetection.setOnClickListener {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, "CAP_IMG")
            values.put(MediaStore.Images.Media.DESCRIPTION, "Captured Image From Camera.")
            imageUri = context?.contentResolver?.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            )
            val camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            camera.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            @Suppress("DEPRECATION") startActivityForResult(camera, textDetectionCode)
        }

        binding.buttonObjectDetection.setOnClickListener {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, "CAP_IMG")
            values.put(MediaStore.Images.Media.DESCRIPTION, "Captured Image From Camera.")
            imageUri = context?.contentResolver?.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            )
            val camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            camera.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            @Suppress("DEPRECATION") startActivityForResult(camera, objectDetectionCode)
        }

        binding.buttonReplayResults.setOnClickListener {
            speakOut(binding.textViewResult.text.toString())
        }
    }

    private fun setupPermissions() {
        val pick = context?.let {
            ContextCompat.checkSelfPermission(
                it, Manifest.permission.READ_MEDIA_IMAGES
            )
        }
        val capture = context?.let {
            ContextCompat.checkSelfPermission(
                it, Manifest.permission.CAMERA
            )
        }
        val readExternal = context?.let {
            ContextCompat.checkSelfPermission(
                it, Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
        val writeExternal = context?.let {
            ContextCompat.checkSelfPermission(
                it, Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        val manageExternal = context?.let {
            ContextCompat.checkSelfPermission(
                it, Manifest.permission.MANAGE_EXTERNAL_STORAGE
            )
        }
        if ((pick != PackageManager.PERMISSION_GRANTED) || (capture != PackageManager.PERMISSION_GRANTED) || (readExternal != PackageManager.PERMISSION_GRANTED) || (writeExternal != PackageManager.PERMISSION_GRANTED) || (manageExternal != PackageManager.PERMISSION_GRANTED)) {
            makeRequest()
        }
    }

    private fun makeRequest() {
        ActivityCompat.requestPermissions(
            context as Activity, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), pickImageCode
        )
        ActivityCompat.requestPermissions(
            context as Activity, arrayOf(Manifest.permission.CAMERA), textDetectionCode
        )
        ActivityCompat.requestPermissions(
            context as Activity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), readExt
        )
        ActivityCompat.requestPermissions(
            context as Activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), writeExt
        )
        ActivityCompat.requestPermissions(
            context as Activity, arrayOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE), writeExt
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            pickImageCode -> {
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
            textDetectionCode -> {
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
        if ((resultCode == RESULT_OK) && (requestCode == pickImageCode)) {
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
        if ((resultCode == RESULT_OK) && (requestCode == textDetectionCode)) {
            binding.imageviewToBeProcessed.setImageURI(imageUri)
            val imageForMLModel =
                imageUri?.let { it1 -> InputImage.fromFilePath(requireContext(), it1) }
            if (imageForMLModel != null) {
                recognizeText(imageForMLModel)
            } else {
                error("Null image.")
            }
        }
        if ((resultCode == RESULT_OK) && (requestCode == objectDetectionCode)) {
            binding.imageviewToBeProcessed.setImageURI(imageUri)
            val imageForMLModel =
                imageUri?.let { it1 -> InputImage.fromFilePath(requireContext(), it1) }
            if (imageForMLModel != null) {
                recognizeObject(imageForMLModel)
            } else {
                error("Null image.")
            }
        }
    }

    private fun recognizeText(image: InputImage) {
        textFromPickedImage = ""
        processDialog.setMessage("Preparing image...")
        processDialog.show()
        val textDetector = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        processDialog.setMessage("Detecting text...")
        textDetector.process(image).addOnSuccessListener { visionText ->
            for (block in visionText.textBlocks) {
                val text = block.text
                textFromPickedImage += text
                for (line in block.lines) {
                    textFromPickedImage += "\n"
                }
            }
            processDialog.dismiss()
            if ((textFromPickedImage == null) || (textFromPickedImage == "")) {
                binding.textViewResult.text = getString(R.string.text_view_result_text)
            } else {
                binding.textViewResult.text = textFromPickedImage
                speakOut(binding.textViewResult.text.toString())
            }
        }.addOnFailureListener { e ->
            Toast.makeText(
                context, "Something went wrong while processing image.", Toast.LENGTH_LONG
            ).show()
            error(e.message.toString())
        }
    }

    private fun recognizeObject(image: InputImage) {
        textFromPickedImage = ""
        processDialog.setMessage("Preparing image...")
        processDialog.show()
        val options = ImageLabelerOptions.Builder().setConfidenceThreshold(0.51f).build()
        val objectDetector = ImageLabeling.getClient(options)
        processDialog.setMessage("Detecting objects...")
        objectDetector.process(image).addOnSuccessListener { imageLabels ->
            for (label in imageLabels) {
                textFromPickedImage += label.text + ", or "
            }
            textFromPickedImage = textFromPickedImage!!.dropLast(5) + "."
            processDialog.dismiss()
            if ((textFromPickedImage == null) || (textFromPickedImage == "")) {
                binding.textViewResult.text = getString(R.string.text_view_result_text)
            } else {
                binding.textViewResult.text = textFromPickedImage
                speakOut(binding.textViewResult.text.toString())
            }
        }.addOnFailureListener { e ->
            Toast.makeText(
                context, "Something went wrong while processing image.", Toast.LENGTH_LONG
            ).show()
            error(e.message.toString())
        }
    }

    private fun speakOut(textForSpeak: String) {
        tts = TextToSpeech(context) {
            if (it == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                tts.setSpeechRate(0.7f)
                @Suppress("DEPRECATION") tts.speak(textForSpeak, TextToSpeech.QUEUE_FLUSH, null)
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