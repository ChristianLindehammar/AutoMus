package com.apple.android.music.sdk.testapp.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.support.v4.media.session.MediaSessionCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.apple.android.music.sdk.testapp.R;
import com.apple.android.music.sdk.testapp.util.UpNextTouchHelperCallback;
import com.squareup.picasso.Picasso;

import java.util.Collections;
import java.util.List;

/**
 * Copyright (C) 2017 Apple, Inc. All rights reserved.
 */
public final class QueueItemAdapter extends RecyclerView.Adapter<QueueItemAdapter.QueueItemViewHolder> implements UpNextTouchHelperCallback.Listener {

    public interface Listener {

        void onQueueItemClicked(@NonNull MediaSessionCompat.QueueItem queueItem);

        void onQueueItemRemoved(@NonNull MediaSessionCompat.QueueItem queueItem);

        void onQueueItemMoved(int from, int to);
    }

    private final Context context;
    private final LayoutInflater layoutInflater;
    private final Listener listener;
    private List<MediaSessionCompat.QueueItem> items;


    public QueueItemAdapter(@NonNull Context context, Listener listener) {
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
        items = Collections.emptyList();
        this.listener = listener;
        setHasStableIds(true);
    }


    @Override
    public QueueItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new QueueItemViewHolder(layoutInflater.inflate(R.layout.list_item_up_next, parent, false));
    }

    @Override
    public void onBindViewHolder(QueueItemViewHolder holder, int position) {
        MediaSessionCompat.QueueItem queueItem = items.get(position);
        Picasso.with(context).load(queueItem.getDescription().getIconUri()).into(holder.iconImageView);
        holder.queueItem = queueItem;
        holder.titleTextView.setText(queueItem.getDescription().getTitle());
        holder.subtitleTextView.setText(queueItem.getDescription().getSubtitle());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public long getItemId(int position) {
        MediaSessionCompat.QueueItem item = items.get(position);
        return item.getQueueId();
    }

    public void setItems(List<MediaSessionCompat.QueueItem> items) {
        if (items == null) {
            items = Collections.emptyList();
        }
        this.items = items;
        notifyDataSetChanged();
    }

    @Override
    public void onItemSwiped(RecyclerView.ViewHolder viewHolder) {
        final int position = viewHolder.getAdapterPosition();
        MediaSessionCompat.QueueItem item = items.get(position);
        items.remove(position);
        notifyItemRemoved(position);
        listener.onQueueItemRemoved(item);
    }

    @Override
    public void onItemMove(RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
        final int sourcePosition = source.getAdapterPosition();
        final int targetPosition = target.getAdapterPosition();
        final MediaSessionCompat.QueueItem item = items.remove(sourcePosition);
        items.add(targetPosition, item);
        notifyItemMoved(sourcePosition, targetPosition);
    }

    @Override
    public void onItemMoveComplete(RecyclerView.ViewHolder source, int sourceStartPosition) {
        listener.onQueueItemMoved(sourceStartPosition, source.getAdapterPosition());
    }

    final class QueueItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final ImageView iconImageView;
        final TextView titleTextView;
        final TextView subtitleTextView;
        final TextView descriptionTextView;
        MediaSessionCompat.QueueItem queueItem;

        QueueItemViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            iconImageView = (ImageView) itemView.findViewById(R.id.list_item_media_icon);
            titleTextView = (TextView) itemView.findViewById(R.id.list_item_media_title);
            subtitleTextView = (TextView) itemView.findViewById(R.id.list_item_media_subtitle);
            descriptionTextView = (TextView) itemView.findViewById(R.id.list_item_media_description);
        }

        @Override
        public void onClick(View v) {
            listener.onQueueItemClicked(queueItem);
        }
    }

}
