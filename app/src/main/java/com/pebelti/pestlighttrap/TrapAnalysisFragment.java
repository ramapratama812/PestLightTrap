package com.pebelti.pestlighttrap;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import java.util.ArrayList;

public class TrapAnalysisFragment extends Fragment {

    private BarChart barChart;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trap_analysis, container, false);

        barChart = view.findViewById(R.id.barChartTrap);
        setupBarChart();

        return view;
    }

    private void setupBarChart() {
        // Data Dummy Tangkapan Hama
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, 4.5f)); // Senin
        entries.add(new BarEntry(1f, 3.2f)); // Selasa
        entries.add(new BarEntry(2f, 2.9f)); // Rabu
        entries.add(new BarEntry(3f, 4.1f)); // Kamis
        entries.add(new BarEntry(4f, 3.4f)); // Jumat
        entries.add(new BarEntry(5f, 2.5f)); // Sabtu
        entries.add(new BarEntry(6f, 2.0f)); // Minggu

        BarDataSet dataSet = new BarDataSet(entries, "Hasil Tangkapan (kg)");
        dataSet.setColor(Color.parseColor("#3F51B5")); // Warna batang
        dataSet.setValueTextColor(Color.parseColor("#191970"));
        dataSet.setValueTextSize(14f);

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);

        // Konfigurasi Sumbu X
        String[] days = new String[]{"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"};
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.parseColor("#191970"));
        xAxis.setTextSize(14f);

        // Konfigurasi Sumbu Y
        barChart.getAxisLeft().setDrawGridLines(false);
        barChart.getAxisLeft().setTextColor(Color.parseColor("#191970"));
        barChart.getAxisLeft().setTextSize(14f);
        barChart.getAxisRight().setEnabled(false); // Nonaktifkan sumbu Y kanan

        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate();
    }
}
