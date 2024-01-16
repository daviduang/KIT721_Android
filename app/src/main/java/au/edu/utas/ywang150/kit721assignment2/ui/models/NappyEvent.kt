package au.edu.utas.ywang150.kit721assignment2.ui.models

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime

@Parcelize
@IgnoreExtraProperties
class NappyEvent(
    @get:Exclude override var id : String? = null,
    val title: String = "Nappy Event",
    val date_time: Timestamp = Timestamp.now(),
    var type: String? = null,
    var imageUrl: String? = null,
    var note: String? = null,
) : HistoryItem(id), Parcelable