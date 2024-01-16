package au.edu.utas.ywang150.kit721assignment2

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import au.edu.utas.ywang150.kit721assignment2.ui.HistoryFragment
import au.edu.utas.ywang150.kit721assignment2.ui.HomeFragment
import au.edu.utas.ywang150.kit721assignment2.ui.SummaryFragment

// Login tag
const val FIREBASE_TAG = "FirebaseLogging"

class MainActivity : AppCompatActivity() {

    // Initialize fragments
    private val homeFragment = HomeFragment()
    private val historyFragment = HistoryFragment()
    private val summaryFragment = SummaryFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set up fragment connection with navigation bar
        replaceFragment(homeFragment)
        val navigationView = findViewById<BottomNavigationView>(R.id.nav_view)
        navigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> replaceFragment(homeFragment)
                R.id.navigation_history -> replaceFragment(historyFragment)
                R.id.navigation_summary -> replaceFragment(summaryFragment)
            }
            true
        }
    }

    // Replace the fragment from fragment container (From https://www.youtube.com/watch?v=v8MbOjBCu0o)
    private fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
    }

    // Update the action bar title
    fun updateActionBarTitle(newTitle: String) {
        val actionBarTitle = findViewById<TextView>(R.id.event_item_title)
        actionBarTitle.text = newTitle
    }

    // Set a listener to an icon
    fun setButtonClickListener(btnId: Int, onClickListener: View.OnClickListener) {
        val icon = findViewById<ImageView>(btnId)
        icon.setOnClickListener(onClickListener)
    }
}