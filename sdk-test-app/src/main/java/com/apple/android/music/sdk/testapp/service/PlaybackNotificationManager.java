package com.apple.android.music.sdk.testapp.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.NotificationCompat;

import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;

import com.apple.android.music.sdk.testapp.R;

import java.util.BitSet;


final class PlaybackNotificationManager extends MediaControllerCompat.Callback {

    private static final int NOTIFICATION_ID = 0xA123;
    private static final String NOTIFICATION_CHANNEL_ID = "playback";

    private final MediaPlaybackService service;
    private final Handler backgroundHandler;
    private final NotificationManagerCompat notificationManager;
    private MediaControllerCompat mediaController;
    private boolean postedNotification;


    PlaybackNotificationManager(@NonNull MediaPlaybackService service, @NonNull Handler backgroundHandler) {
        this.service = service;
        this.backgroundHandler = backgroundHandler;
        notificationManager = NotificationManagerCompat.from(service);
        notificationManager.cancel(NOTIFICATION_ID);
        createNotificationChannel();
        createMediaController();
    }


    @Override
    public void onPlaybackStateChanged(PlaybackStateCompat state) {
        if (postedNotification) {
            updateNotification();
        }
    }


    @Override
    public void onMetadataChanged(MediaMetadataCompat metadata) {
        if (postedNotification) {
            updateNotification();
        }
    }

    @Override
    public void onSessionDestroyed() {
        mediaController.unregisterCallback(this);
        mediaController = null;
    }

    void start() {
        service.startService(new Intent(service, MediaPlaybackService.class));

        if (mediaController == null) {
            createMediaController();
        }

        Notification notification = createNotification();
        if (notification != null) {
            service.startForeground(NOTIFICATION_ID, notification);
            postedNotification = true;
        }
    }


    void stop(boolean removeNotification) {
        service.stopForeground(removeNotification);
        if (removeNotification) {
            if (mediaController != null) {
                mediaController.unregisterCallback(this);
                mediaController = null;
            }
            postedNotification = false;
        }
    }


    private void createMediaController() {
        MediaSessionCompat.Token sessionToken = service.getSessionToken();
        if (sessionToken != null) {
            try {
                mediaController = new MediaControllerCompat(service, service.getSessionToken());
                mediaController.registerCallback(this, backgroundHandler);
            } catch (RemoteException e) {
            }
        }
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
            final String name = service.getString(R.string.playback_notification_channel_name);
            final NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW);
            channel.enableVibration(false);
            channel.enableLights(false);
            notificationManager.createNotificationChannel(channel);
        }
    }


    private Notification createNotification() {
        final PlaybackStateCompat playbackState = mediaController.getPlaybackState();
        if (playbackState == null) {
            return null;
        }

        final int state = playbackState.getState();
        if (state == PlaybackStateCompat.STATE_STOPPED || state == PlaybackStateCompat.STATE_NONE) {
            return null;
        }

        final androidx.media.app.NotificationCompat.MediaStyle notificationStyle = new androidx.media.app.NotificationCompat.MediaStyle();
        notificationStyle.setMediaSession(service.getSessionToken());

        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(service, NOTIFICATION_CHANNEL_ID);
        notificationBuilder.setStyle(notificationStyle);
        notificationBuilder.setSmallIcon(R.drawable.ic_notification_status);
        notificationBuilder.setOngoing(isPlaying(state));
        notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        notificationBuilder.setContentIntent(mediaController.getSessionActivity());

        if (state == PlaybackStateCompat.STATE_PLAYING && playbackState.getPosition() != PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN) {
            notificationBuilder.setWhen(System.currentTimeMillis() - playbackState.getPosition());
            notificationBuilder.setShowWhen(true);
            notificationBuilder.setUsesChronometer(true);
        } else {
            notificationBuilder.setWhen(0);
            notificationBuilder.setShowWhen(false);
            notificationBuilder.setUsesChronometer(false);
        }


        final MediaMetadataCompat mediaMetadata = mediaController.getMetadata();
        if (mediaMetadata != null) {
            final MediaDescriptionCompat mediaDescription = mediaMetadata.getDescription();
            notificationBuilder.setContentTitle(mediaDescription.getTitle());
            notificationBuilder.setContentText(mediaDescription.getSubtitle());
            notificationBuilder.setSubText(mediaDescription.getDescription());
            notificationBuilder.setLargeIcon(mediaDescription.getIconBitmap());
        }

        final long allowedActions = playbackState.getActions();
        int actionCount = 0;
        final BitSet compactActions = new BitSet(5);

        if (isAllowedAction(allowedActions, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)) {
            notificationBuilder.addAction(R.drawable.ic_notification_previous, service.getString(R.string.notification_previous), createActionIntent(service, KeyEvent.KEYCODE_MEDIA_PREVIOUS));
            compactActions.set(actionCount);
            ++actionCount;
        }
        if (isAllowedAction(allowedActions, PlaybackStateCompat.ACTION_PLAY)) {
            notificationBuilder.addAction(R.drawable.ic_notification_play, service.getString(R.string.notification_play), createActionIntent(service, KeyEvent.KEYCODE_MEDIA_PLAY));
            compactActions.set(actionCount);
            ++actionCount;
        } else if (isAllowedAction(allowedActions, PlaybackStateCompat.ACTION_PAUSE)) {
            notificationBuilder.addAction(R.drawable.ic_notification_pause, service.getString(R.string.notification_pause), createActionIntent(service, KeyEvent.KEYCODE_MEDIA_PAUSE));
            compactActions.set(actionCount);
            ++actionCount;
        }
        if (isAllowedAction(allowedActions, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)) {
            notificationBuilder.addAction(R.drawable.ic_notification_next, service.getString(R.string.notification_next), createActionIntent(service, KeyEvent.KEYCODE_MEDIA_NEXT));
            compactActions.set(actionCount);
            ++actionCount;
        }
        if (isAllowedAction(allowedActions, PlaybackStateCompat.ACTION_STOP)) {
            final PendingIntent stopIntent = createActionIntent(service, KeyEvent.KEYCODE_MEDIA_STOP);
            notificationBuilder.setDeleteIntent(stopIntent);
            notificationStyle.setShowCancelButton(true);
            notificationStyle.setCancelButtonIntent(stopIntent);
        }
        if (compactActions.cardinality() > 0) {
            notificationStyle.setShowActionsInCompactView(compactActionsList(compactActions));
        }

        return notificationBuilder.build();
    }


    private void updateNotification() {
        Notification notification = createNotification();
        if (notification != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }


    private static PendingIntent createActionIntent(Context context, int mediaKeyCode) {
        final Intent intent = new Intent(context, MediaPlaybackService.class);
        intent.setAction(Intent.ACTION_MEDIA_BUTTON);
        intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, mediaKeyCode));
        return PendingIntent.getService(context, mediaKeyCode, intent, 0);
    }


    private static boolean isAllowedAction(long allowedActions, long action) {
        return (action & allowedActions) == action;
    }


    private static boolean isPlaying(int state) {
        return state == PlaybackStateCompat.STATE_PLAYING || state == PlaybackStateCompat.STATE_BUFFERING || state == PlaybackStateCompat.STATE_CONNECTING;
    }


    private static int[] compactActionsList(BitSet actionIndices) {
        final int[] result = new int[actionIndices.cardinality()];
        final int count = actionIndices.size();
        for (int i = 0, j = 0; i < count; i++) {
            if (actionIndices.get(i)) {
                result[j++] = i;
            }
        }
        return result;
    }

}
