package org.helioviewer.jhv.timelines.band;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

class BandCacheAll implements BandCache {

    private final List<DateValue> datevals = new ArrayList<>();
    private boolean hasData;

    @Override
    public boolean hasData() {
        return hasData;
    }

    @Override
    public void addToCache(float[] values, long[] dates) {
        int len = values.length;
        if (len > 0) {
            hasData = true;
        }
        int MAX_SIZE = 10000;
        if (datevals.size() >= MAX_SIZE) {
            return;
        }

        for (int i = 0; i < len; i++) {
            if (datevals.size() >= MAX_SIZE)
                break;
            datevals.add(new DateValue(dates[i], values[i]));
        }
        Collections.sort(datevals);
    }

    @Override
    public float[] getBounds(long start, long end) {
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;

        for (DateValue dv : datevals) {
            if (dv.value != Float.MIN_VALUE && start <= dv.milli && dv.milli <= end) {
                min = Math.min(dv.value, min);
                max = Math.max(dv.value, max);
            }
        }
        return new float[]{min, max};
    }

    @Override
    public List<List<DateValue>> getValues(double graphWidth, long start, long end) {
        List<DateValue> list = new ArrayList<>();
        for (DateValue dv : datevals) {
            if (dv.value != Float.MIN_VALUE && start <= dv.milli && dv.milli <= end) {
                list.add(dv);
            }
        }
        return Collections.singletonList(list);
    }

    @Override
    public float getValue(long ts) {
        return Float.MIN_VALUE;
    }

    @Override
    public void serialize(JSONObject jo, double f) {
        JSONArray ja = new JSONArray();
        for (DateValue dv : datevals)
            dv.serialize(ja, f);
        jo.put("data", ja);
    }

}
