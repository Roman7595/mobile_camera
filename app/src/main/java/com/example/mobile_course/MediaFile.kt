package com.example.mobile_course

import android.net.Uri
import kotlinx.serialization.Serializable


@Serializable
class MediaFile(val uri: String, val type: String, val creationDate: String) : java.io.Serializable