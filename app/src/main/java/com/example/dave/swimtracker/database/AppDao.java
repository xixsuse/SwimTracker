package com.example.dave.swimtracker.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.TypeConverters;
import android.arch.persistence.room.Update;

import com.example.dave.swimtracker.database.entities.*;
import com.example.dave.swimtracker.viewmodel.WorkoutViewModel;

import java.util.List;

@Dao
public interface AppDao {
    @Query("select * from dbswimmer where id = :id")
    dbSwimmer getSwimmer(long id);

    @Query("select * from dbSwimmer where firebaseUid = :id")
    dbSwimmer getSwimmerByUid(String id);

    @Query("select * from dbworkout where id = :id")
    dbWorkout getWorkout(long id);

    @Query("select * from dbWorkout")
    List<dbWorkout> getWorkouts();

    @Query("select * from dbworkout where swimmerId = :swimmerId")
    List<dbWorkout> getWorkouts(long swimmerId);

    // shows workouts in ascending order for incompletes, descending for completes
    @Query("select * from dbworkout where swimmerId = :swimmerId and completed = :completed order by date * :completed * -2 + 1, id asc limit :num")
    List<dbWorkout> getWorkouts(long swimmerId, boolean completed, int num);

    @Query("select * from dbWorkout where swimmerId = :swimmerId and completed = :completed and date >= :startDate and date <= :endDate order by " +
            "date * :completed * -2 + 1, date asc, id asc limit :start, :num")
    List<dbWorkout> getWorkouts(long swimmerId, boolean completed, long startDate, long endDate, int start, int num);

    @Query("select count(*) from dbWorkout where swimmerId = :swimmerId and completed = :completed and date >= :startDate and date <= :endDate order by " +
            "date * :completed * -2 + 1, date asc, id asc")
    int getWorkoutCounts(long swimmerId, boolean completed, long startDate, long endDate);

    @Query("select * from dbSet where workoutId = :workoutId order by setType asc, id asc")
    List<dbSet> getSets(long workoutId);

    @Query("select * from dbSetComponent where setId = :setId order by id asc")
    List<dbSetComponent> getSetComponents(long setId);

    @Query("select * from dbSetComponent where id = :setComponentId order by id asc")
    dbSetComponent getSetComponent(long setComponentId);

    @Query("select * from dbSet where id = :setId")
    dbSet getSet(long setId);

    @Query("select * from dbRep where setComponentId = :setComponentId order by id asc")
    List<dbRep> getReps(long setComponentId);

    @Query("select * from dbLap where repId = :repId order by `order` asc")
    List<dbLap> getLaps(long repId);

    @Query("select dbLap.* from dbLap left join dbRep on dbLap.repId = dbRep.id where dbRep.setComponentId = :setComponentId order by " +
            "dbRep.id asc, dbLap.id asc")
    List<dbLap> getSetComponentData(long setComponentId);

    @Query("select dbLap.* from dbLap left join dbRep on dbLap.repId = dbRep.id left join dbSetComponent on dbRep.setComponentId = dbSetComponent.id " +
            "where dbSetComponent.setId = :setId order by dbSetComponent.id asc, dbRep.id asc, dbLap.id asc")
    List<dbLap> getLapsForSet(long setId);

    @TypeConverters(StrokeConverter.class)
    @Query("select dbWorkout.date, dbWorkout.completed, dbSetComponent.setId, dbSet.rounds, dbLap.* from dbLap left join dbRep on dbLap.repId = dbRep.id left join dbSetComponent on dbRep.setComponentId = dbSetComponent.id " +
            "left join dbSet on dbSetComponent.setId = dbSet.id left join dbWorkout on dbSet.workoutId = dbWorkout.id where dbWorkout.id = :workoutId " +
            "order by dbSet.setType asc, dbSet.id asc, dbSetComponent.id asc, dbRep.id, dbLap.`order` asc")
    List<WorkoutViewModel.WorkoutRow> getWorkoutData(long workoutId);

    @Update
    void updateWorkout(dbWorkout workout);

    @Update
    void updateSetComponent(dbSetComponent setComponent);

    @Update
    void updateRep(dbRep rep);

    @Update
    void updateSet(dbSet set);

    @Update
    void updateReps(List<dbRep> reps);

    @Update
    void updateLap(dbLap lap);

    @Update
    int updateLaps(List<dbLap> laps);

    @Insert
    long addSwimmer(dbSwimmer swimmer);

    @Insert
    long addWorkout(dbWorkout workout);

    @Insert
    long addSet(dbSet set);

    @Insert
    long addSetComponent(dbSetComponent setComponent);

    @Insert
    long addRep(dbRep rep);

    @Insert
    long addLap(dbLap lap);

    @Delete
    void deleteWorkout(dbWorkout workout);

    @Delete
    void deleteSet(dbSet set);

    @Delete
    void deleteRep(dbRep rep);

    @Delete
    void deleteLap(dbLap lap);

    @Delete
    void deleteSetComponent(dbSetComponent setComponent);

}
