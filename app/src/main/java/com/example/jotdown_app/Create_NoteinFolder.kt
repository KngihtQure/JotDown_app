package com.example.jotdown_app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.jotdown_app.databinding.ActivityCreateNoteinfolderBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.InputStream

class Create_NoteinFolder : AppCompatActivity() {
    private lateinit var binding: ActivityCreateNoteinfolderBinding
    private var selectedImageUri: Uri? = null
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCreateNoteinfolderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val folderId = intent.getStringExtra("folderId") ?: ""
        val folderName = intent.getStringExtra("folderName") ?: "Folder"

        binding.tvFolderName.text = folderName

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri
                binding.ivImagePreview.setImageURI(uri)
                binding.ivImagePreview.visibility = View.VISIBLE
                binding.btnRemoveImage.visibility = View.VISIBLE
                binding.layoutAddImage.visibility = View.GONE
            }
        }
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            startActivity(Intent(this, Login::class.java))
            finish()
            return
        }
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return


        binding.layoutAddImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.ivImagePreview.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnRemoveImage.setOnClickListener {
            selectedImageUri = null
            binding.ivImagePreview.setImageURI(null)
            binding.ivImagePreview.visibility = View.GONE
            binding.btnRemoveImage.visibility = View.GONE
            binding.layoutAddImage.visibility = View.VISIBLE
        }

        binding.btnSave.setOnClickListener {
            val title = binding.etNoteTitle.text.toString().trim()
            val content = binding.etNoteContent.text.toString().trim()

            if (title.isEmpty()) {
                binding.etNoteTitle.error = "Title is required"
                binding.etNoteTitle.requestFocus()
                return@setOnClickListener
            }

            if (content.isEmpty()) {
                binding.etNoteContent.error = "Body is required"
                binding.etNoteContent.requestFocus()
                return@setOnClickListener
            }

            val base64Image = selectedImageUri?.let { uriToBase64(it) } ?: ""
            saveNoteToDatabase(uid, title, content, base64Image, folderId)
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()


            val rotatedBitmap = rotateImageIfRequired(bitmap, uri)

            val outputStream = ByteArrayOutputStream()

            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
            val bytes = outputStream.toByteArray()

            Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun rotateImageIfRequired(img: Bitmap, selectedImage: Uri): Bitmap {
        val input = contentResolver.openInputStream(selectedImage)
        val ei = if (android.os.Build.VERSION.SDK_INT > 23) {
            ExifInterface(input!!)
        } else {
            ExifInterface(selectedImage.path!!)
        }

        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270)
            else -> img
        }
    }

    private fun rotateImage(img: Bitmap, degree: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        img.recycle()
        return rotatedImg
    }

    private fun saveNoteToDatabase(uid: String, title: String, content: String, imageUrl: String, folderId: String = "") {
        val database = FirebaseFirestore.getInstance().collection("notes").document(uid)
            .collection("user_notes")
        val noteId = database.document().id

        val noteData = mapOf(
            "id" to noteId,
            "title" to title,
            "content" to content,
            "imageUrl" to imageUrl,
            "folderId" to folderId,
            "timestamp" to System.currentTimeMillis()
        )

        database.document(noteId).set(noteData)
            .addOnSuccessListener {
                Toast.makeText(this, "Note saved successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save note: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
