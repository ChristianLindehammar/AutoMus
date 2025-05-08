package com.lindehammarkonsult.automus.ui

/**
 * Interface for activities that can display and update the mini player view.
 */
interface MediaAwareActivity {
    
    /**
     * Show the mini player at the bottom of the screen
     */
    fun showMiniPlayer()
    
    /**
     * Hide the mini player
     */
    fun hideMiniPlayer()
    
    /**
     * Navigate to the full screen now playing view
     */
    fun showNowPlaying()
}