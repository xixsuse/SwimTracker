package com.example.dave.swimtracker.ui;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.dave.swimtracker.R;
import com.example.dave.swimtracker.ui.adapters.WorkoutAdapter;
import com.example.dave.swimtracker.ui.adapters.WorkoutAdapterListener;
import com.example.dave.swimtracker.database.AppDatabase;
import com.example.dave.swimtracker.database.entities.dbWorkout;
import com.example.dave.swimtracker.viewmodel.WorkoutViewModel;
import com.example.dave.swimtracker.viewmodel.WorkoutViewModelFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class WorkoutList extends Fragment {

    private static AppDatabase db;
    private MutableLiveData<List<Dashboard.WorkoutDataSet>> workoutList;
    private WorkoutAdapterListener mListener;
    private static long mSwimmerId;
    private List<Dashboard.WorkoutDataSet> workouts;
    private WorkoutAdapter workoutAdapter;
    private Date date;
    private boolean complete;
    private boolean showCompleted;
    private boolean showScheduled;
    private String TAG;
    private WorkoutViewModel model;

    public WorkoutList() {
    }

    public static WorkoutList newInstance(Bundle bundle) {
        WorkoutList fragment = new WorkoutList();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            this.date = new Date(getArguments().getLong("date"));
            this.complete = getArguments().getBoolean("complete");
            this.TAG = getArguments().getString("TAG");
            showCompleted = getArguments().getBoolean("showCompleted");
            showScheduled = !showCompleted;
        }

        db = AppDatabase.getAppDatabase(this.getActivity());
        WorkoutViewModelFactory factory = new WorkoutViewModelFactory(db);
        model = ViewModelProviders.of(this.getActivity(), factory).get(WorkoutViewModel.class);
        mSwimmerId = model.getSwimmerId();

        workouts = new ArrayList<>();
        workoutAdapter = new WorkoutAdapter(date, complete, workouts, mListener, TAG, model);
        workoutList = new MutableLiveData<>();
        workoutList.observe(this, new Observer<List<Dashboard.WorkoutDataSet>>() {
            @Override
            public void onChanged(@Nullable List<Dashboard.WorkoutDataSet> data) {
                workouts.clear();
                workouts.addAll(data);
                workoutAdapter.notifyDataSetChanged();
            }
        });

        fetchWorkouts(workoutList, showCompleted, showScheduled);
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchWorkouts(workoutList, showCompleted, showScheduled);
    }

    private static void fetchWorkouts(final MutableLiveData<List<Dashboard.WorkoutDataSet>> liveData, boolean completed, boolean scheduled) {
        new AsyncTask<Boolean, Void, Void>() {
            @Override
            protected Void doInBackground(Boolean... status) {
                List<dbWorkout> listWorkouts;
                if (status[0] && status[1])
                    listWorkouts = db.dao().getWorkouts(mSwimmerId);
                else if (status[0])
                    listWorkouts = db.dao().getWorkouts(mSwimmerId, true, 5);
                else if (status[1])
                    listWorkouts = db.dao().getWorkouts(mSwimmerId, false, 5);
                else
                    listWorkouts = new ArrayList<>();

                List<Dashboard.WorkoutDataSet> listData = new ArrayList<>();
                for (dbWorkout w : listWorkouts) {
                    Dashboard.WorkoutDataSet workoutData = new Dashboard.WorkoutDataSet();
                    workoutData.workout = w;
                    workoutData.rows = db.dao().getWorkoutData(w.getId());
                    listData.add(workoutData);
                }
                liveData.postValue(listData);
                return null;
            }
        }.execute(completed, scheduled);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_workout_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setAdapter(workoutAdapter);
        }
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof WorkoutAdapterListener) {
            mListener = (WorkoutAdapterListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
