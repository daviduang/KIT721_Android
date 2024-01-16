package au.edu.utas.ywang150.kit721assignment2.ui.actions
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View

/**
 * Customised swipe left action, for history items customised view (Borrowed from ChatGPT)
 */

abstract class OnSwipeTouchListener(context: Context) : View.OnTouchListener {

    private val gestureDetector: GestureDetector

    init {
        gestureDetector = GestureDetector(context, GestureListener())
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        private val swipeThreshold = 100
        private val swipeVelocityThreshold = 100

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            var result = false
            try {
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > swipeThreshold && Math.abs(velocityX) > swipeVelocityThreshold) {
                        if (diffX < 0) {
                            onSwipeLeft()
                        }
                        if (diffX > 0) {
                            onSwipeRight()
                        }
                        result = true
                    }
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
            return result
        }
    }

    abstract fun onSwipeLeft()

    abstract fun onSwipeRight()
}