package com.example.jotdown_app

data class Note(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0
)
