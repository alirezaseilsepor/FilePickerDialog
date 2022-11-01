package app.king.filePicker

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ResultMedia(
    val id: Long,
    val name: String,
    val size: String?,
    val originalDate: Long,
    var path: String,
    val uriPath: String,
    val folderName: String,
) : Parcelable