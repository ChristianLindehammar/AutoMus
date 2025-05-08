package com.apple.android.music.sdk.testapp.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.apple.android.music.sdk.testapp.R;
import com.squareup.picasso.Picasso;

import java.util.Collections;
import java.util.List;


/**
 * Copyright (C) 2017 Apple, Inc. All rights reserved.
 */
public final class MediaBrowserAdapter extends RecyclerView.Adapter<MediaBrowserAdapter.MediaItemViewHolder> {

    public interface Listener {

        void onMediaItemClicked(@NonNull MediaBrowserCompat.MediaItem mediaItem);

        void onMediaItemMoreClicked(@NonNull MediaBrowserCompat.MediaItem mediaItem, View v);

    }

    private final LayoutInflater layoutInflater;
    private final MediaBrowserCompat mediaBrowser;
    private final Listener listener;
    List<MediaBrowserCompat.MediaItem> mediaItems;
    private SubscriptionCallback subscriptionCallback;
    private String parentId;
    private final Picasso picasso;


    public MediaBrowserAdapter(@NonNull Context context, MediaBrowserCompat mediaBrowser, Listener listener) {
        layoutInflater = LayoutInflater.from(context);
        this.mediaBrowser = mediaBrowser;
        this.listener = listener;
        mediaItems = Collections.emptyList();
        subscriptionCallback = new SubscriptionCallback();
        picasso = Picasso.with(context);
    }


    @Override
    public MediaItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MediaItemViewHolder(layoutInflater.inflate(R.layout.list_item_media, parent, false));
    }


    @Override
    public void onBindViewHolder(MediaItemViewHolder holder, int position) {
        final MediaBrowserCompat.MediaItem mediaItem = mediaItems.get(position);
        if (mediaItem != null) {
            holder.bind(mediaItem);
        }
    }


    @Override
    public int getItemCount() {
        return mediaItems.size();
    }


    public void loadItems(@Nullable String parentId) {
        if (parentId == null) {
            parentId = mediaBrowser.getRoot();
        }
        this.parentId = parentId;
        mediaBrowser.subscribe(parentId, subscriptionCallback);
    }

    final class MediaItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final ImageView iconImageView;
        final TextView titleTextView;
        final TextView subtitleTextView;
        final TextView descriptionTextView;
        final ImageView moreImageView;
        MediaBrowserCompat.MediaItem mediaItem;

        MediaItemViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            iconImageView = (ImageView) itemView.findViewById(R.id.list_item_media_icon);
            titleTextView = (TextView) itemView.findViewById(R.id.list_item_media_title);
            subtitleTextView = (TextView) itemView.findViewById(R.id.list_item_media_subtitle);
            descriptionTextView = (TextView) itemView.findViewById(R.id.list_item_media_description);
            moreImageView = (ImageView) itemView.findViewById(R.id.list_item_media_more);
            moreImageView.setOnClickListener(this);
        }


        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.list_item_media_more) {
                listener.onMediaItemMoreClicked(mediaItem, v);
            } else {
                listener.onMediaItemClicked(mediaItem);
            }
        }


        void bind(MediaBrowserCompat.MediaItem mediaItem) {
            this.mediaItem = mediaItem;
            final MediaDescriptionCompat mediaDescription = mediaItem.getDescription();
            titleTextView.setText(mediaDescription.getTitle());
            subtitleTextView.setText(mediaDescription.getSubtitle());
            descriptionTextView.setText(mediaDescription.getDescription());
            if (mediaDescription.getIconUri() != null) {
                picasso.load(mediaDescription.getIconUri()).into(iconImageView);
            }
            moreImageView.setVisibility(mediaItem.isPlayable() ? View.VISIBLE : View.GONE);
        }

    }

    private final class SubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback {

        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children) {
            mediaItems = children;
            notifyDataSetChanged();
        }

    }

}
