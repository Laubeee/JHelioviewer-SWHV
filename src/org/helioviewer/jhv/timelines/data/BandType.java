package org.helioviewer.jhv.timelines.data;

import java.util.HashMap;

import org.helioviewer.jhv.plugins.eve.lines.BandTypeAPI;
import org.json.JSONObject;

public class BandType {

    private String label;
    private String name;
    private String unitLabel;
    private final HashMap<String, Double> warnLevels = new HashMap<>();
    private double min;
    private double max;
    private boolean isLog;
    private String baseURL;
    private DataProvider dataprovider;

    public BandType() {
    }

    public BandType(JSONObject jo) {
        name = jo.optString("name", "unknown");
        label = jo.optString("label", "");
        unitLabel = jo.optString("unitLabel", "");
        min = jo.optDouble("min", 0);
        max = jo.optDouble("max", 1);
        isLog = jo.optBoolean("isLog", false);
        baseURL = jo.optString("baseURL", "");
        dataprovider = BandTypeAPI.eveDataprovider;
    }

    public void serialize(JSONObject jo) {
        JSONObject bandType = new JSONObject();
        bandType.put("name", name);
        bandType.put("label", label);
        bandType.put("unitLabel", unitLabel);
        bandType.put("min", min);
        bandType.put("max", max);
        bandType.put("isLog", isLog);
        bandType.put("baseURL", baseURL);
        jo.put("bandType", bandType);
    }

    @Override
    public String toString() {
        return label;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String _label) {
        label = _label;
    }

    public String getUnitLabel() {
        return unitLabel;
    }

    public void setUnitLabel(String _unitLabel) {
        unitLabel = _unitLabel;
    }

    public HashMap<String, Double> getWarnLevels() {
        return warnLevels;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double _min) {
        min = _min;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double _max) {
        max = _max;
    }

    public void setName(String _name) {
        name = _name;
    }

    public String getName() {
        return name;
    }

    public void setScale(String scale) {
        isLog = scale.equals("logarithmic");
    }

    public boolean isLogScale() {
        return isLog;
    }

    public void setBaseURL(String _baseURL) {
        baseURL = _baseURL;
    }

    public String getBaseURL() {
        return baseURL;
    }

    public DataProvider getDataprovider() {
        return dataprovider;
    }

    public void setDataprovider(DataProvider _dataprovider) {
        dataprovider = _dataprovider;
    }

}
