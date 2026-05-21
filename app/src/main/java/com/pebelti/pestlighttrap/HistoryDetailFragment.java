package com.pebelti.pestlighttrap;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Calendar;
import java.util.Locale;

public class HistoryDetailFragment extends Fragment {

    private TextView tvHistoryDateBadge;
    private TextView tvHistoryImageDate;
    private ImageView btnBack;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history_detail, container, false);

        // Bind views
        tvHistoryDateBadge = view.findViewById(R.id.tvHistoryDateBadge);
        tvHistoryImageDate = view.findViewById(R.id.tvHistoryImageDate);
        btnBack = view.findViewById(R.id.btnBack);

        // Set dynamic date (Yesterday)
        setupHistoryDate();

        // Handle back button click
        btnBack.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeDashboardFragment())
                    .commit();
        });

        return view;
    }

    private void setupHistoryDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);

        // Format for badge (e.g., "20 MEI")
        java.text.SimpleDateFormat badgeFormat = new java.text.SimpleDateFormat("d MMM", new Locale("id", "ID"));
        String badgeStr = badgeFormat.format(calendar.getTime()).toUpperCase();
        if (tvHistoryDateBadge != null) {
            tvHistoryDateBadge.setText(badgeStr);
        }

        // Format for image header (e.g., "• KAMIS, 20 MEI")
        java.text.SimpleDateFormat imageFormat = new java.text.SimpleDateFormat("EEEE, d MMM", new Locale("id", "ID"));
        String imageDateStr = "• " + imageFormat.format(calendar.getTime()).toUpperCase();
        if (tvHistoryImageDate != null) {
            tvHistoryImageDate.setText(imageDateStr);
        }
    }
}
