package com.johnlotito.intelvideotest;

import android.content.Context;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

public class RecentMediaAdapter extends RecyclerView.Adapter<RecentMediaAdapter.ViewHolder> {
    private final Context mContext;
    private final OnMediaItemSelectedListener mListener;

    private List<RecentMediaItem> mRecentMediaItemList;

    public RecentMediaAdapter(Context context, OnMediaItemSelectedListener listener) {
        mContext = context;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(
                R.layout.item_recent_media, null);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (position == 0) {
            holder.gifTag.setVisibility(View.GONE);
            holder.videoOverlay.setVisibility(View.GONE);
            holder.imageView.setImageResource(R.drawable.bg_attach_camera);
            holder.label.setVisibility(View.VISIBLE);
            holder.label.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_gallery_camera, 0, 0);
            holder.label.setText(R.string.attachment_label_camera);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onCaptureMedia();
                    }
                }
            });
            return;
        }

        if (position == 1) {
            holder.gifTag.setVisibility(View.GONE);
            holder.videoOverlay.setVisibility(View.GONE);
            holder.imageView.setImageResource(R.drawable.bg_attach_gallery);
            holder.label.setVisibility(View.VISIBLE);
            holder.label.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_gallery_library, 0, 0);
            holder.label.setText(R.string.attachment_label_gallery);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onChooseFromGallery();
                    }
                }
            });
            return;
        }

        final RecentMediaItem item = mRecentMediaItemList.get(position - 2);

        if (item.getMediaType() == RecentMediaItem.MEDIA_TYPE_VIDEO) {
            holder.videoOverlay.setVisibility(View.VISIBLE);
        } else {
            holder.videoOverlay.setVisibility(View.GONE);
        }

        if (item.getFullImageUri().toLowerCase().endsWith(".gif")) {
            holder.gifTag.setVisibility(View.VISIBLE);
        } else {
            holder.gifTag.setVisibility(View.GONE);
        }

        if (item.getThumbnailUri() != null) {
            Picasso.with(mContext)
                    .load(item.getThumbnailUri())
                    .placeholder(R.color.bg_loading_image)
                    .into(holder.imageView);
        } else {
            holder.imageView.setBackgroundResource(R.color.bg_loading_image);
            holder.imageView.setImageBitmap(item.getThumbnailBitmap());
        }

        holder.label.setVisibility(View.GONE);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onMediaItemSelected(item.getFullImageUri());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mRecentMediaItemList == null ? 2 : mRecentMediaItemList.size() + 2;
    }

    public void setData(List<RecentMediaItem> recentMediaItems) {
        mRecentMediaItemList = recentMediaItems;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        View gifTag;
        ImageView videoOverlay;
        TextView label;

        public ViewHolder(View itemView) {
            super(itemView);

            imageView = (ImageView) itemView.findViewById(R.id.image_attachment);
            gifTag = itemView.findViewById(R.id.gif_tag);
            videoOverlay = (ImageView) itemView.findViewById(R.id.video_attachment_overlay);
            label = (TextView) itemView.findViewById(R.id.label);
        }
    }

    public static class GalleryQuery {
        public static final String[] PROJECTION = new String[] {
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.MEDIA_TYPE
        };

        public static final int ID = 0;
        public static final int DATA = 1;
        public static final int DATE_ADDED = 2;
        public static final int MIME_TYPE = 3;
        public static final int MEDIA_TYPE = 4;
    }

    public interface OnMediaItemSelectedListener {
        void onCaptureMedia();
        void onChooseFromGallery();
        void onMediaItemSelected(String uri);
    }
}