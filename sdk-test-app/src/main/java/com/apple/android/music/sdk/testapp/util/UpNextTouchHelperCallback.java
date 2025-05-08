package com.apple.android.music.sdk.testapp.util;


import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Copyright (C) 2017 Apple, Inc. All rights reserved.
 */
public final class UpNextTouchHelperCallback extends ItemTouchHelper.Callback {

    public interface Listener {

        void onItemSwiped(RecyclerView.ViewHolder viewHolder);

        void onItemMove(RecyclerView.ViewHolder source, RecyclerView.ViewHolder target);

        void onItemMoveComplete(RecyclerView.ViewHolder source, int sourceStartPosition);
    }

    private final Listener listener;
    private boolean moving;
    private int moveStartPosition;


    public UpNextTouchHelperCallback(Listener listener) {
        this.listener = listener;
    }


    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        if (moving) {
            listener.onItemMoveComplete(viewHolder, moveStartPosition);
            moving = false;
        }
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        final int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        if (!moving) {
            moving = true;
            moveStartPosition = viewHolder.getAdapterPosition();
        }
        listener.onItemMove(viewHolder, target);
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        listener.onItemSwiped(viewHolder);
    }

}
