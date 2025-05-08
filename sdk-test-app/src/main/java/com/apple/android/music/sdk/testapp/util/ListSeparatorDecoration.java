package com.apple.android.music.sdk.testapp.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.apple.android.music.sdk.testapp.R;

/**
 * Copyright (C) 2017 Apple, Inc. All rights reserved.
 */

public final class ListSeparatorDecoration extends RecyclerView.ItemDecoration {

    private final Drawable dividerDrawable;

    public ListSeparatorDecoration(Context context) {
        dividerDrawable = ContextCompat.getDrawable(context, R.drawable.list_item_divider);
    }


    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        if (parent.getChildAdapterPosition(view) != 0) {
            outRect.top = dividerDrawable.getIntrinsicHeight();
        }
    }


    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        final int dividerLeft = parent.getPaddingLeft();
        final int dividerRight = parent.getWidth() - parent.getPaddingRight();
        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount - 1; i++) {
            final View child = parent.getChildAt(i);
            final RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) child.getLayoutParams();
            final int dividerTop = child.getBottom() + layoutParams.bottomMargin;
            final int dividerBottom = dividerTop + dividerDrawable.getIntrinsicHeight();
            dividerDrawable.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom);
            dividerDrawable.draw(c);
        }
    }

}
