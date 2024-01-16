package au.edu.utas.ywang150.kit721assignment2.ui

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import au.edu.utas.ywang150.kit721assignment2.R
import au.edu.utas.ywang150.kit721assignment2.databinding.FragmentSummaryBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

/**
 * Summary Fragment
 */
class SummaryFragment : Fragment() {

    // view binding for fragment (From ChatGPT)
    private var _ui: FragmentSummaryBinding? = null
    private val ui get() = _ui!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _ui = FragmentSummaryBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Date Selecter button
        val dateButton = view.findViewById<TextView>(R.id.date_selecter)
        dateButton.setOnClickListener {
            showDatePickerDialog(dateButton)
        }

        // Copy the summary to click board
        ui.btnShare.setOnClickListener {
            copySummaryToClipboard()
        }
    }

    // Date picker calender view
    private fun showDatePickerDialog(tv: TextView) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // When a date has been selected and confirmed (From ChatGPT)
        val datePickerDialog = DatePickerDialog(requireContext(),
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->

                val selectedDate = "${selectedDayOfMonth}/${selectedMonth + 1}/${selectedYear}"
                tv.text = selectedDate

                // get the documents from database
                fetchEventsByDate(selectedDate)

        }, year, month, day)
        datePickerDialog.show()
    }

    // Retrieve all the events from database according to the selected date string
    private fun fetchEventsByDate(selectedDate: String) {

        // initialize the summary results
        var totalDurationLeft: Int = 0
        var totalDurationRight: Int = 0
        var totalDurationSleep: Int = 0
        var totalTimesWet = 0
        var totalTimesWetDirty = 0

        // generate a date time range for querying data
        val (startOfDay, endOfDay) = getDateRange(selectedDate)

        // get db connection
        val db = Firebase.firestore
        val eventCollection = db.collection("events")

        // retrieve data based on the generated date time range
        eventCollection
            .whereGreaterThanOrEqualTo("date_time", startOfDay)
            .whereLessThanOrEqualTo("date_time", endOfDay)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {

                    // get the duration in milliseconds
                    val durationString = document.getString("duration")
                    val durationInMilliseconds = durationString?.let {
                        toSeconds(
                            it
                        )
                    }

                    when (document.getString("title")) {
                        "Feed Event" -> {

                            when (document.getString("type")) {
                                "Breast Left" -> {

                                    if (durationInMilliseconds != null) {
                                        totalDurationLeft += durationInMilliseconds
                                    }
                                }
                                "Breast Right" -> {

                                    if (durationInMilliseconds != null) {
                                        totalDurationRight += durationInMilliseconds
                                    }
                                }
                            }
                        }
                        "Nappy Event" -> {
                            when (document.getString("type")) {
                                "Wet" -> {
                                    totalTimesWet++
                                }
                                "Wet Dirty" -> {
                                    totalTimesWetDirty++
                                }
                            }
                        }
                        "Sleep Event" -> {

                            if (durationInMilliseconds != null) {
                                totalDurationSleep += durationInMilliseconds
                            }
                        }
                    }
                }

                // set the fetched data to view
                ui.summaryDurationLeft.text = toTimeString(totalDurationLeft)
                ui.summaryDurationRight.text = toTimeString(totalDurationRight)
                ui.summaryDurationSleep.text = toTimeString(totalDurationSleep)
                ui.summaryTimesWet.text = totalTimesWet.toString()
                ui.summaryTimesDirty.text = totalTimesWetDirty.toString()

            }
            .addOnFailureListener { exception ->
                Log.e("FIREBASE_TAG", "Error in retrieving document", exception)
            }
    }

    // Calculate the range of timestamps that includes the desired date
    private fun getDateRange(selectedDate: String): Pair<Timestamp, Timestamp> {
        val dateParts = selectedDate.split("/")
        val day = dateParts[0].toInt()
        val month = dateParts[1].toInt() - 1
        val year = dateParts[2].toInt()

        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = Timestamp(calendar.time)

        calendar.set(year, month, day, 23, 59, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = Timestamp(calendar.time)

        return Pair(startOfDay, endOfDay)
    }

    // Covert a time string (hh:mm:ss) to milliseconds
    private fun toSeconds(timeString: String): Int {
        val timeParts = timeString.split(":")
        val hours = timeParts[0].toInt()
        val minutes = timeParts[1].toInt()
        val seconds = timeParts[2].toInt()

        return hours * 3600 + minutes * 60 + seconds
    }

    // Covert milliseconds to a time string: hh:mm:ss
    private fun toTimeString(inputSeconds: Int): String {

        val hours = inputSeconds / 3600
        val minutes = (inputSeconds % 3600) / 60
        val seconds = inputSeconds % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    // Copy the summary to the click board
    private fun copySummaryToClipboard() {
        val summary = """
        Duration Left: ${ui.summaryDurationLeft.text}
        Duration Right: ${ui.summaryDurationRight.text}
        Duration Sleep: ${ui.summaryDurationSleep.text}
        Times Wet: ${ui.summaryTimesWet.text}
        Times Dirty: ${ui.summaryTimesDirty.text}
        """.trimIndent()

        val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("summary", summary)
        clipboard.setPrimaryClip(clip)

        // Show a toast message to indicate that the summary has been copied
        Toast.makeText(requireContext(), "Summary copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
    super.onDestroyView()

    }
}