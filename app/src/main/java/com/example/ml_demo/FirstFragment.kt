package com.example.ml_demo

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.ml_demo.databinding.FragmentFirstBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions


class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val pickImage = 100
    private var imageUri: Uri? = null
    private var textFromPickedImage: String? = null

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
        binding.buttonStart.setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(gallery, pickImage)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == pickImage) {
            imageUri = data?.data
            binding.imageviewToBeProcessed.setImageURI(imageUri)
            val imageForMLModel = imageUri?.let { it1 -> InputImage.fromFilePath(requireContext(), it1) }
            if (imageForMLModel != null) {
                recognizeText(imageForMLModel)
            } else {
                error("Null image.")
            }
        }
    }

    private fun recognizeText(image: InputImage) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                for (block in visionText.textBlocks) {
                    val text = block.text
                    textFromPickedImage += text
                }
                if(textFromPickedImage == null){
                    binding.textViewResult.text = getString(R.string.text_view_result_text)
                }else{
                    binding.textViewResult.text = textFromPickedImage
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Something went wrong while processing image.", Toast.LENGTH_SHORT).show()
                error(e.message.toString())
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}