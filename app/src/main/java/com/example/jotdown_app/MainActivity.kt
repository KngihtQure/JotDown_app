package com.example.jotdown_app

import android.app.Dialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jotdown_app.databinding.ActivityMainBinding
import com.example.jotdown_app.databinding.EditNoteBinding
import com.example.jotdown_app.databinding.EditFolderBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var folderAdapter: FolderAdapter
    private var notesList = mutableListOf<Note>()
    private var foldersList = mutableListOf<FolderData>()
    private var imagepick: ((String) -> Unit)?= null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        noteAdapter = NoteAdapter(emptyList()){ note ->
            showEditNoteDialog(uid, note)
        }

        binding.rvNotes.layoutManager = LinearLayoutManager(this)
        binding.rvNotes.adapter = noteAdapter

        folderAdapter = FolderAdapter(
            emptyList(),
            onClick = { folder ->
                val intent = Intent(this, Folder::class.java)
                intent.putExtra("folderId", folder.id)
                intent.putExtra("folderName", folder.title)
                startActivity(intent)
            },
            onEditClick = { folder ->
                showEditFolder(uid, folder)
            }
        )
        binding.rvFolders.layoutManager = LinearLayoutManager(this)
        binding.rvFolders.adapter = folderAdapter

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            startActivity(Intent(this, Login::class.java))
            finish()
            return
        }

        loadUserInfo(uid)
        loadNotes(uid)
        loadFolders(uid)

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.btnCreateNote.setOnClickListener {
            startActivity(Intent(this, Create_note::class.java))
        }

        binding.btnCreateFolder.setOnClickListener {
            startActivity(Intent(this, Create_Folder::class.java))
        }

        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterNotes(s.toString())
                filterFolder(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun loadUserInfo(uid: String) {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid

        FirebaseDatabase.getInstance().getReference("users").child(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.child("username").value.toString().replaceFirstChar { it.titlecase() }
                    val email = document.child("email").value.toString()
                    binding.tvGreeting.text = "Hi, ${username}."
                    binding.tvEmail.text = email
                }
            }
    }

    private fun loadNotes(uid: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("notes").document(uid)
            .collection("user_notes")
            .whereEqualTo("folderId", "")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    notesList.clear()
                    for (doc in snapshot.documents) {
                        doc.toObject(Note::class.java)?.let { notesList.add(it) }
                    }
                    noteAdapter.submitList(notesList)
                    binding.layoutEmptyState.visibility = if (notesList.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvNotes.visibility = if (notesList.isEmpty()) View.GONE else View.VISIBLE
                }
            }
    }

    private fun loadFolders(uid: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("folders").document(uid)
            .collection("user_folders")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    foldersList.clear()
                    for (doc in snapshot.documents) {
                        doc.toObject(FolderData::class.java)?.let { foldersList.add(it) }
                    }
                    folderAdapter.submitList(foldersList)
                    binding.layoutEmptyState.visibility = if (foldersList.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvFolders.visibility = View.VISIBLE
                }
            }
    }

    private fun filterNotes(query: String) {
        val filteredList = notesList.filter {
            it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true)
        }
        noteAdapter.submitList(filteredList)
    }

    private fun filterFolder(query: String) {
        val filteredList = foldersList.filter {
            it.title.contains(query, ignoreCase = true)
        }
        folderAdapter.submitList(filteredList)
    }

    private fun showEditNoteDialog(uid:String, note:Note){
        val db = FirebaseFirestore.getInstance()
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
            dialogBinding.btnRemoveImage.visibility = View.VISIBLE
            dialogBinding.ivImagePreview.setImageBitmap(bitmap)

            val params = dialogBinding.btnSave.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.topToBottom = dialogBinding.cardImageContainer.id
            dialogBinding.btnSave.layoutParams = params
        }

        dialogBinding.btnRemoveImage.setOnClickListener {
            imageVal = ""
            dialogBinding.ivImagePreview.setImageBitmap(null)
            dialogBinding.ivImagePreview.visibility = View.GONE
            dialogBinding.btnRemoveImage.visibility = View.GONE
            dialogBinding.layoutAddImage.visibility = View.VISIBLE

            val params = dialogBinding.btnSave.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.topToBottom = dialogBinding.cardImageContainer.id
            dialogBinding.btnSave.layoutParams = params
        }

        dialogBinding.layoutAddImage.setOnClickListener {
            launchpickimage.launch("image/*")
        }

        dialogBinding.ivImagePreview.setOnClickListener {
            launchpickimage.launch("image/*")
        }

        if(note.imageUrl.isNotEmpty()){
            dialogBinding.layoutAddImage.visibility = View.GONE
            dialogBinding.ivImagePreview.visibility = View.VISIBLE
            dialogBinding.btnRemoveImage.visibility = View.VISIBLE

            val params = dialogBinding.btnSave.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.topToBottom = dialogBinding.cardImageContainer.id
            dialogBinding.btnSave.layoutParams = params
            try{
                val imageBytes = Base64.decode(note.imageUrl, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                dialogBinding.ivImagePreview.setImageBitmap(bitmap)
            }catch (e: Exception){
                dialogBinding.ivImagePreview.visibility = View.GONE
                dialogBinding.btnRemoveImage.visibility = View.GONE
                dialogBinding.layoutAddImage.visibility = View.VISIBLE

                val params = dialogBinding.btnSave.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                params.topToBottom = dialogBinding.cardImageContainer.id
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

            db.collection("notes").document(uid)
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
                }.addOnFailureListener {
                    Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
                }
        }

        dialogBinding.btnDelete.setOnClickListener {
            db.collection("notes").document(uid).collection("user_notes").document(note.id)
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

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 50, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }

    private fun showEditFolder(uid:String, folder: FolderData){
        val db = FirebaseFirestore.getInstance()
        val dialog = Dialog(this)
        val dialogBinding = EditFolderBinding.inflate(layoutInflater)

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.etFolderTitle.setText(folder.title)

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            val newtitle = dialogBinding.etFolderTitle.text.toString().trim()

            if (newtitle.isEmpty()){
                dialogBinding.etFolderTitle.error = "Folder name is required"
                return@setOnClickListener
            }

            db.collection("folders").document(uid)
                .collection("user_folders").document(folder.id)
                .update(mapOf(
                    "title" to newtitle,
                    "timestamp" to System.currentTimeMillis()
                ))
                .addOnSuccessListener {
                    Toast.makeText(this, "Folder update", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }.addOnFailureListener {
                    Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
                }
        }

        dialogBinding.btnDelete.setOnClickListener {
            db.collection("notes").document(uid)
                .collection("user_notes")
                .whereEqualTo("folderId", folder.id)
                .get()
                .addOnSuccessListener { notesSnapshot ->

                    val batch = db.batch()

                    for (doc in notesSnapshot.documents) {
                        batch.delete(doc.reference)
                    }

                    val folderRef = db.collection("folders").document(uid)
                        .collection("user_folders").document(folder.id)

                    batch.delete(folderRef)
                    batch.commit()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Folder deleted", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to find notes: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
        dialog.show()
    }
}
