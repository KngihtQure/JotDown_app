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
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: NoteAdapter
    private var notesList = mutableListOf<Note>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        adapter = NoteAdapter(emptyList())
        binding.rvNotes.layoutManager = LinearLayoutManager(this)
        binding.rvNotes.adapter = adapter

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            startActivity(Intent(this, Login::class.java))
            finish()
            return
        }
        val uid = currentUser.uid

        loadUserInfo(uid)
        loadNotes(uid)

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

        // Search Filter
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterNotes(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun loadUserInfo(uid: String) {
        FirebaseDatabase.getInstance().getReference("users").child(uid).get()
            .addOnSuccessListener {
                if (it.exists()) {
                    val username = it.child("username").value.toString()
                    val email = it.child("email").value.toString()
                    binding.tvGreeting.text = "Hi, $username."
                    binding.tvEmail.text = email
                }
            }
    }

    private fun loadNotes(uid: String) {
        FirebaseDatabase.getInstance().getReference("notes").child(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    notesList.clear()
                    for (noteSnapshot in snapshot.children) {
                        val note = noteSnapshot.getValue(Note::class.java)
                        if (note != null) {
                            notesList.add(note)
                        }
                    }
                    notesList.sortByDescending { it.timestamp }
                    adapter.submitList(notesList)

                    if (notesList.isEmpty()) {
                        binding.layoutEmptyState.visibility = View.VISIBLE
                        binding.rvNotes.visibility = View.GONE
                    } else {
                        binding.layoutEmptyState.visibility = View.GONE
                        binding.rvNotes.visibility = View.VISIBLE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun filterNotes(query: String) {
        val filteredList = notesList.filter {
            it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true)
        }
        adapter.submitList(filteredList)
    }
}
