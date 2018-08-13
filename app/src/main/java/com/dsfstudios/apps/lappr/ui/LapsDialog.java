package com.dsfstudios.apps.lappr.ui;

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
import android.widget.ToggleButton;

import com.dsfstudios.apps.lappr.R;
import com.dsfstudios.apps.lappr.SetComponentEditor;
import com.dsfstudios.apps.lappr.SetComponentInterface;
import com.dsfstudios.apps.lappr.Stroke;
import com.dsfstudios.apps.lappr.database.AppDatabase;
import com.dsfstudios.apps.lappr.viewmodel.WorkoutViewModel;

import java.util.Arrays;

import static com.dsfstudios.apps.lappr.SetComponentEditor.TYPE_DRILL;

public class LapsDialog extends DialogFragment implements SetComponentInterface {

    private static AppDatabase mDb;
    private SetComponentEditor editor;
    private WorkoutViewModel model;
    private final SetComponentInterface ui = this;

    static LapsDialog newInstance(long setId, long setComponentId) {
        LapsDialog f = new LapsDialog();
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

        // Remove the progress bar on load
        final ProgressBar progressBar = v.findViewById(R.id.progress);
        progressBar.setVisibility(View.GONE);

        // Set up the spinner for number of reps
        final NumberPicker valueReps = (NumberPicker) v.findViewById(R.id.repPicker);
        valueReps.setMinValue(1);
        valueReps.setMaxValue(20);
        valueReps.setValue(editor.getNumReps());
        valueReps.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                editor.setNumReps(i1);
            }
        });

        // Set up the spinner for distance
        NumberPicker valueDistance = (NumberPicker) v.findViewById(R.id.distancePicker);
        final String[] distanceValues = getResources().getStringArray(R.array.distances);
        valueDistance.setMinValue(0);
        valueDistance.setMaxValue(distanceValues.length - 1);
        valueDistance.setDisplayedValues(distanceValues);
        valueDistance.setValue(Arrays.asList(distanceValues).indexOf(""+editor.getDistance()));
        valueDistance.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                int distance = Integer.parseInt(distanceValues[i1]);
                editor.setDistance(distance);
            }
        });

        // Set up the button bars for stroke
        final Button buttonFree = (Button) v.findViewById(R.id.buttonFr);
        final Button buttonBack = (Button) v.findViewById(R.id.buttonBa);
        final Button buttonBreast = (Button) v.findViewById(R.id.buttonBr);
        final Button buttonFly = (Button) v.findViewById(R.id.buttonFly);
        final Button buttonIM = (Button) v.findViewById(R.id.buttonIM);
        final Button buttonRIM = (Button) v.findViewById(R.id.buttonRIM);
        final Button buttonIMO = (Button) v.findViewById(R.id.buttonIMO);
        final Button buttonChoice = (Button) v.findViewById(R.id.buttonCh);
        final Button[] buttonStrokes = new Button[] {buttonFree, buttonBack, buttonBreast, buttonFly, buttonIM, buttonRIM, buttonIMO, buttonChoice};
        final Stroke[] strokes = new Stroke[] {Stroke.FREESTYLE, Stroke.BACKSTROKE, Stroke.BREASTSTROKE, Stroke.BUTTERFLY, Stroke.IM, Stroke.REVERSE_IM,
                                                Stroke.IM_ORDER, Stroke.CHOICE};

        for (Button button : buttonStrokes) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    for (int i = 0; i < buttonStrokes.length; i++) {
                        Button button = buttonStrokes[i];
                        if (button.getId() == view.getId()) {
                            button.setSelected(true);
                            editor.setStroke(strokes[i]);
                        } else {
                            button.setSelected(false);
                        }
                    }
                }
            });
        }

        Stroke initStroke = editor.getStroke();
        if (initStroke != Stroke.MIXED)
            buttonStrokes[Arrays.asList(strokes).indexOf(initStroke)].setSelected(true);
        else {
            for (int i = 0; i < buttonStrokes.length; i++) {
                buttonStrokes[i].setEnabled(false);
            }
        }

        // Set up the button bars for swim/kick/drill
        final Button buttonSwim = (Button) v.findViewById(R.id.buttonSwim);
        final Button buttonKick = (Button) v.findViewById(R.id.buttonKick);
        final Button buttonDrill = (Button) v.findViewById(R.id.buttonDrill);
        final Button[] buttonTypes = new Button[] {buttonSwim, buttonKick, buttonDrill};
        final Integer[] types = new Integer[] {SetComponentEditor.TYPE_SWIM, SetComponentEditor.TYPE_KICK, SetComponentEditor.TYPE_DRILL};

        for (Button button : buttonTypes) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    for (int i = 0; i < buttonTypes.length; i++) {
                        Button button = buttonTypes[i];
                        if (button.getId() == view.getId()) {
                            button.setSelected(true);
                            editor.setType(types[i]);
                        } else {
                            button.setSelected(false);
                        }
                    }
                }
            });
        }

        int initType = editor.getType();
        if (initType != SetComponentEditor.TYPE_MIXED)
            buttonTypes[initType].setSelected(true);
        else {
            for (int i = 0; i < buttonTypes.length; i++) {
                buttonTypes[i].setEnabled(false);
            }
        }

        Button saveSet = (Button) v.findViewById(R.id.button_next);
        saveSet.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                saveEditor(ui, editor);
            }
        });

    }

    // loads the set component editor in the background, and updates the UI once complete
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
        DialogFragment newFragment = new LapsIntervalDialog();
        Bundle bundle = new Bundle();
        bundle.putLong("setComponentId", setComponentId);
        newFragment.setArguments(bundle);
        newFragment.setTargetFragment(this.getTargetFragment(), 0);
        newFragment.show(getActivity().getSupportFragmentManager(), "workoutDetails");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_laps, container, false);

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