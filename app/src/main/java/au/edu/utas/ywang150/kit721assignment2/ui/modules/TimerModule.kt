package au.edu.utas.ywang150.kit721assignment2.ui.modules

import android.app.Activity
import android.content.Context
import android.os.Build
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import au.edu.utas.ywang150.kit721assignment2.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Timer module for Feed event and Sleep event fragments
 */
@RequiresApi(Build.VERSION_CODES.O)
class TimerModule(context: Context, timerDisplay: TextView, startButton: Button, resetButton: Button) {

    // Initialise timer
    private var timer: Timer = Timer()

    // Initialise the start time
    private lateinit var startTime: String

    // Initialise the end time
    private lateinit var endTime: String

    init {
        startButton.setOnClickListener{

            // Update the 'isSelected' state of start button:
            // If the timer has started, click start button will pause it, otherwise start it
            if (startButton.isSelected) {
                startButton.isSelected = false

                // Pause timer
                timer.pause()

                // Update icon and text of button
                startButton.text = "Start"
                val iconPlay =  ContextCompat.getDrawable(context, R.drawable.baseline_play_40dp)
                startButton.setCompoundDrawablesWithIntrinsicBounds(iconPlay, null, null, null)

            } else {
                startButton.isSelected = true

                // get the current time (From ChatGPT)
                val currentTime = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                startTime = currentTime.format(formatter)

                // Start timer
                timer.start { elapsedTime ->
                    val hours = elapsedTime / 3600000
                    val minutes = (elapsedTime / 60000) % 60
                    val seconds = (elapsedTime / 1000) % 60

                    // prevent the 'CalledFromWrongThreadException'
                    (context as Activity).runOnUiThread {
                        timerDisplay.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    }

                }

                // Update icon and text of button
                startButton.text = "Pause"
                val iconPause =  ContextCompat.getDrawable(context, R.drawable.baseline_pause_40dp)
                startButton.setCompoundDrawablesWithIntrinsicBounds(iconPause, null, null, null)
            }
        }
        resetButton.setOnClickListener {
            timer.reset()
            timerDisplay.text = "00:00:00"
        }
    }

    // Return the start time of each record
    fun getStartTime(): String? {
        return if (this::startTime.isInitialized) {
            startTime
        } else {
            null
        }
    }

}