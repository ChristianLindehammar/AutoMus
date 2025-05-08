package com.apple.android.music.sdk.testapp.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.apple.android.music.playback.model.PlaybackQueueMoveTargetType;
import com.apple.android.music.sdk.testapp.R;
import com.apple.android.music.sdk.testapp.adapter.QueueItemAdapter;
import com.apple.android.music.sdk.testapp.service.MediaControllerCommand;
import com.apple.android.music.sdk.testapp.util.ListSeparatorDecoration;
import com.apple.android.music.sdk.testapp.util.UpNextTouchHelperCallback;

import java.util.List;

/**
 * Copyright (C) 2017 Apple, Inc. All rights reserved.
 */
public final class UpNextFragment extends BaseFragment implements QueueItemAdapter.Listener {

    public static final String TAG = "UpNextFragment";
    private RecyclerView queueRecyclerView;
    private QueueItemAdapter queueItemAdapter;
    private UpNextTouchHelperCallback touchHelperCallback;
    private MediaControllerCompat mediaController;
    private MediaControllerCallback mediaControllerCallback;

    public static UpNextFragment newInstance() {
        return new UpNextFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mediaControllerCallback = new MediaControllerCallback();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_up_next, container, false);
        queueRecyclerView = (RecyclerView) view.findViewById(R.id.queue_recycler_view);
        return view;
    }


    @Override
    public void onStart() {
        super.onStart();
        getActivity().setTitle(getString(R.string.title_up_next));
        queueRecyclerView.addItemDecoration(new ListSeparatorDecoration(getActivity()));
        queueRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        queueItemAdapter = new QueueItemAdapter(getActivity(), this);
        queueRecyclerView.setAdapter(queueItemAdapter);
        mediaController = MediaControllerCompat.getMediaController(getActivity());
        if (mediaController != null) {
            mediaController.registerCallback(mediaControllerCallback);
            mediaControllerCallback.onQueueChanged(mediaController.getQueue());
        }
        touchHelperCallback = new UpNextTouchHelperCallback(queueItemAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(touchHelperCallback);
        touchHelper.attachToRecyclerView(queueRecyclerView);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mediaController != null) {
            mediaController.unregisterCallback(mediaControllerCallback);
        }
    }

    @Override
    public void onQueueItemClicked(@NonNull MediaSessionCompat.QueueItem queueItem) {
        if (mediaController != null) {
            mediaController.getTransportControls().skipToQueueItem(queueItem.getQueueId());
        }
    }

    @Override
    public void onQueueItemRemoved(@NonNull MediaSessionCompat.QueueItem queueItem) {
        final Bundle params = new Bundle(1);
        params.putLong(MediaControllerCommand.COMMAND_ARGUMENT_PLAYBACK_QUEUE_ID, queueItem.getQueueId());
        mediaController.sendCommand(MediaControllerCommand.COMMAND_REMOVE_QUEUE_ITEM, params, null);
    }

    @Override
    public void onQueueItemMoved(int from, int to) {
        List<MediaSessionCompat.QueueItem> queue = mediaController.getQueue();
        MediaSessionCompat.QueueItem sourceItem = queue.get(from);
        MediaSessionCompat.QueueItem targetItem;
        @PlaybackQueueMoveTargetType int moveTargetType;
        if (to == 0) {
            targetItem = queue.get(0);
            moveTargetType = PlaybackQueueMoveTargetType.MOVE_BEFORE_TARGET;
        } else {
            targetItem = queue.get(to);
            moveTargetType = PlaybackQueueMoveTargetType.MOVE_AFTER_TARGET;
        }

        final Bundle params = new Bundle(3);
        params.putLong(MediaControllerCommand.COMMAND_ARGUMENT_PLAYBACK_QUEUE_ID, sourceItem.getQueueId());
        params.putLong(MediaControllerCommand.COMMAND_ARGUMENT_PLAYBACK_QUEUE_ID_TARGET, targetItem.getQueueId());
        params.putInt(MediaControllerCommand.COMMAND_ARGUMENT_PLAYBACK_QUEUE_MOVE_TARGET_TYPE, moveTargetType);
        mediaController.sendCommand(MediaControllerCommand.COMMAND_MOVE_QUEUE_ITEM, params, null);
    }


    private class MediaControllerCallback extends MediaControllerCompat.Callback {

        @Override
        public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
            queueItemAdapter.setItems(queue);
        }

    }

}
