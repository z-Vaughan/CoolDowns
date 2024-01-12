package com.example.cooldowns

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter



class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
    private val fragments: MutableList<Fragment> = mutableListOf()



    override fun getItemCount(): Int = fragments.size
    override fun createFragment(position: Int): Fragment = fragments[position]

    fun getFragments(): List<Fragment> = fragments

    fun addFragment(fragment: Fragment) {
        Log.d("SSPA","addFragment called")
        fragments.add(fragment)
        notifyItemInserted(fragments.size - 1)

    }


    fun removeFragmentByUUID(uuid: String): Int {
        val position = fragments.indexOfFirst {
            it is TimerFragment && it.fragmentUUID == uuid

        }
        Log.d("SSPA", "Current UUID list contains $fragments before destroy")
        Log.d("SSPA", "Destroy Request For $uuid @ Position $position")

        if (position != -1) {
            fragments.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, fragments.size)

            Log.d("SSPA", "Updated UUID list contains $fragments after destroy")
        }
        return position
    }

    // Additional helper method to get UUID of a fragment
    fun getFragmentUUID(fragment: Fragment): String? {
        return if (fragment is TimerFragment) fragment.fragmentUUID else null
    }

    fun getFragmentUUIDs(): List<String> {
        return fragments.filterIsInstance<TimerFragment>().map { it.fragmentUUID }
    }

    fun clearFragments() {
        Log.d("SSPA","clearFragments called for $fragments")
        fragments.clear()
        notifyDataSetChanged()
    }

    fun moveFragment(fromPosition: Int, toPosition: Int) {
        if (fromPosition < fragments.size && toPosition < fragments.size) {
            Log.i("SSPA", "mF: Position $fromPosition -> $toPosition")
            val fragment = fragments.removeAt(fromPosition)
            fragments.add(toPosition, fragment)
            notifyItemMoved(fromPosition, toPosition)
        }
    }


}