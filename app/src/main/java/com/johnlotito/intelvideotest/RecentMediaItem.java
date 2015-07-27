package com.johnlotito.intelvideotest;

import android.graphics.Bitmap;

public class RecentMediaItem {
    public final static int MEDIA_TYPE_IMAGE = 0;
    public final static int MEDIA_TYPE_VIDEO = 1;

    private int mMediaType;
    private String mThumbnailUri;
    private String mFullImageUri;
    private Bitmap mThumbnailBitmap;

    public RecentMediaItem(int mediaType, String fullImageUri) {
        mMediaType = mediaType;
        mFullImageUri = fullImageUri;
    }

    public void setThumbnailUri(String thumbnailUri) {
        mThumbnailUri = thumbnailUri;
    }

    public void setThumbnailBitmap(Bitmap bitmap) {
        mThumbnailBitmap = bitmap;
    }

    public int getMediaType() {
        return mMediaType;
    }

    public String getThumbnailUri() {
        return mThumbnailUri;
    }

    public String getFullImageUri() {
        return mFullImageUri;
    }

    public Bitmap getThumbnailBitmap() {
        return mThumbnailBitmap;
    }

    public boolean isVideo() {
        return mMediaType == MEDIA_TYPE_VIDEO;
    }
}
