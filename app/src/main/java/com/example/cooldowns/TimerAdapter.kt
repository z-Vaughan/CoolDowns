package com.example.cooldowns

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for handling the list of TimerEntry objects in a RecyclerView.
 * This adapter manages the display and interaction of each timer item, including updating countdowns and delete functionality.
 */
class TimerAdapter(
    private val timers: MutableList<TimerEntry>,
    private val countdownUpdateCallback: (Int, String) -> Unit,
    private val deleteTimerCallback: (Int) -> Unit
) : RecyclerView.Adapter<TimerAdapter.TimerViewHolder>() {

    companion object;

    // Currently selected position in the list, used for item selection.
    private var selectedPosition: Int = -1

    // Inflates the item layout and creates a ViewHolder for each item.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_timer, parent, false)
        return TimerViewHolder(view)
    }

    // Binds data to each item in the RecyclerView.
    override fun onBindViewHolder(holder: TimerViewHolder, position: Int) {
        holder.bind(timers[position])
    }

    // Handles partial updates to optimize performance.
    override fun onBindViewHolder(holder: TimerViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isNotEmpty()) {
            // Payload available for partial update.
            val formattedTime = payloads.first() as String
            holder.updateCountdownTime(formattedTime)
        } else {
            // Full update fallback.
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    // Returns the total number of items in the list.
    override fun getItemCount(): Int = timers.size

    // Returns the selected position, used for handling item selection.
    fun getSelectedPosition(): Int = selectedPosition

    // Handles the movement of items within the RecyclerView for drag-and-drop functionality.
    fun onItemMove(fromPosition: Int, toPosition: Int) {
        val movedItem = timers.removeAt(fromPosition)
        timers.add(toPosition, movedItem)
        notifyItemMoved(fromPosition, toPosition)
    }

    /**
     * ViewHolder class for each timer item. Manages the display and interaction for each item.
     */
    inner class TimerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val tvTimerName: TextView = itemView.findViewById(R.id.tvTimerName)
        private val tvTimerCountdown: TextView = itemView.findViewById(R.id.tvRemainingTime)
        private val btnMenu: ImageButton = itemView.findViewById(R.id.btnDeleteTimer)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val restartButton: ImageButton = itemView.findViewById(R.id.btnRestartTimer)

        init {
            itemView.setOnClickListener(this)
            btnMenu.setOnClickListener { showPopupMenu(it) }
            restartButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val timer = timers[position]
                    restartTimer(timer, position)
                }
            }
        }

        // Binds the timer data to the UI components of the item.
        fun bind(timer: TimerEntry) {
            tvTimerName.text = timer.name
            updateProgressBar(timer)
            timer.setCountdownCallback { formattedTime ->
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    updateTimerText(formattedTime)
                }
            }
            if (timer.isCountdownFinished()) {
                tvTimerCountdown.text = "Finished"
            } else {
                tvTimerCountdown.text = timer.getFormattedTime()
            }
        }

        // Updates the progress bar based on the timer's progress.
        private fun updateProgressBar(timer: TimerEntry) {
            progressBar.progress = timer.getProgressPercentage()
        }

        // Handles item drag start visual feedback.
        fun onItemDragStart() {
            itemView.elevation = 10f
            // Additional visual changes can be added here.
        }

        // Handles item drag end visual feedback.
        fun onItemDragEnd() {
            itemView.elevation = 0f
            // Reset visual changes here.
        }

        // Updates the countdown time displayed on the item.
        fun updateCountdownTime(formattedTime: String) {
            tvTimerCountdown.text = formattedTime
        }

        // Updates the timer text. Separated for clarity and potential future use.
        fun updateTimerText(formattedTime: String) {
            tvTimerCountdown.text = formattedTime
        }


        // Method to restart a timer
        private fun restartTimer(timer: TimerEntry, position: Int) {
            timer.stopCountdown(itemView.context)
            timer.startCountdown(itemView.context)
            notifyItemChanged(position)
        }

        // Handles item click events.
        override fun onClick(v: View) {
            selectedPosition = adapterPosition
            val clickedTimer = timers[selectedPosition]
            val context = itemView.context

        }

        // Shows a popup menu for the item, currently used for delete functionality.
        private fun showPopupMenu(view: View) {
            val position = adapterPosition
            val popup = PopupMenu(view.context, view)
            popup.inflate(R.menu.timer_menu)
            popup.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_delete -> {
                        deleteTimerCallback.invoke(position)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }
}
