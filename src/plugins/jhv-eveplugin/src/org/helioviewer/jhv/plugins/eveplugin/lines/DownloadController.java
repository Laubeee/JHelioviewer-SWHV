package org.helioviewer.jhv.plugins.eveplugin.lines;

import java.awt.EventQueue;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;

import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DownloadController {

    private static final int DOWNLOADER_MAX_DAYS_PER_BLOCK = 21;

    private static final DownloadController singletonInstance = new DownloadController();

    private static final HashMap<Band, ArrayList<Interval>> downloadMap = new HashMap<Band, ArrayList<Interval>>();
    private static final HashMap<Band, List<Future<?>>> futureJobs = new HashMap<Band, List<Future<?>>>();

    public static DownloadController getSingletonInstance() {
        return singletonInstance;
    }

    public void updateBand(Band band, long start, long end) {
        List<Interval> missingIntervalsNoExtend = band.getMissingDaysInInterval(start, end);
        if (!missingIntervalsNoExtend.isEmpty()) {
            Interval realQueryInterval = extendQueryInterval(start, end);
            ArrayList<Interval> intervals = getIntervals(band, realQueryInterval);

            if (intervals == null) {
                return;
            }

            int n = intervals.size();
            if (n == 0) {
                fireDownloadStarted(band);
                return;
            }

            DownloadThread[] jobs = new DownloadThread[n];

            int i = 0;
            for (Interval interval : intervals) {
                jobs[i] = new DownloadThread(band, interval);
                ++i;
            }
            addFutureJobs(addDownloads(jobs), band);
            fireDownloadStarted(band);
        }
    }

    private void addFutureJobs(List<Future<?>> newFutureJobs, Band band) {
        List<Future<?>> fj = new ArrayList<Future<?>>();
        if (futureJobs.containsKey(band)) {
            fj = futureJobs.get(band);
        }
        fj.addAll(newFutureJobs);
        futureJobs.put(band, fj);
    }

    private Interval extendQueryInterval(long start, long end) {
        return new Interval(start - 7 * TimeUtils.DAY_IN_MILLIS, end + 7 * TimeUtils.DAY_IN_MILLIS);
    }

    private ArrayList<Interval> getIntervals(Band band, Interval queryInterval) {
        List<Interval> missingIntervals = band.addRequest(band, queryInterval);
        if (missingIntervals.isEmpty()) {
            return null;
        }

        ArrayList<Interval> intervals = new ArrayList<Interval>();
        for (Interval i : missingIntervals) {
            intervals.addAll(Interval.splitInterval(i, DOWNLOADER_MAX_DAYS_PER_BLOCK));
        }

        return intervals;
    }

    public void stopDownloads(Band band) {
        ArrayList<Interval> list = downloadMap.get(band);
        if (list == null) {
            return;
        }
        if (list.isEmpty()) {
            downloadMap.remove(band);
        }
        List<Future<?>> fjs = futureJobs.get(band);
        for (Future<?> fj : fjs) {
            fj.cancel(true);
        }
        futureJobs.remove(band);
        fireDownloadFinished(band);
    }

    public boolean isDownloadActive(Band band) {
        ArrayList<Interval> list = downloadMap.get(band);
        return list != null && !list.isEmpty();
    }

    private void fireDownloadStarted(Band band) {
        EVEPlugin.ldsm.downloadStarted(band);
    }

    private void fireDownloadFinished(Band band) {
        EVEPlugin.ldsm.downloadFinished(band);
    }

    private List<Future<?>> addDownloads(DownloadThread[] jobs) {
        List<Future<?>> futureJobs = new ArrayList<Future<?>>();
        for (DownloadThread job : jobs) {
            Band band = job.getBand();
            Interval interval = job.getInterval();

            ArrayList<Interval> list = downloadMap.get(band);
            if (list == null) {
                list = new ArrayList<Interval>();
            }
            list.add(interval);

            downloadMap.put(band, list);
            futureJobs.add(EVEPlugin.executorService.submit(job));
        }
        return futureJobs;
    }

    private void downloadFinished(final Band band, final Interval interval) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                ArrayList<Interval> list = downloadMap.get(band);
                if (list != null) {
                    list.remove(interval);
                    if (list.isEmpty()) {
                        downloadMap.remove(band);
                    }
                }
                fireDownloadFinished(band);
            }
        });
    }

    private class DownloadThread implements Runnable {

        private final Interval interval;
        private final Band band;

        public DownloadThread(Band band, Interval interval) {
            this.interval = interval;
            this.band = band;
        }

        public Interval getInterval() {
            return interval;
        }

        public Band getBand() {
            return band;
        }

        @Override
        public void run() {
            try {
                requestData();
            } finally {
                downloadFinished(band, interval);
            }
        }

        private void requestData() {
            URL url;

            try {
                url = buildRequestURL(interval, band.getBandType());
            } catch (MalformedURLException e) {
                Log.error("Error creating EVE URL: ", e);
                return;
            }

            try {
                JSONObject json = JSONUtils.getJSONStream(new DownloadStream(url).getInput());

                double multiplier = 1.0;
                if (json.has("multiplier")) {
                    multiplier = json.getDouble("multiplier");
                }

                JSONArray data = json.getJSONArray("data");
                int length = data.length();
                if (length == 0) {
                    return;
                }

                float[] values = new float[length];
                long[] dates = new long[length];

                for (int i = 0; i < length; i++) {
                    JSONArray entry = data.getJSONArray(i);
                    dates[i] = entry.getLong(0) * 1000;
                    values[i] = (float) (entry.getDouble(1) * multiplier);
                }

                addDataToCache(band, values, dates);
            } catch (JSONException e) {
                Log.error("Error Parsing the EVE Response ", e);
            } catch (IOException e) {
                Log.error("Error Parsing the EVE Response ", e);
            }
        }

        private URL buildRequestURL(Interval interval, BandType type) throws MalformedURLException {
            String urlf = type.getBaseURL() + "start_date=%s&end_date=%s&timeline=%s&data_format=json";
            String url = String.format(urlf, TimeUtils.dateFormat.format(interval.start), TimeUtils.dateFormat.format(interval.end), type.getName());
            return new URL(url);
        }

        private void addDataToCache(final Band band, final float[] values, final long[] dates) {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    band.addToCache(values, dates);
                }
            });
        }
    }

}
