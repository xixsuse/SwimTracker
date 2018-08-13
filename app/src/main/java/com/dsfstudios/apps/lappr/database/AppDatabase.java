package com.dsfstudios.apps.lappr.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.os.AsyncTask;

import com.dsfstudios.apps.lappr.database.entities.*;

@Database(entities = {dbSwimmer.class, dbWorkout.class, dbSet.class, dbSetComponent.class, dbRep.class, dbLap.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase db;
    public abstract AppDao dao();

    public static AppDatabase getAppDatabase(Context context) {
        if (db == null) {
            db = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "swimTracker").build();
        }
        return db;
    }

    public static void populateAsync(final AppDatabase db) {
        PopulateDbAsync task = new PopulateDbAsync(db);
        task.execute();
    }

    public static void populateSync(final AppDatabase db) {
        populateWithTestData(db);
    }

    private static void populateWithTestData(AppDatabase db) {
        long swimmerId = db.dao().addSwimmer(new dbSwimmer("a","Dave", 1, 3, 0));
        long workoutId = db.dao().addWorkout(new dbWorkout(swimmerId, 1, true));
        long setId = db.dao().addSet(new dbSet(workoutId, 1, 0));
    }

    private static class PopulateDbAsync extends AsyncTask<Void, Void, Void> {

        private final AppDatabase mDb;

        PopulateDbAsync(AppDatabase db) {
            mDb = db;
        }

        @Override
        protected Void doInBackground(final Void... params) {
            populateWithTestData(mDb);
            return null;
        }

    }

    public static void destroyInstance() {
        db = null;
    }
}

