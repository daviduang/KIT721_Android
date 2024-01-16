package au.edu.utas.ywang150.kit721assignment2.ui.modules

import android.widget.Button
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * Timer Class (From CharGPT)
 */
class Timer() {

    private var interval: Long = 1000
    private var startTime: Long = 0
    private var pauseTime: Long = 0
    private var resetTime: Long = 0
    private var isRunning: Boolean = false

    @OptIn(DelicateCoroutinesApi::class)
    fun start(onTick: (Long) -> Unit) {
        if (!isRunning) {
            startTime = System.currentTimeMillis() - pauseTime - resetTime
            isRunning = true
            GlobalScope.launch {
                while (isRunning) {
                    val elapsedTime = System.currentTimeMillis() - startTime
                    onTick(elapsedTime)
                    delay(interval)
                }
            }
        }
    }

    fun pause() {
        isRunning = false
        pauseTime = System.currentTimeMillis() - startTime
    }

    fun reset() {
        isRunning = false
        startTime = 0
        pauseTime = 0
        resetTime = 0
    }
}