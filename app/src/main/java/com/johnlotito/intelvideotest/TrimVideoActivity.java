package com.johnlotito.intelvideotest;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.intel.inde.mp.IProgressListener;
import com.johnlotito.intelvideotest.util.LogUtils;
import com.johnlotito.intelvideotest.util.StorageUtils;
import com.johnlotito.intelvideotest.video.VideoTranscoder;

import java.io.File;

public class TrimVideoActivity extends com.johnlotito.intelvideotest.video.gallery3d.TrimVideoActivity implements IProgressListener {

    private ProgressBar mProgressBar;
    private File mOutputFile;
    private VideoTranscoder mTranscoder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.INVISIBLE);

        mOutputFile = StorageUtils.getNewImageFile(this, StorageUtils.EXT_MP4, true);
        mTranscoder = new VideoTranscoder(this, getVideoUri().toString(), mOutputFile);

        configureActionBar();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.done:
                startTranscode();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        mTranscoder.destroy();
        super.onDestroy();
    }

    /**
     * *******************************************************
     * IProgressListener Interface Methods
     * *******************************************************
     */

    @Override
    public void onMediaStart() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setIndeterminate(false);
                mProgressBar.setProgress(0);
                mProgressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onMediaProgress(final float progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setProgress((int) (mProgressBar.getMax() * progress));
            }
        });
    }

    @Override
    public void onMediaDone() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setProgress(mProgressBar.getMax());
                mProgressBar.setVisibility(View.INVISIBLE);
                deliverResult();
            }
        });
    }

    @Override
    public void onMediaPause() {

    }

    @Override
    public void onMediaStop() {

    }

    @Override
    public void onError(final Exception ex) {
        LogUtils.e(ex);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(TrimVideoActivity.this, ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * *******************************************************
     * Private Methods
     * *******************************************************
     */

    private void configureActionBar() {
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setTitle(R.string.trim_video);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
    }

    private void startTranscode() {
        mTranscoder.setTrim(getTrimStartTime(), getTrimEndTime());
        mTranscoder.startTranscode(this);
    }

    private void deliverResult() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(mOutputFile), "video/*");
        startActivity(intent);
    }
}
