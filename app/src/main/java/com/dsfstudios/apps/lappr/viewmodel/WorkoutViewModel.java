package com.dsfstudios.apps.lappr.viewmodel;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.arch.persistence.room.Embedded;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.dsfstudios.apps.lappr.CustomMutableLiveData;
import com.dsfstudios.apps.lappr.database.AppDatabase;
import com.dsfstudios.apps.lappr.database.entities.dbLap;
import com.dsfstudios.apps.lappr.database.entities.dbRep;
import com.dsfstudios.apps.lappr.database.entities.dbSet;
import com.dsfstudios.apps.lappr.database.entities.dbSetComponent;
import com.dsfstudios.apps.lappr.database.entities.dbSwimmer;
import com.dsfstudios.apps.lappr.database.entities.dbWorkout;
import com.dsfstudios.apps.lappr.ui.Dashboard;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkoutViewModel extends ViewModel {

    private static AppDatabase mDb;
    private long mSwimmerId;

    // keys for dashboard sections
    private static final int UPCOMING_WORKOUTS = 0;
    private static final int OVERDUE_WORKOUTS = 1;
    private static final int RECENT_WORKOUTS = 2;

    // refresh quantities for dashboard
    private static final int UPCOMING_NUM = 3;
    private static final int OVERDUE_NUM = 3;
    private static final int RECENT_NUM = 5;

    // load/refresh commands for dashboard
    private static final int INIT = 0;
    private static final int LOAD_OVERDUE = 1;
    private static final int LOAD_UPCOMING = 2;
    private static final int LOAD_RECENT = 3;

    // currently selected workout on View Workout screen
    private CustomMutableLiveData<dbWorkout> currentWorkout = new CustomMutableLiveData<>();

    // data for currently selected workout
    private MutableLiveData<List<WorkoutRow>> currentData = new MutableLiveData<>();

    // all workouts currently visible on dashboard by section
    private MutableLiveData<Map<Integer, List<WorkoutDataSet>>> dashboardData = new MutableLiveData<>();

    public WorkoutViewModel() {

    }

    public WorkoutViewModel(AppDatabase db) {
        mDb = db;
    }

    public static class WorkoutRow {
        public long date;
        public boolean completed;
        public long setId;
        public int rounds;
        @Embedded
        public dbLap lap;
    }

    public static class WorkoutDataSet {
        public dbWorkout workout;
        public List<WorkoutRow> rows;
    }

    public void setSwimmerId(long swimmerId) {
        mSwimmerId = swimmerId;
    }

    public void selectWorkout(long workoutId) {
        execSelectWorkout(currentWorkout, currentData, workoutId);
    }

    private static void execSelectWorkout(final CustomMutableLiveData<dbWorkout> workout,
                                          final MutableLiveData<List<WorkoutRow>> rows, long workoutId) {
        new AsyncTask<Long, Void, Void>() {

            @Override
            protected Void doInBackground(Long... workoutIds) {
                dbWorkout selectedWorkout = mDb.dao().getWorkout(workoutIds[0]);
                List<WorkoutRow> selectedData = mDb.dao().getWorkoutData(workoutIds[0]);
                workout.postValue(selectedWorkout);
                rows.postValue(selectedData);
                return null;
            }
        }.execute(workoutId);
    }

    public void addWorkout(Date date, boolean complete) {
        dbWorkout newWorkout = new dbWorkout(mSwimmerId, date.getTime(), complete);
        execAddWorkout(currentWorkout, currentData, newWorkout);
    }

    private static void execAddWorkout(final CustomMutableLiveData<dbWorkout> workout,
                                       final MutableLiveData<List<WorkoutRow>> rows, dbWorkout newWorkout) {
        new AsyncTask<dbWorkout, Void, Void>() {

            @Override
            protected Void doInBackground(dbWorkout... workouts) {
                long workoutId = mDb.dao().addWorkout(workouts[0]);
                mDb.dao().addSet(new dbSet(workoutId, 1, dbSet.SET_TYPE_WARMUP));
                mDb.dao().addSet(new dbSet(workoutId, 1, dbSet.SET_TYPE_MAIN));
                mDb.dao().addSet(new dbSet(workoutId, 1, dbSet.SET_TYPE_COOLDOWN));
                dbWorkout selectedWorkout = mDb.dao().getWorkout(workoutId);
                List<WorkoutRow> selectedData = mDb.dao().getWorkoutData(workoutId);
                workout.postValue(selectedWorkout);
                rows.postValue(selectedData);
                return null;
            }
        }.execute(newWorkout);
    }

    public void cloneWorkout(Date date, boolean complete, long workoutId) {
        dbWorkout newWorkout = new dbWorkout(mSwimmerId, date.getTime(), complete);
        execCloneWorkout(currentWorkout, currentData, newWorkout, workoutId);
    }

    private static void execCloneWorkout(final CustomMutableLiveData<dbWorkout> workout,
                                       final MutableLiveData<List<WorkoutRow>> rows, dbWorkout newWorkout, final long cloneId) {
        new AsyncTask<dbWorkout, Void, Void>() {

            @Override
            protected Void doInBackground(dbWorkout... workouts) {
                long workoutId = mDb.dao().addWorkout(workouts[0]);
                List<dbSet> sets = mDb.dao().getSets(cloneId);
                for (dbSet set : sets) {
                    dbSet newSet = new dbSet(workoutId, set);
                    long setId = mDb.dao().addSet(newSet);
                    List<dbSetComponent> setComponents = mDb.dao().getSetComponents(set.getId());
                    for (dbSetComponent setComponent : setComponents) {
                        dbSetComponent newSetComponent = new dbSetComponent(setId, setComponent);
                        long setComponentId = mDb.dao().addSetComponent(newSetComponent);
                        List<dbRep> reps = mDb.dao().getReps(setComponent.getId());
                        for (dbRep rep : reps) {
                            dbRep newRep = new dbRep(setComponentId, rep);
                            long repId = mDb.dao().addRep(newRep);
                            List<dbLap> laps = mDb.dao().getLaps(rep.getId());
                            for (dbLap lap : laps) {
                                dbLap newLap = new dbLap(repId, setComponentId, lap);
                                mDb.dao().addLap(newLap);
                            }
                        }
                    }
                }

                dbWorkout selectedWorkout = mDb.dao().getWorkout(workoutId);
                List<WorkoutRow> selectedData = mDb.dao().getWorkoutData(workoutId);
                workout.postValue(selectedWorkout);
                rows.postValue(selectedData);
                return null;
            }
        }.execute(newWorkout);
    }

    // Dashboard calls

    public void refreshWorkouts() {
        execRefreshWorkouts(dashboardData, INIT, mSwimmerId);
    }

    public void loadMoreWorkouts(int section) {
        int command = INIT;
        if (section == RECENT_WORKOUTS)
            command = LOAD_RECENT;
        else if (section == UPCOMING_WORKOUTS)
            command = LOAD_UPCOMING;
        else if (section == OVERDUE_WORKOUTS)
            command = LOAD_OVERDUE;
        execRefreshWorkouts(dashboardData, command, mSwimmerId);
    }

    public void updateWorkout(dbWorkout workout) {
        execUpdateWorkout(dashboardData, workout);
    }

    private static void execUpdateWorkout(final MutableLiveData<Map<Integer, List<WorkoutDataSet>>> workoutList, final dbWorkout workout) {
        new AsyncTask<dbWorkout, Void, Void>() {

            @Override
            protected Void doInBackground(dbWorkout... workouts) {
                mDb.dao().updateWorkout(workouts[0]);
                long swimmerId = workouts[0].getSwimmerId();
                execRefreshWorkouts(workoutList, INIT, swimmerId);
                return null;
            }
        }.execute(workout);
    }

    public void deleteWorkout(dbWorkout workout) {
        execDeleteWorkout(dashboardData, workout);
    }

    private static void execDeleteWorkout(final MutableLiveData<Map<Integer, List<WorkoutDataSet>>> workoutList, final dbWorkout workout) {
        new AsyncTask<dbWorkout, Void, Void>() {

            @Override
            protected Void doInBackground(dbWorkout... workouts) {
                mDb.dao().deleteWorkout(workouts[0]);
                Map<Integer, List<WorkoutDataSet>> newList = new HashMap<>();
                for (int key : workoutList.getValue().keySet()) {
                    List<WorkoutDataSet> datasets = workoutList.getValue().get(key);
                    for (WorkoutDataSet w : datasets) {
                        if (w != null)
                            if (w.workout == workout) {
                                datasets.remove(w);
                                break;
                            }
                    }
                    newList.put(key, datasets);
                }
                workoutList.postValue(newList);
                return null;
            }
        }.execute(workout);
    }

    private static void execRefreshWorkouts(final MutableLiveData<Map<Integer, List<WorkoutDataSet>>> workouts, final int command, final long swimmerId) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {

                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);

                Calendar upcomingCal = Calendar.getInstance();
                upcomingCal.setTime(cal.getTime());
                upcomingCal.add(Calendar.DATE, 6);
                Calendar recentCal = Calendar.getInstance();
                recentCal.setTime(cal.getTime());
                recentCal.add(Calendar.DATE, -6);

                long upcomingLimit, recentLimit;
                int[] listSizes = new int[3];
                Map<Integer, List<WorkoutDataSet>> current = workouts.getValue();
                Map<Integer, List<dbWorkout>> workoutsQuery = new HashMap<>();

                // get total counts of each type
                int[] sectionCounts = new int[3];
                sectionCounts[OVERDUE_WORKOUTS] = mDb.dao().getWorkoutCounts(swimmerId, false, 0, cal.getTimeInMillis() - 1);
                sectionCounts[RECENT_WORKOUTS] = mDb.dao().getWorkoutCounts(swimmerId, true, 0, 1000 * 60 * 60 * 24 - 1 + cal.getTimeInMillis());
                sectionCounts[UPCOMING_WORKOUTS] = mDb.dao().getWorkoutCounts(swimmerId, false, cal.getTimeInMillis(), Long.MAX_VALUE);

                // if initializing, limit by default date ranges, otherwise load X number of additional workouts to section
                switch(command) {
                    case INIT:
                        upcomingLimit = upcomingCal.getTimeInMillis();
                        recentLimit = recentCal.getTimeInMillis();
                        listSizes[OVERDUE_WORKOUTS] = OVERDUE_NUM;
                        listSizes[RECENT_WORKOUTS] = 999;
                        listSizes[UPCOMING_WORKOUTS] = 999;
                        workoutsQuery.put(OVERDUE_WORKOUTS, mDb.dao().getWorkouts(swimmerId, false, 0, cal.getTimeInMillis() - 1, 0, listSizes[OVERDUE_WORKOUTS]));
                        workoutsQuery.put(UPCOMING_WORKOUTS, mDb.dao().getWorkouts(swimmerId, false, cal.getTimeInMillis(), upcomingLimit, 0, listSizes[UPCOMING_WORKOUTS]));
                        workoutsQuery.put(RECENT_WORKOUTS, mDb.dao().getWorkouts(swimmerId, true, recentLimit, 1000 * 60 * 60 * 24 - 1 + cal.getTimeInMillis(), 0, listSizes[RECENT_WORKOUTS]));
                        break;
                    case LOAD_OVERDUE:
                        listSizes[OVERDUE_WORKOUTS] = current.get(OVERDUE_WORKOUTS).size() + OVERDUE_NUM;
                        workoutsQuery.put(OVERDUE_WORKOUTS, mDb.dao().getWorkouts(swimmerId, false, 0, cal.getTimeInMillis() - 1, 0, listSizes[OVERDUE_WORKOUTS]));
                        break;
                    case LOAD_RECENT:
                        recentLimit = 0;
                        listSizes[RECENT_WORKOUTS] = current.get(RECENT_WORKOUTS).size() + RECENT_NUM;
                        workoutsQuery.put(RECENT_WORKOUTS, mDb.dao().getWorkouts(swimmerId, true, recentLimit, 1000 * 60 * 60 * 24 - 1 + cal.getTimeInMillis(), 0, listSizes[RECENT_WORKOUTS]));
                        break;
                    case LOAD_UPCOMING:
                        upcomingCal.add(Calendar.DATE, 365 - 6);
                        upcomingLimit = upcomingCal.getTimeInMillis();
                        listSizes[UPCOMING_WORKOUTS] = current.get(UPCOMING_WORKOUTS).size() + UPCOMING_NUM;;
                        workoutsQuery.put(UPCOMING_WORKOUTS, mDb.dao().getWorkouts(swimmerId, false, cal.getTimeInMillis(), upcomingLimit, 0, listSizes[UPCOMING_WORKOUTS]));
                        break;
                    default:
                        break;
                }

                int[] workoutTypes = new int[] {UPCOMING_WORKOUTS, OVERDUE_WORKOUTS, RECENT_WORKOUTS};
                Map<Integer, List<WorkoutDataSet>> workoutDataSets = new HashMap<>();

                for (int i = 0; i < workoutTypes.length; i++) {
                    workoutDataSets.put(workoutTypes[i], new ArrayList<WorkoutDataSet>());
                    List<WorkoutDataSet> workoutList = workoutDataSets.get(workoutTypes[i]);
                    if (workoutsQuery.containsKey(workoutTypes[i])) {
                        int count = 0;
                        for (dbWorkout w : workoutsQuery.get(workoutTypes[i])) {
                            WorkoutDataSet toAdd = new WorkoutDataSet();
                            toAdd.workout = w;
                            toAdd.rows = mDb.dao().getWorkoutData(w.getId());
                            workoutList.add(toAdd);
                            count++;
                        }
                        if (count < sectionCounts[workoutTypes[i]])
                            workoutList.add(null);
                    } else {
                        workoutList.addAll(current.get(workoutTypes[i]));
                    }
                }

                workouts.postValue(workoutDataSets);
                return null;
            }
        }.execute();
    }

    public LiveData<dbWorkout> getSelected() {
        return currentWorkout;
    }

    public LiveData<List<WorkoutRow>> getData() {
        return currentData;
    }

    public LiveData<Map<Integer, List<WorkoutDataSet>>> getDashboard() {
        return dashboardData;
    }

    public long getSwimmerId() {
        return mSwimmerId;
    }

    public AppDatabase getDb() {
        return mDb;
    }
}
