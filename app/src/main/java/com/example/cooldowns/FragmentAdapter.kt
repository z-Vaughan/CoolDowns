package com.example.cooldowns

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

interface OnFragmentDragListener {
    fun onFragmentMove(fromPosition: Int, toPosition: Int)
}

class FragmentAdapter(
    private val fragmentItems: MutableList<FragmentItem>,
    private val onItemClick: (FragmentItem) -> Unit,
    private val onFragmentDragListener: OnFragmentDragListener
) : RecyclerView.Adapter<FragmentAdapter.FragmentViewHolder>() {

    class FragmentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val fragmentNameTextView: TextView = view.findViewById(R.id.tvFragmentName)
        private val jumpToFragmentButton: ImageButton = view.findViewById(R.id.btnJumpToFragment)

        fun bind(fragmentItem: FragmentItem, onClick: (FragmentItem) -> Unit) {
            fragmentNameTextView.text = fragmentItem.title
            jumpToFragmentButton.setOnClickListener {
                onClick(fragmentItem)
            }
        }

        // Handle item drag start visual feedback
        fun onItemDragStart() {
            itemView.elevation = 10f // Optional: elevate the item
        }

        // Handle item drag end visual feedback
        fun onItemDragEnd() {
            itemView.elevation = 0f // Reset elevation
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FragmentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_fragment, parent, false)
        return FragmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: FragmentViewHolder, position: Int) {
        holder.bind(fragmentItems[position], onItemClick)
    }

    override fun getItemCount(): Int = fragmentItems.size

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        // Update the list based on drag and drop
        val movedItem = fragmentItems.removeAt(fromPosition)
        fragmentItems.add(toPosition, movedItem)
        notifyItemMoved(fromPosition, toPosition)
        // Trigger the completion callback
        onFragmentDragListener.onFragmentMove(fromPosition, toPosition)
    }
    
}
