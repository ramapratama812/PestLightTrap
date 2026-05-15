package com.pebelti.pestlighttrap;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class OperationalStatusFragment extends Fragment {

    private ProgressBar progressNightActivity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_operational_status, container, false);

        progressNightActivity = view.findViewById(R.id.progressNightActivity);

        // Animasi ProgressBar saat fragment dibuka (Misal: sudah terisi 60%)
        ObjectAnimator animation = ObjectAnimator.ofInt(progressNightActivity, "progress", 0, 60);
        animation.setDuration(1500); // 1.5 detik
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();

        return view;
    }
}
