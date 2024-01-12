package com.example.cooldowns

import android.util.Log
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class FragmentDragTouchHelperCallback(
    private val adapter: FragmentAdapter,
    private val recyclerView: RecyclerView,
    private val fragmentInteractionListener: FragmentInteractionListener
) : ItemTouchHelper.Callback() {

    // Determines if long press is enabled for drag.
    override fun isLongPressDragEnabled() = true

    /**
     * Called when the item selection state changes. It handles the visual feedback during dragging.
     * @param viewHolder The ViewHolder of the item being interacted with.
     * @param actionState The current action state of the ItemTouchHelper, e.g., drag or idle.
     */
    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        val timerViewHolder = viewHolder as? TimerAdapter.TimerViewHolder

        when (actionState) {
            ItemTouchHelper.ACTION_STATE_DRAG -> {
                // Handles the visual changes when an item starts being dragged.
                timerViewHolder?.onItemDragStart()
                recyclerView.itemAnimator = null // Disables the item animator during drag.
            }
            ItemTouchHelper.ACTION_STATE_IDLE -> {
                // Handles the visual changes when an item stops being dragged.
                timerViewHolder?.onItemDragEnd()

            }
        }
    }

    /**
     * Specifies the allowed movement directions in each state (drag and swipe).
     * @param recyclerView The RecyclerView to which ItemTouchHelper is attached.
     * @param viewHolder The ViewHolder of the item being interacted with.
     * @return Flags defining the enabled move directions.
     */
    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN // Allows up and down movement.
        return makeMovementFlags(dragFlags, 0) // Swipe flags are set to 0 as swiping is not needed.
    }

    /**
     * Called when an item has been dragged and dropped to a new position.
     * @param recyclerView The RecyclerView to which ItemTouchHelper is attached.
     * @param viewHolder The ViewHolder of the item being dragged.
     * @param target The ViewHolder of the item where the dragged item is dropped.
     * @return True if the viewHolder has been moved to the adapter position of target.
     */
    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        val fromPosition = viewHolder.adapterPosition
        val toPosition = target.adapterPosition
        adapter.onItemMove(fromPosition, toPosition) // Only update the list, don't call onFragmentMove yet
        return true
    }

    /**
     * Called when an item has been swiped. In this implementation, swiping functionality is not needed.
     * @param viewHolder The ViewHolder of the item being swiped.
     * @param direction The direction in which the item has been swiped.
     */
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Swiping is not needed in this application.
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        // Now that the drag is completed, call onFragmentMove to update the ViewPager2
        val fromPosition = viewHolder.adapterPosition
        val finalPosition = viewHolder.adapterPosition
        fragmentInteractionListener.onFragmentMoveCompleted(fromPosition, finalPosition)
    }
}
