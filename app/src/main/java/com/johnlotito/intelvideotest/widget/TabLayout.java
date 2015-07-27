package com.johnlotito.intelvideotest.widget;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TabLayout extends SlidingTabLayout {
    private String[] mTabNames;
    private int mCurrentItem;
    private OnTabClickListener mListener;

    public TabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setViewPager(ViewPager viewPager) {
        /* This tab layout does not user a view pager, instead pass in an array of Page names to setTabs */
    }

    public void setTabs(String[] tabNames) {
        mTabStrip.removeAllViews();
        mTabNames = tabNames;

        if (tabNames != null) {
            populateTabStrip();
        }
    }

    public void setOnTabClickListener(OnTabClickListener listener) {
        mListener = listener;
    }

    public void selectTab(int position) {
        for (int i = 0; i < mTabStrip.getChildCount(); i++) {
            mTabStrip.getChildAt(i).setSelected(i == position);
        }

        mCurrentItem = position;
        mTabStrip.onViewPagerPageChanged(position, 0f);

        if (mListener != null) {
            mListener.onTabClick(position);
        }
    }

    private void populateTabStrip() {
        final View.OnClickListener tabClickListener = new TabClickListener();

        for (int i = 0; i < mTabNames.length; i++) {
            View tabView = null;
            TextView tabTitleView = null;

            if (mTabViewLayoutId != 0) {
                // If there is a custom tab view layout id set, try and inflate it
                tabView = LayoutInflater.from(getContext()).inflate(mTabViewLayoutId, mTabStrip,
                        false);
                tabTitleView = (TextView) tabView.findViewById(mTabViewTextViewId);
            }

            if (tabView == null) {
                tabView = createDefaultTabView(getContext());
            }

            if (tabTitleView == null && TextView.class.isInstance(tabView)) {
                tabTitleView = (TextView) tabView;
            }

            if (mDistributeEvenly) {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tabView.getLayoutParams();
                lp.width = 0;
                lp.weight = 1;
            }

            tabTitleView.setText(mTabNames[i]);
            tabView.setOnClickListener(tabClickListener);

            mTabStrip.addView(tabView);
            if (i == mCurrentItem) {
                tabView.setSelected(true);
            }
        }
    }

    private class TabClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            for (int i = 0; i < mTabStrip.getChildCount(); i++) {
                mTabStrip.getChildAt(i).setSelected(false);

                if (v == mTabStrip.getChildAt(i)) {
                    mCurrentItem = i;
                    mTabStrip.onViewPagerPageChanged(i, 0f);
                    mTabStrip.getChildAt(i).setSelected(true);

                    if (mListener != null) {
                        mListener.onTabClick(i);
                    }
                }
            }
        }
    }

    /**
     * Click listener that will be called when a tab item has been clicked.
     */
    public interface OnTabClickListener {
        /**
         * Tab has been clicked.
         *
         * @param position The index of the tab that was clicked
         * @param tab The tab that was clicked
         */
        void onTabClick(int position);
    }
}
