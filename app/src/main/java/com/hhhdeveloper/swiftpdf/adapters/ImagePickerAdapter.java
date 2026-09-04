package com.hhhdeveloper.swiftpdf.adapters;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.hhhdeveloper.swiftpdf.R;
import com.hhhdeveloper.swiftpdf.utils.FileUtil;

import java.util.Collections;
import java.util.List;

public class ImagePickerAdapter extends RecyclerView.Adapter<ImagePickerAdapter.ViewHolder> {


    public interface OnItemRemovedListener {
        void onItemRemoved(int position);
    }

    public interface OnImageEditListener {
        void onEdit(int position, Uri uri);
    }

    private final List<Uri> imageUris;
    private ItemTouchHelper touchHelper;
    private OnItemRemovedListener onItemRemovedListener;
    private OnImageEditListener onImageEditListener;

    public ImagePickerAdapter(List<Uri> imageUris) {
        this.imageUris = imageUris;
    }

    public void setItemTouchHelper(ItemTouchHelper helper) {
        this.touchHelper = helper;
    }

    public void setOnItemRemovedListener(OnItemRemovedListener l) {
        this.onItemRemovedListener = l;
    }

    public void setOnImageEditListener(OnImageEditListener l) {
        this.onImageEditListener = l;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Uri uri = imageUris.get(position);

        // Load thumbnail with Glide
        Glide.with(holder.ivThumbnail.getContext())
                .load(uri)
                .centerCrop()
                .into(holder.ivThumbnail);

        // Set image name
        String name = FileUtil.getFileName(holder.itemView.getContext(), uri);
        holder.tvImageName.setText(name != null ? name : "Image " + (position + 1));
        holder.tvImageSize.setText("Image " + (position + 1) + " of " + imageUris.size());

        holder.ivDragHandle.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                if (touchHelper != null) touchHelper.startDrag(holder);
            }
            return false;
        });

        holder.btnEdit.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && onImageEditListener != null) {
                onImageEditListener.onEdit(pos, uri);
            }
        });

        holder.btnRemove.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) {
                imageUris.remove(pos);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, imageUris.size());
                if (onItemRemovedListener != null) onItemRemovedListener.onItemRemoved(pos);
            }
        });
    }

    @Override
    public int getItemCount() { return imageUris.size(); }

    public boolean onMove(RecyclerView rv, RecyclerView.ViewHolder from, RecyclerView.ViewHolder to) {
        Collections.swap(imageUris, from.getAdapterPosition(), to.getAdapterPosition());
        notifyItemMoved(from.getAdapterPosition(), to.getAdapterPosition());
        return true;
    }

    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        int pos = viewHolder.getAdapterPosition();
        imageUris.remove(pos);
        notifyItemRemoved(pos);
        if (onItemRemovedListener != null) onItemRemovedListener.onItemRemoved(pos);
    }

    public List<Uri> getImageUris() { return imageUris; }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDragHandle, ivThumbnail, btnRemove, btnEdit;
        TextView tvImageName, tvImageSize;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDragHandle = itemView.findViewById(R.id.iv_drag_handle);
            ivThumbnail  = itemView.findViewById(R.id.iv_thumbnail);
            btnRemove    = itemView.findViewById(R.id.btn_remove);
            btnEdit      = itemView.findViewById(R.id.btn_edit);
            tvImageName  = itemView.findViewById(R.id.tv_image_name);
            tvImageSize  = itemView.findViewById(R.id.tv_image_size);
        }
    }
}
