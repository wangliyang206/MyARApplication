package com.cj.mobile.myarapplication.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.cj.mobile.myarapplication.R;

import java.util.List;

/**
 * 滤镜 - 适配器
 */

public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.MyViewHolder> {
    private Context mContext;
    private List<Integer> mData;

    public FilterAdapter(Context mContext, List<Integer> mData) {
        this.mContext = mContext;
        this.mData = mData;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(mContext)
                .inflate(R.layout.item_filter, parent, false));
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        int index = mData.get(position);
        String resName = "filter" + index;
        int nameResId = mContext.getResources().getIdentifier(resName, "string", mContext.getPackageName());
        String name = mContext.getString(nameResId);
        holder.tvName.setText(name);

        resName = "filter_thumb_" + index;
        int imgResId = mContext.getResources().getIdentifier(resName, "drawable", mContext.getPackageName());
        holder.ivImg.setImageResource(imgResId);

        holder.layoutRoot.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(position);
            }
        });
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        private FrameLayout layoutRoot;
        private ImageView ivImg;
        private TextView tvName;

        MyViewHolder(View itemView) {
            super(itemView);
            layoutRoot = (FrameLayout) itemView.findViewById(R.id.layout_root);
            ivImg = (ImageView) itemView.findViewById(R.id.iv_img);
            tvName = (TextView) itemView.findViewById(R.id.tv_name);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    private OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }
}
