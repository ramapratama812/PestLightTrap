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
import android.widget.LinearLayout;
import android.app.DatePickerDialog;
import android.widget.Toast;

public class HistoryDetailFragment extends Fragment {

    private TextView tvHistoryDateBadge;
    private TextView tvHistoryImageDate;
    private ImageView btnBack;
    private LinearLayout btnCalendar;
    private Calendar selectedCalendar = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history_detail, container, false);

        // Bind views
        tvHistoryDateBadge = view.findViewById(R.id.tvHistoryDateBadge);
        tvHistoryImageDate = view.findViewById(R.id.tvHistoryImageDate);
        btnBack = view.findViewById(R.id.btnBack);
        btnCalendar = view.findViewById(R.id.btnCalendar);

        // Set dynamic date (Default to Today)
        setupHistoryDate();

        // Calendar Click Listener
        btnCalendar.setOnClickListener(v -> showDatePicker());

        // Handle back button click
        btnBack.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeDashboardFragment())
                    .commit();
        });

        return view;
    }

    private void setupHistoryDate() {
        // Format for badge (e.g., "20 MEI")
        java.text.SimpleDateFormat badgeFormat = new java.text.SimpleDateFormat("d MMM", new Locale("id", "ID"));
        String badgeStr = badgeFormat.format(selectedCalendar.getTime()).toUpperCase();
        if (tvHistoryDateBadge != null) {
            tvHistoryDateBadge.setText(badgeStr);
        }

        // Format for image header (e.g., "• KAMIS, 20 MEI")
        java.text.SimpleDateFormat imageFormat = new java.text.SimpleDateFormat("EEEE, d MMM", new Locale("id", "ID"));
        String imageDateStr = "• " + imageFormat.format(selectedCalendar.getTime()).toUpperCase();
        if (tvHistoryImageDate != null) {
            tvHistoryImageDate.setText(imageDateStr);
        }
    }

    private void showDatePicker() {
        if (getContext() == null) return;

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (v, year, month, dayOfMonth) -> {
                    selectedCalendar.set(Calendar.YEAR, year);
                    selectedCalendar.set(Calendar.MONTH, month);
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    setupHistoryDate();

                    // TODO: Panggil fungsi untuk me-load data detail riwayat dari Firebase berdasarkan tanggal terpilih
                    Toast.makeText(getContext(), "Menampilkan riwayat untuk: " + tvHistoryDateBadge.getText().toString(), Toast.LENGTH_SHORT).show();
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
}
