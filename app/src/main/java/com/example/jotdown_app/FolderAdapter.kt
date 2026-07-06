package com.example.jotdown_app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.jotdown_app.databinding.ItemFolderBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FolderAdapter(
    private var folders: List<FolderData>,
    private val onClick: (FolderData) -> Unit,
    private val onEditClick: (FolderData) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    inner class FolderViewHolder(val binding: ItemFolderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = folders[position]
        holder.binding.tvFolderTitle.text = folder.title

        val date = Date(folder.timestamp)
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

        holder.binding.tvFolderDate.text = dateFormat.format(date)
        holder.binding.tvFolderTime.text = timeFormat.format(date)

        holder.itemView.setOnClickListener { onClick(folder) }

        holder.binding.tvGotoNote.setOnClickListener { onEditClick(folder) }
    }

    override fun getItemCount() = folders.size

    fun submitList(newList: List<FolderData>) {
        folders = newList
        notifyDataSetChanged()
    }
}