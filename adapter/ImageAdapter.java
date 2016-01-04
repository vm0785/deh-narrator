package com.mmlab.n1.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;


import com.mmlab.n1.ExternalStorage;
import com.mmlab.n1.R;
import com.mmlab.n1.Utils;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 圖片
 * Created by mmlab on 2015/9/30.
 */
public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.MyViewHolder> {

    private static final String TAG = "ImageAdapter";
    private Context mContext = null;

    private List<String> mImages = new ArrayList<>();

    public ImageAdapter(Context context, List<String> mImages) {
        this.mImages = mImages;
        this.mContext = context;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false));
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

        // holder.site_image.setImageDrawable(mContext.getResources().getDrawable(R.drawable.group_selected));

        Picasso.with(mContext)
                .load(new File(ExternalStorage.BASE_ROOT + File.separator + ExternalStorage.TARGET_DIRECTORY + File.separator + Utils.urlToFilename(mImages.get(position))))
                .fit()
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder_error)
                .into(holder.site_image);
        Log.d(TAG, ExternalStorage.BASE_ROOT + File.separator + ExternalStorage.TARGET_DIRECTORY + File.separator + Utils.urlToFilename(mImages.get(position)));

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
        return mImages.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        ImageView site_image;

        public MyViewHolder(View view) {
            super(view);
            site_image = (ImageView) view.findViewById(R.id.site_image);
        }
    }
}