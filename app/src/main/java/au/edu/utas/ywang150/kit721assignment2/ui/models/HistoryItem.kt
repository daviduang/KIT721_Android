package au.edu.utas.ywang150.kit721assignment2.ui.models

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
abstract class HistoryItem(open var id: String? = null)