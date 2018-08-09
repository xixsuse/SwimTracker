package com.example.dave.swimtracker.ui.adapters;

import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.dave.swimtracker.R;
import com.example.dave.swimtracker.WorkoutChart;
import com.example.dave.swimtracker.WorkoutView;
import com.example.dave.swimtracker.database.entities.dbWorkout;
import com.example.dave.swimtracker.ui.Dashboard;
import com.example.dave.swimtracker.viewmodel.WorkoutViewModel;

import java.util.Date;
import java.util.List;

import static android.view.View.inflate;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.ViewHolder> {

    private Date mDate;
    private boolean mComplete;
    private final List<Dashboard.WorkoutDataSet> mValues;
    private final WorkoutAdapterListener mListener;
    private String mTAG;
    private WorkoutViewModel model;

    public WorkoutAdapter(Date date, boolean complete, List<Dashboard.WorkoutDataSet> items, WorkoutAdapterListener listener, String TAG, WorkoutViewModel model) {
        mDate = date;
        mComplete = complete;
        mValues = items;
        mListener = listener;
        mTAG = TAG;
        this.model = model;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_dashboard_workouts, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        String message;
        if (holder.mItem.workout.isCompleted()) {
            holder.mHeader.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_swimming_man, 0, 0, 0);
            message = "   Workout swam on ";
        } else {
            holder.mHeader.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_schedule_24px, 0, 0, 0);
            message = "   Workout scheduled for ";
        }
        message +=  holder.mItem.workout.getDate("MMMM dd, yyyy");
        holder.mHeader.setText(message);
        holder.mChart.setData(holder.mItem.rows);
        holder.mChart.setTouchEnabled(false);
        holder.mChart.invalidate();


        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTAG.equals("Clone Workout")) {
                    model.cloneWorkout(mDate, mComplete, holder.mItem.workout.getId());
                    mListener.viewWorkout(true);
                } else if (mTAG.equals("View Workout")) {
                    model.selectWorkout(holder.mItem.workout.getId());
                    mListener.viewWorkout(false);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public Dashboard.WorkoutDataSet mItem;
        public TextView mHeader;
        public WorkoutChart mChart;
        public LinearLayout mActions;

        public ViewHolder(View view) {
            super(view);
            int height = Resources.getSystem().getDisplayMetrics().heightPixels;
            mView = view;
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height / 3);
            cardParams.setMargins(0, 16, 0, 0);
            mView.setLayoutParams(cardParams);
            mHeader = view.findViewById(R.id.workoutHeader);
            mChart = view.findViewById(R.id.chart);

            // adapter is used for clone workout and workout list where edit actions are not needed
            mActions = view.findViewById(R.id.workoutActions);
            mActions.setVisibility(View.GONE);
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }
}
