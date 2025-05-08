package com.apple.android.music.sdk.testapp.util;

import android.content.Context;

import android.util.AttributeSet;
import android.view.View;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Copyright (C) 2017 Apple, Inc. All rights reserved.
 */
public final class ContainerLayoutBehavior extends AppBarLayout.ScrollingViewBehavior {

    public ContainerLayoutBehavior() {
    }


    public ContainerLayoutBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return (dependency instanceof BottomNavigationView) || super.layoutDependsOn(parent, child, dependency);
    }


    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {
        if (dependency instanceof BottomNavigationView) {
            child.setPadding(child.getPaddingLeft(), child.getPaddingTop(), child.getPaddingRight(), dependency.getHeight());
            return true;
        }
        return super.onDependentViewChanged(parent, child, dependency);
    }

}
