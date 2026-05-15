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
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;

public class TrapAnalysisFragment extends Fragment {

    private BarChart barChart;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trap_analysis, container, false);

        View btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeDashboardFragment())
                    .commit();
        });

        // Tab Switching Logic
        View tabTrap = view.findViewById(R.id.tabTrap);
        View tabBattery = view.findViewById(R.id.tabBattery);
        View layoutTrapContent = view.findViewById(R.id.layoutTrapContent);
        View layoutBatteryContent = view.findViewById(R.id.layoutBatteryContent);
        
        ImageView ivTabTrap = view.findViewById(R.id.ivTabTrap);
        TextView tvTabTrap = view.findViewById(R.id.tvTabTrap);
        ImageView ivTabBattery = view.findViewById(R.id.ivTabBattery);
        TextView tvTabBattery = view.findViewById(R.id.tvTabBattery);

        tabTrap.setOnClickListener(v -> {
            layoutTrapContent.setVisibility(View.VISIBLE);
            layoutBatteryContent.setVisibility(View.GONE);
            
            tabTrap.setBackgroundResource(R.drawable.bg_tab_active);
            tabBattery.setBackground(null);
            
            ivTabTrap.setColorFilter(Color.WHITE);
            tvTabTrap.setTextColor(Color.WHITE);
            ivTabBattery.setColorFilter(Color.parseColor("#7986CB"));
            tvTabBattery.setTextColor(Color.parseColor("#7986CB"));
        });

        tabBattery.setOnClickListener(v -> {
            layoutTrapContent.setVisibility(View.GONE);
            layoutBatteryContent.setVisibility(View.VISIBLE);
            
            tabBattery.setBackgroundResource(R.drawable.bg_tab_active);
            tabTrap.setBackground(null);
            
            ivTabBattery.setColorFilter(Color.WHITE);
            tvTabBattery.setTextColor(Color.WHITE);
            ivTabTrap.setColorFilter(Color.parseColor("#7986CB"));
            tvTabTrap.setTextColor(Color.parseColor("#7986CB"));
        });

        return view;
    }
}
