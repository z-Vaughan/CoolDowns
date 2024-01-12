package com.example.cooldowns

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import kotlinx.coroutines.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder


interface FragmentInteractionListener {
    fun onFragmentDeletionRequested(fragment: TimerFragment)
    fun onFragmentMoveCompleted(fromPosition: Int, toPosition: Int)
}

class MainActivity : AppCompatActivity(), FragmentInteractionListener {

    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: ScreenSlidePagerAdapter

    // ViewModel implementation
    val viewModel: SharedViewModel by viewModels()
    // Delay dependency
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize ViewPager and Adapter
        viewPager = findViewById(R.id.viewPager)
        adapter = ScreenSlidePagerAdapter(this)
        viewPager.adapter = adapter

        loadSavedFragments() // Load saved fragments and update ViewModel
        setupNotificationChannel()
        cycleFragments()
    }

    override fun onPause() {
        super.onPause()
        saveFragmentUUIDs()
    }

    override fun onStop() {
        super.onStop()
        saveFragmentUUIDs()
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    fun onFragmentMove(fromPosition: Int, toPosition: Int) {
        // Static MainFragment already considered, update ViewPager2
        Log.i("MainAct", "oFM: Position $fromPosition -> $toPosition")
        adapter.moveFragment(fromPosition + 1, toPosition + 1)
        saveFragmentUUIDs()
        refreshViewPager()
    }

    fun updateViewModel() {
        val updatedItems = getFragmentItems()
        viewModel.fragmentItems.postValue(updatedItems)
    }

    private fun refreshViewPager() {
        val currentAdapter = viewPager.adapter
        viewPager.adapter = null
        viewPager.adapter = currentAdapter
    }

    private fun loadSavedFragments() {
        val prefs = getSharedPreferences("FragmentPreferences", MODE_PRIVATE)
        val fragmentUUIDsString = prefs.getString("fragmentUUIDs", null)
        val fragmentUUIDs = fragmentUUIDsString?.split(",")?.filter { it.isNotBlank() } ?: listOf()

        adapter.clearFragments()
        adapter.addFragment(MainFragment())
        fragmentUUIDs.forEach { uuid ->
            val title = getFragmentTitleFromPreferences(uuid)
            val timerFragment = TimerFragment.newInstance(userTitle = title, uuid)
            adapter.addFragment(timerFragment)
        }
        updateFragmentTitles()
        updateViewModel()
        refreshViewPager()
        adapter.notifyDataSetChanged()

    }

    private fun updateFragmentTitles() {
        val titles = mutableMapOf<String, String>()
        adapter.getFragments().forEach { fragment ->
            if (fragment is TimerFragment) {
                titles[fragment.fragmentUUID] = fragment.getFragmentTitle()
            }
        }
        viewModel.fragmentTitles.postValue(titles)
    }


    private fun getFragmentTitleFromPreferences(uuid: String): String {
        val fragmentDataPrefs = getSharedPreferences("FragmentData", MODE_PRIVATE)
        return fragmentDataPrefs.getString("$uuid-title", null) ?: "Loading..."
    }

    override fun onFragmentDeletionRequested(fragment: TimerFragment) {

        val uuid = adapter.getFragmentUUID(fragment)

        uuid?.let {
            removeFragmentByUUID(it)
        }
    }



    private fun removeFragmentByUUID(uuid: String) {
        val position = adapter.removeFragmentByUUID(uuid)
        Log.i("MainActivity", "removeFragmentByUUID called for $uuid @ Position $position")
        if (position != -1) {
            saveFragmentUUIDs()
            refreshViewPager()
            updateViewModel()
            adapter.notifyDataSetChanged()
        }
    }


    fun addCategoryFragment(userTitle: String) {
        val newFragment = TimerFragment.newInstance(userTitle)
        Log.i("MainActivity", "addCategory: New category ($newFragment) created")
        adapter.addFragment(newFragment)
        viewPager.setCurrentItem(adapter.itemCount - 1, true)

        saveFragmentUUIDs()
        refreshViewPager()
        updateViewModel()
        adapter.notifyDataSetChanged()


    }

    private fun saveFragmentUUIDs() {
        val fragmentUUIDs = adapter.getFragmentUUIDs().joinToString(separator = ",")
        val prefs = getSharedPreferences("FragmentPreferences", MODE_PRIVATE)
        Log.i("MainActivity", "saveFragmentUUIDs: Saved fragments ($fragmentUUIDs)")
        prefs.edit().apply {
            putString("fragmentUUIDs", fragmentUUIDs)
            apply()
        }
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("YOUR_NOTIFICATION_CHANNEL_ID", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NotificationManager::class.java) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun getFragmentItems(): List<FragmentItem> {
        Log.d("MainActivity", "getFragmentItems: Called")
        val fragmentItems = mutableListOf<FragmentItem>()
        val fragments = adapter.getFragments() // Get the list of fragments from the adapter
        for (fragment in fragments) {
            if (fragment is TimerFragment) {
                val title = fragment.getFragmentTitle()
                Log.d("MainActivity","getFragmentItems: Fragment $title get")
                val uuid = fragment.fragmentUUID
                fragmentItems.add(FragmentItem(title, uuid))
            }
        }
        return fragmentItems
    }


    // Used in MainFragment to transition to a selected TimerFragment
    fun jumpToFragment(uuid: String) {
                val position = adapter.getFragments().indexOfFirst {
            it is TimerFragment && it.fragmentUUID == uuid
        }
        if (position != -1) {
            viewPager.setCurrentItem(position, true)
        }

    }

    // Used in TimerFragment to transition to the MainFragment
    fun jumpToMain() {
        saveFragmentUUIDs()
        refreshViewPager()
        updateViewModel()
        viewPager.setCurrentItem(0,true)
    }


    private fun jumpToLast() {
        coroutineScope.launch {
            val lastPosition = adapter.itemCount + 1
            val wait = 250L
            viewPager.setCurrentItem(lastPosition, true)
            delay(wait)
            viewPager.setCurrentItem(0, true)

        }
    }

    private fun cycleFragments() {
        coroutineScope.launch {


            val fragmentCount = adapter.itemCount
            val delayTime = 250L // Momentary delay (milliseconds)
            val initDelay = fragmentCount * 50L

            delay(initDelay)
            for (position in 0 until fragmentCount) {
                viewPager.setCurrentItem(position, true)
                delay(delayTime) // Wait for a bit before moving to next fragment
            }

            // Return to MainFragment at position 0
            viewPager.setCurrentItem(0, true)
        }
    }

    override fun onFragmentMoveCompleted(fromPosition: Int, toPosition: Int) {
        // Adjust positions to account for the static MainFragment
        val adjustedFromPosition = fromPosition + 1
        val adjustedToPosition = toPosition + 1

        // Move the fragment in the ViewPager2 adapter
        adapter.moveFragment(adjustedFromPosition, adjustedToPosition)
        // Save the new order of fragments
        saveFragmentUUIDs()
        // Refresh the ViewPager to reflect changes
        refreshViewPager()
        // Update the ViewModel to reflect the new order
        updateViewModel()
    }
    // Other methods as required...
}