

package iad1tya.echo.music.playback

internal fun resolveStreamChunkLength(
    requestedLength: Long,
    position: Long,
    knownContentLength: Long?,
    chunkLength: Long,
): Long? {
    if (chunkLength <= 0L || position < 0L) return null

    val remainingLength = knownContentLength?.let { it - position }
    if (remainingLength != null && remainingLength <= 0L) return null

    val requested = if (requestedLength > 0L) requestedLength else null

    val resolvedLength =
        listOfNotNull(
            chunkLength,
            requested,
            remainingLength,
        ).minOrNull() ?: return null

    return if (resolvedLength > 0L) resolvedLength else null
}
