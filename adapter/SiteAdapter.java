package com.mmlab.n1.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.mmlab.n1.R;
import com.mmlab.n1.info.POI;

import java.util.List;

/**
 * Created by mmlab on 2015/9/23.
 */
public class SiteAdapter extends RecyclerView.Adapter<SiteAdapter.MyViewHolder> {

    private static final String TAG = "SiteAdapter";

    ColorGenerator generator = ColorGenerator.MATERIAL;
    private Context mContext = null;

    private List<POI> mSites = null;

    public SiteAdapter(Context context, List<POI> groups) {
        this.mSites = groups;
        this.mContext = context;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_site, parent, false));
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
        // holder.group_icon.setTitleText(mSites.get(position).getName().substring(0, 1));
        TextDrawable drawable = TextDrawable.builder()
                .beginConfig()
                .width(56)
                .height(56)
                .endConfig()
                .buildRect(mSites.get(position).getName().substring(0, 1), generator.getRandomColor());
        holder.group_icon.setImageDrawable(drawable);
        holder.group_title.setText(mSites.get(position).getName());
        holder.group_content.setText(mSites.get(position).getDescript());
        holder.group_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        // 如果設置了回調，則設置點擊事件
        if (mOnItemClickLitener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getLayoutPosition();
                    mOnItemClickLitener.onItemClick(holder.itemView, pos);
                }
            });

            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int pos = holder.getLayoutPosition();
                    mOnItemClickLitener.onItemLongClick(holder.itemView, pos);
                    return false;
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mSites.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        ImageView group_icon;
        TextView group_title;
        TextView group_content;
        ImageButton group_menu;

        public MyViewHolder(View view) {
            super(view);
            group_icon = (ImageView) view.findViewById(R.id.group_icon);
            group_title = (TextView) view.findViewById(R.id.group_title);
            group_content = (TextView) view.findViewById(R.id.group_content);
            group_menu = (ImageButton) view.findViewById(R.id.group_menu);
        }
    }
}
