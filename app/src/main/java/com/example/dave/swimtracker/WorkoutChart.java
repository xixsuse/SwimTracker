package com.example.dave.swimtracker;

import android.arch.persistence.room.Embedded;
import android.content.Context;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.example.dave.swimtracker.database.AppDao;
import com.example.dave.swimtracker.database.AppDatabase;
import com.example.dave.swimtracker.database.entities.dbLap;
import com.example.dave.swimtracker.database.entities.dbSet;
import com.example.dave.swimtracker.database.entities.dbWorkout;
import com.example.dave.swimtracker.viewmodel.WorkoutViewModel;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkoutChart extends LineChart {

    private static float lineWidth = 4f;
    public Map<Stroke, Integer> mColors = setDefaultColors();
    private List<WorkoutViewModel.WorkoutRow> mData;
    private LineData lineData;
    private int distance = 0;
    private Map<Stroke, Integer> strokeDistances;

    public WorkoutChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public WorkoutChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WorkoutChart(Context context) {
        super(context);
    }

    public void setData(List<WorkoutViewModel.WorkoutRow> data) {
        this.mData = data;
        this.lineData = getDefaultLineData(mData);
        this.setDefaultLegend();
        this.setDefaultXAxis();
        this.getDescription().setEnabled(false);
        this.setNoDataText("Add some laps!");
        super.setData(this.lineData);
    }

    public List<WorkoutViewModel.WorkoutRow> getRowData() {
        return this.mData;
    }

    public int getDistance() {
        return distance;
    }

    public Map<Stroke, Integer> getStrokeDistances() {
        return strokeDistances;
    }

    @Override
    public void notifyDataSetChanged() {
        if (this.mData != null)
            this.lineData = getDefaultLineData(this.mData);
        super.notifyDataSetChanged();
    }

    public Map<Stroke, Integer> setDefaultColors() {
        Map<Stroke, Integer> colors = new HashMap<Stroke, Integer>();
        colors.put(Stroke.FREESTYLE, ContextCompat.getColor(this.getContext(), R.color.Freestyle));
        colors.put(Stroke.BACKSTROKE, ContextCompat.getColor(this.getContext(), R.color.Backstroke));
        colors.put(Stroke.BREASTSTROKE, ContextCompat.getColor(this.getContext(), R.color.Breaststroke));
        colors.put(Stroke.BUTTERFLY, ContextCompat.getColor(this.getContext(), R.color.Butterfly));
        colors.put(Stroke.CHOICE, ContextCompat.getColor(this.getContext(), R.color.Choice));
        return colors;
    }

    public LineData getDefaultLineData(List<WorkoutViewModel.WorkoutRow> data) {
        List<ILineDataSet> dataSets = new ArrayList<>();

        int startDistance = 0;
        float startSeconds = 0;

        List<dbLap> laps = new ArrayList<>();
        long currentSetId;
        int currentSetReps;
        for (int i = 0; i < data.size(); i++) {
            List<dbLap> setLaps = new ArrayList<>();
            currentSetId = data.get(i).setId;
            currentSetReps = data.get(i).rounds;
            while (data.get(i).setId == currentSetId) {
                setLaps.add(data.get(i).lap);
                i++;
                if (i == data.size())
                    break;
            }
            i--;
            for (int j = 0; j < currentSetReps; j++)
                laps.addAll(setLaps);
        }

        this.strokeDistances = new HashMap<>();

        for (dbLap lap : laps) {
            List<Entry> entries = new ArrayList<>();
            Stroke lapStroke = lap.getStroke();

            if (!this.strokeDistances.containsKey(lapStroke))
                this.strokeDistances.put(lapStroke, 0);
            int strokeDistance = this.strokeDistances.get(lapStroke);
            this.strokeDistances.put(lapStroke, strokeDistance + lap.getDistance());

            float endSeconds = startSeconds + lap.getSeconds();
            int endDistance = startDistance + lap.getDistance();
            entries.add(new Entry(startSeconds, startDistance));
            entries.add(new Entry(endSeconds, endDistance));
            startSeconds = endSeconds;
            startDistance = endDistance;

            LineDataSet dataSet = new LineDataSet(new ArrayList<>(entries), null);
            dataSet.setColor(mColors.get(lapStroke));
            dataSet.setLineWidth(lineWidth);
            dataSet.setDrawCircles(false);
            dataSet.setDrawValues(false);
            dataSets.add(dataSet);
        }

        LineData lineData = new LineData(dataSets);
        this.distance = startDistance;
        return lineData;
    }

    public void setDefaultLegend() {
        List<LegendEntry> legend = new ArrayList<>();
        Legend currentLegend = this.getLegend();
        Legend.LegendForm legendForm = currentLegend.getForm();
        DashPathEffect legendDash = currentLegend.getFormLineDashEffect();
        float legendFormSize = currentLegend.getFormSize();
        float legendFormLineWidth = currentLegend.getFormLineWidth();

        Stroke[] strokeOrder = new Stroke[] {Stroke.FREESTYLE, Stroke.BACKSTROKE, Stroke.BREASTSTROKE, Stroke.BUTTERFLY, Stroke.CHOICE};
        for (Stroke stroke : strokeOrder) {
            legend.add(new LegendEntry(stroke.stroke(), legendForm, legendFormSize, legendFormLineWidth, legendDash, mColors.get(stroke)));
        }
        currentLegend.setCustom(legend);
    }

    public void setDefaultXAxis() {
        XAxis xAxis = this.getXAxis();
        xAxis.setAxisMinimum(0);
        xAxis.setLabelCount(5);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                String time = String.format("%2d:%02d", (int) Math.floor(value / 60), (int) (value % 60));
                return time;
            }
        });
    }
}
