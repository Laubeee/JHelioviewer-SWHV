package org.helioviewer.jhv.plugins.pfssplugin.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.helioviewer.base.Pair;
import org.helioviewer.base.datetime.TimeUtils;
import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.plugins.pfssplugin.AbolishTask;
import org.helioviewer.jhv.plugins.pfssplugin.PfssSettings;

public class PfssNewDataLoader implements Runnable {

    private static int TIMEOUT_DOWNLOAD_SECONDS = 120;
    private final ScheduledExecutorService pfssPool = Executors.newScheduledThreadPool(6);

    private final Date start;
    private final Date end;
    private final static SortedMap<Integer, ArrayList<Pair<String, Long>>> parsedCache = new TreeMap<Integer, ArrayList<Pair<String, Long>>>();

    public PfssNewDataLoader(Date start, Date end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public void run() {
        if (start != null && end != null && start.before(end)) {
            Thread.currentThread().setName(PfssSettings.THREAD_NAME);

            final Calendar startCal = GregorianCalendar.getInstance();
            startCal.setTime(start);

            final Calendar endCal = GregorianCalendar.getInstance();
            endCal.setTime(new Date(end.getTime() + 31 * 24 * 60 * 60 * 1000));

            int startYear = startCal.get(Calendar.YEAR);
            int startMonth = startCal.get(Calendar.MONTH);

            final int endYear = endCal.get(Calendar.YEAR);
            final int endMonth = endCal.get(Calendar.MONTH);

            do {
                ArrayList<Pair<String, Long>> urls = null;

                try {
                    URL data;
                    Integer cacheKey = startYear * 10000 + startMonth;
                    synchronized (parsedCache) {
                        urls = parsedCache.get(cacheKey);
                    }
                    if (urls == null || urls.isEmpty()) {
                        urls = new ArrayList<Pair<String, Long>>();
                        String m = (startMonth) < 9 ? "0" + (startMonth + 1) : (startMonth + 1) + "";
                        String url = PfssSettings.baseUrl + startYear + "/" + m + "/list.txt";
                        data = new URL(url);
                        BufferedReader in = new BufferedReader(new InputStreamReader(data.openStream(), "UTF-8"));
                        String inputLine;
                        String[] splitted = null;
                        while ((inputLine = in.readLine()) != null) {
                            splitted = inputLine.split(" ");
                            url = splitted[1];
                            Date dd = TimeUtils.utcDateFormat.parse(splitted[0]);
                            urls.add(new Pair<String, Long>(url, dd.getTime()));
                        }
                        in.close();
                        synchronized (parsedCache) {
                            parsedCache.put(cacheKey, urls);
                        }
                    }
                } catch (MalformedURLException e) {
                    Log.warn("Could not read pfss entries : URL unavailable");
                } catch (IOException e) {
                    Log.warn("Could not read pfss entries");
                } catch (ParseException e) {
                    Log.warn("Could not parse date time during pfss loading");
                }
                for (Pair<String, Long> pair : urls) {
                    Long dd = pair.b;
                    String url = pair.a;
                    if (dd > start.getTime() - 24 * 60 * 60 * 1000 && dd < end.getTime() + 24 * 60 * 60 * 1000) {
                        FutureTask<Void> dataLoaderTask = new FutureTask<Void>(new PfssDataLoader(url, dd), null);
                        pfssPool.submit(dataLoaderTask);
                        pfssPool.schedule(new AbolishTask(dataLoaderTask, "Abolish PFSS"), TIMEOUT_DOWNLOAD_SECONDS, TimeUnit.SECONDS);
                    }
                }

                if (startMonth == 11) {
                    startMonth = 0;
                    startYear++;
                } else {
                    startMonth++;
                }
            } while (startYear < endYear && (startYear >= endYear && startMonth <= endMonth));
        }
    }

}
