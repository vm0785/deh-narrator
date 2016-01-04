package com.mmlab.n1.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.mmlab.n1.R;
import com.mmlab.n1.info.Group;

import java.util.List;

/**
 * Created by mmlab on 2015/9/16.
 */
public class MainAdapter extends RecyclerView.Adapter<MainAdapter.MyViewHolder> {

    private static final String TAG = "MainAdapter";
    private Context mContext = null;

    private List<Group> mGroups = null;

    public MainAdapter(Context context, List<Group> groups) {
        this.mGroups = groups;
        this.mContext = context;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_main, parent, false));
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

            if (mGroups.get(position).isConnected) {
                holder.group_icon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.group_selected));
            }else {
                holder.group_icon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.group_unselected));
            }

            holder.group_title.setText(mGroups.get(position).SSID);
            holder.group_content.setText(mGroups.get(position).getEncryptString());
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
        return mGroups.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        TextView group_title;
        TextView group_content;
        ImageButton group_menu;
        ImageView group_icon;

        public MyViewHolder(View view) {
            super(view);
            group_icon = (ImageView) view.findViewById(R.id.group_icon);
            group_title = (TextView) view.findViewById(R.id.group_title);
            group_content = (TextView) view.findViewById(R.id.group_content);
            group_menu = (ImageButton) view.findViewById(R.id.group_menu);
        }
    }
}
