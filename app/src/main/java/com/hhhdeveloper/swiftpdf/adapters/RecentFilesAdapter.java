package com.hhhdeveloper.swiftpdf.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import com.hhhdeveloper.swiftpdf.R;
import com.hhhdeveloper.swiftpdf.models.RecentFile;
import com.hhhdeveloper.swiftpdf.utils.FileUtil;

import java.util.ArrayList;
import java.util.List;

public class RecentFilesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_FILE   = 1;

    public static class DisplayItem {
        public int type;
        public String headerTitle;
        public RecentFile file;

        public DisplayItem(String headerTitle) {
            this.type = TYPE_HEADER;
            this.headerTitle = headerTitle;
        }

        public DisplayItem(RecentFile file) {
            this.type = TYPE_FILE;
            this.file = file;
        }
    }

    private final List<DisplayItem> items = new ArrayList<>();
    private OnFileActionListener listener;
    private boolean isGrouped = false;

    public interface OnFileActionListener {
        void onOpen(RecentFile file);
        void onShare(RecentFile file);
        void onRename(RecentFile file);
        void onDelete(RecentFile file);
        void onEditPages(RecentFile file);
        void onLocate(RecentFile file);
    }

    public RecentFilesAdapter(List<RecentFile> files) {
        this(files, false);
    }

    public RecentFilesAdapter(List<RecentFile> files, boolean groupDates) {
        this.isGrouped = groupDates;
        setFiles(files, groupDates);
    }

    public void setOnFileActionListener(OnFileActionListener l) {
        this.listener = l;
    }

    public void setFiles(List<RecentFile> files) {
        setFiles(files, this.isGrouped);
    }

    public void setFiles(List<RecentFile> files, boolean groupDates) {
        this.isGrouped = groupDates;
        this.items.clear();
        if (files != null && !files.isEmpty()) {
            if (groupDates) {
                this.items.addAll(groupFilesByDate(files));
            } else {
                for (RecentFile f : files) {
                    this.items.add(new DisplayItem(f));
                }
            }
        }
        notifyDataSetChanged();
    }

    private List<DisplayItem> groupFilesByDate(List<RecentFile> rawFiles) {
        List<DisplayItem> list = new ArrayList<>();
        if (rawFiles == null || rawFiles.isEmpty()) return list;

        long now = System.currentTimeMillis();
        long millisInDay = 86400000L;

        List<RecentFile> todayFiles = new ArrayList<>();
        List<RecentFile> yesterdayFiles = new ArrayList<>();
        List<RecentFile> thisWeekFiles = new ArrayList<>();
        List<RecentFile> olderFiles = new ArrayList<>();

        for (RecentFile f : rawFiles) {
            long diff = now - f.getDateCreated();
            if (diff < millisInDay) {
                todayFiles.add(f);
            } else if (diff < 2 * millisInDay) {
                yesterdayFiles.add(f);
            } else if (diff < 7 * millisInDay) {
                thisWeekFiles.add(f);
            } else {
                olderFiles.add(f);
            }
        }

        if (!todayFiles.isEmpty()) {
            list.add(new DisplayItem("Today"));
            for (RecentFile f : todayFiles) list.add(new DisplayItem(f));
        }
        if (!yesterdayFiles.isEmpty()) {
            list.add(new DisplayItem("Yesterday"));
            for (RecentFile f : yesterdayFiles) list.add(new DisplayItem(f));
        }
        if (!thisWeekFiles.isEmpty()) {
            list.add(new DisplayItem("This Week"));
            for (RecentFile f : thisWeekFiles) list.add(new DisplayItem(f));
        }
        if (!olderFiles.isEmpty()) {
            list.add(new DisplayItem("Older"));
            for (RecentFile f : olderFiles) list.add(new DisplayItem(f));
        }

        return list;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_date_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_recent_file, parent, false);
            return new FileViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DisplayItem item = items.get(position);

        if (holder.getItemViewType() == TYPE_HEADER) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            headerHolder.tvHeaderTitle.setText(item.headerTitle);
        } else {
            FileViewHolder fileHolder = (FileViewHolder) holder;
            RecentFile file = item.file;

            int tintColor;
            int bgColor;
            String op = file.getOperation();

            if (op == null) {
                tintColor = fileHolder.itemView.getContext().getResources().getColor(R.color.mergeColor);
                bgColor = 0x1AE53935;
            } else {
                switch (op) {
                    case "MERGE":
                        tintColor = fileHolder.itemView.getContext().getResources().getColor(R.color.mergeColor);
                        bgColor = 0x1AE53935;
                        break;
                    case "SPLIT":
                        tintColor = fileHolder.itemView.getContext().getResources().getColor(R.color.splitColor);
                        bgColor = 0x1A1976D2;
                        break;
                    case "COMPRESS":
                        tintColor = fileHolder.itemView.getContext().getResources().getColor(R.color.compressColor);
                        bgColor = 0x1A388E3C;
                        break;
                    case "IMAGE_TO_PDF":
                        tintColor = fileHolder.itemView.getContext().getResources().getColor(R.color.imageToPdfColor);
                        bgColor = 0x1AF57C00;
                        break;
                    case "LOCK":
                    case "UNLOCK":
                        tintColor = fileHolder.itemView.getContext().getResources().getColor(R.color.viewerGradientEnd);
                        bgColor = 0x1A7B1FA2;
                        break;
                    case "WATERMARK":
                        tintColor = fileHolder.itemView.getContext().getResources().getColor(R.color.colorSecondary);
                        bgColor = 0x1AE91E63;
                        break;
                    default:
                        tintColor = fileHolder.itemView.getContext().getResources().getColor(R.color.mergeColor);
                        bgColor = 0x1AE53935;
                        break;
                }
            }

            fileHolder.ivFileIcon.setImageResource(file.getOperationIconRes());
            fileHolder.ivFileIcon.setImageTintList(android.content.res.ColorStateList.valueOf(tintColor));
            if (fileHolder.ivFileIconContainer != null) {
                fileHolder.ivFileIconContainer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgColor));
            }

            fileHolder.tvFileName.setText(file.getFileName());
            fileHolder.tvFileSize.setText(file.getFormattedSize());
            fileHolder.tvFileDate.setText(FileUtil.formatDate(file.getDateCreated()));

            if (file.getOperation() != null) {
                fileHolder.tvOperationBadge.setText(file.getOperation());
                fileHolder.tvOperationBadge.setVisibility(View.VISIBLE);
            } else {
                fileHolder.tvOperationBadge.setVisibility(View.GONE);
            }

            fileHolder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onOpen(file);
            });

            fileHolder.btnMoreOptions.setOnClickListener(v -> showBottomSheetOptions(fileHolder.itemView.getContext(), file));
        }
    }

    private void showBottomSheetOptions(Context context, RecentFile file) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.layout_file_options_sheet, null);

        TextView tvTitle = view.findViewById(R.id.tv_sheet_title);
        tvTitle.setText(file.getFileName());

        view.findViewById(R.id.layout_open).setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) listener.onOpen(file);
        });
        view.findViewById(R.id.layout_share).setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) listener.onShare(file);
        });
        View layoutEditPages = view.findViewById(R.id.layout_edit_pages);
        if (file.getFileName().toLowerCase().endsWith(".pdf")) {
            layoutEditPages.setVisibility(View.VISIBLE);
            layoutEditPages.setOnClickListener(v -> {
                dialog.dismiss();
                if (listener != null) listener.onEditPages(file);
            });
        } else {
            layoutEditPages.setVisibility(View.GONE);
        }

        view.findViewById(R.id.layout_rename).setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) listener.onRename(file);
        });
        view.findViewById(R.id.layout_locate).setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) listener.onLocate(file);
        });
        view.findViewById(R.id.layout_delete).setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) listener.onDelete(file);
        });

        dialog.setContentView(view);
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeaderTitle;
        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeaderTitle = itemView.findViewById(R.id.tv_header_title);
        }
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFileIcon, btnMoreOptions;
        View ivFileIconContainer;
        TextView tvFileName, tvFileSize, tvFileDate, tvOperationBadge;

        FileViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFileIcon          = itemView.findViewById(R.id.iv_file_icon);
            ivFileIconContainer = itemView.findViewById(R.id.iv_file_icon_container);
            tvFileName          = itemView.findViewById(R.id.tv_file_name);
            tvFileSize          = itemView.findViewById(R.id.tv_file_size);
            tvFileDate          = itemView.findViewById(R.id.tv_file_date);
            tvOperationBadge    = itemView.findViewById(R.id.tv_operation_badge);
            btnMoreOptions      = itemView.findViewById(R.id.btn_more_options);
        }
    }
}
