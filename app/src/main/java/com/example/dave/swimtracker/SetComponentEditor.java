/*
Loads the details of a set component to the UI, accepts user changes, and provides a save functionality to commit those changes
 */

package com.example.dave.swimtracker;

import com.example.dave.swimtracker.database.AppDatabase;
import com.example.dave.swimtracker.database.entities.dbLap;
import com.example.dave.swimtracker.database.entities.dbRep;
import com.example.dave.swimtracker.database.entities.dbSetComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SetComponentEditor {

    private static final int DEFAULT_LAP_LENGTH = 25;

    public static final int TYPE_MIXED = -1;
    public static final int TYPE_SWIM = 0;
    public static final int TYPE_KICK = 1;
    public static final int TYPE_DRILL = 2;

    public static final int PACE_EZ = 0;
    public static final int PACE_MOD = 1;
    public static final int PACE_FAST = 2;
    public static final int PACE_SPRINT = 3;

    public static final int DEFAULT_REPS = 1;
    public static final int DEFAULT_DISTANCE = 100;
    public static final Stroke DEFAULT_STROKE = Stroke.FREESTYLE;
    public static final int DEFAULT_TYPE = TYPE_SWIM;
    public static final int DEFAULT_PACE = PACE_MOD;

    public static final int TIME_INTERVAL = 0;
    public static final int REST_INTERVAL = 1;
    public static final int TOTAL_INTERVAL = 2;
    public static final int NO_INTERVAL = 3;

    public static final int DEFAULT_INTERVAL_PER_100 = 90;
    public static final int DEFAULT_REST_INTERVAL = 10;

    public static final String[] options_sec = new String[] {"00","05","10","15","20","25","30","35","40","45","50","55"};
    public static final int max_sec = options_sec.length - 1;
    private static final int[] options_delta = new int[] {-30, -25, -20, -15, -10, -5, 0, 5, 10, 15, 20, 25, 30};
    public static final int max_min = 60;

    private AppDatabase mDb;
    private dbSetComponent setComponent;
    private long setComponentId;
    private List<dbRep> reps;
    private Map<dbRep, List<dbLap>> laps;

    private int intervalType;
    private int min;
    private int sec;
    private int delta;

    public int numReps;
    private int type;
    public int distance;
    private int pace;
    private Stroke stroke;

    // use getters for initial fetch/setters within editor not saving
    // save commits changes

    public SetComponentEditor(AppDatabase db) {
        this.mDb = db;
    }

    public void init(long setComponentId, long setId) {

        if (setComponentId == -1) {
            setComponent = new dbSetComponent(setId, 0, 90, 0);
            this.setComponentId = mDb.dao().addSetComponent(setComponent);
        } else
            this.setComponentId = setComponentId;
        this.setComponent = mDb.dao().getSetComponent(this.setComponentId);

        this.reps = mDb.dao().getReps(setComponentId);
        this.laps =  new HashMap<>();
        for (dbRep rep : reps) {
            laps.put(rep, mDb.dao().getLaps(rep.getId()));
        }

        this.intervalType = setComponent.getIntervalType();
        this.min = (int) setComponent.getBaseInterval() / 60;
        this.sec = (int) setComponent.getBaseInterval() % 60;
        this.delta = (int) setComponent.getDeltaInterval();

        // fields below get current value and are set by user
        this.numReps = this.getNumReps();
        this.type = this.getType();
        this.distance = this.getDistance();
        this.pace = this.getPace();
        this.stroke = this.getStroke();
    }

    public long save() {
        int totalTime = this.min * 60 + this.sec;
        int existingReps = this.laps.size();
        int newReps = this.numReps;

        // should store calculated reps, distance, stroke in set component for easy access in adapter
        setComponent.setBaseInterval(totalTime);
        setComponent.setIntervalType(intervalType);
        if (intervalType == TIME_INTERVAL || intervalType == REST_INTERVAL)
            setComponent.setDeltaInterval(delta);
        else
            setComponent.setDeltaInterval(0);
        setComponent.setReps(newReps);
        setComponent.setDistance(distance);
        setComponent.setStroke(stroke);
        setComponent.setType(type);
        mDb.dao().updateSetComponent(setComponent);

        List<dbRep> repsToRemove = new ArrayList<>();
        for (int rep = 0; rep < Math.max(newReps, existingReps); rep++) {
            float repTime = totalTime + rep * delta;
            switch(intervalType) {
                case TIME_INTERVAL:
                    repTime = totalTime + rep * delta;
                    break;
                case REST_INTERVAL:
                    repTime = DEFAULT_INTERVAL_PER_100 * distance / 100 + totalTime + rep * delta;
                    break;
                case TOTAL_INTERVAL:
                    repTime = totalTime / newReps;
                    break;
                case NO_INTERVAL:
                    repTime = DEFAULT_INTERVAL_PER_100 * distance / 100;
                    break;
            }
            dbRep currentRep;
            long repId;
            if (rep < existingReps) {
                // rep initially existed
                currentRep = this.reps.get(rep);
                repId = currentRep.getId();
                if (rep < this.numReps) {
                    // rep should remain
                    currentRep.setPace(this.pace);
                    mDb.dao().updateRep(currentRep);
                } else {
                    // rep should be deleted
                    repsToRemove.add(currentRep);
                    this.laps.remove(currentRep);
                    mDb.dao().deleteRep(currentRep);
                    continue;
                }
            } else {
                // rep needs to be added
                currentRep = new dbRep(this.setComponentId, this.pace);
                this.reps.add(currentRep);
                this.laps.put(currentRep, new ArrayList<dbLap>());
                repId = mDb.dao().addRep(currentRep);
            }

            int existingLaps = this.laps.get(currentRep).size();
            int newLaps = this.distance / DEFAULT_LAP_LENGTH;

            Map<dbRep, List<dbLap>> lapsToRemove = new HashMap<>();
            for (int lap = 0; lap < Math.max(newLaps, existingLaps); lap++) {
                float lapTime = repTime / newLaps;
                dbLap currentLap;

                if (lap < existingLaps) {
                    // lap initially existed
                    currentLap = this.laps.get(currentRep).get(lap);
                    if (lap < newLaps) {
                        // lap should remain
                        currentLap.setDistance(DEFAULT_LAP_LENGTH);
                        currentLap.setStroke(getLapStroke(rep, lap));
                        currentLap.setType(getLapType(rep, lap));
                        currentLap.setSeconds(lapTime);
                        currentLap.setOrder(lap);
                        mDb.dao().updateLap(currentLap);
                    } else {
                        // lap should be deleted
                        if (!lapsToRemove.containsKey(currentRep))
                            lapsToRemove.put(currentRep, new ArrayList<dbLap>());
                        lapsToRemove.get(currentRep).add(currentLap);
                        mDb.dao().deleteLap(currentLap);
                    }
                } else {
                    // add lap
                    // if lap doesn't exist, determine which rep/lap will be copied for Mixed/ambiguous stroke/type
                    int repNum = rep, lapNum = lap;
                    if (rep >= existingReps)
                        repNum = rep % Math.max(1, existingReps);
                    else if (lap >= existingLaps)
                        lapNum = lap % Math.max(1, existingLaps);

                    int lapType;
                    Stroke lapStroke;
                    if (this.stroke == Stroke.MIXED)
                        lapStroke = getLapStroke(repNum, lapNum);
                    else
                        lapStroke = getLapStroke(rep, lap);
                    if (this.type == TYPE_MIXED)
                        lapType = getLapType(repNum, lapNum);
                    else
                        lapType = getLapType(rep, lap);

                    currentLap = new dbLap(repId, setComponentId, DEFAULT_LAP_LENGTH, lap, lapStroke, lapType, lapTime);
                    this.laps.get(currentRep).add(currentLap);
                    mDb.dao().addLap(currentLap);
                }
            }

            // remove deleted reps and laps
            for (dbRep repToRemove : repsToRemove) {
                this.reps.remove(repToRemove);
            }
            for (dbRep lapToRemove : lapsToRemove.keySet()) {
                List<dbLap> laps = lapsToRemove.get(lapToRemove);
                this.laps.get(lapToRemove).removeAll(laps);
            }
        }
        return this.setComponentId;
    }

    public int getNumLapsForRep(int repNum) {
        dbRep rep = this.reps.get(repNum);
        return this.laps.get(rep).size();
    }

    public Stroke getStrokeForLap(int repNum, int lapNum) {
        dbRep rep = this.reps.get(repNum);
        return this.laps.get(rep).get(lapNum).getStroke();
    }

    public Stroke nextStrokeForLap(int repNum, int lapNum) {
        dbRep rep = this.reps.get(repNum);
        dbLap lap = this.laps.get(rep).get(lapNum);
        lap.setStroke(lap.getStroke().nextStroke());
        this.stroke = this.getStroke();
        return lap.getStroke();
    }

    public int getTypeForLap(int repNum, int lapNum) {
        dbRep rep = this.reps.get(repNum);
        return this.laps.get(rep).get(lapNum).getType();
    }

    public int nextTypeForLap(int repNum, int lapNum) {
        dbRep rep = this.reps.get(repNum);
        dbLap lap = this.laps.get(rep).get(lapNum);
        lap.setType((lap.getType() + 1) % 3);
        this.type = this.getType();
        return lap.getType();
    }

    public int getNumReps() {
        if (this.laps.isEmpty())
            return DEFAULT_REPS;
        return this.laps.size();
    }

    public void setNumReps(int numReps) {
        this.numReps = numReps;
    }

    public int getDistance() {
        if (this.laps.isEmpty())
            return DEFAULT_DISTANCE;
        int distance = 0;
        for (dbRep rep : this.laps.keySet()) {
            for (dbLap lap : this.laps.get(rep)) {
                distance += lap.getDistance();
            }
        }
        return distance / this.laps.size();
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public int getPace() {
        if (this.laps.isEmpty())
            return DEFAULT_PACE;
        int totalPace = 0;
        for (dbRep rep : this.laps.keySet()) {
            totalPace += rep.getPace();
        }
        return totalPace / this.laps.size();
    }

    public void setPace(int pace) {
        this.pace = pace;
    }

    public String printPace() {
        switch(this.pace) {
            case PACE_EZ:
                return "EZ/Warm Up";
            case PACE_MOD:
                return "Moderate";
            case PACE_FAST:
                return "Fast";
            case PACE_SPRINT:
                return "Sprint";
            default:
                return "";
        }
    }

    private Stroke getLapStroke(int repNum, int lapNum) {
        Stroke[] IM = new Stroke[] {Stroke.BUTTERFLY, Stroke.BACKSTROKE, Stroke.BREASTSTROKE, Stroke.FREESTYLE};
        Stroke[] reverseIM = new Stroke[] {Stroke.FREESTYLE, Stroke.BREASTSTROKE, Stroke.BACKSTROKE, Stroke.BUTTERFLY};
        int strokeNum;
        switch(this.stroke) {
            case IM:
                strokeNum = (int) (lapNum / ((float) this.distance / (IM.length * DEFAULT_LAP_LENGTH)));
                return IM[strokeNum % IM.length];
            case REVERSE_IM:
                strokeNum = (int) (lapNum / ((float) this.distance / (reverseIM.length * DEFAULT_LAP_LENGTH)));
                return reverseIM[strokeNum % reverseIM.length];
            case IM_ORDER:
                return IM[repNum % IM.length];
            case MIXED:
                dbRep rep = this.reps.get(repNum);
                return this.laps.get(rep).get(lapNum).getStroke();
            default:
                return this.stroke;
        }
    }

    private int getLapType(int repNum, int lapNum) {
        switch (this.type) {
            case TYPE_MIXED:
                dbRep rep = this.reps.get(repNum);
                return this.laps.get(rep).get(lapNum).getType();
            default:
                return this.type;
        }
    }

    public Stroke getStroke() {
        if (this.laps.isEmpty())
            return DEFAULT_STROKE;
        Map<dbRep, List<Stroke>> repStrokes = new HashMap<>();
        for (dbRep rep : this.reps) {
            for (dbLap lap : this.laps.get(rep)) {
                if (!repStrokes.containsKey(rep))
                    repStrokes.put(rep, new ArrayList<Stroke>());
                List<Stroke> strokeList = repStrokes.get(rep);
                strokeList.add(lap.getStroke());
            }
        }

        Stroke[] IM = new Stroke[] {Stroke.BUTTERFLY, Stroke.BACKSTROKE, Stroke.BREASTSTROKE, Stroke.FREESTYLE};
        Stroke[] reverseIM = new Stroke[] {Stroke.FREESTYLE, Stroke.BREASTSTROKE, Stroke.BACKSTROKE, Stroke.BUTTERFLY};

        Stroke[] strokes = new Stroke[repStrokes.size()];
        int repIndex = 0;
        for (dbRep rep : this.reps) {
            List<Stroke> strokeList = repStrokes.get(rep);
            if (matchesOrder(strokeList, IM, false, false))
                strokes[repIndex] = Stroke.IM;
            else if (matchesOrder(strokeList, reverseIM, false, false))
                strokes[repIndex] = Stroke.REVERSE_IM;
            else if (matchesOrder(strokeList, null, true, true))
                strokes[repIndex] = strokeList.get(0);
            else {
                strokes[repIndex] = Stroke.MIXED;
            }
            repIndex++;
        }

        List<Stroke> allStrokes = Arrays.asList(strokes);

        if (matchesOrder(allStrokes, IM, true, false))
            return Stroke.IM_ORDER;
        else if (matchesOrder(allStrokes, null, true, true))
            return allStrokes.get(0);
        else {
            return Stroke.MIXED;
        }
    }

    private boolean matchesOrder(List<Stroke> strokeList, Stroke[] strokeOrder, boolean repeatOrder, boolean sameStroke) {
        if (sameStroke)
            strokeOrder = new Stroke[] {strokeList.get(0)};
        if (strokeList.size() < strokeOrder.length)
            return false;
        int lapsPerStroke;
        if (repeatOrder)
            lapsPerStroke = 1;
        else
            lapsPerStroke = strokeList.size() / strokeOrder.length;
        for (int i = 0; i < strokeList.size(); i++) {
            if (strokeList.get(i) == strokeOrder[(i / lapsPerStroke) % strokeOrder.length]) {
                continue;
            } else {
                return false;
            }
        }
        return true;
    }

    public void setStroke(Stroke stroke) {
        this.stroke = stroke;
    }

    public int getType() {
        if (this.laps.isEmpty())
            return DEFAULT_TYPE;
        int type = TYPE_MIXED;
        for (dbRep rep : this.laps.keySet()) {
            for (dbLap lap : this.laps.get(rep)) {
                if (type == TYPE_MIXED)
                    type = lap.getType();
                else if (lap.getType() != type)
                    return TYPE_MIXED;
            }
        }
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setInterval(int secs) {
        setMin(secs / 60);
        setSec((secs % 60) / 5);
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMin() {
        return this.min;
    }

    public void setSec(int sec) {
        this.sec = Integer.parseInt(options_sec[sec]);
    }

    public int getSec() {
        return sec / 5;
    }

    public void setDelta(int delta) {
        this.delta = options_delta[delta];
    }

    public int getDelta() {
        return delta / 5 + 6;
    }

    public String printDelta() {
        String seconds_text;
        if (Math.abs(delta) < 10) {
            seconds_text = "0" + Math.abs(delta);
        } else {
            seconds_text = "" + Math.abs(delta);
        }

        String message;
        if (delta == 0) {
            message = "Constant Interval (0:00)";
        } else if (delta < 0) {
            message = "Descending Interval (-0:" + seconds_text + ")";
        } else {
            message = "Ascending Interval (+0:" + seconds_text + ")";
        }

        return message;
    }

    public int getIntervalType() {
        return this.intervalType;
    }

    public void setIntervalType(int intervalType) {
        this.intervalType = intervalType;
    }

    public long getSetComponentId () {
        return this.setComponentId;
    }
}
