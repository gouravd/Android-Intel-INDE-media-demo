/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.johnlotito.intelvideotest.video.gallery3d;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.johnlotito.intelvideotest.R;
import com.johnlotito.intelvideotest.video.gallery3d.controls.ControllerOverlay;
import com.johnlotito.intelvideotest.video.gallery3d.controls.TrimControllerOverlay;

public abstract class TrimVideoActivity extends AppCompatActivity implements
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener,
        ControllerOverlay.Listener {

    public static final String START_TIME = "com.johnlotito.intelvideotest.extra.START_TIME";
    public static final String END_TIME = "com.johnlotito.intelvideotest.extra.END_TIME";
    public static final String EXTRA_MESSAGE = "com.johnlotito.intelvideotest.extra.MESSAGE";

    private VideoView mVideoView;
    private TrimControllerOverlay mController;
    private Uri mUri;
    private final Handler mHandler = new Handler();
    public ProgressDialog mProgress;
    private long mTrimStartTime = 0;
    private long mTrimEndTime = 0;
    private long mVideoPosition = 0;
    public static final String KEY_TRIM_START = "trim_start";
    public static final String KEY_TRIM_END = "trim_end";
    public static final String KEY_VIDEO_POSITION = "video_pos";
    private int mDuration;

    protected long getTrimStartTime() {
        return mTrimStartTime;
    }

    protected long getTrimEndTime() {
        return mTrimEndTime;
    }

    protected Uri getVideoUri() {
        return mUri;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Context context = getApplicationContext();

            configureActionBar();

            Intent intent = getIntent();
            mUri = intent.getData();
            setContentView(R.layout.activity_trim_video);
            View rootView = findViewById(R.id.trim_view_root);
            mVideoView = (VideoView) rootView.findViewById(R.id.surface_view);

            int maxDuration = 30 * 1000;
            mController = new TrimControllerOverlay(context, maxDuration);
            ((ViewGroup) rootView).addView(mController.getView());
            mController.setListener(this);
            mController.setCanReplay(true);

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();

            if (ContentResolver.SCHEME_FILE.equals(mUri.getScheme())) {
                retriever.setDataSource(mUri.getPath());
            } else {
                retriever.setDataSource(context, mUri);
            }

            mDuration = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            mController.setTimes(0, mDuration, 0, 0);
            mTrimEndTime = Math.min(mDuration, maxDuration);
            mVideoView.setOnErrorListener(this);
            mVideoView.setOnCompletionListener(this);
            mVideoView.setVideoURI(mUri);

            if (savedInstanceState == null) {
                String message = getIntent().getStringExtra(EXTRA_MESSAGE);

                if (!TextUtils.isEmpty(message)) {
                    View layout = getLayoutInflater().inflate(R.layout.toast, null);
                    TextView text = (TextView) layout.findViewById(R.id.text);
                    text.setText(message);
                    Toast toast = new Toast(this);
                    toast.setDuration(Toast.LENGTH_LONG);
                    int toastPadding = getResources().getDimensionPixelSize(R.dimen.toast_padding);
                    toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, toastPadding);
                    toast.setView(layout);
                    toast.show();
                }
            }

            playVideo();
        } catch (Exception e) {
            setResult(RESULT_FIRST_USER);
            finish();
        }
    }

    private void configureActionBar() {
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setTitle(R.string.trim_video);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        mVideoView.seekTo((int) mVideoPosition);
        mVideoView.resume();
        mHandler.post(mProgressChecker);
    }

    @Override
    public void onPause() {
        mHandler.removeCallbacksAndMessages(null);
        mVideoPosition = mVideoView.getCurrentPosition();
        mVideoView.suspend();
        super.onPause();
    }

    @Override
    public void onStop() {
        if (mProgress != null) {
            mProgress.dismiss();
            mProgress = null;
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        mVideoView.stopPlayback();
        super.onDestroy();
    }

    private final Runnable mProgressChecker = new Runnable() {
        @Override
        public void run() {
            int pos = setProgress();
            mHandler.postDelayed(mProgressChecker, 200 - (pos % 200));
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.trimmer, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.done) {
            Intent results = new Intent();
            results.setData(mUri);
            results.putExtra(START_TIME, mTrimStartTime);
            results.putExtra(END_TIME, mTrimEndTime);
            setResult(Activity.RESULT_OK, results);
            finish();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(KEY_TRIM_START, (int) mTrimStartTime);
        savedInstanceState.putInt(KEY_TRIM_END, (int) mTrimEndTime);
        savedInstanceState.putInt(KEY_VIDEO_POSITION, (int) mVideoPosition);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mTrimStartTime = savedInstanceState.getInt(KEY_TRIM_START, 0);
        mTrimEndTime = savedInstanceState.getInt(KEY_TRIM_END, 0);
        mVideoPosition = savedInstanceState.getInt(KEY_VIDEO_POSITION, 0);
    }

    // This updates the time bar display (if necessary). It is called by
    // mProgressChecker and also from places where the time bar needs
    // to be updated immediately.
    private int setProgress() {
        mVideoPosition = mVideoView.getCurrentPosition();
        // If the video position is smaller than the starting point of trimming,
        // correct it.
        if (mVideoPosition < mTrimStartTime) {
            mVideoPosition = mTrimStartTime;
        }
        // If the position is bigger than the end point of trimming, show the
        // replay button and pause.
        if (mVideoPosition >= mTrimEndTime && mTrimEndTime > 0) {
            if (mVideoPosition > mTrimEndTime) {
                mVideoPosition = mTrimEndTime;
            }
            mController.showEnded();
            mVideoView.pause();
        }

        if (mVideoView.isPlaying()) {
            mController.setTimes((int) mVideoPosition, mDuration, (int) mTrimStartTime, (int) mTrimEndTime);
        }

        return (int) mVideoPosition;
    }

    private void playVideo() {
        mVideoView.start();
        mController.showPlaying();
        setProgress();
    }

    private void pauseVideo() {
        mVideoView.pause();
        mController.showPaused();
    }

    @Override
    public void onPlayPause() {
        if (mVideoView.isPlaying()) {
            pauseVideo();
        } else {
            playVideo();
        }
    }

    @Override
    public void onSeekStart() {
        pauseVideo();
    }

    @Override
    public void onSeekMove(int time) {
        mVideoView.seekTo(time);
    }

    @Override
    public void onSeekEnd(int time, int start, int end) {
        mVideoView.seekTo(time);
        mTrimStartTime = start;
        mTrimEndTime = end;
        setProgress();
    }

    @Override
    public void onShown() {
    }

    @Override
    public void onHidden() {
    }

    @Override
    public void onReplay() {
        mVideoView.seekTo((int) mTrimStartTime);
        playVideo();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mController.showEnded();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }
}