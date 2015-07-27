package com.johnlotito.intelvideotest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.johnlotito.intelvideotest.util.AndroidUtils;
import com.johnlotito.intelvideotest.util.LogUtils;
import com.johnlotito.intelvideotest.util.StorageUtils;
import com.johnlotito.intelvideotest.util.VideoUtils;
import com.johnlotito.intelvideotest.widget.GridDividerDecoration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class RecentMediaFragment extends Fragment implements RecentMediaAdapter.OnMediaItemSelectedListener,
    LoaderManager.LoaderCallbacks<List<RecentMediaItem>> {

    public static final String TAG = "com.johnlotito.intelvideotest.SelectMediaFragment";

    private static final String EXTRA_TEMP_FILE_PATH = "com.johnlotito.intelvideotest.extra.TEMP_FILE_PATH";

    private static final int LOADER_RECENT_MEDIA = 0;

    private static final int REQUEST_SELECT_MEDIA = 11;
    private static final int REQUEST_CAPTURE_MEDIA = 12;

    private RecyclerView mGridView;
    private RecentMediaAdapter mAdapter;

    private File mTempCameraFile;

    /**********************************************************
     * Fragment Lifecycle Methods
     *********************************************************/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_TEMP_FILE_PATH)) {
            mTempCameraFile = new File(savedInstanceState.getString(EXTRA_TEMP_FILE_PATH));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recent_media, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mGridView = (RecyclerView) view.findViewById(R.id.media_grid);
        mGridView.setLayoutManager(new GridLayoutManager(getActivity(),
                getResources().getInteger(R.integer.attachment_media_column_count)));
        mGridView.setItemAnimator(new DefaultItemAnimator());
        mGridView.addItemDecoration(new GridDividerDecoration(getActivity()));
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new RecentMediaAdapter(getActivity(), this);
        mGridView.setAdapter(mAdapter);

        getLoaderManager().restartLoader(LOADER_RECENT_MEDIA, null, this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case REQUEST_CAPTURE_MEDIA:
                String imageUri = Uri.fromFile(mTempCameraFile).toString();
                onMediaItemSelected(imageUri);

                MediaScannerConnection.scanFile(getActivity().getApplicationContext(),
                        new String[]{mTempCameraFile.getAbsolutePath()}, null, null);
                break;

            case REQUEST_SELECT_MEDIA:
                if (data.getType() == null || data.getType().startsWith("image/")
                        || data.getType().startsWith("video/")) {
                    if (data.getData() != null) {
                        String mediaUri = data.getDataString();

                        if (ContentResolver.SCHEME_FILE.equals(data.getData().getScheme())) {
                            mediaUri = Uri.decode(mediaUri);
                        }

                        onMediaItemSelected(mediaUri);
                    }
                }
                break;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mTempCameraFile != null) {
            outState.putString(EXTRA_TEMP_FILE_PATH, mTempCameraFile.getAbsolutePath());
        }
    }

    /**********************************************************
     * LoaderManager.LoaderCallbacks Methods
     *********************************************************/

    @Override
    public Loader<List<RecentMediaItem>> onCreateLoader(int id, Bundle args) {
        if (id == LOADER_RECENT_MEDIA) {
            return new RecentMediaLoader(getActivity());
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<List<RecentMediaItem>> loader, List<RecentMediaItem> data) {
        if (loader.getId() == LOADER_RECENT_MEDIA) {
            mAdapter.setData(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<RecentMediaItem>> loader) {
        if (loader.getId() == LOADER_RECENT_MEDIA) {
            mAdapter.setData(null);
        }
    }

    /**********************************************************
     * RecentMediaAdapter.OnMediaItemSelectedListener Interface Methods
     *********************************************************/

    @Override
    public void onCaptureMedia() {
        showCaptureMediaChooserDialog();
    }

    @Override
    public void onChooseFromGallery() {
        selectMediaFromGallery();
    }

    @Override
    public void onMediaItemSelected(String uri) {
        if (VideoUtils.isVideoUri(getActivity(), uri)) {
            Intent intent = new Intent(getActivity(), TrimVideoActivity.class);
            intent.setData(Uri.parse(uri));
            startActivity(intent);
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(uri), "image/*");
            startActivity(intent);
        }
    }

    /**********************************************************
     * Private Methods
     *********************************************************/

    private void showCaptureMediaChooserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setItems(R.array.attachment_capture_media, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        takePicture();
                        break;
                    case 1:
                        takeVideo();
                        break;
                }
            }
        });
        builder.create().show();
    }

    private void takePicture() {
        try {
            mTempCameraFile = StorageUtils.getNewImageFile(StorageUtils.EXT_JPG);

            if (mTempCameraFile == null) {
                mTempCameraFile = File.createTempFile("IMG_", "." + StorageUtils.EXT_JPG);
            }

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mTempCameraFile));
            startActivityForResult(intent, REQUEST_CAPTURE_MEDIA);
        } catch (IOException ex) {
            LogUtils.e(ex);
        }
    }

    private void takeVideo() {
        try {
            mTempCameraFile = StorageUtils.getNewImageFile(StorageUtils.EXT_MP4);

            if (mTempCameraFile == null) {
                mTempCameraFile = File.createTempFile("VID_", "." + StorageUtils.EXT_MP4);
            }

            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mTempCameraFile));
            intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30);
            startActivityForResult(intent, REQUEST_CAPTURE_MEDIA);
        } catch (IOException ex) {
            LogUtils.e(ex);
        }
    }

    private void selectMediaFromGallery() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);

        if (AndroidUtils.isKitKat()) {
            i.setType("*/*");
            String[] types = new String[]{"video/*", "image/*"};
            i.putExtra(Intent.EXTRA_MIME_TYPES, types);
        } else {
            i.setType("video/*,image/*");
        }

        startActivityForResult(i, REQUEST_SELECT_MEDIA);
    }
}
