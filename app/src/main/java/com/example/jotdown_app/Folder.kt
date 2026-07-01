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

class Folder : AppCompatActivity() {

    private lateinit var binding: ActivityFolderBinding
    private lateinit var noteadapter: NoteAdapter
    private var notesList = mutableListOf<Note>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFolderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val folderId = intent.getStringExtra("folderId") ?: ""
        val folderName = intent.getStringExtra("folderName") ?: "Folder"

        binding.tvFolderName.text = folderName

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        noteadapter = NoteAdapter(emptyList())
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
}