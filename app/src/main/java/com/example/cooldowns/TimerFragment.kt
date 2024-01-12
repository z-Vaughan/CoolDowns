package com.example.cooldowns


import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.UUID


class TimerFragment : Fragment() {

    private lateinit var recyclerViewFragment: RecyclerView
    private lateinit var emptyViewFragment: TextView
    private lateinit var emptyViewFragmentAlert: ImageView
    private lateinit var timerAdapter: TimerAdapter
    private val timerEntries = mutableListOf<TimerEntry>()
    private val updateHandler = Handler(Looper.getMainLooper())

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateTimers()
            updateHandler.postDelayed(this, 1000)
        }
    }

    var fragmentUUID: String = UUID.randomUUID().toString() // Unique identifier
    private var fragmentTitle: String = "Loading..." // User-defined title


    companion object {
        fun newInstance(userTitle: String, uuid: String? = null): TimerFragment {
            val fragment = TimerFragment()
            val args = Bundle()
            args.putString("fragmentTitle", userTitle)
            // Generate new UUID if not provided, else use the existing one
            args.putString("fragmentUUID", uuid ?: UUID.randomUUID().toString())
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            fragmentTitle = it.getString("fragmentTitle", null)
            // Use existing UUID
            fragmentUUID = it.getString("fragmentUUID", UUID.randomUUID().toString())
        }
    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_timer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        recyclerViewFragment = view.findViewById(R.id.recyclerViewFragment)
        emptyViewFragment = view.findViewById(R.id.emptyViewFragment)
        emptyViewFragmentAlert = view.findViewById(R.id.emptyViewFragmentAlert)
        // Setup UI components
        setupRecyclerView()
        loadTimers() // Load persisted timers
        updateRecyclerView()
        setupButtons(view) // Setup button listeners
        updateFragmentTitle(view) // Update the fragment title


    }

    private fun setupRecyclerView() {

        timerAdapter = TimerAdapter(timerEntries, { position, formattedTime ->
            timerAdapter.notifyItemChanged(position, formattedTime)
        }, { position ->
            deleteTimer(position)
        })
        recyclerViewFragment.adapter = timerAdapter
        recyclerViewFragment.layoutManager = LinearLayoutManager(context)

        // Disabling animation changes in RecyclerView for smoother experience.
        (recyclerViewFragment.itemAnimator as? androidx.recyclerview.widget.SimpleItemAnimator)?.supportsChangeAnimations = false

        //Enable Drag-and-Drop
        val callback = TimerDragTouchHelperCallback(timerAdapter, recyclerViewFragment)
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerViewFragment)
    }

    private fun updateRecyclerView() {
        if (timerEntries.isEmpty()) {
            recyclerViewFragment.visibility = View.GONE
            emptyViewFragment.visibility = View.VISIBLE
            emptyViewFragmentAlert.visibility = View.VISIBLE
        } else {
            recyclerViewFragment.visibility = View.VISIBLE
            emptyViewFragment.visibility = View.GONE
            emptyViewFragmentAlert.visibility = View.GONE
        }
    }

    private fun setupButtons(view: View) {
        view.findViewById<Button>(R.id.btnAdd).setOnClickListener {
            showAddTimerDialog()
        }
        view.findViewById<ImageButton>(R.id.btnDeleteFragment).setOnClickListener {
            deleteFragmentDialog()
        }
        view.findViewById<ImageButton>(R.id.btnJumpToMain).setOnClickListener {
            jumpToMain()
        }
    }

    private fun updateFragmentTitle(view: View) {
        view.findViewById<TextView>(R.id.tvFragmentTitle)?.text = fragmentTitle
    }

    fun getFragmentTitle(): String {
        return fragmentTitle
    }

    override fun onResume() {
        super.onResume()
        updateHandler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        updateHandler.removeCallbacks(updateRunnable)
        saveFragmentData() // Save fragment data when fragment is paused
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("TimerFragment", "$fragmentUUID , $fragmentTitle : Destroyed")
    }

    private fun updateTimers() {
        timerEntries.forEachIndexed { index, timerEntry ->
            timerEntry.updateBasedOnSystemTime()
            timerAdapter.notifyItemChanged(index)
        }
    }

    private fun showAddTimerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_timer_entry, null)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add New Timer")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                processNewTimerEntry(dialogView)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    // Function to process new timer entry from dialog.
    private fun processNewTimerEntry(dialogView: View) {
        val nameInput = dialogView.findViewById<EditText>(R.id.editTextName)
        val hoursInput = dialogView.findViewById<EditText>(R.id.editTextHours)
        val minutesInput = dialogView.findViewById<EditText>(R.id.editTextMinutes)
        val secondsInput = dialogView.findViewById<EditText>(R.id.editTextSeconds)

        // Extracting the input values.
        val name = nameInput?.text.toString()
        val hours = hoursInput?.text.toString().toIntOrNull() ?: 0
        val minutes = minutesInput?.text.toString().toIntOrNull() ?: 0
        val seconds = secondsInput?.text.toString().toIntOrNull() ?: 0

        // Validating and adding the new timer entry.
        if (name.isNotBlank() && (hours >= 0 && minutes >= 0 && seconds >= 0)) {
            addTimerEntry(name, hours, minutes, seconds)
        } else {
            showToast("Invalid input")
        }
    }

    private fun addTimerEntry(name: String, hours: Int, minutes: Int, seconds: Int) {
        val newEntry = TimerEntry(name, hours, minutes, seconds) { formattedTime ->
            // Update logic for timer (placeholder).
        }
        timerEntries.add(newEntry)
        // Use requireContext() to get the context for the TimerEntry
        newEntry.startCountdown(requireContext())
        timerAdapter.notifyItemInserted(timerEntries.size - 1)
        updateRecyclerView()
    }

    private fun deleteTimer(position: Int) {
        timerEntries[position].stopCountdown(requireContext())
        timerEntries.removeAt(position)
        timerAdapter.notifyItemRemoved(position)
        updateRecyclerView()
    }


    private fun deleteFragmentDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_fragment, null)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm Delete Category")
            .setView(dialogView)
            .setPositiveButton("Yes") { _, _ ->
                requestFragmentDeletion()
            }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun requestFragmentDeletion() {
        Log.d("TimerFragment", "$fragmentTitle - $fragmentUUID - Requesting Destroy")
        (activity as? FragmentInteractionListener)?.onFragmentDeletionRequested(this)

    }


    private fun saveFragmentData() {
        Log.d("TimerFragment","Fragment with UUID: $fragmentUUID and Title: $fragmentTitle saved")
        val sharedPreferences = requireContext().getSharedPreferences("FragmentData", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("$fragmentUUID-title", fragmentTitle)
        val serializedTimers = timerEntries.joinToString(separator = "|") { it.serialize() }
        editor.putString("$fragmentUUID-timers", serializedTimers)
        editor.apply()
    }

    private fun loadTimers() {
        val sharedPreferences = requireContext().getSharedPreferences("FragmentData", MODE_PRIVATE)
        val serializedTimers = sharedPreferences.getString("$fragmentUUID-timers", null) ?: return
        timerEntries.clear()
        serializedTimers.split("|").filter { it.isNotEmpty() }.forEach { serializedTimer ->
            TimerEntry.deserialize(serializedTimer) { formattedTime ->
                // Timer update logic
            }?.let { timerEntries.add(it) }
        }
        timerAdapter.notifyDataSetChanged()

    }



    // Function to display a short toast message.
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }


    private fun jumpToMain() {
        (activity as? MainActivity)?.jumpToMain()
    }
    // Additional methods as required...
}
