package com.cj.mobile.myarapplication.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cj.mobile.myarapplication.R;
import com.cj.mobile.myarapplication.model.Ornament;

import java.util.List;

/**
 * 装饰适配器
 */

public class OrnamentAdapter extends RecyclerView.Adapter<OrnamentAdapter.MyViewHolder>{
    private Context mContext;
    private List<Ornament> mData;

    public OrnamentAdapter(Context mContext, List<Ornament> mData) {
        this.mContext = mContext;
        this.mData = mData;
    }

    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(mContext)
                .inflate(R.layout.item_ornament, parent, false));
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        final Ornament bean = mData.get(position);
        if (bean != null) {
            holder.ivImg.setColorFilter(Color.WHITE);
            holder.ivImg.setImageResource(bean.getImgResId());

            holder.ivImg.setOnClickListener(view -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(position);
                }
            });
        } else {
            holder.ivImg.setImageResource(0);
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivImg;

        MyViewHolder(View itemView) {
            super(itemView);
            ivImg = (ImageView) itemView.findViewById(R.id.iv_img);
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
