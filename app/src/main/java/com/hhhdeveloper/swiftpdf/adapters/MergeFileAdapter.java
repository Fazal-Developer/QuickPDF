package com.hhhdeveloper.swiftpdf.adapters;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.hhhdeveloper.swiftpdf.R;
import com.hhhdeveloper.swiftpdf.models.PdfFile;

import java.util.Collections;
import java.util.List;

public class MergeFileAdapter extends RecyclerView.Adapter<MergeFileAdapter.ViewHolder> {

    private final List<PdfFile> files;
    private ItemTouchHelper touchHelper;
    private OnItemRemovedListener onItemRemovedListener;

    public interface OnItemRemovedListener {
        void onItemRemoved(int position);
    }

    public MergeFileAdapter(List<PdfFile> files) {
        this.files = files;
    }

    public void setItemTouchHelper(ItemTouchHelper helper) {
        this.touchHelper = helper;
    }

    public void setOnItemRemovedListener(OnItemRemovedListener l) {
        this.onItemRemovedListener = l;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_merge_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PdfFile file = files.get(position);
        holder.tvFileName.setText(file.getName());
        holder.tvFileSize.setText(file.getFormattedSize());

        holder.ivDragHandle.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                if (touchHelper != null) touchHelper.startDrag(holder);
            }
            return false;
        });

        holder.btnRemove.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) {
                files.remove(pos);
                notifyItemRemoved(pos);
                if (onItemRemovedListener != null) onItemRemovedListener.onItemRemoved(pos);
            }
        });
    }

    @Override
    public int getItemCount() { return files.size(); }

    // Drag-and-drop support
    public boolean onMove(RecyclerView rv, RecyclerView.ViewHolder from, RecyclerView.ViewHolder to) {
        Collections.swap(files, from.getAdapterPosition(), to.getAdapterPosition());
        notifyItemMoved(from.getAdapterPosition(), to.getAdapterPosition());
        return true;
    }

    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        int pos = viewHolder.getAdapterPosition();
        files.remove(pos);
        notifyItemRemoved(pos);
        if (onItemRemovedListener != null) onItemRemovedListener.onItemRemoved(pos);
    }

    public List<PdfFile> getFiles() { return files; }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDragHandle, btnRemove;
        TextView tvFileName, tvFileSize;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDragHandle = itemView.findViewById(R.id.iv_drag_handle);
            btnRemove    = itemView.findViewById(R.id.btn_remove);
            tvFileName   = itemView.findViewById(R.id.tv_file_name);
            tvFileSize   = itemView.findViewById(R.id.tv_file_size);
        }
    }
}
