package au.edu.utas.ywang150.kit721assignment2.ui.modules

import android.view.View
import android.widget.Button

/**
 * Option buttons module, used in Nappy event fragment and Feed event fragment:
 *
 * For selecting mutually exclusive events
 */
class OptionButtonModule(view: View, options: List<Int>) {

    private val view = view
    private lateinit var selectedButton: Button

    init {

        options.forEach {
            val buttonId: Int = it
            val optionButton: Button = view.findViewById(buttonId)

            // Initialise the selected button if any option button has selected from the view
            if (optionButton.isSelected) {
                selectedButton = optionButton
            }

            // Set on click listener to every option buttons
            optionButton.setOnClickListener {
                updateButtonState(optionButton, listOfOtherButtons(buttonId, options))
            }
        }
    }

    // Update isSelected state of buttons
    private fun updateButtonState(selected: Button, others: List<Button>) {
        selected.isSelected = true
        others.forEach { it.isSelected = false }
        selectedButton = selected
    }

    // Generate a list of unselected buttons
    private fun listOfOtherButtons(buttonId: Int, buttons: List<Int>): List<Button> {
        val buttonList = mutableListOf<Button>()

        buttons.forEach{
            val button: Button = view.findViewById(it)

            // Only take into the output list when the button is different from the selected button
            if (buttonId != it) {
                buttonList.add(button)
            }
        }
        return buttonList
    }

    // Get selected button if the selection has been made by user, otherwise return null
    fun getSelectedButton(): Button? {
        return if (this::selectedButton.isInitialized) {
            selectedButton
        } else {
            null
        }
    }
}