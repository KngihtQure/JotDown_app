package com.example.jotdown_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.jotdown_app.databinding.ItemNoteBinding
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(
    private var items: List<Note>
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Note) {
            binding.tvNoteTitle.text = item.title
            binding.tvNoteContent.text = item.content

            if (item.imageUrl.isNotEmpty()) {
                binding.ivNoteImage.visibility = View.VISIBLE
                binding.ivNoteImage.load(item.imageUrl)
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
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun submitList(newItems: List<Note>) {
        items = newItems
        notifyDataSetChanged()
    }
}
