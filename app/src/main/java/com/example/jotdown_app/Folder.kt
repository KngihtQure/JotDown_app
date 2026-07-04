package com.example.jotdown_app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jotdown_app.databinding.ActivityFolderBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.jotdown_app.databinding.EditNoteBinding
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Base64
import android.view.View
import android.view.Window
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import java.io.ByteArrayOutputStream

class Folder : AppCompatActivity() {

    private lateinit var binding: ActivityFolderBinding
    private lateinit var noteadapter: NoteAdapter
    private var notesList = mutableListOf<Note>()
    private var imagepick: ((String) -> Unit)?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFolderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val folderId = intent.getStringExtra("folderId") ?: ""
        val folderName = intent.getStringExtra("folderName") ?: "Folder"

        binding.tvFolderName.text = folderName

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        noteadapter = NoteAdapter(emptyList()){ note ->
            showEditNoteDialog(uid, note)
        }
        binding.rvFolderNotes.layoutManager = LinearLayoutManager(this)
        binding.rvFolderNotes.adapter = noteadapter

        FirebaseFirestore.getInstance()
            .collection("notes").document(uid)
            .collection("user_notes")
            .whereEqualTo("folderId", folderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                notesList.clear()
                snapshot?.documents?.forEach { doc ->
                    doc.toObject(Note::class.java)?.let { notesList.add(it) }
                }
                noteadapter.submitList(notesList)

                if (notesList.isEmpty()) {
                    binding.rvFolderNotes.visibility = android.view.View.GONE
                    binding.layoutEmptyState.visibility = android.view.View.VISIBLE
                } else {
                    binding.rvFolderNotes.visibility = android.view.View.VISIBLE
                    binding.layoutEmptyState.visibility = android.view.View.GONE
                }
            }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnCreateNote.setOnClickListener {
            val intent = Intent(this, Create_NoteinFolder::class.java)
            intent.putExtra("folderId", folderId)
            intent.putExtra("folderName", folderName)
            startActivity(intent)
        }

        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterNotes(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }
    private fun filterNotes(query: String) {
        val filteredList = notesList.filter {
            it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true)
        }
        noteadapter.submitList(filteredList)
    }

    private fun showEditNoteDialog(uid:String, note:Note){
        val dialog = Dialog(this)
        val dialogBinding = EditNoteBinding.inflate(layoutInflater)

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.etNoteTitle.setText(note.title)
        dialogBinding.etNoteContent.setText(note.content)

        var imageVal = note.imageUrl
        imagepick = {newImage -> imageVal = newImage
            val imageBytes = Base64.decode(newImage, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            dialogBinding.layoutAddImage.visibility = View.GONE
            dialogBinding.ivImagePreview.visibility = View.VISIBLE
            dialogBinding.ivImagePreview.setImageBitmap(bitmap)

            val params = dialogBinding.btnSave.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.topToBottom = dialogBinding.ivImagePreview.id
            dialogBinding.btnSave.layoutParams = params
        }

        dialogBinding.layoutAddImage.setOnClickListener {
            launchpickimage.launch("image/*")
        }

        dialogBinding.ivImagePreview.setOnClickListener {
            launchpickimage.launch("image/*")
        }

        dialog.setOnDismissListener {
            imagepick = null
        }
        if(note.imageUrl.isNotEmpty()){
            dialogBinding.layoutAddImage.visibility = View.GONE
            dialogBinding.ivImagePreview.visibility = View.VISIBLE

            val params = dialogBinding.btnSave.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.topToBottom = dialogBinding.ivImagePreview.id
            dialogBinding.btnSave.layoutParams = params

            try{
                val imageBytes = Base64.decode(note.imageUrl, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                dialogBinding.ivImagePreview.setImageBitmap(bitmap)
            }catch (e: Exception){
                dialogBinding.ivImagePreview.visibility = View.GONE
                dialogBinding.layoutAddImage.visibility = View.VISIBLE

                val params = dialogBinding.btnSave.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                params.topToBottom = dialogBinding.layoutAddImage.id
                dialogBinding.btnSave.layoutParams = params
            }
        }else{
            dialogBinding.ivImagePreview.visibility = View.GONE
            dialogBinding.layoutAddImage.visibility = View.VISIBLE
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener{
            val newtitle = dialogBinding.etNoteTitle.text.toString().trim()
            val newcontent = dialogBinding.etNoteContent.text.toString().trim()

            if (newtitle.isEmpty()){
                dialogBinding.etNoteTitle.error = "Title is required"
                return@setOnClickListener
            }

            FirebaseFirestore.getInstance().collection("notes").document(uid)
                .collection("user_notes").document(note.id)
                .update(
                    mapOf(
                        "title" to newtitle,
                        "content" to newcontent,
                        "imageUrl" to imageVal,
                        "timestamp" to System.currentTimeMillis()
                    )
                )
                .addOnSuccessListener {
                    Toast.makeText(this, "Note updated", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        dialogBinding.btnDelete.setOnClickListener {
            FirebaseFirestore.getInstance().collection("notes").document(uid).collection("user_notes").document(note.id)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }.addOnFailureListener {
                    Toast.makeText(this, "Deletion failed", Toast.LENGTH_SHORT).show()
                }
        }
        dialog.show()
    }
    private val launchpickimage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ){ uri: Uri? -> uri ?: return@registerForActivityResult
        val imageBase64 = uriToBase64(uri)
        imagepick?.invoke(imageBase64)
    }

    private fun  uriToBase64(uri: Uri): String{
        val bitmap = contentResolver.openInputStream(uri).use{input ->
            BitmapFactory.decodeStream(input)
        }

        val resize = Bitmap.createScaledBitmap(bitmap, 50, 50, true)

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 50, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }
}