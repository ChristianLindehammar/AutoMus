package com.lindehammarkonsult.automus.shared.playback

import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import com.lindehammarkonsult.automus.shared.model.PlaybackState
import com.lindehammarkonsult.automus.shared.model.Track
import com.lindehammarkonsult.automus.shared.model.toMedia3Item
import com.lindehammarkonsult.automus.shared.repository.AppleMusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


private const val TAG = "AppleMusicPlayer"

/**
 * A custom Player implementation that bridges Media3 Player interface with the Apple Music SDK.
 * This implementation delegates all playback operations to the AppleMusicRepository, which
 * in turn communicates with the native Apple Music SDK.
 */
@UnstableApi
class AppleMusicPlayer(
    private val repository: AppleMusicRepository,
    private val coroutineScope: CoroutineScope
) : Player {

    private val listeners = mutableListOf<Player.Listener>()
    
    // Current state
    private var currentMediaItem: MediaItem? = null
    private var isPlaying = false
    private var playbackState = Player.STATE_IDLE
    private var currentPosition = 0L
    private var bufferedPosition = 0L
    private var repeatMode = Player.REPEAT_MODE_OFF
    private var shuffleModeEnabled = false
    private var currentTimeline = Timeline.EMPTY
    private var currentMediaItemIndex = 0
    private var mediaItemCount = 0
    private var currentTracks = listOf<Track>()
    
    init {
        // Observe changes from the repository's playback state
        coroutineScope.launch {
            repository.playbackState.collectLatest { state ->
                handlePlaybackStateUpdate(state)
            }
        }
    }
    
    /**
     * Handle updates from the AppleMusicRepository's playback state
     */
    private fun handlePlaybackStateUpdate(state: PlaybackState) {
        val wasPlaying = isPlaying
        val prevMediaItem = currentMediaItem
        
        // Update our internal state
        isPlaying = state.isPlaying
        currentPosition = state.position
        shuffleModeEnabled = state.shuffleMode
        
        // Update repeat mode
        repeatMode = when (state.repeatMode) {
            com.lindehammarkonsult.automus.shared.model.RepeatMode.NONE -> Player.REPEAT_MODE_OFF
            com.lindehammarkonsult.automus.shared.model.RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            com.lindehammarkonsult.automus.shared.model.RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        
        // Update playback state
        playbackState = if (state.buffering) {
            Player.STATE_BUFFERING
        } else if (state.isPlaying) {
            Player.STATE_READY
        } else if (state.error != null) {
            Player.STATE_IDLE
        } else {
            Player.STATE_READY
        }
        
        // Handle media item update
        state.currentTrack?.let { track ->
            // Convert Track to MediaItem
            val newMediaItem = track.toMedia3Item()
            if (currentMediaItem?.mediaId != newMediaItem.mediaId) {
                currentMediaItem = newMediaItem
                // Update listeners about the media item transition
                for (listener in listeners) {
                    listener.onMediaItemTransition(newMediaItem, Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
                }
            }
        }
        
        // Update queue (timeline)
        if (state.queue != currentTracks) {
            currentTracks = state.queue
            mediaItemCount = state.queue.size
            // Create a new timeline from the queue
            updateTimeline()
        }
        
        // Notify listeners about state changes
        if (wasPlaying != isPlaying) {
            for (listener in listeners) {
                listener.onIsPlayingChanged(isPlaying)
            }
        }
        
        if (prevMediaItem != currentMediaItem) {
            currentMediaItem?.mediaMetadata?.let { metadata ->
                for (listener in listeners) {
                    listener.onMediaMetadataChanged(metadata)
                }
            }
        }
        
        // Notify about playback state change
        for (listener in listeners) {
            listener.onPlaybackStateChanged(playbackState)
        }
    }
    
    /**
     * Update the timeline based on the current queue
     */
    private fun updateTimeline() {
        currentTimeline = object : Timeline() {
            override fun getWindowCount(): Int = mediaItemCount
            
            override fun getWindow(
                windowIndex: Int,
                window: Window,
                defaultPositionProjectionUs: Long
            ): Window {
                val item = if (windowIndex < currentTracks.size) {
                    currentTracks[windowIndex].toMedia3Item()
                } else if (currentMediaItem != null) {
                    currentMediaItem
                } else {
                    MediaItem.EMPTY
                }
                
                // In Media3, window.set() has different parameter order
                return window.set(
                    /* uid= */ Window.SINGLE_WINDOW_UID,
                    /* mediaItem= */ item,
                    /* manifest= */ null,
                    /* presentationStartTimeMs= */ C.TIME_UNSET,
                    /* windowStartTimeMs= */ C.TIME_UNSET,
                    /* elapsedRealtimeEpochOffsetMs= */ C.TIME_UNSET,
                    /* isSeekable= */ true,
                    /* isDynamic= */ false,
                    /* liveConfiguration= */ null,
                    /* defaultPositionUs= */ 0L,
                    /* durationUs= */ C.TIME_UNSET,
                    /* firstPeriodIndex= */ 0,
                    /* lastPeriodIndex= */ 0,
                    /* positionInFirstPeriodUs= */ 0L
                )
            }
            
            override fun getPeriodCount(): Int = mediaItemCount
            
            override fun getPeriod(periodIndex: Int, period: Period, setIds: Boolean): Period {
                return period.set(
                    /* id= */ periodIndex,
                    /* uid= */ periodIndex,
                    /* windowIndex= */ periodIndex,
                    /* durationUs= */ C.TIME_UNSET,
                    /* positionInWindowUs= */ 0
                )
            }
            
            override fun getIndexOfPeriod(uid: Any): Int {
                return if (uid is Int && uid >= 0 && uid < mediaItemCount) uid else C.INDEX_UNSET
            }
            
            override fun getUidOfPeriod(periodIndex: Int): Any {
                return periodIndex
            }
        }
        
        // Notify listeners about timeline change
        for (listener in listeners) {
            listener.onTimelineChanged(currentTimeline, Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
        }
    }
    
    // Player interface implementation
    
    override fun addListener(listener: Player.Listener) {
        listeners.add(listener)
    }
    
    override fun removeListener(listener: Player.Listener) {
        listeners.remove(listener)
    }
    
    override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean) {
        // In Apple Music SDK, we don't have a direct equivalent to this.
        // We'll just note the first item and play it.
        if (mediaItems.isNotEmpty()) {
            setMediaItem(mediaItems[0], resetPosition)
        }
    }
    
    override fun setMediaItems(
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ) {
        // Similar to above, but with a specific start index
        if (mediaItems.isNotEmpty() && startIndex >= 0 && startIndex < mediaItems.size) {
            val mediaItem = mediaItems[startIndex]
            setMediaItem(mediaItem, false)
            if (startPositionMs > 0) {
                seekTo(startPositionMs)
            }
        }
    }
    
    override fun setMediaItem(mediaItem: MediaItem) {
        setMediaItem(mediaItem, true)
    }
    
    override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) {
        val mediaId = mediaItem.mediaId
        currentMediaItem = mediaItem
        
        // Determine what type of media item this is and play it through the repository
        when {
            mediaId.startsWith("playlist_") -> {
                repository.playPlaylist(mediaId.removePrefix("playlist_"))
            }
            mediaId.startsWith("album_") -> {
                repository.playAlbum(mediaId.removePrefix("album_"))
            }
            else -> {
                repository.playTrack(mediaId)
            }
        }
    }
    
    override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) {
        setMediaItem(mediaItem)
        seekTo(startPositionMs)
    }
    
    override fun addMediaItem(mediaItem: MediaItem) {
        // Apple Music SDK doesn't support adding a single item to the queue
        // We'll just replace the current item if nothing is playing
        if (currentMediaItem == null) {
            setMediaItem(mediaItem)
        }
    }
    
    override fun addMediaItem(index: Int, mediaItem: MediaItem) {
        // Not directly supported in Apple Music SDK
        if (currentMediaItem == null) {
            setMediaItem(mediaItem)
        }
    }
    
    override fun addMediaItems(mediaItems: MutableList<MediaItem>) {
        // Not directly supported in Apple Music SDK
        if (currentMediaItem == null && mediaItems.isNotEmpty()) {
            setMediaItem(mediaItems[0])
        }
    }
    
    override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>) {
        // Not directly supported in Apple Music SDK
        if (currentMediaItem == null && mediaItems.isNotEmpty()) {
            setMediaItem(mediaItems[0])
        }
    }
    
    override fun setMediaItems(mediaItems: List<MediaItem>) {
        // Implementation of the abstract method
        if (mediaItems.isNotEmpty()) {
            setMediaItem(mediaItems[0])
        }
    }
    
    override fun moveMediaItem(currentIndex: Int, newIndex: Int) {
        // Not supported in Apple Music SDK
    }
    
    override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) {
        // Not supported in Apple Music SDK
    }
    
    override fun removeMediaItem(index: Int) {
        // Not supported in Apple Music SDK
    }
    
    override fun removeMediaItems(fromIndex: Int, toIndex: Int) {
        // Not supported in Apple Music SDK
    }
    
    override fun clearMediaItems() {
        repository.stopPlayback()
        currentMediaItem = null
        
        // Update listeners
        for (listener in listeners) {
            listener.onTimelineChanged(Timeline.EMPTY, Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
        }
    }
    
    override fun isCommandAvailable(command: Int): Boolean {
        return when (command) {
            Player.COMMAND_PLAY_PAUSE -> true
            Player.COMMAND_SEEK_TO_NEXT -> true
            Player.COMMAND_SEEK_TO_PREVIOUS -> true
            Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM -> true
            Player.COMMAND_SET_SHUFFLE_MODE -> true
            Player.COMMAND_SET_REPEAT_MODE -> true
            Player.COMMAND_GET_CURRENT_MEDIA_ITEM -> true
            Player.COMMAND_GET_TIMELINE -> true
            Player.COMMAND_GET_METADATA -> true
            else -> false
        }
    }

    override fun canAdvertiseSession(): Boolean {
        // Return true if this player can be advertised in the session
        // This is typically true for media players that can be controlled by external components
        return true
    }

    override fun prepare() {
        // Apple Music SDK handles preparation automatically
    }
    
    override fun play() {
        repository.resumePlayback()
    }
    
    override fun pause() {
        repository.pausePlayback()
    }
    
    override fun stop() {
        repository.stopPlayback()
    }
    
    override fun seekToDefaultPosition() {
        seekTo(0)
    }
    
    override fun seekToDefaultPosition(windowIndex: Int) {
        seekTo(windowIndex, 0)
    }
    
    override fun seekTo(positionMs: Long) {
        repository.seekTo(positionMs)
    }
    
    override fun seekTo(windowIndex: Int, positionMs: Long) {
        if (windowIndex != currentMediaItemIndex && windowIndex >= 0 && windowIndex < mediaItemCount) {
            // First try to seek to the window
            if (windowIndex > currentMediaItemIndex) {
                val steps = windowIndex - currentMediaItemIndex
                for (i in 0 until steps) {
                    repository.skipToNext()
                }
            } else {
                val steps = currentMediaItemIndex - windowIndex
                for (i in 0 until steps) {
                    repository.skipToPrevious()
                }
            }
            currentMediaItemIndex = windowIndex
        }
        
        // Then seek to the position
        seekTo(positionMs)
    }
    
    override fun setPlaybackParameters(playbackParameters: androidx.media3.common.PlaybackParameters) {
        // Not supported in Apple Music SDK
    }

    override fun setPlaybackSpeed(speed: Float) {
        // Apple Music doesn't support changing playback speed, but we need to implement this
        // Log the request but don't take any action
        Log.w(TAG, "setPlaybackSpeed: Apple Music doesn't support changing playback speed. Requested speed: $speed")
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (playWhenReady) {
            play()
        } else {
            pause()
        }
    }
    
    override fun getPlaybackState(): Int = playbackState
    
    override fun getPlayWhenReady(): Boolean = isPlaying
    
    override fun isPlaying(): Boolean = isPlaying
    
    override fun getRepeatMode(): Int = repeatMode
    
    override fun setRepeatMode(repeatMode: Int) {
        val appleMusicRepeatMode = when (repeatMode) {
            Player.REPEAT_MODE_OFF -> com.lindehammarkonsult.automus.shared.model.RepeatMode.NONE
            Player.REPEAT_MODE_ONE -> com.lindehammarkonsult.automus.shared.model.RepeatMode.ONE
            Player.REPEAT_MODE_ALL -> com.lindehammarkonsult.automus.shared.model.RepeatMode.ALL
            else -> com.lindehammarkonsult.automus.shared.model.RepeatMode.NONE
        }
        
        repository.setRepeatMode(appleMusicRepeatMode)
    }
    
    override fun getShuffleModeEnabled(): Boolean = shuffleModeEnabled
    
    override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
        repository.setShuffleMode(shuffleModeEnabled)
    }
    
    override fun getCurrentTimeline(): Timeline = currentTimeline
    
    override fun getCurrentPeriodIndex(): Int = currentMediaItemIndex
    
    override fun getCurrentWindowIndex(): Int = currentMediaItemIndex
    override fun getCurrentMediaItemIndex(): Int {
        // Return the current item index from our internal state
        return currentMediaItemIndex
    }

    override fun getNextWindowIndex(): Int {
        return if (hasNextMediaItem()) currentMediaItemIndex + 1 else C.INDEX_UNSET
    }

    override fun getNextMediaItemIndex(): Int {
        // If there's a next item, return next index, otherwise INDEX_UNSET
        return if (hasNextMediaItem()) currentMediaItemIndex + 1 else C.INDEX_UNSET
    }

    override fun getPreviousWindowIndex(): Int {
        return if (hasPreviousMediaItem()) currentMediaItemIndex - 1 else C.INDEX_UNSET
    }

    override fun getPreviousMediaItemIndex(): Int {
        // If there's a previous item, return previous index, otherwise INDEX_UNSET
        return if (hasPreviousMediaItem()) currentMediaItemIndex - 1 else C.INDEX_UNSET
    }

    override fun getCurrentMediaItem(): MediaItem? = currentMediaItem
    
    override fun getMediaItemCount(): Int = mediaItemCount
    
    override fun getMediaItemAt(index: Int): MediaItem {
        return if (index >= 0 && index < currentTracks.size) {
            currentTracks[index].toMedia3Item()
        } else {
            MediaItem.EMPTY
        }
    }
    
    override fun getDuration(): Long {
        return currentMediaItem?.mediaMetadata?.extras?.getLong("duration") ?: C.TIME_UNSET
    }
    
    override fun getCurrentPosition(): Long = currentPosition
    
    override fun getBufferedPosition(): Long = bufferedPosition
    
    override fun getBufferedPercentage(): Int {
        val duration = duration
        return if (duration > 0) ((bufferedPosition * 100) / duration).toInt() else 0
    }
    
    override fun getTotalBufferedDuration(): Long = bufferedPosition
    
    override fun isCurrentWindowDynamic(): Boolean = false
    override fun isCurrentMediaItemDynamic(): Boolean {
        // Apple Music content isn't dynamic (like live streams with changing durations),
        // so always return false
        return false
    }

    override fun isCurrentWindowLive(): Boolean = false
    override fun isCurrentMediaItemLive(): Boolean {
        // Apple Music content isn't live streaming content, so always return false
        return false
    }

    override fun getCurrentLiveOffset(): Long = C.TIME_UNSET
    
    override fun isCurrentWindowSeekable(): Boolean = true
    override fun isCurrentMediaItemSeekable(): Boolean {
        // Apple Music content is typically seekable
        return true
    }

    override fun isPlayingAd(): Boolean = false
    
    override fun getCurrentAdGroupIndex(): Int = C.INDEX_UNSET
    
    override fun getCurrentAdIndexInAdGroup(): Int = C.INDEX_UNSET
    
    override fun getContentDuration(): Long = duration
    
    override fun getContentPosition(): Long = currentPosition
    
    override fun getContentBufferedPosition(): Long = bufferedPosition
    
    override fun getAudioAttributes(): androidx.media3.common.AudioAttributes {
        return androidx.media3.common.AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
    }
    
    override fun getVolume(): Float = 1.0f
    override fun clearVideoSurface() {
        // Apple Music doesn't support video playback in this implementation
        // No-op implementation to avoid crashes
    }

    override fun clearVideoSurface(surface: Surface?) {
        // Apple Music doesn't support video playback in this implementation
        // No-op implementation to avoid crashes
    }

    override fun setVideoSurface(surface: Surface?) {
        // Apple Music doesn't support video playback in this implementation
        // No-op implementation to avoid crashes
    }

    override fun setVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        // Apple Music doesn't support video playback in this implementation
        // No-op implementation to avoid crashes
    }

    override fun clearVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        // Apple Music doesn't support video playback in this implementation
        // No-op implementation to avoid crashes
    }

    override fun setVideoSurfaceView(surfaceView: SurfaceView?) {
        // Apple Music doesn't support video playback in this implementation
        // No-op implementation to avoid crashes
    }

    override fun clearVideoSurfaceView(surfaceView: SurfaceView?) {
        // Apple Music doesn't support video playback in this implementation
        // No-op implementation to avoid crashes
    }

    override fun setVideoTextureView(textureView: TextureView?) {
        // Apple Music doesn't support video playback in this implementation
        // No-op implementation to avoid crashes
    }

    override fun clearVideoTextureView(textureView: TextureView?) {
        // Apple Music doesn't support video playback in this implementation
        // No-op implementation to avoid crashes
    }

    override fun getVideoSize(): VideoSize {
        // Apple Music doesn't support video playback in this implementation
        // Return an empty video size
        return VideoSize.UNKNOWN
    }

    override fun getSurfaceSize(): Size {
        // Apple Music doesn't support video playback in this implementation
        // Return an empty surface size
        return Size.UNKNOWN
    }

    override fun getCurrentCues(): CueGroup {
        // Apple Music doesn't support captions in this implementation
        // Return an empty cue group
        return CueGroup.EMPTY_TIME_ZERO
    }

    override fun setVolume(volume: Float) {
        // Volume control not available through Apple Music SDK
    }
    
    override fun getDeviceInfo(): androidx.media3.common.DeviceInfo {
        return androidx.media3.common.DeviceInfo.Builder(
            androidx.media3.common.DeviceInfo.PLAYBACK_TYPE_LOCAL
        ).build()
    }
    
    override fun getDeviceVolume(): Int = 100
    
    override fun isDeviceMuted(): Boolean = false
    
    override fun setDeviceVolume(volume: Int) {
        // Not supported in Apple Music SDK
    }
    
    override fun setDeviceVolume(volume: Int, flags: Int) {
        // Not supported in Apple Music SDK
    }
    
    override fun increaseDeviceVolume() {
        // Not supported in Apple Music SDK
    }
    
    override fun increaseDeviceVolume(flags: Int) {
        // Not supported in Apple Music SDK
    }
    
    override fun decreaseDeviceVolume() {
        // Not supported in Apple Music SDK
    }
    
    override fun decreaseDeviceVolume(flags: Int) {
        // Not supported in Apple Music SDK
    }
    
    override fun setDeviceMuted(muted: Boolean) {
        // Not supported in Apple Music SDK
    }
    
    override fun setDeviceMuted(muted: Boolean, flags: Int) {
        // Not supported in Apple Music SDK
    }

    override fun setAudioAttributes(audioAttributes: AudioAttributes, handleAudioFocus: Boolean) {
        // Store the audio attributes for potential future use
        // This implementation doesn't need to do anything with the attributes directly
        // since the Apple Music SDK handles audio routing internally
        
        // If handleAudioFocus is true, we would normally set up audio focus handling here
        // but that's already managed by the repository or system
        Log.d(TAG, "setAudioAttributes: content type=${audioAttributes.contentType}, usage=${audioAttributes.usage}")
    }

    override fun seekToNext() {
        repository.skipToNext()
    }
    
    override fun seekToPrevious() {
        repository.skipToPrevious()
    }

    override fun hasNext(): Boolean {
        // In a real implementation, this would check if there are more tracks in the queue
        // For now, we'll always return true to indicate there's always a next track
        // This simplifies the implementation but may not be accurate in all cases
        return true
    }

    override fun hasNextWindow(): Boolean {
        // Delegate to hasNextMediaItem for consistent behavior
        return hasNextMediaItem()
    }

    override fun seekBack() {
        val newPosition = currentPosition - 10000 // 10 seconds back
        seekTo(if (newPosition < 0) 0 else newPosition)
    }
    
    override fun seekForward() {
        val duration = duration
        val newPosition = currentPosition + 10000 // 10 seconds forward
        seekTo(if (duration != C.TIME_UNSET && newPosition > duration) duration else newPosition)
    }
    
    // Remove hasPrevious() as it's not part of the Media3 Player interface
    
    override fun hasPreviousMediaItem(): Boolean = currentMediaItemIndex > 0
    override fun seekToPreviousWindow() {
        // Go to previous window (track)
        if (hasPreviousMediaItem()) {
            seekToPreviousMediaItem()
        }
    }

    override fun seekToPreviousMediaItem() {
        // Move to the previous media item in the queue
        repository.skipToPrevious()
    }

    // Remove hasNext() as it's not part of the Media3 Player interface
    
    override fun hasNextMediaItem(): Boolean = currentMediaItemIndex < mediaItemCount - 1
    override fun next() {
        // Delegate to seekToNext for consistent behavior
        seekToNext()
    }

    override fun seekToNextWindow() {
        // Go to next window (track)
        if (hasNextMediaItem()) {
            seekToNextMediaItem()
        }
    }

    override fun seekToNextMediaItem() {
        // Move to the next media item in the queue
        repository.skipToNext()
    }

    override fun getPlaybackParameters(): androidx.media3.common.PlaybackParameters {
        return androidx.media3.common.PlaybackParameters.DEFAULT
    }
    
    override fun getPlaybackSuppressionReason(): Int = Player.PLAYBACK_SUPPRESSION_REASON_NONE
    
    override fun getPlayerError(): androidx.media3.common.PlaybackException? = null
    
    override fun getTrackSelectionParameters(): androidx.media3.common.TrackSelectionParameters {
        return androidx.media3.common.TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT
    }
    
    override fun setTrackSelectionParameters(parameters: androidx.media3.common.TrackSelectionParameters) {
        // Not supported in Apple Music SDK
    }

    override fun getMediaMetadata(): MediaMetadata {
        // Return metadata for the current media item
        return currentMediaItem?.mediaMetadata ?: MediaMetadata.EMPTY
    }

    override fun getPlaylistMetadata(): androidx.media3.common.MediaMetadata {
        return androidx.media3.common.MediaMetadata.EMPTY
    }
    
    override fun setPlaylistMetadata(mediaMetadata: androidx.media3.common.MediaMetadata) {
        // Not supported in Apple Music SDK
    }

    override fun getCurrentManifest(): Any? {
        // Apple Music doesn't use manifests in the same way as DASH/HLS content
        // Always return null as there's no manifest for the current item
        return null
    }

    override fun getSeekBackIncrement(): Long = 10000 // 10 seconds
    
    override fun getSeekForwardIncrement(): Long = 10000 // 10 seconds
    
    override fun getMaxSeekToPreviousPosition(): Long = 3000 // 3 seconds
    
    override fun isLoading(): Boolean = playbackState == Player.STATE_BUFFERING
    
    override fun getAvailableCommands(): Player.Commands {
        return Player.Commands.Builder()
            .add(Player.COMMAND_PLAY_PAUSE)
            .add(Player.COMMAND_SEEK_TO_NEXT)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
            .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
            .add(Player.COMMAND_SET_SHUFFLE_MODE)
            .add(Player.COMMAND_SET_REPEAT_MODE)
            .add(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)
            .add(Player.COMMAND_GET_TIMELINE)
            .add(Player.COMMAND_GET_METADATA)
            .build()
    }
    
    override fun release() {
        // No specific release needed
    }

    override fun getCurrentTracks(): Tracks {
        // Since Apple Music doesn't expose track selection options in the same way as Media3,
        // return an empty Tracks object
        return Tracks.EMPTY
    }

    /**
     * Returns the looper associated with the application thread that's used to access the player and
     * on which player events are received.
     */
    override fun getApplicationLooper(): Looper {
        return Looper.getMainLooper()
    }
    
    /**
     * Replaces the media item at the specified index with a new one.
     * This is not fully supported in Apple Music SDK, so we'll play the new item
     * if it's at the current index.
     */
    override fun replaceMediaItem(index: Int, mediaItem: MediaItem) {
        if (index == currentMediaItemIndex) {
            // If replacing the current item, just play the new item
            setMediaItem(mediaItem, false)
        }
        // Otherwise, we can't modify the Apple Music queue directly
    }
    
    /**
     * Replaces a range of media items with new ones.
     * This is not fully supported in Apple Music SDK, so we'll just play the first new item
     * if the current item is within the range being replaced.
     */
    override fun replaceMediaItems(fromIndex: Int, toIndex: Int, mediaItems: List<MediaItem>) {
        // Check if current playing item is within the range being replaced
        if (currentMediaItemIndex in fromIndex until toIndex && mediaItems.isNotEmpty()) {
            // Just play the first new item
            setMediaItem(mediaItems[0], false)
        }
        // Otherwise, we can't modify the Apple Music queue directly
    }
}
