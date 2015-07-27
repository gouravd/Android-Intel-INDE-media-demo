package com.johnlotito.intelvideotest.util;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StorageUtils {
    public static final String EXT_JPG = "jpg";
    public static final String EXT_GIF = "gif";
    public static final String EXT_MP4 = "mp4";

    public static final String TEMP_ATTACHMENT_DIRECTORY_NAME = "temp_images";
    public static final String ALBUM_NAME = "IntelVideoTestGallery";

    public static File getGalleryDirectory() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();

            File dir = new File(root + File.separator + ALBUM_NAME);
            if (dir.mkdirs() || dir.isDirectory()) {
                return dir;
            }
        }

        return null;
    }

    public static File getLocalImagesTempDirectory(Context context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                !Environment.isExternalStorageRemovable()) {

            //seems like this can still be null even if the above are true
            File dir = new File(context.getExternalCacheDir(), TEMP_ATTACHMENT_DIRECTORY_NAME);
            if (dir.mkdirs() || dir.isDirectory()) {
                File nomedia = new File(dir, ".nomedia");
                if (!nomedia.exists()) {
                    try {
                        nomedia.createNewFile();
                    } catch (IOException e) {
                        LogUtils.e("Failed while creating local .nomedia file to temp image attachment directory.");
                        LogUtils.e(e);
                    }
                }

                return dir;
            }
        }

        return new File(context.getCacheDir(), TEMP_ATTACHMENT_DIRECTORY_NAME);
    }

    public static File getNewImageFile(String extension) {
        return getNewImageFile(null, extension, false);
    }

    public static File getNewImageFile(Context context, String extension, boolean temp) {
        File dir;
        if (temp) {
            dir = getLocalImagesTempDirectory(context);
        } else {
            dir = getGalleryDirectory();
        }

        if (dir != null) {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = String.format("IMG_%s.%s", timeStamp, extension);
            File file = new File(dir, fileName);

            if (file.exists()) {
                file.delete();
            }

            return file;
        }

        return null;
    }
}
