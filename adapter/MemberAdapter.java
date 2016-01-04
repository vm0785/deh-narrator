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
import com.mmlab.n1.info.Profile;

import java.util.List;

/**
 * Created by mmlab on 2015/9/16.
 */
public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MyViewHolder> {

    private static final String TAG = "MemberAdapter";
    private Context mContext = null;

    private List<Profile> mMembers = null;

    public MemberAdapter(Context context, List<Profile> members) {
        this.mMembers = members;
        this.mContext = context;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member, parent, false));
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

            if (mMembers.get(position).isConnected) {
                holder.member_icon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.member_connected));
            }else {
                holder.member_icon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.member_disconnected));
            }

            holder.member_title.setText(mMembers.get(position).FB_NAME);
            holder.member_content.setText(mMembers.get(position).IP_ADDRESS);
            holder.member_menu.setOnClickListener(new View.OnClickListener() {
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
        return mMembers.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        TextView member_title;
        TextView member_content;
        ImageButton member_menu;
        ImageView member_icon;

        public MyViewHolder(View view) {
            super(view);
            member_icon = (ImageView) view.findViewById(R.id.member_icon);
            member_title = (TextView) view.findViewById(R.id.member_title);
            member_content = (TextView) view.findViewById(R.id.member_content);
            member_menu = (ImageButton) view.findViewById(R.id.member_menu);
        }
    }
}
