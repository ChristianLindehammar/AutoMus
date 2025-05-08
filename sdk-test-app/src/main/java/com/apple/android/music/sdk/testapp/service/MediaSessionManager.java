package com.apple.android.music.sdk.testapp.service;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ResultReceiver;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.util.Pair;

import com.apple.android.music.playback.model.MediaContainerType;
import com.apple.android.music.playback.model.MediaItemType;
import com.apple.android.music.playback.model.MediaPlayerException;
import com.apple.android.music.playback.model.ErrorConditionException;
import com.apple.android.music.playback.model.PlaybackRepeatMode;
import com.apple.android.music.playback.model.PlaybackShuffleMode;
import com.apple.android.music.playback.model.PlaybackState;
import com.apple.android.music.playback.controller.MediaPlayerController;
import com.apple.android.music.playback.model.PlayerMediaItem;
import com.apple.android.music.playback.model.PlayerQueueItem;
import com.apple.android.music.playback.queue.CatalogPlaybackQueueItemProvider;
import com.apple.android.music.playback.queue.PlaybackQueueInsertionType;
import com.apple.android.music.playback.queue.PlaybackQueueItemProvider;
import com.apple.android.music.sdk.testapp.R;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;


final class MediaSessionManager extends MediaSessionCompat.Callback implements MediaPlayerController.Listener, Handler.Callback {

    private static final String TAG = "MediaSessionManager";
    private static final DateFormat RELEASE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final int MESSAGE_INIT = 1;
    private static final int MESSAGE_LOAD_ARTWORK = 2;
    private static final int MESSAGE_UPDATE_ARTWORK = 3;

    private final Context context;
    private final MediaPlayerController playerController;
    private final MediaSessionCompat mediaSession;
    private final MediaMetadataCompat.Builder metadataBuilder;
    private MediaMetadataCompat metadata;
    private final PlaybackStateCompat.Builder playbackStateBuilder;
    private final Handler backgroundHandler;
    private final Handler mainHandler;
    private final int artworkWidth;
    private final int artworkHeight;
    private AlbumArtworkTarget artworkTarget;
    private ArrayList<MediaSessionCompat.QueueItem> queueItems;


    MediaSessionManager(@NonNull Context context, @NonNull Handler backgroundHandler, @NonNull MediaPlayerController playerController, @NonNull MediaSessionCompat mediaSession) {
        this.context = context;
        this.playerController = playerController;
        this.playerController.addListener(this);
        this.mediaSession = mediaSession;
        metadataBuilder = new MediaMetadataCompat.Builder();
        playbackStateBuilder = new PlaybackStateCompat.Builder();
        this.backgroundHandler = new Handler(backgroundHandler.getLooper(), this);
        mainHandler = new Handler(Looper.getMainLooper(), this);
        artworkWidth = context.getResources().getDimensionPixelSize(R.dimen.notification_artwork_width);
        artworkHeight = context.getResources().getDimensionPixelSize(R.dimen.notification_artwork_width);
        this.backgroundHandler.sendEmptyMessage(MESSAGE_INIT);
    }


    @Override
    public void onCommand(String command, Bundle extras, ResultReceiver cb) {
        extras.setClassLoader(getClass().getClassLoader());
        if (MediaControllerCommand.COMMAND_REMOVE_QUEUE_ITEM.equals(command)) {
            playerController.removeQueueItemWithId(extras.getLong(MediaControllerCommand.COMMAND_ARGUMENT_PLAYBACK_QUEUE_ID));
        } else if (MediaControllerCommand.COMMAND_MOVE_QUEUE_ITEM.equals(command)) {
            final long sourceItemId = extras.getLong(MediaControllerCommand.COMMAND_ARGUMENT_PLAYBACK_QUEUE_ID);
            final long targetItemId = extras.getLong(MediaControllerCommand.COMMAND_ARGUMENT_PLAYBACK_QUEUE_ID_TARGET);
            final int moveTargetType = extras.getInt(MediaControllerCommand.COMMAND_ARGUMENT_PLAYBACK_QUEUE_MOVE_TARGET_TYPE);
            playerController.moveQueueItemWithId(sourceItemId, targetItemId, moveTargetType);
        } else if (MediaControllerCommand.COMMAND_ADD_QUEUE_ITEMS.equals(command)) {
            final PlaybackQueueItemProvider provider = extras.getParcelable(MediaControllerCommand.COMMAND_ARGUMENT_PLAYBACK_QUEUE_ITEM_PROVIDER);
            final @PlaybackQueueInsertionType int insertionType = extras.getInt(MediaControllerCommand.COMMAND_ARGUMENT_PLAYBACK_QUEUE_INSERTION_TYPE);
            playerController.addQueueItems(provider, insertionType);
        }
    }


    @Override
    public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
        return false;
    }


    @Override
    public void onPrepare() {
    }


    @Override
    public void onPrepareFromMediaId(String mediaId, Bundle extras) {
    }


    @Override
    public void onPrepareFromSearch(String query, Bundle extras) {
    }


    @Override
    public void onPrepareFromUri(Uri uri, Bundle extras) {
    }


    @Override
    public void onPlay() {
        playerController.play();
    }


    @Override
    public void onPlayFromMediaId(String mediaId, Bundle extras) {
        CatalogPlaybackQueueItemProvider.Builder builder = new CatalogPlaybackQueueItemProvider.Builder();
        int containerType = MediaContainerType.NONE;
        int itemType = MediaItemType.UNKNOWN;
        if (extras != null) {
            containerType = extras.getInt("containerType", MediaContainerType.NONE);
            itemType = extras.getInt("itemType", MediaItemType.UNKNOWN);
        }
        if (containerType != MediaContainerType.NONE) {
            builder.containers(containerType, mediaId);
        } else {
            builder.items(itemType, mediaId);
        }
        playerController.prepare(builder.build(), true);
    }


    @Override
    public void onPlayFromSearch(String query, Bundle extras) {
    }


    @Override
    public void onPlayFromUri(Uri uri, Bundle extras) {
    }


    @Override
    public void onSkipToQueueItem(long id) {
        playerController.skipToQueueItemWithId(id);
    }


    @Override
    public void onPause() {
        playerController.pause();
    }


    @Override
    public void onSkipToNext() {
        playerController.skipToNextItem();
    }


    @Override
    public void onSkipToPrevious() {
        playerController.skipToPreviousItem();
    }


    @Override
    public void onFastForward() {
    }


    @Override
    public void onRewind() {
    }


    @Override
    public void onStop() {
        playerController.stop();
    }


    @Override
    public void onSeekTo(long pos) {
        playerController.seekToPosition(pos);
    }


    @Override
    public void onSetRating(RatingCompat rating) {
    }


    @Override
    public void onSetRepeatMode(int repeatMode) {
    }


    @Override
    public void onSetShuffleMode(int shuffleMode) {
    }


    @Override
    public void onCustomAction(String action, Bundle extras) {
    }


    @Override
    public void onAddQueueItem(MediaDescriptionCompat description) {
    }


    @Override
    public void onAddQueueItem(MediaDescriptionCompat description, int index) {
        Log.d(TAG, "onRemoveQueueItem()");
    }


    @Override
    public void onRemoveQueueItem(MediaDescriptionCompat description) {
        Log.d(TAG, "onRemoveQueueItem()");
    }


    @Override
    public void onPlayerStateRestored(@NonNull MediaPlayerController playerController) {
        Log.d(TAG, "onPlayerStateRestored()");
    }

    @Override
    public void onPlaybackStateChanged(@NonNull MediaPlayerController playerController, int previousState, int currentState) {
        Log.d(TAG, "onPlaybackStateChanged() prevState: " + previousState + " currentState: " + currentState);
        updatePlaybackState(currentState, playerController.isBuffering());
    }

    @Override
    public void onPlaybackStateUpdated(@NonNull MediaPlayerController playerController) {
        Log.d(TAG, "onPlaybackStateUpdated()");
    }


    @Override
    public void onBufferingStateChanged(@NonNull MediaPlayerController playerController, boolean buffering) {
        Log.d(TAG, "onBufferingStateChanged() buffering: " + buffering);
        updatePlaybackState(playerController.getPlaybackState(), buffering);
    }


    @Override
    public void onCurrentItemChanged(@NonNull MediaPlayerController playerController, @Nullable PlayerQueueItem previousItem, @Nullable PlayerQueueItem currentItem) {
        Log.d(TAG, "onCurrentItemChanged() prevItemQueueId: " + (previousItem != null ? previousItem.getPlaybackQueueId() : -1)
        + " currItemQueueId: " + (currentItem != null ? currentItem.getPlaybackQueueId() : -1));
        updateMetaData(previousItem, currentItem);
        updatePlaybackState(playerController.getPlaybackState(), playerController.isBuffering());
    }

    @Override
    public void onItemEnded(@NonNull MediaPlayerController playerController, @NonNull PlayerQueueItem queueItem, long endPosition) {
        Log.d(TAG, "onItemEnded() queueItem: " + queueItem.getPlaybackQueueId() + " endPosition: " + endPosition);
    }

    @Override
    public void onMetadataUpdated(@NonNull MediaPlayerController playerController, @NonNull PlayerQueueItem currentItem) {
        Log.d(TAG, "onMetadataUpdated() item: " + currentItem.getPlaybackQueueId());
        updateMetaData(null, currentItem);
    }

    @Override
    public void onPlaybackQueueChanged(@NonNull MediaPlayerController playerController, @NonNull List<PlayerQueueItem> playbackQueueItems) {
        Log.d(TAG, "onPlaybackQueueChanged() numOfItems: " + playbackQueueItems.size());
        updateQueueItems(playbackQueueItems);
    }

    @Override
    public void onPlaybackQueueItemsAdded(@NonNull MediaPlayerController playerController, int queueInsertionType, int containerType, int itemType) {
        Log.d(TAG, "onPlaybackQueueItemsAdded() insertionType: " + queueInsertionType + " containerType: " + containerType + " itemType: " + itemType);
    }

    @Override
    public void onPlaybackError(@NonNull MediaPlayerController playerController, @NonNull MediaPlayerException error) {
        Log.d(TAG, "onPlaybackError()");
        Throwable t = error.getCause();
        if (t instanceof ErrorConditionException) {
            ErrorConditionException errorCondition = (ErrorConditionException)t;
            Log.d(TAG, "onPlaybackError() errorCode: " + errorCondition.getErrorCode());
        }
    }

    @Override
    public void onPlaybackRepeatModeChanged(@NonNull MediaPlayerController playerController, @PlaybackRepeatMode int currentRepeatMode) {
        Log.d(TAG, "onPlaybackRepeatModeChanged()");
        mediaSession.setRepeatMode(convertRepeatMode(currentRepeatMode));
    }


    @Override
    public void onPlaybackShuffleModeChanged(@NonNull MediaPlayerController playerController, @PlaybackShuffleMode int currentShuffleMode) {
        Log.d(TAG, "onPlaybackShuffleModeChanged()");
        mediaSession.setShuffleMode(convertShuffleMode(currentShuffleMode));
    }


    @Override
    @SuppressWarnings("unchecked")
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_INIT:
                init();
                return true;
            case MESSAGE_LOAD_ARTWORK:
                String artworkUrl = (String)msg.obj;
                if (artworkTarget != null) {
                    Picasso.with(context).cancelRequest(artworkTarget);
                }
                artworkTarget = new AlbumArtworkTarget(artworkUrl);
                Picasso.with(context).load(artworkUrl).into(artworkTarget);
                return true;
            case MESSAGE_UPDATE_ARTWORK:
                Pair<String, Bitmap> artworkData = (Pair<String, Bitmap>)msg.obj;
                if (artworkData.first.equals(metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI))) {
                    metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artworkData.second);
                    metadata = metadataBuilder.build();
                    mediaSession.setMetadata(metadata);
                }
                return true;
        }
        return false;
    }


    private void init() {
        queueItems = new ArrayList<>();
        updatePlaybackState(playerController.getPlaybackState(), playerController.isBuffering());
        updateMetaData(null, playerController.getCurrentItem());
        updateQueueItems(playerController.getQueueItems());
    }


    private void updatePlaybackState(@PlaybackState int currentState, boolean buffering) {
        playbackStateBuilder.setState(convertPlaybackState(currentState, buffering), playerController.getCurrentPosition(), playerController.getPlaybackRate());
        playbackStateBuilder.setBufferedPosition(playerController.getBufferedPosition());
        playbackStateBuilder.setActions(allowedActions(playerController));
        mediaSession.setPlaybackState(playbackStateBuilder.build());
        mediaSession.setActive(currentState != PlaybackState.STOPPED);
    }


    private void updateMetaData(PlayerQueueItem previousItem, PlayerQueueItem currentItem) {
        String title = null;
        String albumTitle = null;
        String artistName = null;
        String albumArtistName = null;
        long duration = -1;
        String releaseDate = null;
        String genreName = null;
        String composerName = null;
        String url = null;
        String artworkUrl = null;

        if (currentItem != null) {
            final PlayerMediaItem item = currentItem.getItem();
            title = item.getTitle();
            albumTitle = item.getAlbumTitle();
            artistName = item.getArtistName();
            albumArtistName = item.getAlbumArtistName();
            duration = item.getDuration();
            releaseDate = formatReleaseDate(item.getReleaseDate());
            genreName = item.getGenreName();
            composerName = item.getComposerName();
            url = item.getUrl();
            artworkUrl = item.getArtworkUrl(artworkWidth, artworkHeight);
        }

        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, title);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, albumTitle);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artistName);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, albumArtistName);
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DATE, releaseDate);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_GENRE, genreName);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_COMPOSER, composerName);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, url);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, artworkUrl);
        // TODO: Add additional fields

        boolean artworkChanged = loadAlbumArtwork(currentItem, previousItem);
        if (artworkChanged) {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null);
        }

        metadata = metadataBuilder.build();
        mediaSession.setMetadata(metadata);
    }


    private void updateQueueItems(List<PlayerQueueItem> playbackQueueItems) {
        final int itemCount = playbackQueueItems.size();
        queueItems.clear();
        queueItems.ensureCapacity(itemCount);
        final MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder();
        for (int i = 0; i < itemCount; i++) {
            final PlayerQueueItem queueItem = playbackQueueItems.get(i);
            final PlayerMediaItem item = queueItem.getItem();
            builder.setTitle(item.getTitle());
            builder.setSubtitle(item.getArtistName());
            builder.setIconUri(Uri.parse(item.getArtworkUrl()));
            builder.setMediaId(item.getSubscriptionStoreId());
            queueItems.add(new MediaSessionCompat.QueueItem(builder.build(), queueItem.getPlaybackQueueId()));
        }
        mediaSession.setQueue(queueItems);
    }


    private boolean loadAlbumArtwork(PlayerQueueItem currentItem, PlayerQueueItem previousItem) {
        String previousArtworkUrl = previousItem != null ? previousItem.getItem().getArtworkUrl(artworkWidth, artworkHeight) : null;
        String currentArtworkUrl = currentItem != null ? currentItem.getItem().getArtworkUrl(artworkWidth, artworkHeight) : null;

        if (currentArtworkUrl != null && !currentArtworkUrl.equals(previousArtworkUrl)) {
            mainHandler.obtainMessage(MESSAGE_LOAD_ARTWORK, currentArtworkUrl).sendToTarget();
            return true;
        }
        return false;
    }


    private static int convertPlaybackState(@PlaybackState int playbackState, boolean buffering) {
        switch (playbackState) {
            case PlaybackState.STOPPED:
                return PlaybackStateCompat.STATE_STOPPED;
            case PlaybackState.PAUSED:
                return PlaybackStateCompat.STATE_PAUSED;
            case PlaybackState.PLAYING:
                return buffering ? PlaybackStateCompat.STATE_BUFFERING : PlaybackStateCompat.STATE_PLAYING;
            default:
                return PlaybackStateCompat.STATE_NONE;
        }
    }

    private static int convertRepeatMode(@PlaybackRepeatMode int repeatMode) {
        switch (repeatMode) {
            case PlaybackRepeatMode.REPEAT_MODE_ALL:
                return PlaybackStateCompat.REPEAT_MODE_ALL;
            case PlaybackRepeatMode.REPEAT_MODE_ONE:
                return PlaybackStateCompat.REPEAT_MODE_ONE;
            case PlaybackRepeatMode.REPEAT_MODE_OFF:
            default:
                return PlaybackStateCompat.REPEAT_MODE_NONE;
        }
    }


    private static int convertShuffleMode(@PlaybackShuffleMode int shuffleMode) {
        switch (shuffleMode) {
            case PlaybackShuffleMode.SHUFFLE_MODE_OFF:
                return PlaybackStateCompat.SHUFFLE_MODE_NONE;
            case PlaybackShuffleMode.SHUFFLE_MODE_SONGS:
                return PlaybackStateCompat.SHUFFLE_MODE_ALL;
        }
        return PlaybackStateCompat.SHUFFLE_MODE_NONE;
    }


    private static long allowedActions(MediaPlayerController playerController) {
        // TODO: This will need to take into account queue state, etc as to whether skip is allowed
        long result = PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
        switch (playerController.getPlaybackState()) {
            case PlaybackState.PLAYING:
                result |= PlaybackStateCompat.ACTION_PAUSE;
                break;
            case PlaybackState.PAUSED:
                result |= PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_STOP;
                break;
            case PlaybackState.STOPPED:
                result |= PlaybackStateCompat.ACTION_PLAY;
                break;
        }
        return result;
    }


    private static String formatReleaseDate(Date releaseDate) {
        if (releaseDate == null) {
            return null;
        }
        return RELEASE_DATE_FORMAT.format(releaseDate);
    }


    private class AlbumArtworkTarget implements Target {

        String url;

        AlbumArtworkTarget(String url) {
            this.url = url;
        }


        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
            backgroundHandler.obtainMessage(MESSAGE_UPDATE_ARTWORK, new Pair<>(url, bitmap)).sendToTarget();
        }


        @Override
        public void onBitmapFailed(Drawable drawable) {
        }


        @Override
        public void onPrepareLoad(Drawable drawable) {
        }


        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AlbumArtworkTarget)) {
                return false;
            }
            AlbumArtworkTarget other = (AlbumArtworkTarget)obj;
            return url.equals(other.url);
        }


        @Override
        public int hashCode() {
            return url.hashCode();
        }

    }

}
