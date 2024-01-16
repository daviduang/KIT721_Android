package au.edu.utas.ywang150.kit721assignment2.ui

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionInflater
import androidx.transition.TransitionManager
import au.edu.utas.ywang150.kit721assignment2.FIREBASE_TAG
import au.edu.utas.ywang150.kit721assignment2.MainActivity
import au.edu.utas.ywang150.kit721assignment2.R
import au.edu.utas.ywang150.kit721assignment2.databinding.FragmentHistoryBinding
import au.edu.utas.ywang150.kit721assignment2.databinding.ItemHistoryBinding
import au.edu.utas.ywang150.kit721assignment2.ui.actions.OnSwipeTouchListener
import au.edu.utas.ywang150.kit721assignment2.ui.models.FeedEvent
import au.edu.utas.ywang150.kit721assignment2.ui.models.HistoryItem
import au.edu.utas.ywang150.kit721assignment2.ui.models.NappyEvent
import au.edu.utas.ywang150.kit721assignment2.ui.models.SleepEvent
import au.edu.utas.ywang150.kit721assignment2.ui.modules.OptionButtonModule
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlin.math.log

/**
 * History Fragment
 */
class HistoryFragment : Fragment() {

    // view binding for fragment (From ChatGPT)
    private var _ui: FragmentHistoryBinding? = null
    private val ui get() = _ui!!

    // A history list for storing record of all history items retrieved
    private var histories = mutableListOf<HistoryItem>()

    // A history list for storing view of all history items
    private var historyItems = mutableListOf<HistoryItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _ui = FragmentHistoryBinding.inflate(inflater, container, false)
        return ui.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set View Selection buttons
        ui.btnHistoryGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                val selectedButton = group.findViewById<Button>(checkedId)

                Log.d("Selected option:", selectedButton?.text.toString())

                when (checkedId) {
                    ui.btnHistoryFeed.id -> {
                        filterHistoryList("Feed")
                    }
                    ui.btnHistorySleep.id -> {
                        filterHistoryList("Sleep")
                    }
                    ui.btnHistoryNappy.id -> {
                        filterHistoryList("Nappy")
                    }
                    ui.btnHistoryAll.id -> {
                        filterHistoryList("All")
                    }
                }
            }
        }

        // get db connection
        val db = Firebase.firestore
        Log.d("FIREBASE", "Firebase connected: ${db.app.name}")

        // Retrieve all events from the database in reverse chronological order based on their timestamp
        val eventCollection = db.collection("events")
        eventCollection
            .orderBy("date_time", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                historyItems.clear() //this line clears the list, and prevents a bug where items would be duplicated upon rotation of screen
                for (document in result) {
                    val historyItem = documentToHistoryItem(document)
                    if (historyItem != null) {
                        historyItem.id = document.id
                        Log.d(FIREBASE_TAG, historyItem.toString())
                        historyItems.add(historyItem)
                        histories.add(historyItem)
                    }
                }
                (ui.listHistory.adapter as HistoryAdapter).notifyDataSetChanged()
            }

        ui.listHistory.adapter = HistoryAdapter(history = historyItems)
        ui.listHistory.layoutManager = LinearLayoutManager(context)
    }

    // ViewHolder for RecyclerView
    class HistoryHolder(var ui: ItemHistoryBinding) : RecyclerView.ViewHolder(ui.root)

    // RecyclerView Adapter
    inner class HistoryAdapter(private val history: MutableList<HistoryItem>) :
        RecyclerView.Adapter<HistoryHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryHolder {
            val ui = ItemHistoryBinding.inflate(layoutInflater, parent, false)
            return HistoryHolder(ui)
        }

        // Update the history item list
        fun updateData(newData: MutableList<HistoryItem>) {
            history.clear()
            history.addAll(newData)
            notifyDataSetChanged()
        }

        override fun onBindViewHolder(holder: HistoryHolder, position: Int) {

            // Set the initial state of the item view
            holder.itemView.isActivated = false

            // Set initial history item view with omitted note
            setView(holder,history, position, holder.itemView.isActivated)

            /*
            * Second Customised feature (Since the Alarm is not quite stable): actions for each history item
            *  1. tap: shows a full view of the event note if it is omitted
            *  2. Swipe left: shows the edit and delete button group
            *  3. Swipe Right: hide the edit and delete button group
            * */

            // Tap show the full note
            holder.itemView.setOnClickListener {
                holder.itemView.isActivated = !holder.itemView.isActivated

                // Update the history item view
                setView(holder,history, position, holder.itemView.isActivated)
            }

            // Hide edit and delete buttons for initialisation
            holder.ui.editButton.visibility = View.GONE
            holder.ui.deleteButton.visibility = View.GONE

            // Define the transitions (From chatGPT)
            val slideFromRight = TransitionInflater.from(requireContext()).inflateTransition(R.transition.slide_from_right)
            val slideToRight = TransitionInflater.from(requireContext()).inflateTransition(R.transition.slide_to_right)

            // Customised defined a left swipe action, handling the edit and delete button visibility
            holder.itemView.setOnTouchListener(object : OnSwipeTouchListener(requireContext()) {

                // When user swipe left, the edit and delete button group will show
                override fun onSwipeLeft() {
                    TransitionManager.beginDelayedTransition(holder.ui.root as ViewGroup, slideFromRight)
                    holder.ui.editButton.visibility = View.VISIBLE
                    holder.ui.deleteButton.visibility = View.VISIBLE
                }

                // When user swipe right, the edit and delete button group will hide
                override fun onSwipeRight() {
                    TransitionManager.beginDelayedTransition(holder.ui.root as ViewGroup, slideToRight)
                    holder.ui.editButton.visibility = View.GONE
                    holder.ui.deleteButton.visibility = View.GONE
                }
            })

            // Delete an event history
            val clickedItem = history[position]
            val clickedItemId = clickedItem.id
            holder.ui.deleteButton.setOnClickListener {

                val alertDialogBuilder = AlertDialog.Builder(holder.itemView.context)
                alertDialogBuilder.setTitle("Are you sure you want to delete this record?")

                // When the deletion has been confirmed by the user, delete the document from the
                // collection and refresh the recycler view
                alertDialogBuilder.setPositiveButton("Yes") { dialog, _ ->
                    dialog.dismiss()

                    // Delete this event history from database
                    val db = Firebase.firestore
                    val eventCollection = db.collection("events")

                    if (clickedItemId != null) {
                        eventCollection.document(clickedItemId)
                            .delete()
                            .addOnSuccessListener {

                                // Remove the item from the local data source and notify the adapter
                                history.removeAt(position)
                                notifyDataSetChanged()

                                Log.d("FIREBASE_TAG", "Document successfully deleted!")
                            }
                            .addOnFailureListener { exception ->
                                Log.e("FIREBASE_TAG", "Error deleting document", exception)
                            }
                    }
                }
                alertDialogBuilder.setNegativeButton("No"){ dialog, _ ->
                    dialog.dismiss()
                }
                val alertDialog = alertDialogBuilder.create()
                alertDialog.show()
            }

            // Edit an event history
            holder.ui.editButton.setOnClickListener {

                // Direct to the editing page based on the type of event history item
                when (clickedItem) {
                    is FeedEvent -> {
                        Log.d("REDIRECT_TAG", "FeedEvent")

                        // Set the custom layout as the action bar view
                        (activity as AppCompatActivity).supportActionBar?.setDisplayShowCustomEnabled(true)
                        (activity as AppCompatActivity).supportActionBar?.setCustomView(R.layout.tool_bar)

                        // Set the action bar title based on the fragment
                        (activity as MainActivity).updateActionBarTitle("Feed Event")

                        // Transfer to the corresponding fragment
                        val feedEventFragment = FeedEventFragment.newInstance(clickedItem)
                        requireActivity().supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.fragment_container, feedEventFragment)
                            .addToBackStack(null)
                            .commit()
                    }
                    is NappyEvent -> {
                        Log.d("REDIRECT_TAG", "NappyEvent")

                        // Set the custom layout as the action bar view
                        (activity as AppCompatActivity).supportActionBar?.setDisplayShowCustomEnabled(true)
                        (activity as AppCompatActivity).supportActionBar?.setCustomView(R.layout.tool_bar)

                        // Set the action bar title based on the fragment
                        (activity as MainActivity).updateActionBarTitle("Change Nappy Event")

                        // Transfer to the corresponding fragment
                        val nappyEventFragment = NappyEventFragment.newInstance(clickedItem)
                        requireActivity().supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.fragment_container, nappyEventFragment)
                            .addToBackStack(null)
                            .commit()
                    }
                    is SleepEvent -> {
                        Log.d("REDIRECT_TAG", "SleepEvent")

                        // Set the custom layout as the action bar view
                        (activity as AppCompatActivity).supportActionBar?.setDisplayShowCustomEnabled(true)
                        (activity as AppCompatActivity).supportActionBar?.setCustomView(R.layout.tool_bar)

                        // Set the action bar title based on the fragment
                        (activity as MainActivity).updateActionBarTitle("Sleep Event")

                        // Transfer to the corresponding fragment
                        val sleepEventFragment = SleepEventFragment.newInstance(clickedItem)
                        requireActivity().supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.fragment_container, sleepEventFragment)
                            .addToBackStack(null)
                            .commit()
                    }
                }
            }
        }
        override fun getItemCount(): Int {
            return history.size
        }
    }

    // Make a custom serializer for firebase (from CharGPT)
    private fun documentToHistoryItem(document: DocumentSnapshot): HistoryItem? {
        return when (document.getString("title")) {
            "Feed Event" -> document.toObject<FeedEvent>()
            "Nappy Event" -> document.toObject<NappyEvent>()
            "Sleep Event" -> document.toObject<SleepEvent>()
            else -> null
        }
    }

    // Set the view of each history item based on activation state
    private fun setView(holder: HistoryHolder,history: MutableList<HistoryItem>, position: Int, activated: Boolean) {
        if (activated) {

            // Load title with full details of different events to the view
            when (val historyItem = history[position]) {
                is FeedEvent -> {
                    holder.ui.eventItemTitle.text = historyItem.title
                    holder.ui.eventItemDetail.text = holder.itemView.context.getString(
                        R.string.feed_event_details,
                        historyItem.start_time,
                        historyItem.end_time,
                        historyItem.duration,
                        historyItem.type,
                        historyItem.note
                    )
                }
                is NappyEvent -> {
                    holder.ui.eventItemTitle.text = historyItem.title
                    holder.ui.eventItemDetail.text = holder.itemView.context.getString(
                        R.string.nappy_event_details,
                        historyItem.type,
                        historyItem.note
                    )
                }
                is SleepEvent -> {
                    holder.ui.eventItemTitle.text = historyItem.title
                    holder.ui.eventItemDetail.text = holder.itemView.context.getString(
                        R.string.sleep_event_details,
                        historyItem.start_time,
                        historyItem.end_time,
                        historyItem.duration,
                        historyItem.note
                    )
                }
            }
        } else {

            // Load title with omitted notes of different events to the view
            when (val historyItem = history[position]) {
                is FeedEvent -> {
                    holder.ui.eventItemTitle.text = historyItem.title
                    holder.ui.eventItemDetail.text = holder.itemView.context.getString(
                        R.string.feed_event_details,
                        historyItem.start_time,
                        historyItem.end_time,
                        historyItem.duration,
                        historyItem.type,
                        omittedNote(historyItem.note)
                    )
                }
                is NappyEvent -> {
                    holder.ui.eventItemTitle.text = historyItem.title
                    holder.ui.eventItemDetail.text = holder.itemView.context.getString(
                        R.string.nappy_event_details,
                        historyItem.type,
                        omittedNote(historyItem.note)
                    )
                }
                is SleepEvent -> {
                    holder.ui.eventItemTitle.text = historyItem.title
                    holder.ui.eventItemDetail.text = holder.itemView.context.getString(
                        R.string.sleep_event_details,
                        historyItem.start_time,
                        historyItem.end_time,
                        historyItem.duration,
                        omittedNote(historyItem.note)
                    )
                }
            }
        }
    }

    // Make a omitting string based on the length of the input string
    private fun omittedNote(note: String?, maxLength: Int = 7): String? {

        var omittedNote = note

        // If there is no note, return empty string
        if (note == null) {
            return ""
        }

        // If the note contains new line, drop the contents after the new line
        if (note.contains("\n"))  {

            // Remove content after newline character
            omittedNote = note.split("\n").first().replace("\n", "")
        }

        // If the note length is exceed the maximum length
        if (omittedNote != null) {
            if (omittedNote.length >= maxLength) {

                if (omittedNote != null) {
                    omittedNote = omittedNote.substring(0, note.length.coerceAtMost(maxLength))
                }
            }
        }

        // If the note has been omitted, add '...' at the end, otherwise keep the original text
        return if (omittedNote == note) {
            omittedNote
        } else {
            "$omittedNote..."
        }
    }

    // Filter the history list and update it
    private fun filterHistoryList(eventType: String) {
        historyItems = when (eventType) {
            "Feed" -> histories.filterIsInstance<FeedEvent>().toMutableList()
            "Sleep" -> histories.filterIsInstance<SleepEvent>().toMutableList()
            "Nappy" -> histories.filterIsInstance<NappyEvent>().toMutableList()
            else -> histories.toMutableList()
        }

        (ui.listHistory.adapter as HistoryAdapter).updateData(historyItems)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _ui = null
    }
}