package com.example.dave.swimtracker.ui;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.icu.lang.UProperty;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.dave.swimtracker.R;
import com.example.dave.swimtracker.Stroke;
import com.example.dave.swimtracker.WorkoutChart;
import com.example.dave.swimtracker.WorkoutView;
import com.example.dave.swimtracker.database.AppDatabase;
import com.example.dave.swimtracker.database.entities.dbWorkout;
import com.example.dave.swimtracker.viewmodel.WorkoutViewModel;
import com.example.dave.swimtracker.viewmodel.WorkoutViewModelFactory;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.view.View.inflate;

public class Dashboard extends Fragment {

    private static AppDatabase mDb;
    private static long mSwimmerId;
    private DashboardListener mListener;
    private WorkoutViewModel model;
    private MutableLiveData<Map<Integer, List<WorkoutDataSet>>> workoutData;

    // keys for workoutData map
    private static final int UPCOMING_WORKOUTS = 0;
    private static final int OVERDUE_WORKOUTS = 1;
    private static final int RECENT_WORKOUTS = 2;

    // refresh quantities
    private static final int UPCOMING_NUM = 3;
    private static final int OVERDUE_NUM = 3;
    private static final int RECENT_NUM = 5;

    // load/refresh commands
    private static final int INIT = 0;
    private static final int LOAD_OVERDUE = 1;
    private static final int LOAD_UPCOMING = 2;
    private static final int LOAD_RECENT = 3;

    public static class WorkoutDataSet {
        public dbWorkout workout;
        public List<WorkoutViewModel.WorkoutRow> rows;
    }

    public Dashboard() {
        // Required empty public constructor
    }

    public static Dashboard newInstance() {
        Dashboard fragment = new Dashboard();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDb = AppDatabase.getAppDatabase(this.getContext());

        WorkoutViewModelFactory factory = new WorkoutViewModelFactory(AppDatabase.getAppDatabase(getContext()));
        model = ViewModelProviders.of(this.getActivity(), factory).get(WorkoutViewModel.class);
        mSwimmerId = model.getSwimmerId();

        model.getDashboard().observe(this, new Observer<Map<Integer, List<WorkoutViewModel.WorkoutDataSet>>>() {
            @Override
            public void onChanged(@Nullable Map<Integer, List<WorkoutViewModel.WorkoutDataSet>> dashboardData) {
                printWorkouts(dashboardData);
            }
        });

        model.refreshWorkouts();
    }

    @Override
    public void onResume() {
        super.onResume();
        model.refreshWorkouts();
    }

    public void createWorkout() {
        final Calendar cal = Calendar.getInstance();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.dialog_create_workout, null);

        final LinearLayout dialogContainer = dialogLayout.findViewById(R.id.dialog);
        final Switch completeSwitch = (Switch) dialogContainer.findViewById(R.id.workoutComplete);
        completeSwitch.setChecked(false);

        DatePicker datePicker = dialogLayout.findViewById(R.id.dialogDatePicker);
        datePicker.init(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker datePicker, int i, int i1, int i2) {
                cal.set(Calendar.YEAR, i);
                cal.set(Calendar.MONTH, i1);
                cal.set(Calendar.DAY_OF_MONTH, i2);
            }
        });
        builder.setView(dialogLayout);
        builder.setMessage(R.string.createDialogMessage).setTitle(R.string.createDialogTitle);
        builder.setPositiveButton(R.string.createDialogNew, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mListener.createBlankWorkout(cal.getTime(), completeSwitch.isChecked());
            }
        });
        builder.setNegativeButton(R.string.createDialogClone, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mListener.selectWorkout(cal.getTime().getTime(), completeSwitch.isChecked(), "Clone Workout");
                dialogInterface.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void scheduleWorkout(final dbWorkout workout) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(workout.getDate());

        AlertDialog.Builder dateDialog = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.dialog_create_workout, null);

        final LinearLayout dialogContainer = dialogLayout.findViewById(R.id.dialog);

        final Switch completeSwitch = (Switch) dialogContainer.findViewById(R.id.workoutComplete);
        completeSwitch.setChecked(workout.isCompleted());

        DatePicker datePicker = dialogLayout.findViewById(R.id.dialogDatePicker);
        datePicker.init(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker datePicker, int i, int i1, int i2) {
                cal.set(Calendar.YEAR, i);
                cal.set(Calendar.MONTH, i1);
                cal.set(Calendar.DAY_OF_MONTH, i2);
            }
        });
        dateDialog.setView(dialogLayout);
        dateDialog.setTitle(R.string.workoutDateTitle);
        dateDialog.setPositiveButton(R.string.workoutDatePositive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                workout.setCompleted(completeSwitch.isChecked());
                workout.setDate(cal.getTime().getTime());
                //execUpdateWorkout(workoutData, workout);
                model.updateWorkout(workout);
            }
        });
        dateDialog.setNegativeButton(R.string.workoutDateNegative, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        AlertDialog dialogDate = dateDialog.create();
        dialogDate.show();
    }

    public void notesWorkout(final dbWorkout workout) {
        AlertDialog.Builder notesDialog = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getLayoutInflater();
        View notesLayout = inflater.inflate(R.layout.dialog_notes, null);

        LinearLayout dialogContainer = notesLayout.findViewById(R.id.dialog);

        final EditText notes = (EditText) dialogContainer.findViewById(R.id.notes);
        notes.setText(workout.getNotes());

        notesDialog.setTitle(R.string.workoutNotesTitle);

        notesDialog.setPositiveButton(R.string.workoutNotesPositive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                workout.setNotes(notes.getText().toString());
                //execUpdateWorkout(workoutData, workout);
                model.updateWorkout(workout);
            }
        });

        notesDialog.setNegativeButton(R.string.workoutNotesNegative, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        notesDialog.setView(notesLayout);
        AlertDialog dialogDate = notesDialog.create();
        dialogDate.show();
    }

    public void deleteWorkout(final dbWorkout workout) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.deleteWorkoutDialog);
        builder.setPositiveButton(R.string.deleteWorkoutPositive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //execDeleteWorkout(workoutData, workout);
                model.deleteWorkout(workout);
            }
        });
        builder.setNegativeButton(R.string.deleteWorkoutNegative, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void printWorkouts(Map<Integer, List<WorkoutViewModel.WorkoutDataSet>> workoutMap) {

        ViewGroup layout = (ViewGroup) getView().findViewById(R.id.workoutSection);
        layout.removeAllViews();

        int height = Resources.getSystem().getDisplayMetrics().heightPixels;

        int recentsAdded = 0;
        int totalDistance = 0;

        LinearLayout.LayoutParams breakParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        breakParams.setMargins(0, 16, 0, 0);

        // create TextView for Upcoming Workout and Recent Workouts
        TextView upcomingWorkout = new TextView(this.getContext());
        upcomingWorkout.setLayoutParams(breakParams);
        upcomingWorkout.setGravity(Gravity.CENTER);
        upcomingWorkout.setBackgroundResource(R.color.caldroid_holo_blue_light);
        upcomingWorkout.setText(R.string.titleUpcomingWorkouts);
        upcomingWorkout.setTextSize(24);

        TextView upcomingEmpty = new TextView(this.getContext());
        upcomingEmpty.setLayoutParams(breakParams);
        upcomingEmpty.setGravity(Gravity.CENTER);
        upcomingEmpty.setText(R.string.titleUpcomingEmpty);
        upcomingEmpty.setTextSize(20);

        TextView recentWorkouts = new TextView(this.getContext());
        recentWorkouts.setLayoutParams(breakParams);
        recentWorkouts.setGravity(Gravity.CENTER);
        recentWorkouts.setBackgroundResource(R.color.caldroid_holo_blue_light);
        recentWorkouts.setText(R.string.titleRecentWorkouts);
        recentWorkouts.setTextSize(24);

        TextView recentEmpty = new TextView(this.getContext());
        recentEmpty.setLayoutParams(breakParams);
        recentEmpty.setGravity(Gravity.CENTER);
        recentEmpty.setText(R.string.titleRecentEmpty);
        recentEmpty.setTextSize(20);

        TextView overdueWorkout = new TextView(this.getContext());
        overdueWorkout.setLayoutParams(breakParams);
        overdueWorkout.setGravity(Gravity.CENTER);
        overdueWorkout.setBackgroundResource(R.color.caldroid_holo_blue_light);
        overdueWorkout.setText(R.string.titleOverdueWorkouts);
        overdueWorkout.setTextSize(24);

        Map<Stroke, Integer> strokeDistances = new HashMap<>();
        int[] dailyDistances = new int[7];
        final Calendar current = Calendar.getInstance();
        current.set(Calendar.HOUR_OF_DAY, 0);
        current.set(Calendar.MILLISECOND, 0);
        current.set(Calendar.MINUTE, 0);
        current.set(Calendar.SECOND, 0);

        int sections[] = new int[] {OVERDUE_WORKOUTS, UPCOMING_WORKOUTS, RECENT_WORKOUTS};

        for (int i = 0; i < sections.length; i++) {
            List<WorkoutViewModel.WorkoutDataSet> workouts = workoutMap.get(sections[i]);

            final int sectionId = sections[i];

            // section headers
            if (sections[i] == UPCOMING_WORKOUTS && upcomingWorkout.getParent() == null) {
                layout.addView(upcomingWorkout);
                if (workouts.size() == 0) {
                    layout.addView(upcomingEmpty);
                    continue;
                }
            } else if (sections[i] == RECENT_WORKOUTS && recentWorkouts.getParent() == null) {
                layout.addView(recentWorkouts);
                if (workouts.size() == 0) {
                    layout.addView(recentEmpty);
                    continue;
                }
            } else if (sections[i] == OVERDUE_WORKOUTS && overdueWorkout.getParent() == null) {
                if (workouts.size() == 0)
                    continue;
                layout.addView(overdueWorkout);
            }

            // print out workouts in section
            for (final WorkoutViewModel.WorkoutDataSet data : workouts) {

                if (data == null) {
                    TextView loadMore = new TextView(this.getContext());
                    loadMore.setLayoutParams(breakParams);
                    loadMore.setGravity(Gravity.CENTER);
                    loadMore.setText(R.string.labelLoadMore);
                    loadMore.setTextSize(14);
                    loadMore.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_refresh_24px, 0);
                    loadMore.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //loadMoreWorkouts(workoutData, sectionId);
                            model.loadMoreWorkouts(sectionId);
                        }
                    });
                    layout.addView(loadMore);
                    continue;
                }

                final long workoutId = data.workout.getId();

                // set up workout header
                View headerView = inflate(getContext(), R.layout.fragment_dashboard_workouts, null);
                headerView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        model.selectWorkout(workoutId);
                        mListener.viewWorkout(false);
                    }
                });
                headerView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        deleteWorkout(data.workout);
                        return true;
                    }
                });

                LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height / 3, 1);
                sectionParams.setMargins(0, 16, 0, 0);
                headerView.setLayoutParams(sectionParams);

                Date workoutDate = new Date(data.workout.getDate());
                Calendar cal = Calendar.getInstance();
                cal.setTime(workoutDate);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MILLISECOND, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);

                // set up header
                TextView workoutHeader = headerView.findViewById(R.id.workoutHeader);
                String message;
                if (data.workout.isCompleted()) {
                    workoutHeader.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_swimming_man, 0, 0, 0);
                    message = "   Workout swam on ";
                } else {
                    Drawable icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_baseline_schedule_24px).getConstantState().newDrawable().mutate();
                    if (current.after(cal))
                        icon.setColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP);
                    workoutHeader.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
                    message = "   Workout scheduled for ";
                }

                message +=  data.workout.getDate("MMMM dd, yyyy");
                workoutHeader.setText(message);

                WorkoutChart workoutChart = headerView.findViewById(R.id.chart);
                workoutChart.setTouchEnabled(false);
                //workoutChart.setLayoutParams(sectionParams);
                if (data.rows.size() > 0)
                    workoutChart.setData(data.rows);
                else
                    workoutChart.setNoDataText("Workout is empty!\nAdd some laps!");
                workoutChart.invalidate();

                // set up actions
                TextView actionNotes = headerView.findViewById(R.id.actionNotes);
                actionNotes.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        notesWorkout(data.workout);
                    }
                });

                TextView actionDate = headerView.findViewById(R.id.actionDate);
                actionDate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        scheduleWorkout(data.workout);
                    }
                });

                TextView actionDelete = headerView.findViewById(R.id.actionDelete);
                actionDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        deleteWorkout(data.workout);
                    }
                });

                layout.addView(headerView);

                int daysPrior = 6 + (int) ((cal.getTimeInMillis() - current.getTimeInMillis()) / (1000 * 60 * 60 * 24));
                if (data.workout.isCompleted() && workoutChart.getDistance() > 0 && daysPrior >= 0 && daysPrior <= 6) {
                    recentsAdded++;
                    totalDistance += workoutChart.getDistance();
                    // get distance and stroke data for pie/bar charts
                    Map<Stroke, Integer> workoutStrokeDistances = workoutChart.getStrokeDistances();
                    for (Stroke stroke : workoutStrokeDistances.keySet()) {
                        if (!strokeDistances.containsKey(stroke))
                            strokeDistances.put(stroke, 0);
                        strokeDistances.put(stroke, strokeDistances.get(stroke) + workoutStrokeDistances.get(stroke));
                    }

                    dailyDistances[daysPrior] += workoutChart.getDistance();
                }
            }
        }



        if (recentWorkouts.getParent() == null) {
            layout.addView(recentWorkouts);
            layout.addView(recentEmpty);
        }

        TextView valueWorkouts = (TextView) getView().findViewById(R.id.valueWorkouts);
        valueWorkouts.setText("" + recentsAdded);
        TextView valueDistance = (TextView) getView().findViewById(R.id.valueDistance);
        DecimalFormat formatter = new DecimalFormat("#,###,###");
        valueDistance.setText(formatter.format(totalDistance));

        BarChart distanceChart = (BarChart) getView().findViewById(R.id.distanceChart);
        distanceChart.setData(distanceSummary(dailyDistances));
        distanceChart.invalidate();
        XAxis xAxis = distanceChart.getXAxis();
        xAxis.setLabelCount(7);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(current.getTime());
                cal.add(Calendar.DATE, -1 * (6 - (int) value));
                return new SimpleDateFormat("MMM dd").format(cal.getTime());
            }
        });

        distanceChart.getAxisLeft().setAxisMinimum(0);
        distanceChart.getAxisRight().setEnabled(false);
        distanceChart.getAxisLeft().setEnabled(false);
        distanceChart.getLegend().setEnabled(false);
        distanceChart.getDescription().setEnabled(false);

        PieChart strokeChart = (PieChart) getView().findViewById(R.id.strokeChart);
        strokeChart.setData(strokeSummary(strokeDistances));
        strokeChart.invalidate();
        strokeChart.getDescription().setEnabled(false);
        strokeChart.setDrawEntryLabels(false);
        strokeChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);

        if (totalDistance == 0)
            strokeChart.setVisibility(View.GONE);
        else
            strokeChart.setVisibility(View.VISIBLE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_dashboard, container, false);

        FloatingActionButton btnAddWorkout = (FloatingActionButton) view.findViewById(R.id.addWorkoutButton);
        btnAddWorkout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createWorkout();
            }
        });

        // get container to dynamically append views to
        ViewGroup layout = (ViewGroup) view.findViewById(R.id.svLayout);
        int height = Resources.getSystem().getDisplayMetrics().heightPixels;

        // set up layout params for dynamically added elements

        LinearLayout.LayoutParams chartParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height / 3);
        chartParams.setMargins(0, 16, 0, 0);

        // create charts

        BarChart distanceSummary = view.findViewById(R.id.distanceChart);
        distanceSummary.setLayoutParams(chartParams);
        distanceSummary.setBackgroundColor(Color.WHITE);

        PieChart strokeSummary = view.findViewById(R.id.strokeChart);
        strokeSummary.setLayoutParams(chartParams);
        strokeSummary.setBackgroundColor(Color.WHITE);

        return view;
    }

    private BarData distanceSummary(int[] dailyDistances) {
        BarChart distances = new BarChart(getContext());
        List<BarEntry> entries = new ArrayList<>();

        for (int i = 0; i < dailyDistances.length; i++)
            entries.add(new BarEntry(i, dailyDistances[i]));

        BarDataSet dataSet = new BarDataSet(entries, "Distance by Day");
        BarData data = new BarData(dataSet);
        return data;
    }

    private PieData strokeSummary(Map<Stroke, Integer> strokeDistances) {
        Map<Stroke, Integer> strokeColors = mListener.getStrokeColors();
        int[] colorsArray = new int[strokeDistances.size()];
        List<PieEntry> entries = new ArrayList<>();
        int i = 0;
        for (Stroke stroke : strokeDistances.keySet()) {
            entries.add(new PieEntry(strokeDistances.get(stroke), stroke.stroke()));
            colorsArray[i] = strokeColors.get(stroke);
            i++;
        }
        PieDataSet dataSet = new PieDataSet(entries, null);

        dataSet.setColors(colorsArray);

        PieData data = new PieData(dataSet);
        return data;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof DashboardListener) {
            mListener = (DashboardListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface DashboardListener {
        void createBlankWorkout(Date date, boolean complete);
        void selectWorkout(long date, boolean complete, String TAG);
        void viewWorkout(boolean editMode);
        Map<Stroke, Integer> getStrokeColors();
    }
}
