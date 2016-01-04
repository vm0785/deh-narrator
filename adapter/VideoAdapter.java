package com.mmlab.n1.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.mmlab.n1.R;
import com.mmlab.n1.Utils;
import com.mmlab.n1.preference.Preset;

import java.util.List;

/**
 * Created by mmlab on 2015/9/30.
 */
public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.MyViewHolder> {

    private static final String TAG = "VideoAdapter";
    private Context mContext = null;

    private List<String> mVideos = null;

    public VideoAdapter(Context context, List<String> mVideos) {
        this.mVideos = mVideos;
        this.mContext = context;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video, parent, false));
    }

    public interface OnItemClickLitener {
        void onItemClick(View view, int position);

        void onItemLongClick(View view, int position);
    }

    private OnItemClickLitener mOnItemClickLitener;

    public void setOnItemClickLitener(OnItemClickLitener mOnItemClickLitener) {
        this.mOnItemClickLitener = mOnItemClickLitener;
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {

        if (Preset.loadFilePreferences(mContext, Utils.urlToFilename(mVideos.get(position))) != -1)
            holder.site_video.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_video_download));
        else
            holder.site_video.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_video_undownload));

        // 如果設置了回調，則設置點擊事件
        if (mOnItemClickLitener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    int pos = holder.getLayoutPosition();
                    mOnItemClickLitener.onItemClick(holder.itemView, pos);
                }
            });

            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    int pos = holder.getLayoutPosition();
                    mOnItemClickLitener.onItemLongClick(holder.itemView, pos);
                    return false;
                }
            });
        }
    }

    public int getItemCount() {
        return mVideos.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        ImageView site_video;

        public MyViewHolder(View view) {
            super(view);
            site_video = (ImageView) view.findViewById(R.id.site_video);
        }
    }

}