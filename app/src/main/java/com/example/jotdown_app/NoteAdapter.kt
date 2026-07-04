package com.example.jotdown_app

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.jotdown_app.databinding.ItemNoteBinding
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(
    private var items: List<Note>,
    private val onClick: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Note) {
            binding.tvNoteTitle.text = item.title
            binding.tvNoteContent.text = item.content

            if (item.imageUrl.isNotEmpty()) {
                binding.ivNoteImage.visibility = View.VISIBLE
                try {
                    val imageBytes = Base64.decode(item.imageUrl, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    binding.ivNoteImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    binding.ivNoteImage.visibility = View.GONE
                }
            } else {
                binding.ivNoteImage.visibility = View.GONE
            }

            val date = Date(item.timestamp)
            val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

            binding.tvNoteDate.text = dateFormat.format(date)
            binding.tvNoteTime.text = timeFormat.format(date)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = items[position]
        holder.bind(note)
        holder.itemView.setOnClickListener {
            onClick(note)
        }
    }

    override fun getItemCount() = items.size

    fun submitList(newItems: List<Note>) {
        items = newItems
        notifyDataSetChanged()
    }
}
