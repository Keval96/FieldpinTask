package com.keval.fieldpintask.ui.common

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.keval.fieldpintask.R
import com.keval.fieldpintask.domain.model.SiteStatus
import com.keval.fieldpintask.ui.theme.StatusActive
import com.keval.fieldpintask.ui.theme.StatusClosed
import com.keval.fieldpintask.ui.theme.StatusNeedsRepair

@get:StringRes
val SiteStatus.label: Int
    get() = when (this) {
        SiteStatus.ACTIVE -> R.string.status_active
        SiteStatus.NEEDS_REPAIR -> R.string.status_needs_repair
        SiteStatus.CLOSED -> R.string.status_closed
    }

val SiteStatus.color: Color
    get() = when (this) {
        SiteStatus.ACTIVE -> StatusActive
        SiteStatus.NEEDS_REPAIR -> StatusNeedsRepair
        SiteStatus.CLOSED -> StatusClosed
    }
