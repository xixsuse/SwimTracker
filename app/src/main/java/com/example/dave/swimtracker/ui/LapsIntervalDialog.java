/*
User interface for lap interval selection, interacts with SetComponentEditor to edit laps
 */
package com.example.dave.swimtracker.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;

import com.example.dave.swimtracker.R;
import com.example.dave.swimtracker.SetComponentEditor;
import com.example.dave.swimtracker.SetComponentInterface;
import com.example.dave.swimtracker.database.AppDatabase;
import com.example.dave.swimtracker.viewmodel.WorkoutViewModel;

public class LapsIntervalDialog extends DialogFragment implements SetComponentInterface {

    private static AppDatabase mDb;
    private SetComponentEditor editor;
    private WorkoutViewModel model;
    private final SetComponentInterface ui = this;

    static LapsIntervalDialog newInstance(long setId, long setComponentId) {
        LapsIntervalDialog f = new LapsIntervalDialog();
        Bundle bundle = new Bundle();
        bundle.putLong("setId", setId);
        bundle.putLong("setComponentId", setComponentId);
        f.setArguments(bundle);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDb = AppDatabase.getAppDatabase(this.getActivity());
        model = ViewModelProviders.of((FragmentActivity) getActivity()).get(WorkoutViewModel.class);
        long setId = getArguments().getLong("setId");
        long setComponentId = getArguments().getLong("setComponentId");

        editor = new SetComponentEditor(mDb);
        loadEditor(ui, editor, setComponentId, setId);
    }

    public void updateUI() {
        View v = getView();

        final ProgressBar progressBar = v.findViewById(R.id.progress);
        progressBar.setVisibility(View.GONE);

        // Set up the button bars for interval type
        final Button buttonRep = (Button) v.findViewById(R.id.intervalRep);
        final Button buttonRest = (Button) v.findViewById(R.id.intervalRest);
        final Button buttonTotal = (Button) v.findViewById(R.id.intervalTotal);
        final Button buttonNone = (Button) v.findViewById(R.id.intervalNone);
        final Button[] buttonIntervals = new Button[] {buttonRep, buttonRest, buttonTotal, buttonNone};
        final Integer[] intervals = new Integer[] {SetComponentEditor.TIME_INTERVAL, SetComponentEditor.REST_INTERVAL, SetComponentEditor.TOTAL_INTERVAL,
                                                SetComponentEditor.NO_INTERVAL};

        int initType = editor.getIntervalType();
        buttonIntervals[initType].setSelected(true);

        // set interval time
        final TimePicker timePicker = (TimePicker) v.findViewById(R.id.intervalPicker);
        timePicker.setIs24HourView(true);

        final NumberPicker minutePicker = (NumberPicker) v.findViewById(Resources.getSystem().getIdentifier("hour","id","android"));
        minutePicker.setMaxValue(SetComponentEditor.max_min);
        minutePicker.setValue(editor.getMin());
        minutePicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                editor.setMin(i1);
            }
        });

        final NumberPicker secondPicker = (NumberPicker) v.findViewById(Resources.getSystem().getIdentifier("minute","id","android"));
        secondPicker.setMaxValue(SetComponentEditor.max_sec);
        secondPicker.setDisplayedValues(SetComponentEditor.options_sec);
        secondPicker.setValue(editor.getSec());
        secondPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                editor.setSec(i1);
            }
        });

        // set up interval delta
        final TextView textInterval = (TextView) v.findViewById(R.id.displayInterval);
        final SeekBar seekInterval = (SeekBar) v.findViewById(R.id.intervalBar);
        textInterval.setText(editor.printDelta());

        seekInterval.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                editor.setDelta(i);
                textInterval.setText(editor.printDelta());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        seekInterval.setProgress(editor.getDelta());

        // set up interval type change listeners
        // hides/shows various objects so set up last
        for (Button button : buttonIntervals) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    for (int i = 0; i < buttonIntervals.length; i++) {
                        Button button = buttonIntervals[i];
                        if (button.getId() == view.getId()) {
                            button.setSelected(true);
                            editor.setIntervalType(intervals[i]);

                            // custom logic to reset interval and set visibility of interval selectors
                            switch(i) {
                                case SetComponentEditor.TIME_INTERVAL:
                                    timePicker.setVisibility(View.VISIBLE);
                                    seekInterval.setVisibility(View.VISIBLE);
                                    textInterval.setVisibility(View.VISIBLE);
                                    editor.setInterval(SetComponentEditor.DEFAULT_INTERVAL_PER_100 * editor.distance / 100);
                                    break;
                                case SetComponentEditor.REST_INTERVAL:
                                    timePicker.setVisibility(View.VISIBLE);
                                    seekInterval.setVisibility(View.VISIBLE);
                                    textInterval.setVisibility(View.VISIBLE);
                                    editor.setInterval(SetComponentEditor.DEFAULT_REST_INTERVAL);
                                    break;
                                case SetComponentEditor.TOTAL_INTERVAL:
                                    timePicker.setVisibility(View.VISIBLE);
                                    seekInterval.setVisibility(View.INVISIBLE);
                                    textInterval.setVisibility(View.INVISIBLE);
                                    editor.setInterval((int) (1.0 * SetComponentEditor.DEFAULT_INTERVAL_PER_100 * (1.0 * editor.numReps * editor.distance / 100)));
                                    break;
                                case SetComponentEditor.NO_INTERVAL:
                                    timePicker.setVisibility(View.INVISIBLE);
                                    seekInterval.setVisibility(View.INVISIBLE);
                                    textInterval.setVisibility(View.INVISIBLE);
                                    break;
                            }
                            minutePicker.setValue(editor.getMin());
                            secondPicker.setValue(editor.getSec());
                        } else {
                            button.setSelected(false);
                        }
                    }
                }
            });
        }

        // set up next dialog button
        Button saveSet = (Button) v.findViewById(R.id.button_next);
        saveSet.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                saveEditor(ui, editor);
            }
        });

    }

    // makes async request to load details of set component via the set component editor, refreshes UI on completion
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

    // makes async request to save changes to the set component via set component editor
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

    // called after save is completed, replaces this dialog with lap details dialog
    public void nextDialog(long setComponentId) {
        dismiss();
        DialogFragment newFragment = new RecordWorkoutDetails();
        Bundle bundle = new Bundle();
        bundle.putLong("setComponentId", setComponentId);
        newFragment.setArguments(bundle);
        newFragment.setTargetFragment(this.getTargetFragment(), 0);
        newFragment.show(getActivity().getSupportFragmentManager(), "workoutDetails");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_interval, container, false);

        // increase size of dialog
        ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

        ProgressBar progressBar = v.findViewById(R.id.progress);
        progressBar.setVisibility(View.VISIBLE);

        return v;
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
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}