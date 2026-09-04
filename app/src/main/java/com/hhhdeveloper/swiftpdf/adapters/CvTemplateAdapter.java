package com.hhhdeveloper.swiftpdf.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hhhdeveloper.swiftpdf.databinding.ItemCvTemplateBinding;
import com.hhhdeveloper.swiftpdf.models.cv.CvModel;
import com.hhhdeveloper.swiftpdf.models.cv.CvTemplate;
import com.hhhdeveloper.swiftpdf.models.cv.PersonalInfo;

import java.util.List;

public class CvTemplateAdapter extends RecyclerView.Adapter<CvTemplateAdapter.ViewHolder> {

    public interface OnTemplateClickListener {
        void onTemplateSelected(CvTemplate template);
    }

    private List<CvTemplate> templateList;
    private final OnTemplateClickListener listener;
    private final CvModel sampleCvModel;

    public CvTemplateAdapter(List<CvTemplate> templateList, OnTemplateClickListener listener) {
        this.templateList = templateList;
        this.listener = listener;

        // Sample model to render miniature A4 preview on each card
        sampleCvModel = new CvModel();
        PersonalInfo sampleInfo = new PersonalInfo("Hamza Fazal", "Software Engineer & Mobile Developer", "hamzafazal@deesu.pk", "+92 300 1234567", "Islamabad, Pakistan", "linkedin.com/in/hamzafazal", "hamzafazal.deesu.org");
        sampleCvModel.setPersonalInfo(sampleInfo);
        sampleCvModel.setSummary("Software Engineering student and multi-disciplinary developer passionate about building fast Android apps.");
        sampleCvModel.getSkillsList().add("Android");
        sampleCvModel.getSkillsList().add("Java");
        sampleCvModel.getSkillsList().add("Python");
    }

    public void updateTemplates(List<CvTemplate> newList) {
        this.templateList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCvTemplateBinding binding = ItemCvTemplateBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CvTemplate template = templateList.get(position);
        holder.bind(template);
    }

    @Override
    public int getItemCount() {
        return templateList != null ? templateList.size() : 0;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCvTemplateBinding binding;

        ViewHolder(ItemCvTemplateBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CvTemplate template) {
            binding.tvTemplateName.setText(template.getName());
            binding.tvTemplateDesc.setText(template.getDescription());

            // Badges
            binding.tvAtsBadge.setVisibility(template.isAtsFriendly() ? View.VISIBLE : View.GONE);
            binding.tvPhotoBadge.setVisibility(template.hasPhotoOption() ? View.VISIBLE : View.GONE);

            // Bind sample model with template ID to render real mini A4 preview!
            CvModel previewModel = new CvModel();
            previewModel.setSelectedTemplateId(template.getId());
            previewModel.setPersonalInfo(sampleCvModel.getPersonalInfo());
            previewModel.setSummary(sampleCvModel.getSummary());
            previewModel.setSkillsList(sampleCvModel.getSkillsList());
            binding.miniPreviewCanvas.setCvModel(previewModel);

            View.OnClickListener clickListener = v -> {
                if (listener != null) listener.onTemplateSelected(template);
            };

            binding.btnUseTemplate.setOnClickListener(clickListener);
            binding.getRoot().setOnClickListener(clickListener);
        }
    }
}
