package com.example.cooldowns

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

//TODO: Ensure fragment titles are correctly passed to FragmentItems
//Also: Implement update after adding categories



class MainFragment : Fragment() {


    private lateinit var emptyViewMain: TextView
    private lateinit var emptyViewMainAlert: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FragmentAdapter
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val fragmentItems = mutableListOf<FragmentItem>()
    private var interactionListener: FragmentInteractionListener? = null




    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentInteractionListener) {
            interactionListener = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        recyclerView = view.findViewById(R.id.recyclerView)
        emptyViewMain = view.findViewById(R.id.emptyViewMain)
        emptyViewMainAlert = view.findViewById(R.id.emptyViewMainAlert)
        setupRecyclerView()
        setupButtons(view) // Setup button listeners
        loadFragmentItems()

        // View Model Observer1
        (activity as? MainActivity)?.viewModel?.fragmentItems?.observe(viewLifecycleOwner) { items ->
            Log.i("OBSERVER", "CHANGE OBSERVED")
            fragmentItems.clear()
            fragmentItems.addAll(items)
            adapter.notifyDataSetChanged()
            updateRecyclerView()
        }

        // ViewModel Observer2
        (activity as? MainActivity)?.viewModel?.fragmentTitles?.observe(viewLifecycleOwner) { titles ->
            Log.i("OBSERVER", "CHANGE OBSERVED")
            adapter.notifyDataSetChanged()
            updateRecyclerView()
        }



    }

    override fun onResume() {
        super.onResume()
        updateViewModel()
        updateRecyclerView()
    }



    private fun setupRecyclerView() {
        adapter = FragmentAdapter(
            fragmentItems,
            { fragmentItem ->
                // Handle item click, navigate to respective TimerFragment
                jumpToFragment(fragmentItem.uuid)
            },
            object : OnFragmentDragListener {
                override fun onFragmentMove(fromPosition: Int, toPosition: Int) {
                    // Handle the fragment move, update the main activity's view model and adapter
                    (activity as? MainActivity)?.onFragmentMove(fromPosition, toPosition)
                }
            }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Ensure "tick" animations are suppressed
        (recyclerView.itemAnimator as? androidx.recyclerview.widget.SimpleItemAnimator)?.supportsChangeAnimations = false

        val callback = FragmentDragTouchHelperCallback(adapter, recyclerView, activity as FragmentInteractionListener)
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun updateRecyclerView() {
        if (fragmentItems.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyViewMain.visibility = View.VISIBLE
            emptyViewMainAlert.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyViewMain.visibility = View.GONE
            emptyViewMainAlert.visibility = View.GONE
        }
    }


    private fun setupButtons(view: View) {
        // Initialize "+" button
        view.findViewById<Button>(R.id.btnAdd).setOnClickListener {
            showCategoryDialog()

        }

        // Other buttons as needed

    }

    private fun slowLoad() {
        coroutineScope.launch {
            val delayTime = 1000L // delay for 1 second (1000 milliseconds)

            delay(delayTime) // Wait for a second

            updateViewModel()
        }
    }

    // Method for adding categories
    private fun showCategoryDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("New Category")
            .setView(R.layout.dialog_fragment_entry)
            .setPositiveButton("OK") { dialog, _ ->
                val editText = (dialog as AlertDialog).findViewById<EditText>(R.id.categoryName)
                val userTitle = editText?.text.toString()
                if (userTitle.isNotBlank()) {
                    (activity as? MainActivity)?.addCategoryFragment(userTitle)

                }
            }
            .setNegativeButton("Cancel", null)
            .show()


    }


    private fun loadFragmentItems() {
        fragmentItems.clear()
        fragmentItems.addAll((activity as? MainActivity)?.getFragmentItems() ?: emptyList())
        Log.d("MainFragment","got $fragmentItems")
        adapter.notifyDataSetChanged()
    }

    private fun jumpToFragment(uuid: String) {

        (activity as? MainActivity)?.jumpToFragment(uuid)
    }

    private fun updateViewModel() {
        (activity as? MainActivity)?.updateViewModel()
    }

    // Other methods and interfaces...
}