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
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;

public class BatteryMonitoringFragment extends Fragment {

    private PieChart pieChart;
    private BarChart barChartHistory;
    private BarChart barChartPest;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_battery_monitoring, container, false);

        pieChart = view.findViewById(R.id.pieChartBattery);
        barChartHistory = view.findViewById(R.id.barChartHistory);
        barChartPest = view.findViewById(R.id.barChartPest);

        setupPieChart();
        setupBarChart();
        setupBarChartPest();

        // Tab Switching Logic
        View tabTrap = view.findViewById(R.id.tabTrapAnalytics);
        View tabBattery = view.findViewById(R.id.tabBatteryAnalytics);
        View layoutTrapContent = view.findViewById(R.id.layoutTrapAnalyticsContent);
        View layoutBatteryContent = view.findViewById(R.id.layoutBatteryAnalyticsContent);

        ImageView ivTabTrap = view.findViewById(R.id.ivTabTrapAnal);
        TextView tvTabTrap = view.findViewById(R.id.tvTabTrapAnal);
        ImageView ivTabBatt = view.findViewById(R.id.ivTabBattAnal);
        TextView tvTabBatt = view.findViewById(R.id.tvTabBattAnal);

        TextView tvTitle = view.findViewById(R.id.tvAnalyticsTitle);

        tabTrap.setOnClickListener(v -> {
            tvTitle.setText("Analisis Trap");
            layoutTrapContent.setVisibility(View.VISIBLE);
            layoutBatteryContent.setVisibility(View.GONE);
            tabTrap.setBackgroundResource(R.drawable.bg_tab_active);
            tabBattery.setBackground(null);
            ivTabTrap.setColorFilter(Color.WHITE);
            tvTabTrap.setTextColor(Color.WHITE);
            ivTabBatt.setColorFilter(Color.parseColor("#191970"));
            tvTabBatt.setTextColor(Color.parseColor("#191970"));
        });

        tabBattery.setOnClickListener(v -> {
            tvTitle.setText("Analisis Baterai");
            layoutTrapContent.setVisibility(View.GONE);
            layoutBatteryContent.setVisibility(View.VISIBLE);
            tabBattery.setBackgroundResource(R.drawable.bg_tab_active);
            tabTrap.setBackground(null);
            ivTabBatt.setColorFilter(Color.WHITE);
            tvTabBatt.setTextColor(Color.WHITE);
            ivTabTrap.setColorFilter(Color.parseColor("#191970"));
            tvTabTrap.setTextColor(Color.parseColor("#191970"));
        });

        View btnBack = view.findViewById(R.id.btnBackAnalytics);
        btnBack.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeDashboardFragment())
                    .commit();
        });

        return view;
    }

    private void setupBarChartPest() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, 100f));
        entries.add(new BarEntry(1, 80f));
        entries.add(new BarEntry(2, 65f));
        entries.add(new BarEntry(3, 90f));
        entries.add(new BarEntry(4, 75f));
        entries.add(new BarEntry(5, 55f));
        entries.add(new BarEntry(6, 45f));

        BarDataSet dataSet = new BarDataSet(entries, "Weekly Catch");
        dataSet.setColor(Color.parseColor("#5C6BC0"));
        dataSet.setDrawValues(true);
        dataSet.setValueTextColor(Color.parseColor("#191970"));
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + "%";
            }
        });

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.5f);
        barChartPest.setData(data);

        // Styling X-Axis
        XAxis xAxis = barChartPest.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(Color.parseColor("#7986CB"));
        xAxis.setTextSize(9f);
        String[] days = new String[]{"4.5kg\nMON", "3.2kg\nTUE", "2.9kg\nWED", "4.1kg\nTHU", "3.4kg\nFRI", "2.5kg\nSAT", "2.0kg\nSUN"};
        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));
        xAxis.setLabelCount(7);
        xAxis.setYOffset(10f);

        // Styling Y-Axis
        YAxis leftAxis = barChartPest.getAxisLeft();
        leftAxis.setDrawAxisLine(false);
        leftAxis.setGridColor(Color.parseColor("#E8EAF6"));
        leftAxis.setGridLineWidth(1f);
        leftAxis.enableGridDashedLine(10f, 10f, 0f); // Dashed lines like in design
        leftAxis.setTextColor(Color.parseColor("#9FA8DA"));
        leftAxis.setTextSize(9f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(120f);
        leftAxis.setLabelCount(5);

        barChartPest.getAxisRight().setEnabled(false);
        barChartPest.getDescription().setEnabled(false);
        barChartPest.getLegend().setEnabled(false);
        barChartPest.setDrawGridBackground(false);
        barChartPest.setExtraBottomOffset(20f);
        barChartPest.animateY(1000);
        barChartPest.invalidate();
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
        dataSet.setColors(Color.parseColor("#5C6BC0"), Color.parseColor("#5C6BC0"), Color.parseColor("#5C6BC0"), Color.parseColor("#5C6BC0"));
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
