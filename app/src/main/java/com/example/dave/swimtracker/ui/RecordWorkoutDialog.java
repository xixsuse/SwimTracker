package com.example.dave.swimtracker.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.dave.swimtracker.R;
import com.example.dave.swimtracker.SetComponentEditor;
import com.example.dave.swimtracker.SetComponentInterface;
import com.example.dave.swimtracker.Stroke;
import com.example.dave.swimtracker.database.AppDatabase;
import com.example.dave.swimtracker.database.entities.dbLap;
import com.example.dave.swimtracker.database.entities.dbRep;
import com.example.dave.swimtracker.database.entities.dbSetComponent;
import com.example.dave.swimtracker.viewmodel.WorkoutViewModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecordWorkoutDialog extends android.support.v4.app.DialogFragment implements SetComponentInterface {

    private static AppDatabase mDb;
    private SetComponentEditor editor;
    private WorkoutViewModel model;
    private final SetComponentInterface ui = this;

    static RecordWorkoutDialog newInstance(long setId, long setComponentId) {
        RecordWorkoutDialog f = new RecordWorkoutDialog();
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

        TextView valueReps = (TextView) v.findViewById(R.id.valueReps);
        valueReps.setText(""+editor.getNumReps());
        final SeekBar seekReps = (SeekBar) v.findViewById(R.id.seekReps);
        seekReps.setProgress(editor.getNumReps());

        // set up Stroke spinner
        Spinner strokeSpinner = (Spinner) v.findViewById(R.id.entryStroke);
        Stroke currentStroke = editor.getStroke();
        Stroke[] selectableStrokes;
        boolean enableStrokeSpinner = true;
        if (currentStroke == Stroke.MIXED) {
            // Mixed is merely a label and consists of a custom set of strokes so it is not changeable at this level
            selectableStrokes = new Stroke[]{Stroke.MIXED};
            enableStrokeSpinner = false;
        } else {
            selectableStrokes = new Stroke[]{Stroke.FREESTYLE, Stroke.BACKSTROKE, Stroke.BREASTSTROKE, Stroke.BUTTERFLY,
                    Stroke.IM, Stroke.REVERSE_IM, Stroke.IM_ORDER, Stroke.CHOICE};
        }
        ArrayAdapter<Stroke> strokeAdapter = new ArrayAdapter<>(v.getContext(), R.layout.support_simple_spinner_dropdown_item, selectableStrokes);
        strokeAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        strokeSpinner.setAdapter(strokeAdapter);
        strokeSpinner.setEnabled(enableStrokeSpinner);
        strokeSpinner.setSelection(strokeAdapter.getPosition(currentStroke));

        strokeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Stroke selected = (Stroke) adapterView.getItemAtPosition(i);
                if (selected != Stroke.MIXED)
                    editor.setStroke(selected);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // set up Distance adapter
        final Spinner distanceSpinner = (Spinner) v.findViewById(R.id.entryDistance);
        ArrayAdapter<CharSequence> distanceAdapter = ArrayAdapter.createFromResource(getActivity().getApplicationContext(), R.array.distances, R.layout.support_simple_spinner_dropdown_item);
        distanceAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        distanceSpinner.setAdapter(distanceAdapter);
        distanceSpinner.setSelection(distanceAdapter.getPosition(""+editor.getDistance()));

        distanceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editor.setDistance(Integer.parseInt(adapterView.getItemAtPosition(i).toString()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // set up swim/kick/drill radio buttons
        RadioGroup radioTypes = (RadioGroup) v.findViewById(R.id.radioTypes);
        radioTypes.clearCheck();
        radioTypes.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch(i) {
                    case R.id.radioSwim:
                        editor.setType(SetComponentEditor.TYPE_SWIM);
                        break;
                    case R.id.radioKick:
                        editor.setType(SetComponentEditor.TYPE_KICK);
                        break;
                    case R.id.radioDrill:
                        editor.setType(SetComponentEditor.TYPE_DRILL);
                        break;
                }
            }
        });
        RadioButton radioSwim = (RadioButton) v.findViewById(R.id.radioSwim);
        RadioButton radioKick = (RadioButton) v.findViewById(R.id.radioKick);
        RadioButton radioDrill = (RadioButton) v.findViewById(R.id.radioDrill);

        int type = editor.getType();
        switch (type) {
            case -1:
                radioSwim.setEnabled(false);
                radioKick.setEnabled(false);
                radioDrill.setEnabled(false);
                break;
            case SetComponentEditor.TYPE_SWIM:
                radioSwim.setChecked(true);
                break;
            case SetComponentEditor.TYPE_KICK:
                radioKick.setChecked(true);
                break;
            case SetComponentEditor.TYPE_DRILL:
                radioDrill.setChecked(true);
                break;
        }

        final TimePicker timePicker = (TimePicker) v.findViewById(R.id.timePicker);
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

        // set up interval ascending/descending selector
        final SeekBar seekInterval = (SeekBar) v.findViewById(R.id.seekIntervalDelta);
        final TextView textInterval = (TextView) v.findViewById(R.id.textIntervalDelta);
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

        // set up interval radio buttons
        RadioButton radioBaseInterval = (RadioButton) v.findViewById(R.id.radioBaseInterval);
        RadioButton radioRestInterval = (RadioButton) v.findViewById(R.id.radioRestInterval);
        RadioButton radioTotalInterval = (RadioButton) v.findViewById(R.id.radioTotalInterval);
        RadioButton radioNoInterval = (RadioButton) v.findViewById(R.id.radioNoInterval);

        int intervalType = editor.getIntervalType();
        switch (intervalType) {
            case SetComponentEditor.TIME_INTERVAL:
                radioBaseInterval.setChecked(true);
                break;
            case SetComponentEditor.REST_INTERVAL:
                radioRestInterval.setChecked(true);
                break;
            case SetComponentEditor.TOTAL_INTERVAL:
                radioTotalInterval.setChecked(true);
                seekInterval.setVisibility(View.INVISIBLE);
                textInterval.setVisibility(View.INVISIBLE);
                break;
            case SetComponentEditor.NO_INTERVAL:
                radioNoInterval.setChecked(true);
                timePicker.setVisibility(View.INVISIBLE);
                seekInterval.setVisibility(View.INVISIBLE);
                textInterval.setVisibility(View.INVISIBLE);
                break;
        }

        RadioGroup radioIntervalTypes = (RadioGroup) v.findViewById(R.id.radioIntervals);
        radioIntervalTypes.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch(i) {
                    case R.id.radioBaseInterval:
                        timePicker.setVisibility(View.VISIBLE);
                        seekInterval.setVisibility(View.VISIBLE);
                        textInterval.setVisibility(View.VISIBLE);
                        editor.setIntervalType(SetComponentEditor.TIME_INTERVAL);
                        editor.setInterval(SetComponentEditor.DEFAULT_INTERVAL_PER_100 * editor.distance / 100);
                        break;
                    case R.id.radioRestInterval:
                        timePicker.setVisibility(View.VISIBLE);
                        seekInterval.setVisibility(View.VISIBLE);
                        textInterval.setVisibility(View.VISIBLE);
                        editor.setIntervalType(SetComponentEditor.REST_INTERVAL);
                        editor.setInterval(10);
                        break;
                    case R.id.radioTotalInterval:
                        timePicker.setVisibility(View.VISIBLE);
                        seekInterval.setVisibility(View.INVISIBLE);
                        textInterval.setVisibility(View.INVISIBLE);
                        editor.setIntervalType(SetComponentEditor.TOTAL_INTERVAL);
                        int defaultSecs = SetComponentEditor.DEFAULT_INTERVAL_PER_100 * (editor.numReps * editor.distance / 100);
                        editor.setInterval(defaultSecs);
                        break;
                    case R.id.radioNoInterval:
                        timePicker.setVisibility(View.INVISIBLE);
                        seekInterval.setVisibility(View.INVISIBLE);
                        textInterval.setVisibility(View.INVISIBLE);
                        editor.setIntervalType(SetComponentEditor.NO_INTERVAL);
                        break;
                }
                minutePicker.setValue(editor.getMin());
                secondPicker.setValue(editor.getSec());
            }
        });

        Button saveSet = (Button) v.findViewById(R.id.button_next);
        saveSet.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                saveEditor(ui, editor);
            }
        });

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
        View v = inflater.inflate(R.layout.record_workout_entry, container, false);

        // increase size of dialog
        ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

        // listen for changes to reps
        final SeekBar seekReps = (SeekBar) v.findViewById(R.id.seekReps);
        final TextView valueReps = (TextView) v.findViewById(R.id.valueReps);

        seekReps.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (i == 0) {
                    seekBar.setProgress(1);
                    i = 1;
                }
                valueReps.setText(""+i);
                editor.setNumReps(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

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