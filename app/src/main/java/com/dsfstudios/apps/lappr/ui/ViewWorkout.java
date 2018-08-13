package com.dsfstudios.apps.lappr.ui;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.dsfstudios.apps.lappr.CustomMutableLiveData;
import com.dsfstudios.apps.lappr.LapDescriber;
import com.dsfstudios.apps.lappr.R;
import com.dsfstudios.apps.lappr.SetComponentEditor;
import com.dsfstudios.apps.lappr.Stroke;
import com.dsfstudios.apps.lappr.WorkoutChart;
import com.dsfstudios.apps.lappr.database.entities.dbLap;
import com.dsfstudios.apps.lappr.database.entities.dbRep;
import com.dsfstudios.apps.lappr.database.entities.dbWorkout;
import com.dsfstudios.apps.lappr.ui.adapters.SetComponentAdapter;
import com.dsfstudios.apps.lappr.WorkoutView;
import com.dsfstudios.apps.lappr.database.AppDatabase;
import com.dsfstudios.apps.lappr.database.entities.dbSet;
import com.dsfstudios.apps.lappr.database.entities.dbSetComponent;
import com.dsfstudios.apps.lappr.viewmodel.WorkoutViewModel;
import com.dsfstudios.apps.lappr.viewmodel.WorkoutViewModelFactory;
import com.github.mikephil.charting.data.LineData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewWorkout extends Fragment implements ViewWorkoutListener {

    // Flag to determine if the workout is editable, which shows additional icons (add, edit, etc.) if on
    private boolean editMode;

    // Instance of the app's view model
    private WorkoutViewModel model;

    // Listener object for edit set component functionality
    private ViewWorkoutListener mListener;

    // List of the workout's sets and its data fetched from the view model
    private MutableLiveData<List<SetData>> setData;

    // List of the workout's laps fetched from the view model
    private MutableLiveData<List<WorkoutViewModel.WorkoutRow>> lapData;

    // Combines set data from the database with additional tables, essentially stores data from a join Query
    private static class SetData {
        dbSet set;
        List<dbSetComponent> setComponents;
        Map<Long, List<dbLap>> laps;
    }

    // Instance of the app's database
    private static AppDatabase db;

    public ViewWorkout() {
        // Required empty public constructor, options menu is different for this fragment
        setHasOptionsMenu(true);
    }

    public static ViewWorkout newInstance(Bundle bundle) {
        ViewWorkout fragment = new ViewWorkout();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        WorkoutViewModelFactory factory = new WorkoutViewModelFactory(AppDatabase.getAppDatabase(getContext()));
        this.model = ViewModelProviders.of(this.getActivity(), factory).get(WorkoutViewModel.class);

        db = AppDatabase.getAppDatabase(getContext());

        model.getSelected().observe(this, new Observer<dbWorkout>() {
            @Override
            public void onChanged(@Nullable dbWorkout dbWorkout) {
                //updateHeader(dbWorkout);
                updateData(setData, lapData, dbWorkout);
                updateHeader(dbWorkout);
            }
        });

        setData = new MutableLiveData<>();
        setData.observe(this, new Observer<List<SetData>>() {
            @Override
            public void onChanged(@Nullable List<SetData> setData) {
                updateSets(setData);
            }
        });

        lapData = new MutableLiveData<>();
        lapData.observe(this, new Observer<List<WorkoutViewModel.WorkoutRow>>() {
            @Override
            public void onChanged(@Nullable List<WorkoutViewModel.WorkoutRow> workoutRows) {
                updateChart(workoutRows);
            }
        });

        editMode = getArguments().getBoolean("editMode");
    }

    @Override
    public void onResume() {
        super.onResume();
        dbWorkout workout = model.getSelected().getValue();
        if (workout != null) {
            updateHeader(model.getSelected().getValue());
            updateData(setData, lapData, model.getSelected().getValue());
        }
    }

    public boolean onBackPressed() {
        if (isEditMode()) {
            setEditMode(false);
            return true;
        } else
            return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.view_workout, menu);
        super.onCreateOptionsMenu(menu, inflater);

        menu.findItem(R.id.editWorkout).setVisible(!editMode);
        menu.findItem(R.id.addSet).setVisible(editMode);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.editWorkout:
                setEditMode(true);
                return false;
            case R.id.addSet:
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(R.string.addSetDialog);
                builder.setItems(R.array.setTypes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int setType) {
                        addSet(setType);
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                return false;
            case R.id.shareWorkout:
                shareWorkout();
                return false;
        }
        return super.onOptionsItemSelected(item);
    }

    private static void updateData(final MutableLiveData<List<SetData>> data, final MutableLiveData<List<WorkoutViewModel.WorkoutRow>> laps,
                                   dbWorkout workout) {
        // takes in workout from view model and queries for all set data
        new AsyncTask<dbWorkout, Void, Void>() {
            @Override
            protected Void doInBackground(dbWorkout... workouts) {
                long workoutId = workouts[0].getId();
                db.dao().updateWorkout(workouts[0]);

                List<dbSet> listSets = db.dao().getSets(workoutId);
                List<SetData> setDataSets = new ArrayList<>();
                for (dbSet s : listSets) {
                    SetData toAdd = new SetData();
                    toAdd.set = s;
                    toAdd.setComponents = db.dao().getSetComponents(s.getId());
                    toAdd.laps = new HashMap<>();
                    for (dbSetComponent setComponent : toAdd.setComponents) {
                        List<dbLap> setComponentLaps = db.dao().getSetComponentData(setComponent.getId());
                        toAdd.laps.put(setComponent.getId(), setComponentLaps);
                    }
                    setDataSets.add(toAdd);
                }
                data.postValue(setDataSets);

                List<WorkoutViewModel.WorkoutRow> newLaps = db.dao().getWorkoutData(workoutId);
                laps.postValue(newLaps);

                return null;
            }
        }.execute(workout);
    }

    public void addSet(int setType) {
        long workoutId = model.getSelected().getValue().getId();
        dbSet newSet = new dbSet(workoutId, 1, setType);
        execAddSet(setData, lapData, newSet);
    }

    private static void execAddSet(final MutableLiveData<List<SetData>> liveData, final MutableLiveData<List<WorkoutViewModel.WorkoutRow>> liveLaps,
                                   dbSet set) {
        new AsyncTask<dbSet, Void, Void>() {

            @Override
            protected Void doInBackground(dbSet... set) {
                db.dao().addSet(set[0]);
                dbWorkout workout = db.dao().getWorkout(set[0].getWorkoutId());
                updateData(liveData, liveLaps, workout);
                return null;
            }
        }.execute(set);
    }

    public void deleteSet(final dbSet set) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.deleteSetDialog);
        builder.setPositiveButton(R.string.deleteSetPositive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                execDeleteSet(setData, lapData, set);
            }
        });
        builder.setNegativeButton(R.string.deleteSetNegative, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private static void execDeleteSet(final MutableLiveData<List<SetData>> liveData, final MutableLiveData<List<WorkoutViewModel.WorkoutRow>> liveLaps,
                                      dbSet set) {
        new AsyncTask<dbSet, Void, Void>() {

            @Override
            protected Void doInBackground(dbSet... sets) {
                dbWorkout workout = db.dao().getWorkout(sets[0].getWorkoutId());
                db.dao().deleteSet(sets[0]);
                updateData(liveData, liveLaps, workout);
                return null;
            }
        }.execute(set);
    }

    public void updateSet(final dbSet set) {
        execUpdateSet(setData, lapData, set);
    }

    private static void execUpdateSet(final MutableLiveData<List<SetData>> liveData, final MutableLiveData<List<WorkoutViewModel.WorkoutRow>> liveLaps,
                                      dbSet set) {
        new AsyncTask<dbSet, Void, Void>() {

            @Override
            protected Void doInBackground(dbSet... sets) {
                db.dao().updateSet(sets[0]);
                dbWorkout workout = db.dao().getWorkout(sets[0].getWorkoutId());
                updateData(liveData, liveLaps, workout);
                return null;
            }
        }.execute(set);
    }

    public void shareWorkout() {
        List<SetData> data = setData.getValue();

        AlertDialog.Builder notesDialog = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getLayoutInflater();
        View notesLayout = inflater.inflate(R.layout.dialog_share, null);

        LinearLayout dialogContainer = notesLayout.findViewById(R.id.dialog);

        int numLines = 0;
        final EditText notes = (EditText) dialogContainer.findViewById(R.id.notes);
        SpannableStringBuilder lines = new SpannableStringBuilder();

        /* Legend for swim, kick, drill */
        SpannableStringBuilder strSwim = new SpannableStringBuilder("Swim");
        SpannableStringBuilder strKick = new SpannableStringBuilder("Kick");
        strKick.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableStringBuilder strDrill = new SpannableStringBuilder("Drill");
        strDrill.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 0, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        lines.append("Key:\t\t");
        lines.append(strSwim);
        lines.append("\t");
        lines.append(strKick);
        lines.append("\t");
        lines.append(strDrill);
        lines.append("\n\n");

        /* Loop through and print out each set component in a descriptive way */

        for (SetData set : data) {
            if (set.setComponents.size() > 0) {
                lines.append(set.set.toString());
                numLines++;
                for (dbSetComponent setComponent : set.setComponents) {
                    lines.append("\n\t");
                    lines.append(setComponent.toString());
                    lines.append(" on ");
                    lines.append(setComponent.intervalToString());
                    lines.append(" ");
                    lines.append(setComponent.deltaToString());
                    numLines++;
                    if (setComponent.getStroke() == Stroke.MIXED) {
                        LapDescriber description = new LapDescriber(set.laps.get(setComponent.getId()));
                        lines.append(description.toText());
                        numLines += setComponent.getReps();
                    }
                }
                lines.append("\n\n");
                numLines+= 2;
            }
        }

        notes.setLines(numLines);
        notes.setText(lines);

        notesDialog.setTitle(R.string.workoutShareTitle);

        final String headerText = getString(R.string.workoutShareHeader);

        notesDialog.setPositiveButton(R.string.workoutSharePositive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/html");
                intent.putExtra(Intent.EXTRA_SUBJECT, "Swim workout");
                intent.putExtra(Intent.EXTRA_TEXT, headerText + notes.getText());

                startActivity(Intent.createChooser(intent, "Send Email"));
            }
        });

        notesDialog.setNegativeButton(R.string.workoutShareNegative, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        notesDialog.setView(notesLayout);
        AlertDialog dialogDate = notesDialog.create();
        dialogDate.show();
    }

    public void notesWorkout() {
        final dbWorkout workout = model.getSelected().getValue();

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
                execUpdateWorkout(workout);
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

    public void scheduleWorkout() {
        final dbWorkout workout = model.getSelected().getValue();

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
                execUpdateWorkout(workout);
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

    private static void execUpdateWorkout(final dbWorkout workout) {
        new AsyncTask<dbWorkout, Void, Void>() {

            @Override
            protected Void doInBackground(dbWorkout... workouts) {
                db.dao().updateWorkout(workouts[0]);
                return null;
            }
        }.execute(workout);
    }

    public void deleteWorkout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.deleteWorkoutDialog);
        builder.setPositiveButton(R.string.deleteWorkoutPositive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                execDeleteWorkout(model.getSelected().getValue());
                getFragmentManager().popBackStack();
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

    private static void execDeleteWorkout(final dbWorkout workout) {
        new AsyncTask<dbWorkout, Void, Void>() {

            @Override
            protected Void doInBackground(dbWorkout... workout) {
                db.dao().deleteWorkout(workout[0]);
                return null;
            }
        }.execute(workout);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_workout, container, false);
        Context context = view.getContext();

        // progress
        ProgressBar progressBar = view.findViewById(R.id.load_progress);
        progressBar.setVisibility(View.VISIBLE);

        // main layout to add sections to
        LinearLayout layout = view.findViewById(R.id.headerLayout);

        int height = Resources.getSystem().getDisplayMetrics().heightPixels;
        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height / 4, 1);
        sectionParams.setMargins(0, 16, 0, 0);

        // set up workout header
        View headerView = inflater.inflate(R.layout.fragment_dashboard_workouts, container, false);
        WorkoutChart workoutChart = headerView.findViewById(R.id.chart);
        workoutChart.setLayoutParams(sectionParams);

        LinearLayout actionsLayout = headerView.findViewById(R.id.workoutActions);

        TextView actionNotes = headerView.findViewById(R.id.actionNotes);
        actionNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                notesWorkout();
            }
        });

        TextView actionSchedule = headerView.findViewById(R.id.actionDate);
        actionSchedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scheduleWorkout();
            }
        });

        TextView actionDelete = headerView.findViewById(R.id.actionDelete);
        actionDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteWorkout();
            }
        });

        if (!editMode)
            actionsLayout.setVisibility(View.INVISIBLE);

        layout.addView(headerView);

        return view;
    }

    public void updateHeader(dbWorkout workout) {
        TextView workoutHeader = getView().findViewById(R.id.workoutHeader);
        String message;
        if (workout.isCompleted()) {
            workoutHeader.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_swimming_man, 0, 0, 0);
            message = "   Workout swam on ";
        } else {
            workoutHeader.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_schedule_24px, 0, 0, 0);
            message = "   Workout scheduled for ";
        }
        message +=  workout.getDate("MMMM dd, yyyy");
        workoutHeader.setText(message);
    }

    public void updateChart(List<WorkoutViewModel.WorkoutRow> data) {
        WorkoutChart workoutChart = getView().findViewById(R.id.chart);
        workoutChart.setData(data);
        workoutChart.invalidate();

        ProgressBar progressBar = getView().findViewById(R.id.load_progress);
        progressBar.setVisibility(View.GONE);
    }

    public void updateActions() {
        LinearLayout actionsLayout = getView().findViewById(R.id.workoutActions);
        if (editMode)
            actionsLayout.setVisibility(View.VISIBLE);
        else
            actionsLayout.setVisibility(View.INVISIBLE);
    }

    public void updateSets(List<SetData> sets) {
        int[] colors = new int[3];
        colors[dbSet.SET_TYPE_WARMUP] = R.color.colorWarmup;
        colors[dbSet.SET_TYPE_MAIN] = R.color.colorMain;
        colors[dbSet.SET_TYPE_COOLDOWN] = R.color.colorCoolDown;

        // need resolved colors to pass to non-contextual adapters
        int[] cardColors = new int[3];
        cardColors[dbSet.SET_TYPE_WARMUP] = ContextCompat.getColor(getContext(), R.color.colorWarmupCard);
        cardColors[dbSet.SET_TYPE_MAIN] = ContextCompat.getColor(getContext(), R.color.colorMainCard);
        cardColors[dbSet.SET_TYPE_COOLDOWN] = ContextCompat.getColor(getContext(), R.color.colorCoolDownCard);

        int[] titles = new int[3];
        titles[dbSet.SET_TYPE_WARMUP] = R.string.setTypeWarmup;
        titles[dbSet.SET_TYPE_MAIN] = R.string.setTypeMain;
        titles[dbSet.SET_TYPE_COOLDOWN] = R.string.setTypeCoolDown;

        //List<dbSet> sets = mDb.dao().getSets(currentWorkout.getId());
        int height = Resources.getSystem().getDisplayMetrics().heightPixels;
        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height / 5, 1);
        sectionParams.setMargins(0, 16, 0, 0);

        LinearLayout setsLayout = getView().findViewById(R.id.setsLayout);
        setsLayout.removeAllViews();

        for (final SetData set : sets) {
            final dbSet iSet = set.set;
            final long setId = iSet.getId();
            int setType = iSet.getSetType();

            View setView = getLayoutInflater().inflate(R.layout.fragment_workout_group, null);
            setView.setLayoutParams(sectionParams);
            setView.setId((int) setId);
            setsLayout.addView(setView);

            TextView headerText = (TextView) setView.findViewById(R.id.labelSetType);
            headerText.setText(titles[setType]);
            LinearLayout headerRow = (LinearLayout) setView.findViewById(R.id.headerRow);
            headerRow.setBackgroundResource(colors[setType]);

            ImageView buttonAddToSet = (ImageView) setView.findViewById(R.id.buttonAddToSet);
            ImageView buttonDeleteSet = (ImageView) setView.findViewById(R.id.buttonDeleteSet);

            if (editMode) {
                buttonAddToSet.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mListener.editSetComponent(setId, -1);
                    }
                });

                buttonDeleteSet.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        deleteSet(iSet);
                    }
                });
            } else {
                buttonAddToSet.setVisibility(View.GONE);
                buttonDeleteSet.setVisibility(View.GONE);
            }

            final TextView valueRounds = (TextView) setView.findViewById(R.id.valueRounds);
            int setRounds = iSet.getRounds();
            valueRounds.setText(String.valueOf(setRounds));

            SeekBar seekRounds = (SeekBar) setView.findViewById(R.id.seekRounds);
            // SeekBar requires unique IDs
            seekRounds.setId((int) setId);
            seekRounds.setProgress(setRounds - 1);

            seekRounds.setEnabled(editMode);

            seekRounds.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    iSet.setRounds(i + 1);
                    updateSet(iSet);
                    valueRounds.setText(String.valueOf(iSet.getRounds()));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            // set up adapter with list of Sets
            int cardColor = cardColors[setType];
            RecyclerView listSetComponents = (RecyclerView) setView.findViewById(R.id.listSetComponents);
            LinearLayoutManager listSetsLm = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
            listSetComponents.setLayoutManager(listSetsLm);

            SetComponentAdapter newAdapter = new SetComponentAdapter(set.setComponents, cardColor, editMode, mListener);
            listSetComponents.setAdapter(newAdapter);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mListener = (ViewWorkoutListener) this;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.invalidateOptionsMenu();
        // update sets to redraw edit buttons
        updateSets(setData.getValue());
        updateActions();
    }

    public void editSetComponent(long setId, long setComponentId) {
        //DialogFragment newFragment = (DialogFragment) new RecordWorkoutDialog();
        DialogFragment newFragment = (DialogFragment) new LapsDialog();
        Bundle bundle = new Bundle();
        bundle.putLong("setId", setId);
        bundle.putLong("setComponentId", setComponentId);
        newFragment.setArguments(bundle);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getActivity().getSupportFragmentManager(), "workoutEntry");
    }

    public void deleteSetComponent(final dbSetComponent setComponent) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.deleteSetComponentDialog);
        builder.setPositiveButton(R.string.deleteSetComponentPositive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                execDeleteSetComponent(setData, lapData, setComponent);
            }
        });
        builder.setNegativeButton(R.string.deleteSetComponentNegative, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private static void execDeleteSetComponent(final MutableLiveData<List<SetData>> liveData, final MutableLiveData<List<WorkoutViewModel.WorkoutRow>> liveLaps,
                                      dbSetComponent setComponent) {
        new AsyncTask<dbSetComponent, Void, dbWorkout>() {

            @Override
            protected dbWorkout doInBackground(dbSetComponent... setComponent) {
                dbSet set = db.dao().getSet(setComponent[0].getSetId());
                dbWorkout workout = db.dao().getWorkout(set.getWorkoutId());
                db.dao().deleteSetComponent(setComponent[0]);
                return workout;
            }

            @Override
            protected void onPostExecute(dbWorkout workout) {
                updateData(liveData, liveLaps, workout);
            }
        }.execute(setComponent);
    }

    public void updateSetComponents() {
        List<WorkoutViewModel.WorkoutRow> laps = new ArrayList<>(lapData.getValue());
        lapData.postValue(laps);
        dbWorkout workout = model.getSelected().getValue();
        updateData(setData, lapData, workout);
    }
}
