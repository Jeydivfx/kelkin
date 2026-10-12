package com.example.kelkin.utils

import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi

internal fun subtitleMenuAction(ignoredFlags: Int, hasTextOverride: Boolean, textDisabled: Boolean): Int =
    if (ignoredFlags != C.SELECTION_FLAG_FORCED.inv()) 0
    else if (hasTextOverride) 1
    else if (!textDisabled) -1
    else 0

/** Media3 1.2's subtitle menu signals None using ignored flags, rather than disabling text. */
@UnstableApi
internal fun normalizeSubtitleSelection(parameters: TrackSelectionParameters): TrackSelectionParameters {
    val explicitlySelected = parameters.overrides.keys.any { it.type == C.TRACK_TYPE_TEXT }
    return when (subtitleMenuAction(parameters.ignoredTextSelectionFlags, explicitlySelected,
        parameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT))) {
        1 -> parameters.buildUpon()
            .setIgnoredTextSelectionFlags(0)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .build()
        -1 -> parameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        else -> parameters
    }
}
