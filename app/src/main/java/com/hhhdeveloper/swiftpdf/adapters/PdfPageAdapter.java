package com.hhhdeveloper.swiftpdf.adapters;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hhhdeveloper.swiftpdf.R;
import com.hhhdeveloper.swiftpdf.models.PageItem;
import com.hhhdeveloper.swiftpdf.utils.PdfRendererCache;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.ViewHolder> {

    private final List<PageItem> pageItems;
    private final PdfRendererCache rendererCache;
    private final OnPageDeleteListener deleteListener;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnPageDeleteListener {
        void onDelete(int position);
    }

    public PdfPageAdapter(List<PageItem> pageItems, PdfRendererCache rendererCache, OnPageDeleteListener deleteListener) {
        this.pageItems = pageItems;
        this.rendererCache = rendererCache;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pdf_page, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PageItem item = pageItems.get(position);
        holder.tvPageIndex.setText("Page " + (position + 1));

        // Clear thumbnail before loading
        holder.ivThumbnail.setImageBitmap(null);

        executor.submit(() -> {
            try {
                PdfRenderer renderer = rendererCache.getRenderer(item.sourceFile);
                if (item.originalPageIndex < renderer.getPageCount()) {
                    PdfRenderer.Page page = renderer.openPage(item.originalPageIndex);
                    
                    int width = 300;
                    int height = 400;
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    bitmap.eraseColor(android.graphics.Color.WHITE);
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    page.close();

                    mainHandler.post(() -> {
                        if (holder.getAdapterPosition() == position) {
                            holder.ivThumbnail.setImageBitmap(bitmap);
                        }
                    });
                }
            } catch (Exception ignored) {}
        });

        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && deleteListener != null) {
                deleteListener.onDelete(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return pageItems.size();
    }

    public boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(pageItems, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        mainHandler.postDelayed(() -> notifyItemRangeChanged(0, pageItems.size()), 350);
        return true;
    }

    public List<PageItem> getPageItems() {
        return pageItems;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView tvPageIndex;
        ImageView btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_page_thumbnail);
            tvPageIndex = itemView.findViewById(R.id.tv_page_index);
            btnDelete = itemView.findViewById(R.id.btn_delete_page);
        }
    }
}
