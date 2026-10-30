package com.keval.fieldpintask.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone

data class PickedContact(val name: String, val phone: String)

/** Picking a phone row grants one-off read access to it, so no Contacts permission is needed. */
fun contactPickerIntent(): Intent = Intent(Intent.ACTION_PICK, Phone.CONTENT_URI)

fun readPickedContact(context: Context, uri: Uri): PickedContact? =
    context.contentResolver.query(uri, arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) PickedContact(cursor.getString(0).orEmpty(), cursor.getString(1).orEmpty()) else null
    }

fun dialIntent(phone: String): Intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null))
