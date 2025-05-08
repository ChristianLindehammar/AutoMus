package com.apple.android.music.sdk.testapp.fragment;


import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.apple.android.music.playback.model.MediaContainerType;
import com.apple.android.music.playback.model.MediaItemType;
import com.apple.android.music.playback.queue.CatalogPlaybackQueueItemProvider;
import com.apple.android.music.playback.queue.PlaybackQueueInsertionType;
import com.apple.android.music.playback.queue.PlaybackQueueItemProvider;
import com.apple.android.music.sdk.testapp.R;
import com.apple.android.music.sdk.testapp.activity.MainActivity;
import com.apple.android.music.sdk.testapp.adapter.MediaBrowserAdapter;
import com.apple.android.music.sdk.testapp.service.MediaControllerCommand;
import com.apple.android.music.sdk.testapp.util.ListSeparatorDecoration;


/**
 * Copyright (C) 2017 Apple, Inc. All rights reserved.
 */
public final class BrowseFragment extends BaseFragment implements MediaBrowserAdapter.Listener {

    public static final String TAG = "BrowseFragment";
    private static final String ARGUMENT_PARENT_MEDIA_ID = "parentMediaId";
    private static final String ARGUMENT_TITLE = "title";

    private RecyclerView listRecyclerView;
    private MediaBrowserAdapter adapter;
    private MediaBrowserCompat mediaBrowser;
    private String parentId;
    private String title;


    public static BrowseFragment newInstance(String title, String parentMediaId) {
        final BrowseFragment fragment = new BrowseFragment();
        final Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_PARENT_MEDIA_ID, parentMediaId);
        arguments.putString(ARGUMENT_TITLE, title);
        fragment.setArguments(arguments);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parentId = getArguments().getString(ARGUMENT_PARENT_MEDIA_ID);
        title = getArguments().getString(ARGUMENT_TITLE, getString(R.string.title_browse));
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_browse, container, false);
        listRecyclerView = (RecyclerView)view.findViewById(R.id.list);
        listRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        listRecyclerView.addItemDecoration(new ListSeparatorDecoration(getActivity()));
        if (adapter != null) {
            listRecyclerView.setAdapter(adapter);
        }
        return view;
    }


    @Override
    public void onStart() {
        super.onStart();
        getActivity().setTitle(title);
        if (adapter == null && mediaBrowser != null) {
            adapter = new MediaBrowserAdapter(getActivity(), mediaBrowser, this);
            listRecyclerView.setAdapter(adapter);
            adapter.loadItems(parentId);
        }
    }

    @Override
    public void onMediaBrowserConnected(MediaBrowserCompat mediaBrowser) {
        super.onMediaBrowserConnected(mediaBrowser);
        this.mediaBrowser = mediaBrowser;
        if (adapter == null && getActivity() != null) {
            adapter = new MediaBrowserAdapter(getActivity(), mediaBrowser, this);
            listRecyclerView.setAdapter(adapter);
            adapter.loadItems(parentId);
        }
    }

    @Override
    public void onMediaItemClicked(@NonNull MediaBrowserCompat.MediaItem mediaItem) {
        if (mediaItem.isBrowsable()) {
            final MainActivity mainActivity = (MainActivity)getActivity();
            mainActivity.browseMedia(mediaItem.getDescription().getTitle().toString(), mediaItem.getMediaId());
        } else if (mediaItem.isPlayable()) {
            playItem(mediaItem);
        }
    }


    @Override
    public void onMediaItemMoreClicked(@NonNull final MediaBrowserCompat.MediaItem mediaItem, View v) {
        final PopupMenu popupMenu = new PopupMenu(getActivity(), v);
        popupMenu.inflate(R.menu.media_item_popup);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.media_item_popup_play:
                        playItem(mediaItem);
                        return true;
                    case R.id.media_item_popup_shuffle:
                        shufflePlayItem(mediaItem);
                        return true;
                    case R.id.media_item_popup_play_next:
                        playNext(mediaItem);
                        return true;
                    case R.id.media_item_popup_play_later:
                        playLater(mediaItem);
                        return true;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    private void playItem(MediaBrowserCompat.MediaItem mediaItem) {
        final MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(getActivity());
        if (mediaController != null) {
            final Uri mediaUri = mediaItem.getDescription().getMediaUri();
            if (mediaUri != null) {
                mediaController.getTransportControls().playFromUri(mediaUri, mediaItem.getDescription().getExtras());
            } else {
                mediaController.getTransportControls().playFromMediaId(mediaItem.getMediaId(), mediaItem.getDescription().getExtras());
            }
        }
    }


    private void shufflePlayItem(MediaBrowserCompat.MediaItem mediaItem) {
    }


    private void playNext(MediaBrowserCompat.MediaItem mediaItem) {
        if (!mediaItem.isPlayable() || mediaItem.getMediaId() == null) {
            return;
        }
        final MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(getActivity());
        if (mediaController != null) {
            PlaybackQueueItemProvider provider = createProvider(mediaItem);
            Bundle params = new Bundle(2);
            params.putParcelable(MediaControllerCommand.COMMAND_ARGUMENT_PLAYBACK_QUEUE_ITEM_PROVIDER, provider);
            params.putInt(MediaControllerCommand.COMMAND_ARGUMENT_PLAYBACK_QUEUE_INSERTION_TYPE, PlaybackQueueInsertionType.INSERTION_TYPE_AFTER_CURRENT_ITEM);
            mediaController.sendCommand(MediaControllerCommand.COMMAND_ADD_QUEUE_ITEMS, params, null);
        }
    }


    private void playLater(MediaBrowserCompat.MediaItem mediaItem) {
        if (!mediaItem.isPlayable() || mediaItem.getMediaId() == null) {
            return;
        }
        final MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(getActivity());
        if (mediaController != null) {
            PlaybackQueueItemProvider provider = createProvider(mediaItem);
            Bundle params = new Bundle(2);
            params.putParcelable(MediaControllerCommand.COMMAND_ARGUMENT_PLAYBACK_QUEUE_ITEM_PROVIDER, provider);
            params.putInt(MediaControllerCommand.COMMAND_ARGUMENT_PLAYBACK_QUEUE_INSERTION_TYPE, PlaybackQueueInsertionType.INSERTION_TYPE_AT_END);
            mediaController.sendCommand(MediaControllerCommand.COMMAND_ADD_QUEUE_ITEMS, params, null);
        }
    }

    private PlaybackQueueItemProvider createProvider(MediaBrowserCompat.MediaItem mediaItem) {
        CatalogPlaybackQueueItemProvider.Builder builder = new CatalogPlaybackQueueItemProvider.Builder();
        int containerType = MediaContainerType.NONE;
        int itemType = MediaItemType.UNKNOWN;
        Bundle extras = mediaItem.getDescription().getExtras();
        if (extras != null) {
            containerType = extras.getInt("containerType", MediaContainerType.NONE);
            itemType = extras.getInt("itemType", MediaItemType.UNKNOWN);
        }
        if (containerType != MediaContainerType.NONE) {
            builder.containers(containerType, mediaItem.getMediaId());
        } else {
            builder.items(itemType, mediaItem.getMediaId());
        }
        return builder.build();
    }
}
