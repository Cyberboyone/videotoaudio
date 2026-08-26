package com.nakudin.videotoaudio.domain.usecase

import android.content.Context
import com.nakudin.videotoaudio.model.ConversionRequest
import com.nakudin.videotoaudio.model.ConversionResult

/** Abstraction over the on-device audio conversion engine (Android MediaCodec). */
interface AudioConverter {

    /**
     * Run a conversion synchronously (suspending) on a background dispatcher.
     *
     * @param context  Android context, used to resolve the output directory.
     * @param request  The conversion parameters.
     * @param onProgress Called with an integer percentage (0..100) as the
     *                   conversion progresses, when such information is available.
     * @return The final [ConversionResult].
     */
    suspend fun convert(
        context: Context,
        request: ConversionRequest,
        onProgress: (Int) -> Unit
    ): ConversionResult

    /** Cancel any in-progress conversion. */
    fun cancel()
}
