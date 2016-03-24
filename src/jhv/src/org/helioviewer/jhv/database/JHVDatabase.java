package org.helioviewer.jhv.database;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.helioviewer.jhv.base.cache.RequestCache;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.data.datatype.event.JHVAssociation;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKParam;
import org.helioviewer.jhv.data.datatype.event.SWEKParameter;
import org.helioviewer.jhv.data.datatype.event.SWEKParameterFilter;
import org.helioviewer.jhv.threads.JHVThread;
import org.helioviewer.jhv.threads.JHVThread.ConnectionThread;

public class JHVDatabase {

    private final static ArrayBlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<Runnable>(10000);
    private final static ExecutorService executor = new ThreadPoolExecutor(1, 1, 10000L, TimeUnit.MILLISECONDS, blockingQueue, new JHVThread.NamedDbThreadFactory("JHVDatabase"), new ThreadPoolExecutor.DiscardPolicy());
    private static long ONEWEEK = 1000 * 60 * 60 * 24 * 7;
    public static int config_hash;

    public static byte[] compress(final String str) throws IOException {
        if ((str == null) || (str.length() == 0)) {
            return null;
        }
        ByteArrayOutputStream obj = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(obj);
        gzip.write(str.getBytes("UTF-8"));
        gzip.close();
        return obj.toByteArray();
    }

    private static boolean isCompressed(final byte[] compressed) {
        return (compressed[0] == (byte) (GZIPInputStream.GZIP_MAGIC)) && (compressed[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));
    }

    public static String decompress(final byte[] compressed) {
        String outStr = "";
        try {
            if ((compressed == null) || (compressed.length == 0)) {
                return "";
            }
            if (isCompressed(compressed)) {
                GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed));
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gis, "UTF-8"));

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    outStr += line;
                }
            } else {
                outStr = new String(compressed);
            }
        } catch (IOException e) {
            System.out.println("could not decompress");
        }
        return outStr;
    }

    private static int getEventTypeId(Connection connection, JHVEventType eventType) {
        int typeId = _getEventTypeId(connection, eventType);
        if (typeId == -1) {
            insertEventTypeIfNotExist(connection, eventType);
            typeId = _getEventTypeId(connection, eventType);
        }

        return typeId;
    }

    private static int _getEventTypeId(Connection connection, JHVEventType event) {
        int typeId = -1;
        String sqlt = "SELECT id FROM event_type WHERE name=? AND supplier=?";
        try {
            PreparedStatement pstatement = connection.prepareStatement(sqlt);
            pstatement.setQueryTimeout(30);
            pstatement.setString(1, event.getEventType().getEventName());
            pstatement.setString(2, event.getSupplier().getSupplierName());
            ResultSet rs = pstatement.executeQuery();
            if (!rs.isClosed() && rs.next()) {
                typeId = rs.getInt(1);
                rs.close();
            }
            pstatement.close();
        } catch (SQLException e) {
            Log.error("Could not fetch event type " + event.getEventType().getEventName() + event.getSupplier().getSupplierName() + e.getMessage());
        }
        return typeId;
    }

    private static void insertEventTypeIfNotExist(Connection connection, JHVEventType eventType) {
        try {
            String sqlt = "INSERT INTO event_type(name, supplier) VALUES(?,?)";

            PreparedStatement pstatement = connection.prepareStatement(sqlt);
            pstatement.setQueryTimeout(30);
            pstatement.setString(1, eventType.getEventType().getEventName());
            pstatement.setString(2, eventType.getSupplier().getSupplierName());
            pstatement.executeUpdate();
            pstatement.close();

            String dbName = eventType.getSupplier().getDatabaseName();
            String createtbl = "CREATE TABLE " + dbName + " (";
            for (SWEKParameter p : eventType.getEventType().getParameterList()) {
                SWEKParameterFilter pf = p.getParameterFilter();
                if (pf != null)
                    createtbl += p.getParameterName() + " " + pf.getDbType() + ",";
            }
            createtbl += "event_id INTEGER, id INTEGER PRIMARY KEY AUTOINCREMENT, FOREIGN KEY(event_id) REFERENCES events(id), UNIQUE(event_id) ON CONFLICT REPLACE );";
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);
            statement.executeUpdate(createtbl);

        } catch (SQLException e) {
            Log.error("Failed to insert event type " + e.getMessage());
        }
    }

    private static void insertLinkIfNotExist(Connection connection, int left_id, int right_id) {
        try {
            String sqlt = "INSERT INTO event_link(left_id, right_id) VALUES(?,?)";
            PreparedStatement pstatement = connection.prepareStatement(sqlt);
            pstatement.setQueryTimeout(30);
            pstatement.setInt(1, left_id);
            pstatement.setInt(2, right_id);
            pstatement.executeUpdate();
            pstatement.close();
        } catch (SQLException e) {
            Log.error("Failed to insert event type " + e.getMessage());
        }
    }

    private static Integer[] insertLinkIfNotExist(Connection connection, String left_uid, String right_uid) {
        Integer[] ids = new Integer[] { getIdFromUID(connection, left_uid), getIdFromUID(connection, right_uid) };

        if (ids[0] != -1 && ids[1] != -1) {
            insertLinkIfNotExist(connection, ids[0], ids[1]);

        } else {
            Log.error("Could not add association to database " + ids[0] + " " + ids[1]);
        }
        return ids;
    }

    private static int getIdFromUID(Connection connection, String uid) {
        int id = _getIdFromUID(connection, uid);
        if (id == -1) {
            insertVoidEvent(connection, uid);
            id = _getIdFromUID(connection, uid);
        }
        return id;
    }

    private static int _getIdFromUID(Connection connection, String uid) {
        int id = -1;
        String sqlt = "SELECT id FROM events WHERE uid=?";
        try {
            PreparedStatement pstatement = connection.prepareStatement(sqlt);
            pstatement.setQueryTimeout(30);
            pstatement.setString(1, uid);
            ResultSet rs = pstatement.executeQuery();
            if (!rs.isClosed() && rs.next()) {
                id = rs.getInt(1);
                rs.close();
            }
            pstatement.close();
        } catch (SQLException e) {
            Log.error("Could not fetch id from uid " + e.getMessage());
        }
        return id;
    }

    private static void insertVoidEvent(Connection connection, String uid) {
        try {
            String sql = "INSERT INTO events(uid) VALUES(?)";
            PreparedStatement pstatement = connection.prepareStatement(sql);
            pstatement.setQueryTimeout(30);
            pstatement.setString(1, uid);
            pstatement.executeUpdate();
            pstatement.close();
        } catch (SQLException e) {
            Log.error("Could not insert event" + e.getMessage());
        }
    }

    public static Integer[] dump_association2db(String left, String right) {
        FutureTask<Integer[]> ft = new FutureTask<Integer[]>(new DumpAssociation2Db(left, right));
        executor.execute(ft);
        try {
            return ft.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return new Integer[] { -1, -1 };
        } catch (ExecutionException e) {
            e.printStackTrace();
            return new Integer[] { -1, -1 };
        }
    }

    private static class DumpAssociation2Db implements Callable<Integer[]> {
        private final String left;
        private final String right;

        public DumpAssociation2Db(String _left, String _right) {
            left = _left;
            right = _right;
        }

        @Override
        public Integer[] call() {
            Connection connection = ConnectionThread.getConnection();
            if (connection == null)
                return new Integer[] { -1, -1 };
            return insertLinkIfNotExist(connection, left, right);
        }
    }

    public static Integer getEventId(String uid) {
        FutureTask<Integer> ft = new FutureTask<Integer>(new GetEventId(uid));
        executor.execute(ft);
        try {
            return ft.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return -1;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private static class GetEventId implements Callable<Integer> {

        private final String uid;

        public GetEventId(String _uid) {
            uid = _uid;
        }

        @Override
        public Integer call() {
            int generatedKey = -1;
            Connection connection = ConnectionThread.getConnection();
            if (connection == null)
                return generatedKey;
            try {
                String sql = "SELECT id from events WHERE uid=?";
                PreparedStatement pstatement = connection.prepareStatement(sql);
                pstatement.setQueryTimeout(30);
                pstatement.setString(1, uid);
                ResultSet generatedKeys = pstatement.executeQuery();
                if (generatedKeys.next()) {
                    generatedKey = generatedKeys.getInt(1);
                }
                generatedKeys.close();
                pstatement.close();
            } catch (SQLException e) {
                Log.error("Could not select event with uid " + uid + e.getMessage());
            }
            return generatedKey;
        }
    }

    public static Integer dump_event2db(byte[] compressedJson, long start, long end, String uid, JHVEventType type, ArrayList<JHVDatabaseParam> paramList) {
        FutureTask<Integer> ft = new FutureTask<Integer>(new DumpEvent2Db(compressedJson, start, end, uid, type, paramList));
        executor.execute(ft);
        try {
            return ft.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return -1;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private static class DumpEvent2Db implements Callable<Integer> {
        private final long start;
        private final long end;
        private final byte[] compressedJson;
        private final String uid;
        JHVEventType type;
        private final ArrayList<JHVDatabaseParam> paramList;

        public DumpEvent2Db(byte[] _compressedJson, long _start, long _end, String _uid, JHVEventType _type, ArrayList<JHVDatabaseParam> _paramList) {
            compressedJson = _compressedJson;
            uid = _uid;
            start = _start;
            end = _end;
            type = _type;
            paramList = _paramList;
        }

        @Override
        public Integer call() {
            Connection connection = ConnectionThread.getConnection();
            if (connection == null)

                return -1;
            try
            {
                int typeId = getEventTypeId(connection, type);
                if (typeId != -1) {
                    {
                        String sql = "INSERT INTO events(type_id, uid,  start, end, data) VALUES(?,?,?,?,?)";
                        PreparedStatement pstatement = connection.prepareStatement(sql);
                        pstatement.setQueryTimeout(30);
                        pstatement.setInt(1, typeId);
                        pstatement.setString(2, uid);
                        pstatement.setLong(3, start);
                        pstatement.setLong(4, end);
                        pstatement.setBinaryStream(5, new ByteArrayInputStream(compressedJson), compressedJson.length);
                        pstatement.executeUpdate();
                        pstatement.close();
                    }
                    int generatedKey = -1;
                    {
                        String sql = "SELECT last_insert_rowid()";
                        PreparedStatement pstatement = connection.prepareStatement(sql);
                        pstatement.setQueryTimeout(30);
                        ResultSet generatedKeys = pstatement.executeQuery();
                        if (generatedKeys.next()) {
                            generatedKey = generatedKeys.getInt(1);
                        }
                        pstatement.close();
                    }
                    {
                        String fieldString = "";
                        String varString = "";
                        for (JHVDatabaseParam p : this.paramList) {
                            fieldString += "," + p.getParamName();
                            varString += ",?";
                        }
                        String sql = "INSERT INTO " + type.getSupplier().getDatabaseName() + "(event_id" + fieldString + ") VALUES(?" + varString + ")";
                        PreparedStatement pstatement = connection.prepareStatement(sql);
                        pstatement.setQueryTimeout(30);
                        pstatement.setInt(1, generatedKey);
                        int index = 2;
                        for (JHVDatabaseParam p : this.paramList) {
                            if (p.isInt()) {
                                pstatement.setInt(index, p.getIntValue());
                            } else if (p.isString()) {
                                pstatement.setString(index, p.getStringValue());
                            }
                            index++;
                        }
                        pstatement.executeUpdate();
                        pstatement.close();
                    }
                }
                else {
                    Log.error("Failed to insert event");
                }
            } catch (SQLException e) {
                Log.error("Could not insert event " + e.getMessage());
                return -1;
            }
            return 0;
        }
    }

    public static void addDaterange2db(Date start, Date end, JHVEventType type) {
        executor.execute(new AddDateRange2db(start, end, type));
    }

    private static class AddDateRange2db implements Runnable {

        private final JHVEventType type;
        private final Date start;
        private final Date end;

        public AddDateRange2db(Date _start, Date _end, JHVEventType _type) {
            start = _start;
            end = _end;
            type = _type;
        }

        @Override
        public void run() {
            Connection connection = ConnectionThread.getConnection();
            if (connection == null)
                return;
            HashMap<JHVEventType, RequestCache> dCache = ConnectionThread.downloadedCache;
            RequestCache typedCache = dCache.get(type);
            if (typedCache == null)
                return;
            typedCache.adaptRequestCache(start, end);
            int typeId = getEventTypeId(connection, type);
            try {
                String sqld = "DELETE FROM date_range where type_id=?";
                PreparedStatement dstatement = connection.prepareStatement(sqld);
                dstatement.setQueryTimeout(30);
                dstatement.setInt(1, typeId);
                dstatement.executeUpdate();
                for (Interval<Date> interval : typedCache.getAllRequestIntervals()) {
                    if (typeId != -1) {
                        String sql = "INSERT INTO date_range(type_id,  start, end) VALUES(?,?,?)";
                        PreparedStatement pstatement = connection.prepareStatement(sql);
                        pstatement.setQueryTimeout(30);
                        pstatement.setQueryTimeout(30);
                        pstatement.setInt(1, typeId);
                        pstatement.setLong(2, interval.getStart().getTime());
                        pstatement.setLong(3, interval.getEnd().getTime());
                        pstatement.executeUpdate();
                        pstatement.close();
                    }
                }
            } catch (SQLException e) {
                Log.error("Could not serialize date_range to database " + e.getMessage());
            }
        }

    }

    public static ArrayList<Interval<Date>> db2daterange(JHVEventType type) {
        FutureTask<ArrayList<Interval<Date>>> ft = new FutureTask<ArrayList<Interval<Date>>>(new Db2DateRange(type));
        executor.execute(ft);
        try {
            return ft.get();
        } catch (InterruptedException e) {
            return new ArrayList<Interval<Date>>();
        } catch (ExecutionException e) {
            return new ArrayList<Interval<Date>>();
        }

    }

    private static class Db2DateRange implements Callable<ArrayList<Interval<Date>>> {

        private final JHVEventType type;

        public Db2DateRange(JHVEventType _type) {
            type = _type;
        }

        @Override
        public ArrayList<Interval<Date>> call() {
            Connection connection = ConnectionThread.getConnection();
            HashMap<JHVEventType, RequestCache> dCache = ConnectionThread.downloadedCache;
            RequestCache typedCache = dCache.get(type);
            if (typedCache == null) {
                typedCache = new RequestCache();
                long lastEvent = Math.min(System.currentTimeMillis(), getLastEvent(connection, type));
                long invalidationDate = lastEvent - ONEWEEK * 2;
                dCache.put(type, typedCache);
                if (connection != null) {
                    int typeId = getEventTypeId(connection, type);
                    if (typeId != -1) {
                        try {
                            String sqlt = "SELECT start, end FROM date_range where type_id=? order by start, end ";
                            PreparedStatement pstatement = connection.prepareStatement(sqlt);
                            pstatement.setQueryTimeout(30);
                            pstatement.setInt(1, typeId);
                            ResultSet rs = pstatement.executeQuery();
                            while (!rs.isClosed() && rs.next()) {
                                Date beginDate = new Date(Math.min(invalidationDate, rs.getLong(1)));
                                Date endDate = new Date(Math.min(invalidationDate, rs.getLong(2)));
                                typedCache.adaptRequestCache(beginDate, endDate);
                            }
                            rs.close();
                            pstatement.close();
                        } catch (SQLException e) {
                            Log.error("Could db2daterange " + e.getMessage());
                        }
                    }
                }
            }
            /* for usage in other thread return full copy! */
            ArrayList<Interval<Date>> copy = new ArrayList<Interval<Date>>();
            for (Interval<Date> interval : typedCache.getAllRequestIntervals()) {
                copy.add(new Interval(new Date(interval.getStart().getTime()), new Date(interval.getEnd().getTime())));
            }
            return copy;
        }
    }

    private static long getLastEvent(Connection connection, JHVEventType type) {
        int typeId = getEventTypeId(connection, type);
        long last_timestamp = -1;
        if (typeId != -1) {
            try {
                String sqlt = "SELECT end FROM events WHERE type_id=? order by end DESC LIMIT 1";
                PreparedStatement pstatement = connection.prepareStatement(sqlt);
                pstatement.setQueryTimeout(30);
                pstatement.setInt(1, typeId);
                ResultSet rs = pstatement.executeQuery();
                if (!rs.isClosed() && rs.next()) {
                    last_timestamp = rs.getLong(1);
                }
                rs.close();
                pstatement.close();
            } catch (SQLException e) {
                Log.error("Could not fetch id from uid " + e.getMessage());
            }
        }
        return last_timestamp;
    }

    public static ArrayList<JsonEvent> events2Program(long start, long end, JHVEventType type, List<SWEKParam> params) {
        FutureTask<ArrayList<JsonEvent>> ft = new FutureTask<ArrayList<JsonEvent>>(new Events2Program(start, end, type, params));
        executor.execute(ft);
        try {
            return ft.get();
        } catch (InterruptedException e) {
            System.out.println(e);
            return new ArrayList<JsonEvent>();
        } catch (ExecutionException e) {
            System.out.println(e);
            return new ArrayList<JsonEvent>();
        }
    }

    public static class JsonEvent {

        public int id;
        public byte[] json;
        public JHVEventType type;
        public long start;
        public long end;

        public JsonEvent(byte[] _json, JHVEventType _type, int _id, long _start, long _end) {
            start = _start;
            end = _end;
            type = _type;
            id = _id;
            json = _json;
        }

    }

    private static class Events2Program implements Callable<ArrayList<JsonEvent>> {
        private final JHVEventType type;
        private final long start;
        private final long end;
        private final List<SWEKParam> params;

        public Events2Program(long _start, long _end, JHVEventType _type, List<SWEKParam> _params) {
            type = _type;
            start = _start;
            end = _end;
            params = _params;
        }

        @Override
        public ArrayList<JsonEvent> call() {
            Connection connection = ConnectionThread.getConnection();
            ArrayList<JsonEvent> eventList = new ArrayList<JsonEvent>();
            if (connection == null)
                return eventList;
            int typeId = getEventTypeId(connection, type);
            if (typeId != -1) {
                try {
                    String join = "LEFT JOIN " + type.getSupplier().getDatabaseName() + " AS tp ON tp.event_id=e.id";
                    String and = "";
                    for (SWEKParam p : params) {
                        if (!p.param.equals("provider")) {
                            and += "AND tp." + p.param + p.operand.getStringRepresentation() + p.value + " ";
                        }
                    }
                    String sqlt = "SELECT e.id, e.start, e.end, e.data FROM events AS e "
                            + join
                            + " WHERE e.start>=? and e.end <=? and e.type_id=? "
                            + and + " order by e.start, e.end ";
                    PreparedStatement pstatement = connection.prepareStatement(sqlt);
                    pstatement.setQueryTimeout(30);
                    pstatement.setLong(1, start);
                    pstatement.setLong(2, end);
                    pstatement.setInt(3, typeId);
                    ResultSet rs = pstatement.executeQuery();
                    boolean next = rs.next();
                    while (!rs.isClosed() && next) {
                        int id = rs.getInt(1);
                        long start = rs.getLong(2);
                        long end = rs.getLong(3);
                        byte[] json = rs.getBytes(4);
                        eventList.add(new JsonEvent(json, type, id, start, end));
                        next = rs.next();
                    }
                    rs.close();
                    pstatement.close();
                } catch (SQLException e)
                {
                    Log.error("Could not fetch events " + e.getMessage());
                    return eventList;
                }
            }
            return eventList;
        }
    }

    public static ArrayList<JHVAssociation> associations2Program(long start, long end, JHVEventType type) {
        FutureTask<ArrayList<JHVAssociation>> ft = new FutureTask<ArrayList<JHVAssociation>>(new Associations2Program(start, end, type));
        executor.execute(ft);
        try {
            return ft.get();
        } catch (InterruptedException e) {
            return new ArrayList<JHVAssociation>();
        } catch (ExecutionException e) {
            return new ArrayList<JHVAssociation>();
        }
    }

    private static class Associations2Program implements Callable<ArrayList<JHVAssociation>> {
        private final JHVEventType type;
        private final long start;
        private final long end;
        private static String sqlt = "SELECT left_events.id, right_events.id FROM event_link "
                + "LEFT JOIN events AS left_events ON left_events.id=event_link.left_id "
                + "LEFT JOIN events AS right_events ON right_events.id=event_link.right_id "
                + "WHERE left_events.start>=? and left_events.end <=? and left_events.type_id=? order by left_events.start, left_events.end ";

        public Associations2Program(long _start, long _end, JHVEventType _type) {
            type = _type;
            start = _start;
            end = _end;
        }

        @Override
        public ArrayList<JHVAssociation> call() {
            Connection connection = ConnectionThread.getConnection();
            ArrayList<JHVAssociation> assocList = new ArrayList<JHVAssociation>();
            if (connection == null)
                return assocList;
            int typeId = getEventTypeId(connection, type);
            if (typeId != -1) {
                try {
                    PreparedStatement pstatement = connection.prepareStatement(sqlt);
                    pstatement.setQueryTimeout(30);
                    pstatement.setLong(1, start);
                    pstatement.setLong(2, end);
                    pstatement.setInt(3, typeId);
                    ResultSet rs = pstatement.executeQuery();
                    boolean next = rs.next();
                    while (!rs.isClosed() && next) {
                        int left = rs.getInt(1);
                        int right = rs.getInt(2);

                        assocList.add(new JHVAssociation(left, right));
                        next = rs.next();
                    }
                    rs.close();
                    pstatement.close();
                } catch (SQLException e)
                {
                    Log.error("Could not fetch associations " + e.getMessage());
                    return assocList;
                }
            }
            return assocList;
        }
    }
}
