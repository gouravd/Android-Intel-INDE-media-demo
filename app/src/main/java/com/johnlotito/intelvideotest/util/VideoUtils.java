package com.johnlotito.intelvideotest.util;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

public class VideoUtils {
    public static boolean isVideoUri(Context context, String uri) {
        Uri realUri = Uri.parse(uri);
        String type = null;

        if (ContentResolver.SCHEME_FILE.equals(realUri.getScheme())) {
            int index = uri.lastIndexOf(".");

            String extension = index != -1 && index + 1 < uri.length() ?
                    uri.substring(index + 1) : null;

            if (extension != null) {
                MimeTypeMap mime = MimeTypeMap.getSingleton();
                type = mime.getMimeTypeFromExtension(extension);
            }

            if (type != null) {
                return type.startsWith("video/");
            } else {
                return uri.contains(MediaStore.Video.Media.INTERNAL_CONTENT_URI.toString())
                        || uri.contains(MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString());
            }
        } else {
            type = context.getContentResolver().getType(realUri);
            return type != null && type.startsWith("video/");
        }
    }
}
