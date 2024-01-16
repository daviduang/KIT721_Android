package au.edu.utas.ywang150.kit721assignment2.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import au.edu.utas.ywang150.kit721assignment2.MainActivity
import au.edu.utas.ywang150.kit721assignment2.R
import au.edu.utas.ywang150.kit721assignment2.ui.modules.AlarmModule
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * Home fragment
 */
class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* Link buttons with fragments */
        setButtonListener(R.id.btn_sleep, SleepEventFragment())
        setButtonListener(R.id.btn_feed, FeedEventFragment())
        setButtonListener(R.id.btn_nappy, NappyEventFragment())

        /* Set the alarm button (Customized feature) */
        val alarmButton = view.findViewById<Button>(R.id.btn_alarm)

        // Load the 'isSelected' state for alarm button from SharedPreferences
        val sharedPref = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val alarmButtonState = sharedPref.getBoolean("AlarmButtonState", false)
        alarmButton.isSelected = alarmButtonState

        alarmButton.setOnClickListener {

            // Update the isSelected state
            alarmButton.isSelected = !alarmButton.isSelected

            // Store the alarm button 'isSelected' state inside SharedPreferences
            val preferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putBoolean("AlarmButtonState", alarmButton.isSelected)
            editor.apply()

            val alarmModule = AlarmModule()

            if (alarmButton.isSelected) {

                /* Calculate the daily wake up alarm time (Average of sleep end time) */

                // get db connection
                val db = Firebase.firestore
                Log.d("FIREBASE", "Firebase connected: ${db.app.name}")

                // Get a reference to the 'events' collection
                val eventsCollection = db.collection("events")

                // Filter out the sleeping events
                val sleepingEventsQuery = eventsCollection.whereEqualTo("title", "Sleep Event")

                // Calculate the average end time of the sleeping events
                sleepingEventsQuery.get().addOnSuccessListener { querySnapshot ->
                    var totalHours = 0
                    var totalMinutes = 0
                    var totalSeconds = 0
                    var count = 0
                    for (document in querySnapshot.documents) {

                        // If the end time string is not empty, increment the hours, minutes and seconds
                        val endTime = document.getString("end_time")
                        if (endTime != null) {
                            val parts = endTime.split(":")
                            if (parts.size == 3) {
                                totalHours += parts[0].toInt()
                                totalMinutes += parts[1].toInt()
                                totalSeconds += parts[2].toInt()
                                count++
                            }
                        }
                    }

                    // If there is no recorded time found from the database, show warning message
                    if (totalHours == 0 && totalMinutes == 0 && totalSeconds == 0) {
                        Toast.makeText(requireContext(),
                            "Insufficient Sleep Record, please record some sleeping event!",
                            Toast.LENGTH_SHORT).show()
                    }

                    // Calculate the average end time hour, minute and second for wake up alarm
                    val averageHours = if (count > 0) totalHours / count else 0
                    val averageMinutes = if (count > 0) totalMinutes / count else 0
                    val averageSeconds = if (count > 0) totalSeconds / count else 0

                    // Schedule the daily alarm
                    alarmModule.scheduleDailyAlarm(requireContext(),
                        averageHours, averageMinutes, averageSeconds)

                    Log.d("FIREBASE_TAG", "Average end(wake up) time of sleeping events:" +
                            " $averageHours:$averageMinutes:$averageSeconds")
                }.addOnFailureListener { e ->
                    Log.w("FIREBASE_TAG", "Error getting sleeping events", e)
                }

            } else {

                // Cancel the daily alarm
                alarmModule.cancelAlarm(requireContext())
            }
        }
    }

    /* Set button listener with fragment */
    private fun setButtonListener(buttonId: Int, fragment: Fragment) {

        val button = view?.findViewById<Button>(buttonId)

        button?.setOnClickListener() {

            // Set the custom layout as the action bar view
            (activity as AppCompatActivity).supportActionBar?.setDisplayShowCustomEnabled(true)
            (activity as AppCompatActivity).supportActionBar?.setCustomView(R.layout.tool_bar)

            // Set the action bar title based on the fragment
            when (button.text) {
                "Feed" -> (activity as MainActivity).updateActionBarTitle("Feed Event")
                "Sleep" -> (activity as MainActivity).updateActionBarTitle("Sleep Event")
                "Change Nappy" -> (activity as MainActivity).updateActionBarTitle("Change Nappy Event")
            }

            // Transfer to a fragment
            val transaction = parentFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}