package com.dsfstudios.apps.lappr;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dsfstudios.apps.lappr.viewmodel.WorkoutViewModel;

public class WorkoutView extends FrameLayout {

    //private WorkoutViewModel.WorkoutData workoutData;

    public WorkoutView(Context context) {
        super(context);
    }

    public WorkoutView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WorkoutView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
/*
    public WorkoutView(Context context, WorkoutViewModel.WorkoutData workoutData) {
        super(context);
        this.workoutData = workoutData;
        initView();
    }

    public void setData(WorkoutViewModel.WorkoutData workoutData) {
        this.workoutData = workoutData;
        initView();
    }

    private void initView() {
        View newWorkout = inflate(getContext(), R.layout.fragment_dashboard_workouts, null);

        TextView workoutHeader = (TextView) newWorkout.findViewById(R.id.workoutHeader);
        String message;

        if (workoutData.isCompleted()) {
            workoutHeader.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_swimming_man, 0, 0, 0);
            message = "   Workout swam on ";
        } else {
            workoutHeader.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_schedule_24px, 0, 0, 0);
            message = "   Workout scheduled for ";
        }
        message +=  workoutData.getDate("MMMM dd, yyyy");
        workoutHeader.setText(message);

        WorkoutChart chart = newWorkout.findViewById(R.id.chart);
        chart.setData(workoutData);
        chart.plotWorkout();
        chart.getAxisLeft().setAxisMinimum(0);
        chart.getLegend().setEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(false);
        chart.invalidate();

        addView(newWorkout);
    }
    */
}
