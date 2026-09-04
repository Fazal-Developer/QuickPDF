package com.hhhdeveloper.swiftpdf.ui.cv;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.hhhdeveloper.swiftpdf.R;

/**
 * Consolidated CV Maker Entry Point.
 * Forwards legacy references to CvLandingFragment.
 */
public class CvMakerFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cv_landing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Automatically redirect to primary landing screen if navigated via legacy id
        if (getNavController() != null) {
            getNavController().navigate(R.id.nav_cv_landing);
        }
    }

    private androidx.navigation.NavController getNavController() {
        try {
            return Navigation.findNavController(requireView());
        } catch (Exception e) {
            return null;
        }
    }
}
