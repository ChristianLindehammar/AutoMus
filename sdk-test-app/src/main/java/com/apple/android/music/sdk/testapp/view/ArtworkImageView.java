package com.apple.android.music.sdk.testapp.view;

import android.content.Context;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Copyright (C) 2017 Apple, Inc. All rights reserved.
 */
public final class ArtworkImageView extends ImageView {

    public ArtworkImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }


    public ArtworkImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if (widthMode != MeasureSpec.UNSPECIFIED) {
            setMeasuredDimension(widthSize, widthSize);
        } else if (heightMode != MeasureSpec.UNSPECIFIED) {
            setMeasuredDimension(heightSize, heightSize);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
}
