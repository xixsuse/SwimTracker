/*
Purpose: Takes in a list of laps, builds a laps description text output, and allows for toText output
 */

package com.dsfstudios.apps.lappr;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;

import com.dsfstudios.apps.lappr.database.entities.dbLap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LapDescriber {

    private static final Stroke[] ORDER_IM = new Stroke[] {Stroke.BACKSTROKE, Stroke.BREASTSTROKE, Stroke.BUTTERFLY, Stroke.FREESTYLE};
    private static final Stroke[] ORDER_REVERSE_IM = new Stroke[] {Stroke.FREESTYLE, Stroke.BUTTERFLY, Stroke.BREASTSTROKE, Stroke.BACKSTROKE};

    private List<LapsDescription> description;

    // Constructor takes in a list of laps and builds a list of lap descriptions
    public LapDescriber (List<dbLap> laps) {
        long repId = -1;
        Map<Long, List<dbLap>> repLaps = new HashMap<>();
        List<Long> repOrder = new ArrayList<>();
        for (dbLap lap : laps) {
            if (lap.getRepId() != repId) {
                repLaps.put(lap.getRepId(), new ArrayList<dbLap>());
                repOrder.add(lap.getRepId());
                repId = lap.getRepId();
            }
            List<dbLap> currentRep = repLaps.get(lap.getRepId());
            currentRep.add(lap);
        }

        this.description = new ArrayList<>();
        for (long descRepId : repOrder) {
            this.description.add(describeLaps(repLaps.get(descRepId)));
        }
    }

    public static class LapsDescription {
        int repeats;
        List<SpannableStringBuilder> strokes;

        LapsDescription(int repeats, List<SpannableStringBuilder> strokes) {
            this.repeats = repeats;
            this.strokes = strokes;
        }

        SpannableStringBuilder toText() {
            SpannableStringBuilder strokeOutput = new SpannableStringBuilder(strokes.get(0));
            for (int i = 1; i < strokes.size(); i++) {
                strokeOutput.append(", ");
                strokeOutput.append(strokes.get(i));
            }
            SpannableStringBuilder result = new SpannableStringBuilder("" + repeats + " x ");
            result.append(strokeOutput);
            return result;
        }
    }

    // Searches the list of laps to determine any patterns and returns a lap description
    private LapsDescription describeLaps(List<dbLap> laps) {
        List<Integer> factors = new ArrayList<>();
        int i = 1;
        int product = i;
        int size = laps.size();
        while (product < size) {
            if (size % i == 0) {
                factors.add(i);
                product *= i;
            }
            i++;
        }

        for (int factor : factors) {
            dbLap[] subList = new dbLap[factor];
            subList = laps.subList(0, factor).toArray(subList);
            List<SpannableStringBuilder> factorOrder = matchesOrder(laps, subList);
            if (factorOrder != null) {
                // check if subList equals IM or reverse IM
                return new LapsDescription(size / factor, factorOrder);
            }
        }

        return new LapsDescription(1, matchesOrder(laps, laps.toArray(new dbLap[laps.size()])));
    }

    private List<SpannableStringBuilder> matchesOrder(List<dbLap> lapsOrig, dbLap[] lapOrder) {
        Stroke[] matchOrder = new Stroke[lapsOrig.size()];
        List<SpannableStringBuilder> order = new ArrayList<>();
        if (lapsOrig.size() < lapOrder.length)
            return null;
        //int lapsPerStroke = lapsOrig.size() / lapOrder.length;
        for (int i = 0; i < lapsOrig.size(); i++) {
            if (lapsOrig.get(i).getStroke() == lapOrder[i % lapOrder.length].getStroke()
                    && lapsOrig.get(i).getType() == lapOrder[i % lapOrder.length].getType()) {
                matchOrder[i] = lapsOrig.get(i).getStroke();
                SpannableStringBuilder current = new SpannableStringBuilder(lapsOrig.get(i).getStroke().strokeAbbrev());
                switch(lapsOrig.get(i).getType()) {
                    case SetComponentEditor.TYPE_KICK:
                        current.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, current.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case SetComponentEditor.TYPE_DRILL:
                        current.setSpan(new android.text.style.StyleSpan(Typeface.ITALIC), 0, current.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    default:
                        break;
                }
                order.add(current);
            } else {
                return null;
            }
        }
        return order.subList(0, lapOrder.length);
    }

    public SpannableStringBuilder toText() {
        SpannableStringBuilder result = new SpannableStringBuilder();
        int repNum = 1;
        for (LapsDescription rep : this.description) {
            result.append("\n\t\tRep ");
            result.append(""+repNum);
            result.append(": ");
            result.append(rep.toText());
            repNum++;
        }
        return result;
    }
}
