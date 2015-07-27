package com.johnlotito.intelvideotest;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.v4.content.AsyncTaskLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Queries for the user's recent gallery items and from the result builds a list of MediaItems
 * that will contain information about each of the results,
 * i.e. Thumbnail path
 */
public class RecentMediaLoader extends AsyncTaskLoader<List<RecentMediaItem>> {
    private final ForceLoadContentObserver mObserver;

    private List<RecentMediaItem> mResults;

    public RecentMediaLoader(Context context) {
        super(context);

        mObserver = new ForceLoadContentObserver();
    }

    @Override
    protected void onStartLoading() {
        if (mResults != null) {
            deliverResult(mResults);
        }
        if (takeContentChanged() || mResults == null) {
            forceLoad();
        }
    }

    @Override
    public List<RecentMediaItem> loadInBackground() {
        List<RecentMediaItem> items = null;

        Cursor cursor = performQuery();
        if (cursor != null) {
            items = fetchThumbnails(cursor);
            cursor.close();
        }

        getContext().getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, mObserver);

        getContext().getContentResolver().registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, false, mObserver);

        return items;
    }

    @Override
    public void deliverResult(List<RecentMediaItem> data) {
        if (isReset()) {
            return;
        }

        mResults = data;

        if (isStarted()) {
            super.deliverResult(data);
        }
    }

    /**********************************************************
     * Private Methods
     *********************************************************/

    private Cursor performQuery() {
        String selection;
        String[] selectionArgs;

        selection = String.format("%s = ? OR %s = ?",
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.MEDIA_TYPE);

        selectionArgs = new String[] {
                String.format("%d", MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
                String.format("%d", MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
        };

        return getContext().getContentResolver().query(
                MediaStore.Files.getContentUri("external"),
                RecentMediaAdapter.GalleryQuery.PROJECTION, selection, selectionArgs,
                MediaStore.Files.FileColumns.DATE_ADDED + " DESC LIMIT 50");
    }

    private List<RecentMediaItem> fetchThumbnails(Cursor cursor) {
        List<RecentMediaItem> results = new ArrayList<>(cursor.getCount());

        while (!isReset() && cursor.moveToNext()) {
            String filePath = cursor.getString(RecentMediaAdapter.GalleryQuery.DATA);
            String uri = String.format("%s://%s", ContentResolver.SCHEME_FILE, filePath);
            long id = cursor.getLong(RecentMediaAdapter.GalleryQuery.ID);

            RecentMediaItem item = null;
            if(cursor.getInt(RecentMediaAdapter.GalleryQuery.MEDIA_TYPE) ==
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                item = buildImageItem(uri, id);
            } else {

                item = buildVideoItem(filePath, uri, id);
            }

            if (item != null) {
                results.add(item);
            }
        }

        return results;
    }

    private RecentMediaItem buildImageItem(String fullUri, long id) {
        Cursor cursor = MediaStore.Images.Thumbnails.queryMiniThumbnail(
                getContext().getContentResolver(),
                id, MediaStore.Images.Thumbnails.MINI_KIND,
                null);

        RecentMediaItem item = new RecentMediaItem(RecentMediaItem.MEDIA_TYPE_IMAGE, fullUri);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String thumbnailFilePath = cursor.getString(
                        cursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA));
                String thumbnailUri = String.format("%s://%s",
                        ContentResolver.SCHEME_FILE, thumbnailFilePath);

                item.setThumbnailUri(thumbnailUri);
            }
            cursor.close();
        }

        if (item.getThumbnailUri() == null) {
            Bitmap thumbnail = MediaStore.Images.Thumbnails.getThumbnail(
                    getContext().getContentResolver(), id,
                    MediaStore.Images.Thumbnails.MINI_KIND, null);
            item.setThumbnailBitmap(thumbnail);
        }

        return item;
    }

    private RecentMediaItem buildVideoItem(String filePath, String fullUri, long id) {
        Cursor cursor = getContext().getContentResolver().query(
                MediaStore.Video.Thumbnails.getContentUri("external"),
                new String[] {
                        MediaStore.Video.Thumbnails._ID,
                        MediaStore.Video.Thumbnails.DATA
                },
                String.format(
                        "%s IN (SELECT _id FROM video WHERE _data = ?)",
                        MediaStore.Video.Thumbnails.VIDEO_ID
                ),
                new String[] { filePath },
                null);

        RecentMediaItem item = new RecentMediaItem(RecentMediaItem.MEDIA_TYPE_VIDEO, fullUri);;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String thumbnailFilePath = cursor.getString(1);
                String thumbnailUri = String.format("%s://%s",
                        ContentResolver.SCHEME_FILE, thumbnailFilePath);

                item.setThumbnailUri(thumbnailUri);
            }
            cursor.close();
        }

        if (item.getThumbnailUri() == null) {
            Bitmap thumbnail = MediaStore.Video.Thumbnails.getThumbnail(
                    getContext().getContentResolver(), id,
                    MediaStore.Video.Thumbnails.MINI_KIND, null);
            item.setThumbnailBitmap(thumbnail);
        }

        return item;
    }

}
