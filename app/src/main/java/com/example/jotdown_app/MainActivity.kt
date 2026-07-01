package com.example.jotdown_app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jotdown_app.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var folderAdapter: FolderAdapter
    private var notesList = mutableListOf<Note>()
    private var foldersList = mutableListOf<FolderData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        noteAdapter = NoteAdapter(emptyList())
        binding.rvNotes.layoutManager = LinearLayoutManager(this)
        binding.rvNotes.adapter = noteAdapter

        folderAdapter = FolderAdapter(emptyList()) { folder ->
            val intent = Intent(this, Folder::class.java)
            intent.putExtra("folderId", folder.id)
            intent.putExtra("folderName", folder.title)
            startActivity(intent)
        }
        binding.rvFolders.layoutManager = LinearLayoutManager(this)
        binding.rvFolders.adapter = folderAdapter

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            startActivity(Intent(this, Login::class.java))
            finish()
            return
        }
        val uid = currentUser.uid

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
        FirebaseFirestore.getInstance().collection("notes").document(uid)
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
        FirebaseFirestore.getInstance().collection("folders").document(uid)
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
}
