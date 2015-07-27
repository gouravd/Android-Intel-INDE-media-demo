package com.johnlotito.intelvideotest.video;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Build;

import com.intel.inde.mp.AudioFormat;
import com.intel.inde.mp.IProgressListener;
import com.intel.inde.mp.MediaComposer;
import com.intel.inde.mp.MediaFile;
import com.intel.inde.mp.MediaFileInfo;
import com.intel.inde.mp.Uri;
import com.intel.inde.mp.VideoFormat;
import com.intel.inde.mp.android.AndroidMediaObjectFactory;
import com.intel.inde.mp.android.AudioFormatAndroid;
import com.intel.inde.mp.android.VideoFormatAndroid;
import com.intel.inde.mp.android.graphics.VideoEffect;
import com.intel.inde.mp.domain.Pair;
import com.intel.inde.mp.domain.Resolution;
import com.johnlotito.intelvideotest.util.LogUtils;

import java.io.File;
import java.io.IOException;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class VideoTranscoder {

    private final Context mContext;
    private MediaComposer mMediaComposer;
    private AndroidMediaObjectFactory mFactory;
    private MediaFileInfo mMediaFileInfo;

    private final Uri mSourceUri;
    private final File mOutputFile;

    private long mTrimStartTime;
    private long mTrimEndTime;

    // Video Params
    private final String mVideoMimeType = "video/avc";
    private int mVideoBitRateInKBytes = 2 * 1024;
    private int mVideoFrameRate = 30;
    private int mVideoIFrameInterval = 10;
    private int mVideoRotationOut = 0;
    private Resolution mFrameSize;

    // Audio Params
    private final String mAudioMimeType = "audio/mp4a-latm";
    private int mAudioSampleRate = 48000;
    private int mAudioChannelCount = 2;
    private int mAudioBitRate = 96 * 1024;
    private int mAudioProfile = MediaCodecInfo.CodecProfileLevel.AACObjectLC;

    public VideoTranscoder(Context context, String sourceUri, File outputFile) {
        mContext = context.getApplicationContext();
        mSourceUri = new Uri(sourceUri);
        mOutputFile = outputFile;
    }

    public void startTranscode(IProgressListener listener) {
        try {
            mFactory = new AndroidMediaObjectFactory(mContext);

            setOutputVideoOrientation();
            setEncodingParameters();
            transcode(listener);
        } catch (Exception e) {
            LogUtils.e(e);
        }
    }

    public void destroy() {
        if (mMediaComposer != null) {
            mMediaComposer.stop();
        }
    }

    public void setTrim(long start, long end) {
        mTrimStartTime = start * 1000;
        mTrimEndTime = end * 1000;
    }

    public void stop() {
        if (mMediaComposer != null) {
            mMediaComposer.stop();
        }
    }

    protected void setEncodingParameters() {
        try {
            mMediaFileInfo = new MediaFileInfo(mFactory);
            mMediaFileInfo.setUri(mSourceUri);

            setVideoEncodingParameters(mMediaFileInfo);
            setAudioEncodingParameters(mMediaFileInfo);
        } catch (Exception e) {
            String message = (e.getMessage() != null) ? e.getMessage() : e.toString();
            LogUtils.e(message, e);
        }
    }

    /**
     * *******************************************************
     * Private Methods
     * *******************************************************
     */

    private void setAudioEncodingParameters(MediaFileInfo mediaFileInfo) {
        AudioFormat audioFormat = (AudioFormat) mediaFileInfo.getAudioFormat();
        if (audioFormat != null) {
            // TODO : These may throw exceptions if the values are not available in the
            // TODO : MediaFileInfo class, so should we try catch each one individually?
            mAudioSampleRate = Math.min(audioFormat.getAudioSampleRateInHz(), mAudioSampleRate);
            mAudioChannelCount = Math.min(audioFormat.getAudioChannelCount(), mAudioChannelCount);
            mAudioBitRate = Math.min(audioFormat.getAudioBitrateInBytes(), mAudioBitRate);
            mAudioProfile = audioFormat.getAudioProfile();
        } else {
            LogUtils.e("Audio format info unavailable");
        }
    }

    private void setVideoEncodingParameters(MediaFileInfo mediaFileInfo) {
        VideoFormat formatToCopy = (VideoFormat) mediaFileInfo.getVideoFormat();
        mFrameSize = formatToCopy.getVideoFrameSize();

        if(mVideoRotationOut == 90 || mVideoRotationOut == 270 || mVideoRotationOut == -90) {
            mFrameSize = new Resolution(mFrameSize.height(), mFrameSize.width());
        }

        try {
            mVideoBitRateInKBytes = formatToCopy.getVideoBitRateInKBytes();
        }
        catch(RuntimeException ex) {
            LogUtils.e(ex);
        }

        try {
            mVideoFrameRate = formatToCopy.getVideoFrameRate();
        }
        catch(RuntimeException ex) {
            LogUtils.e(ex);
        }

        try {
            mVideoIFrameInterval = formatToCopy.getVideoIFrameInterval();
        }
        catch(RuntimeException ex) {
            LogUtils.e(ex);
        }
    }

    private void transcode(IProgressListener listener) throws IOException {
        mMediaComposer = new MediaComposer(mFactory, listener);
        mMediaComposer.addSourceFile(mSourceUri);
        mMediaComposer.setTargetFile(mOutputFile.getAbsolutePath());
        mMediaComposer.setTargetVideoFormat(getTargetVideoFormat());
        mMediaComposer.setTargetAudioFormat(getTargetAudioFormat());
        mMediaComposer.addVideoEffect(getOrientationCorrectionEffect());

        if (mTrimEndTime > mTrimStartTime) {
            MediaFile mediaFile = mMediaComposer.getSourceFiles().get(0);
            mediaFile.addSegment(new Pair<>(mTrimStartTime, mTrimEndTime));
        }

        mMediaComposer.start();
    }

    private VideoFormatAndroid getTargetVideoFormat() {
        VideoFormatAndroid videoFormat = new VideoFormatAndroid(
                mVideoMimeType, mFrameSize.width(), mFrameSize.height());

        videoFormat.setVideoBitRateInKBytes(mVideoBitRateInKBytes);
        videoFormat.setVideoFrameRate(mVideoFrameRate);
        videoFormat.setVideoIFrameInterval(mVideoIFrameInterval);
        videoFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);

        return videoFormat;
    }

    private AudioFormatAndroid getTargetAudioFormat() {
        AudioFormatAndroid audioFormat = new AudioFormatAndroid(
                mAudioMimeType, mAudioSampleRate, mAudioChannelCount);

        audioFormat.setAudioBitrateInBytes(mAudioBitRate);
        audioFormat.setAudioProfile(mAudioProfile);
        audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);

        return audioFormat;
    }

    private VideoEffect getOrientationCorrectionEffect() {
        VideoEffect effect = new VideoEffect(120, mFactory.getEglUtil());
        effect.setSegment(new Pair<>(1L, 1L));

        return effect;
    }

    private void setOutputVideoOrientation() {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(mContext, android.net.Uri.parse(mSourceUri.getString()));
        String orientation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        mVideoRotationOut = Integer.parseInt(orientation);
    }
}
