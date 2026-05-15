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
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import java.util.ArrayList;

public class BatteryMonitoringFragment extends Fragment {

    private PieChart pieChart;
    private BarChart barChartHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_battery_monitoring, container, false);

        pieChart = view.findViewById(R.id.pieChartBattery);
        barChartHistory = view.findViewById(R.id.barChartHistory);

        setupPieChart();
        setupBarChart();

        return view;
    }

    private void setupPieChart() {
        ArrayList<PieEntry> entries = new ArrayList<>();
        // Membagi lingkaran menjadi beberapa bagian (misal 4 slice untuk desain putus-putus)
        entries.add(new PieEntry(25f, ""));
        entries.add(new PieEntry(25f, ""));
        entries.add(new PieEntry(25f, ""));
        entries.add(new PieEntry(25f, ""));

        PieDataSet dataSet = new PieDataSet(entries, "");
        // Warna hijau terang dan lembut
        dataSet.setColors(Color.parseColor("#4CAF50"), Color.parseColor("#4CAF50"), Color.parseColor("#4CAF50"), Color.parseColor("#4CAF50"));
        dataSet.setSliceSpace(8f); // Memberi jarak antar bagian
        dataSet.setDrawValues(false);

        PieData data = new PieData(dataSet);
        
        pieChart.setData(data);
        pieChart.setHoleRadius(75f); // Lubang di tengah
        pieChart.setTransparentCircleRadius(80f);
        pieChart.setTransparentCircleColor(Color.parseColor("#E8F5E9")); // Warna lingkaran transparan di dalam
        pieChart.setTransparentCircleAlpha(110);
        pieChart.setCenterText("70%\nCharged");
        pieChart.setCenterTextSize(28f);
        pieChart.setCenterTextColor(Color.parseColor("#191970"));
        pieChart.setDrawEntryLabels(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.setTouchEnabled(false);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    private void setupBarChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, 60f));
        entries.add(new BarEntry(1, 75f));
        entries.add(new BarEntry(2, 90f));
        entries.add(new BarEntry(3, 85f));
        entries.add(new BarEntry(4, 70f));
        entries.add(new BarEntry(5, 95f));
        entries.add(new BarEntry(6, 80f));

        BarDataSet dataSet = new BarDataSet(entries, "History");
        dataSet.setColor(Color.parseColor("#7986CB"));
        dataSet.setDrawValues(false);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);
        barChartHistory.setData(data);

        // Styling X-Axis
        XAxis xAxis = barChartHistory.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(Color.parseColor("#7986CB"));
        xAxis.setTextSize(10f);
        String[] days = new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));
        xAxis.setLabelCount(7);

        // Styling Y-Axis Kiri
        YAxis leftAxis = barChartHistory.getAxisLeft();
        leftAxis.setDrawAxisLine(false);
        leftAxis.setGridColor(Color.parseColor("#E8EAF6"));
        leftAxis.setTextColor(Color.parseColor("#7986CB"));
        leftAxis.setTextSize(10f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setLabelCount(3, true); // Hanya tampilkan 0, 50, 100
        leftAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + "%";
            }
        });

        // Sembunyikan Y-Axis Kanan
        barChartHistory.getAxisRight().setEnabled(false);

        barChartHistory.getDescription().setEnabled(false);
        barChartHistory.getLegend().setEnabled(false);
        barChartHistory.setTouchEnabled(false);
        barChartHistory.animateY(1000);
        barChartHistory.invalidate();
    }
}
