package au.edu.utas.ywang150.kit721assignment2.ui.models

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime

@Parcelize
@IgnoreExtraProperties
class FeedEvent(
    @get:Exclude override var id : String? = null,
    val title: String = "Feed Event",
    val date_time: Timestamp = Timestamp.now(),
    var start_time: String? = null,
    var end_time: String? = null,
    var duration: String? = null,
    var type: String? = null,
    var note: String? = null,
) : HistoryItem(id), Parcelable