package com.dsfstudios.apps.lappr.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dsfstudios.apps.lappr.SetComponentInterface;
import com.dsfstudios.apps.lappr.ui.adapters.LapAdapter;
import com.dsfstudios.apps.lappr.R;
import com.dsfstudios.apps.lappr.SetComponentEditor;
import com.dsfstudios.apps.lappr.Stroke;
import com.dsfstudios.apps.lappr.database.AppDatabase;
import com.dsfstudios.apps.lappr.database.entities.dbLap;
import com.dsfstudios.apps.lappr.database.entities.dbSetComponent;
import com.dsfstudios.apps.lappr.viewmodel.WorkoutViewModel;

import java.util.List;
import java.util.Map;

public class RecordWorkoutDetails extends DialogFragment implements SetComponentInterface {

    private static AppDatabase mDb;
    private WorkoutViewModel model;
    private OnWorkoutDetailsListener mListener;
    private SetComponentEditor editor;
    private long setComponentId;
    private final SetComponentInterface ui = this;

    static RecordWorkoutDetails newInstance(long setComponentId) {
        RecordWorkoutDetails fragment = new RecordWorkoutDetails();
        Bundle bundle = new Bundle();
        bundle.putLong("setComponentId", setComponentId);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentActivity mActivity = (FragmentActivity) getActivity();
        model = ViewModelProviders.of(mActivity).get(WorkoutViewModel.class);
        this.setComponentId = getArguments().getLong("setComponentId");
        //editor = new SetComponentEditor(model.getDb(), -1, setComponentId);
        mDb = AppDatabase.getAppDatabase(this.getActivity());
        editor = new SetComponentEditor(mDb);
        loadEditor(ui, editor, setComponentId, -1);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_record_workout_details, container, false);
        // increase size of dialog
        ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

        TextView instructions = (TextView) view.findViewById(R.id.textInstructions);
        String message = "Tap to change stroke\nHold tap to cycle swim/kick/drill";
        instructions.setText(message);

        TextView borderSwim = (TextView) view.findViewById(R.id.borderSwim);
        borderSwim.setText("Swim");
        GradientDrawable swimBg = new GradientDrawable();
        swimBg.setStroke(10, Color.BLACK);
        swimBg.setCornerRadius(5);
        borderSwim.setBackground(swimBg);

        TextView borderKick = (TextView) view.findViewById(R.id.borderKick);
        borderKick.setText("Kick");
        GradientDrawable kickBg = new GradientDrawable();
        kickBg.setStroke(10, Color.RED);
        kickBg.setCornerRadius(5);
        borderKick.setBackground(kickBg);

        TextView borderDrill = (TextView) view.findViewById(R.id.borderDrill);
        borderDrill.setText("Drill");
        GradientDrawable drillBg = new GradientDrawable();
        drillBg.setStroke(10, Color.YELLOW);
        drillBg.setCornerRadius(5);
        borderDrill.setBackground(drillBg);

        final ProgressBar progressBar = view.findViewById(R.id.progress);
        progressBar.setVisibility(View.VISIBLE);

        Button btnSave = (Button) view.findViewById(R.id.btnSave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                saveEditor(ui, editor);
            }
        });

        return view;
    }

    private static void loadEditor(final SetComponentInterface ui, final SetComponentEditor editor, long setComponentId, long setId) {
        new AsyncTask<Long, Void, Void>() {

            @Override
            protected Void doInBackground(Long... ids) {
                editor.init(ids[0], ids[1]);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                ui.updateUI();
            }
        }.execute(setComponentId, setId);
    }

    private static void saveEditor(final SetComponentInterface ui, final SetComponentEditor editor) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                editor.save();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                ui.nextDialog(editor.getSetComponentId());
            }
        }.execute();
    }


    public void updateUI() {
        View view = getView();
        RecyclerView gridLaps = (RecyclerView) view.findViewById(R.id.gridLaps);
        gridLaps.setLayoutManager(new LinearLayoutManager(view.getContext(), LinearLayoutManager.HORIZONTAL, false));
        gridLaps.setAdapter(new LapAdapter(editor, mListener));

        ProgressBar progressBar = view.findViewById(R.id.progress);
        progressBar.setVisibility(View.GONE);
    }

    public void nextDialog(long setComponentId) {
        ViewWorkoutListener activity = (ViewWorkoutListener) getTargetFragment();
        activity.updateSetComponents();
        dismiss();
    }

    @Override
    public void onResume() {
        super.onResume();
        ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnWorkoutDetailsListener) {
            mListener = (OnWorkoutDetailsListener) context;
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

    public interface OnWorkoutDetailsListener {
        Map<Stroke, Integer> getStrokeColors();
    }
}
