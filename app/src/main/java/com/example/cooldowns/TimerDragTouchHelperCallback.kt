package com.example.cooldowns

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

/**
 * This class extends ItemTouchHelper.Callback and is used to enable drag-and-drop functionality
 * in a RecyclerView. It interacts with a custom adapter (TimerAdapter) to handle the movement
 * and states of the items within the RecyclerView. This class allows for items to be dragged and
 * reordered by long pressing on them.
 */
class TimerDragTouchHelperCallback(
    private val adapter: TimerAdapter,
    private val recyclerViewFragment: RecyclerView
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
                recyclerViewFragment.itemAnimator = null // Disables the item animator during drag.
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
        adapter.onItemMove(viewHolder.adapterPosition, target.adapterPosition) // Notifies adapter of the move.
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
}
