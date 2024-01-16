package au.edu.utas.ywang150.kit721assignment2.ui

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import au.edu.utas.ywang150.kit721assignment2.FIREBASE_TAG
import au.edu.utas.ywang150.kit721assignment2.MainActivity
import au.edu.utas.ywang150.kit721assignment2.R
import au.edu.utas.ywang150.kit721assignment2.databinding.FragmentSleepEventBinding
import au.edu.utas.ywang150.kit721assignment2.ui.models.SleepEvent
import au.edu.utas.ywang150.kit721assignment2.ui.modules.TimerModule
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Sleep Event creation and edition Fragment
 */
class SleepEventFragment : Fragment() {

    private var sleepEvent: SleepEvent? = null

    // View binding (From ChatGPT)
    private var _ui: FragmentSleepEventBinding? = null
    private val ui get() = _ui!!

    // Pass data into this fragment for editing view (From ChatGPT)
    companion object {
        private const val ARG_SLEEP_EVENT = "arg_sleep_event"

        fun newInstance(sleepEvent: SleepEvent? = null): SleepEventFragment {
            val args = Bundle()
            args.putParcelable(ARG_SLEEP_EVENT, sleepEvent)
            val fragment = SleepEventFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // View binding (From ChatGPT)
        _ui = FragmentSleepEventBinding.inflate(inflater, container, false)
        return ui.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve the passed data (From ChatGPT)
        if (arguments != null) {
            sleepEvent = requireArguments().getParcelable(ARG_SLEEP_EVENT)!!
        }
        sleepEvent?.let { event ->
            // Update UI with event values
            val startTime = event.start_time
            val endTime = event.end_time
            val duration = event.duration
            val note = event.note

            // Update UI elements

            // Update Timer view

            // Hide timer buttons
            ui.btnStartPauseSleep.visibility = View.GONE
            ui.btnResetSleep.visibility = View.GONE

            // Set record
            val timerRecord = view.findViewById<ViewGroup>(R.id.timer_record)
            timerRecord.visibility = View.VISIBLE
            ui.timerValueSleep.text = duration
            ui.timerRecord.timerStartRecord.text = startTime
            ui.timerRecord.timerEndRecord.text = endTime

            // Update optional note
            ui.sleepingNote.setText(note)
        }

        // Set Go Back button
        (activity as MainActivity).setButtonClickListener(R.id.btn_back) {
            parentFragmentManager.popBackStack()
        }

        // Set Timer module
        val timerValue = view.findViewById<TextView>(R.id.timer_value_sleep)
        val startButton = view.findViewById<Button>(R.id.btn_start_pause_sleep)
        val resetButton = view.findViewById<Button>(R.id.btn_reset_sleep)
        val timerModule = TimerModule(requireContext(), timerValue, startButton, resetButton)

        (activity as MainActivity).setButtonClickListener(R.id.btn_done) {

            // Retrieve sleeping duration recorded by timer
            val duration = timerValue.text.toString()

            // Retrieve input text from optional note field
            val noteText: EditText = view.findViewById(R.id.sleeping_note)
            val inputNote: String = noteText.text.toString()

            // If the timer is not used
            if (ui.timerValueSleep.text.toString() == "00:00:00") {
                val alertDialogBuilder = AlertDialog.Builder(requireContext())
                alertDialogBuilder.setTitle("Please record the event by timer!")
                alertDialogBuilder.setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                val alertDialog = alertDialogBuilder.create()
                alertDialog.show()
            }

            else {

                // get db connection
                val db = Firebase.firestore
                Log.d("FIREBASE", "Firebase connected: ${db.app.name}")

                // If there are some data passed in this fragment, then update the db instead of create
                if (arguments != null) {

                    // Get the document
                    val sleepEventCollection = sleepEvent?.id?.let { it1 ->
                        db.collection("events").document(it1)
                    }

                    // A map of field names and their new values
                    val updates = hashMapOf<String, Any>(
                        "note" to inputNote
                    )

                    // Update the database
                    sleepEventCollection?.update(updates)
                        ?.addOnSuccessListener {
                            Log.d("Firestore", "Document successfully updated!")

                            // Success prompt
                            val alertDialogBuilder = AlertDialog.Builder(requireContext())
                            alertDialogBuilder.setTitle("Record has been updated!")
                            alertDialogBuilder.setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                            }
                        }
                        ?.addOnFailureListener { e ->
                            Log.w("Firestore", "Error updating document", e)
                        }
                } else {

                    // get the start time from the timer
                    val startTime = timerModule.getStartTime()

                    // calculate the end time from start time and duration (From ChatGPT)
                    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                    val formattedStartTime = LocalTime.parse(startTime, formatter)
                    val formattedDuration = Duration.between(LocalTime.MIN, LocalTime.parse(duration, formatter))
                    val formattedEndTime = formattedStartTime.plus(formattedDuration)
                    val endTime = formattedEndTime.format(formatter)

                    // create a new sleep event
                    val newSleepEvent = SleepEvent(
                        start_time = startTime,
                        end_time = endTime,
                        duration = duration,
                        note = inputNote
                    )

                    val sleepEventCollection = db.collection("events")

                    sleepEventCollection
                        .add(newSleepEvent)
                        .addOnSuccessListener {
                            Log.d(FIREBASE_TAG, "Document created with id ${it.id}")
                            newSleepEvent.id = it.id

                            // Show a toast message to indicate that the document has been created
                            Toast.makeText(requireContext(), "Sleep Event has been created!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Log.e(FIREBASE_TAG, "Error writing document", it)
                        }
                }
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()

        // Disable the customized action bar when leaving this fragment
        (activity as AppCompatActivity).supportActionBar?.setDisplayShowCustomEnabled(false)
    }
}