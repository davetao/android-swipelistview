package com.fortysevendeg.swipelistview;
/*
 * Copyright 2013 Lars Werkman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;

public class QuickReturnListView extends ListView {

    private int mItemCount;
    private int mItemOffsetY[];
    private boolean scrollIsComputed = false;
    private int mHeight;

    /**/
    private int mCachedVerticalScrollRange;
    float mQuickReturnHeight = 56f;

    private static final int STATE_ONSCREEN = 0;
    private static final int STATE_OFFSCREEN = 1;
    private static final int STATE_RETURNING = 2;
    private int mState = STATE_ONSCREEN;
    private int mScrollY;
    private int mMinRawY = 0;

    private View stickyView;
    private View headerPlaceholder;

    private TranslateAnimation anim;
    public QuickReturnListView(Context context) {
        super(context);
    }

    public QuickReturnListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public QuickReturnListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setup() {
        setupLayoutObserver();
        this.setOnScrollListener(new AbsListView.OnScrollListener() {
            @SuppressLint("NewApi")
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                handleScrolling(firstVisibleItem);
            }

            @Override public void onScrollStateChanged(AbsListView view, int scrollState) {}
        });
    }

    public void setStickyView(View stickyView) {
        this.stickyView = stickyView;
    }

    public void setHeaderPlaceholder(View headerPlaceholder) {
        this.headerPlaceholder = headerPlaceholder;
    }

    public void setupLayoutObserver() {
        scrollIsComputed = false;
        if(stickyView != null) {
            final QuickReturnListView listView = this;
            listView.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            if(!scrollIsComputed) {
                                listView.computeScrollY();
                                mQuickReturnHeight = stickyView.getMeasuredHeight();
                                mCachedVerticalScrollRange = listView.getListHeight();
                            }
                        }
                    });

        }

    }

    public void handleScrolling(int firstVisibleItem) {

        if(this.stickyView == null || this.headerPlaceholder == null)
            return;

        final QuickReturnListView listView = this;

        mScrollY = 0;
        int translationY = 0;

        if (listView.scrollYIsComputed()) {
            mScrollY = listView.getComputedScrollY();
        }

        if(mScrollY == 0) {
            this.stickyView.setTranslationY(0);
            return;
        }

        int rawY = this.headerPlaceholder.getTop() - Math.min( mCachedVerticalScrollRange - listView.getHeight(), mScrollY);

        switch (mState) {
            case STATE_OFFSCREEN:
                if (rawY <= mMinRawY) {
                    mMinRawY = rawY;
                } else {
                    mState = STATE_RETURNING;
                }
                translationY = rawY;
                break;

            case STATE_ONSCREEN:
                if (rawY < -mQuickReturnHeight) {
                    mState = STATE_OFFSCREEN;
                    mMinRawY = rawY;
                }
                translationY = rawY;
                break;

            case STATE_RETURNING:
                translationY = (rawY - mMinRawY) - (int)mQuickReturnHeight;
                if (translationY > 0) {
                    translationY = 0;
                    mMinRawY = rawY - (int)mQuickReturnHeight;
                }

                if (rawY > 0) {
                    mState = STATE_ONSCREEN;
                    translationY = rawY;
                }

                if (translationY < -mQuickReturnHeight) {
                    mState = STATE_OFFSCREEN;
                    mMinRawY = rawY;
                }
                break;
        }
        stickyView.setTranslationY(translationY);
    }

    public int getListHeight() {
        return mHeight;
    }

    public void computeScrollY() {
        mHeight = 0;
        mItemCount = getAdapter().getCount();

        if(mItemCount < 1) {
            return;
        }

        mItemOffsetY = new int[mItemCount];
        View view = getAdapter().getView(0, null, this);
        for (int i = 0; i < mItemCount; ++i) {
            if(i < 2) {
                view = getAdapter().getView(i, null, this);
                view.measure( MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            }
            mItemOffsetY[i] = mHeight;
            mHeight += view.getMeasuredHeight();
        }
        scrollIsComputed = true;
    }

    public boolean scrollYIsComputed() {
        return scrollIsComputed;
    }

    public int getComputedScrollY() {
        int pos, nScrollY, nItemY;
        View view = null;
        pos = getFirstVisiblePosition();
        view = getChildAt(0);
        nItemY = view.getTop();
        nScrollY = mItemOffsetY[pos] - nItemY;
        return nScrollY;
    }
}