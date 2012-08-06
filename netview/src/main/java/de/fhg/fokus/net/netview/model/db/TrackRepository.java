/* Netview - a software component to visualize packet tracks, hop-by-hop delays,
 *           sampling stats and resource consumption. Netview requires the deployment of
 *           distributed probes (impd4e) and a central packet matcher to correlate the
 *           obervations.
 *
 *           The probe can be obtained at http://impd4e.sourceforge.net/downloads.html
 *
 * Copyright (c) 2011
 *
 * Fraunhofer FOKUS
 * www.fokus.fraunhofer.de
 *
 * in cooperation with
 *
 * Technical University Berlin
 * www.av.tu-berlin.de
 *
 * Ramon Masek <ramon.masek@fokus.fraunhofer.de>
 * Christian Henke <c.henke@tu-berlin.de>
 * Carsten Schmoll <carsten.schmoll@fokus.fraunhofer.de>
 * Julian Vetter <julian.vetter@fokus.fraunhofer.de>
 * Jens Krenzin <jens.krenzin@fokus.fraunhofer.de>
 * Michael Gehring <michael.gehring@fokus.fraunhofer.de>
 * Tacio Grespan Santos
 * Fabian Wolff
 *
 * For questions/comments contact packettracking@fokus.fraunhofer.de
 *
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation;
 * either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <http://www.gnu.org/licenses/>.
 */
package de.fhg.fokus.net.netview.model.db;


import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import org.geonames.InvalidParameterException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;

import de.fhg.fokus.net.netview.control.Controllable;
import de.fhg.fokus.net.netview.model.Node;
import de.fhg.fokus.net.netview.model.NodeMeasurementProperties;
import de.fhg.fokus.net.netview.model.NodePhysicalProperties;
import de.fhg.fokus.net.netview.model.NodeViewProperties;
import de.fhg.fokus.net.netview.model.ObjectFactory;
import de.fhg.fokus.net.netview.view.map.NetViewMapMarker;
import de.fhg.fokus.net.worldmap.MapMarker;
import de.fhg.fokus.net.worldmap.layers.track.Track;
import de.fhg.fokus.net.worldmap.layers.track.Flow;
import de.fhg.fokus.net.worldmap.layers.track.Bearer;
import de.fhg.fokus.net.worldmap.layers.track.TrackPlayer;
import de.fhg.fokus.net.worldmap.util.EventSupport;
import de.fhg.fokus.net.ptapi.PacketTrackRecord;
import de.fhg.fokus.net.ptapi.PtInterfaceStats;
import de.fhg.fokus.net.ptapi.PtProbeLocation;
import de.fhg.fokus.net.ptapi.PtProbeStats;
import de.fhg.fokus.net.ptapi.PtBearerInformation;

import de.fhg.fokus.net.geo.WayPoint;
import de.fhg.fokus.net.netview.control.DataSourcesController;
import de.fhg.fokus.net.netview.model.Probe;

import org.geonames.PostalCodeSearchCriteria;
import org.geonames.WebService;
import org.geonames.PostalCode;
import org.geonames.Toponym;

/**
 * 
 * @author FhG-FOKUS NETwork Research
 *
 */
public class TrackRepository implements TrackPlayer.TrackSource, Controllable, TrackRecordRepository {
    // sys

    private final Logger logger = LoggerFactory.getLogger(getClass());
    // --
    private final EbeanServer eServer;
    private ConcurrentNavigableMap<Long, Track> trackMap = new ConcurrentSkipListMap<Long, Track>();
    private final ConcurrentMap<Long, MapMarker> probeIdMarkerMap;
    private final Lock preloadLock = new ReentrantLock();
    private final Map<Long, List<MapMarker>> trackMarkerCache = new HashMap<Long, List<MapMarker>>();
    public DataSourcesController dsc = null;
    private Set<Node> nodes = null;
    //
    // event support
    //

    public static final class EventData {

        public long startTs;
        public long stopTs;
        public long numberOfTracks;
        public long numberOfNewTracks;
        /**
         * In milliseconds.
         */
        public long elapsedTime;

        public EventData(long startTs, long stopTs) {
            this.startTs = startTs;
            this.stopTs = stopTs;
        }

        public EventData(long startTs, long stopTs, long numberOfTracks, long numberOfNewTracks,
                long elapsedTime) {
            super();
            this.startTs = startTs;
            this.stopTs = stopTs;
            this.numberOfTracks = numberOfTracks;
            this.numberOfNewTracks = numberOfNewTracks;
            this.elapsedTime = elapsedTime;
        }
    }

    public static enum EventType {

        /**
         * EventData: startTs, stopTs
         */
        LOADING_TRACKS_STARTED,
        /**
         * EventData: startTs, stopTs, elapsedTime, numberOfTracks
         */
        LOADING_TRACKS_FINISHED,
        /**
         * EventData: startTs, stopTs
         */
        LOADING_TRACKS_FAILED
    }
    public final EventSupport<EventType, EventData> eventSupport;
    /**
     * Database timestamp unit 
     */
    private final TimeUnit DB_TS_TIMEUNIT = TimeUnit.MICROSECONDS;

    public TrackRepository(EbeanServer eServer,
            ScheduledExecutorService scheduler, ConcurrentMap<Long, MapMarker> probeIdMarkerMap) {
        super();
        this.eServer = eServer;
        this.probeIdMarkerMap = probeIdMarkerMap;
        this.eventSupport = new EventSupport<EventType, EventData>(scheduler);
    }
    private long lastStartTs, lastStopTs;
    private long aggregationInterval = DB_TS_TIMEUNIT.convert(1, TimeUnit.SECONDS);

    private static class Preloader {

        boolean preloadIntervalShouldChange;
        long newPreloadStart;
        long newPreloadStop;
    };
    private Preloader preloader = new Preloader();
    private boolean warnOnMissingMarkers = true;

    // private long aggregationInterval = 10000000;
    public void setNodesHashSet(Set<Node> set) {
        this.nodes = set;
    }

    public Set<Node> getNodesHashSet() {
        return this.nodes;
    }

    @Override
    public Set<Track> getTracks(long startTs, long stopTs) {
        Set<Track> result = new ConcurrentSkipListSet<Track>();

        if (trackMap.size() == 0) {
            return result;
        }

        long intervalLower = DB_TS_TIMEUNIT.convert(startTs, TimeUnit.MILLISECONDS);
        long intervalUpper = DB_TS_TIMEUNIT.convert(stopTs, TimeUnit.MILLISECONDS);

        ConcurrentNavigableMap<Long, Track> tmp = trackMap.subMap(intervalLower, true, intervalUpper, false);

        for (Long ts : tmp.navigableKeySet()) {
            Track t = tmp.get(ts);
            if (t != null) {
                NetViewMapMarker m = (NetViewMapMarker) t.view.markers.get(0);
                if (m.getShowTraffic()) {
                    result.add(t);
                }
            }
        }
        return result;
    }
    
    public ArrayList<Bearer> getBearers(Track track) {
        JenkinsHash hashCalculator = new JenkinsHash();
        ArrayList<Bearer> bearers = new ArrayList<Bearer>();
        
        long startTs = track.getStartTs();
        long stopTs = track.getStopTs();
        
        int counter = 1;
        
        try {
            String sql_query = "SELECT count(*) as volume, "
                    + "BEARER_INFORMATION.* FROM "
                    + "RAW_TRACK_DATA, BEARER_INFORMATION "
                    + "WHERE RAW_TRACK_DATA.RULE_ID = "
                    + "BEARER_INFORMATION.RULE_ID AND "
                    + "track_id=:trackIdSQL AND RULE_FLAG = 1 "
                    + "AND ts BETWEEN :startTsSQL AND :stopTsSQL "
                    + "GROUP BY BEARER_INFORMATION.UID, "
                    + "RAW_TRACK_DATA.RULE_ID";
            
            SqlQuery query = eServer.createSqlQuery(sql_query)
                    .setParameter("trackIdSQL", track.getTrackId())
                    .setParameter("startTsSQL", startTs)
                    .setParameter("stopTsSQL", stopTs);
            List<SqlRow> results = query.findList();

            if (results.size() > 0) {
                for (SqlRow row : results) {
                    ByteBuffer hashBuffer = ByteBuffer.allocate(24);
                    hashBuffer.putLong(row.getLong("rule_id"));
		    hashBuffer.putLong(row.getLong("uid"));
                    hashBuffer.putLong(track.getTrackId());
                    
                    Bearer bearer = ObjectFactory.createAnimated(
                            track,
                            row.getInteger("volume"),
                            (long) hashCalculator.hash(hashBuffer.array()),
                            row.getLong("rule_id"),
                            row.getString("apn"),
                            row.getString("rule_name"),
                            row.getString("imsi"),
                            row.getLong("qci"),
                            row.getLong("maximum_download"),
                            row.getLong("maximum_upload"),
                            row.getLong("guaranteed_download"),
                            row.getLong("guaranteed_upload"),
                            row.getLong("apn_download"),
                            row.getLong("apn_upload"),
                            row.getString("source_address"),
                            row.getString("destination_address"),
                            row.getInteger("source_port"),
                            row.getInteger("destination_port"),
                            track.view.markers,
                            0.3*counter+0.7);
                    bearers.add(bearer);
                    counter++;
                }
            }
        } catch (Exception e) {
            logger.debug(e.getMessage());
        }
        
        return bearers;
    }
        
    public ArrayList<Flow> getFlows(Track track, Bearer bearer) {
        JenkinsHash hashCalculator = new JenkinsHash();
        ArrayList<Flow> flows = new ArrayList<Flow>(); 
        long startTs = track.getStartTs();
        long stopTs = track.getStopTs();
        int counter = 1;

        try {
            String sql_query = "SELECT count(*) as volume, "
                    + "source_address, destination_address, source_port, "
                    + "destination_port FROM raw_track_data WHERE rule_id="
                    + ":ruleIdSQL AND track_id=:trackIdSQL AND ts BETWEEN "
                    + ":startTsSQL AND :stopTsSQL GROUP BY source_address, "
                    + "destination_address, source_port, destination_port";
            
            SqlQuery query = eServer.createSqlQuery(sql_query)
                    .setParameter("ruleIdSQL", bearer.getRuleId())
                    .setParameter("trackIdSQL", track.getTrackId())
                    .setParameter("startTsSQL", startTs)
                    .setParameter("stopTsSQL", stopTs);
            List<SqlRow> results = query.findList();
            
            if (results.size() > 0) {
                for (SqlRow row : results) {
                    ByteBuffer hashBuffer = ByteBuffer.allocate(16);
                    hashBuffer.putLong(bearer.getBearerId());
                    hashBuffer.putInt(row.getInteger("source_port"));
                    hashBuffer.putInt(row.getInteger("destination_port"));
                     
                    Flow flow = ObjectFactory.createAnimated(
                            bearer,
                            (long) hashCalculator.hash(hashBuffer.array()),
                            row.getString("source_address"),
                            row.getString("destination_address"),
                            row.getInteger("source_port"), 
                            row.getInteger("destination_port"),
                            row.getInteger("volume"),
                            track.view.markers,
                            (0.3/results.size())*counter+
                            (bearer.view.curveFactor-(0.3/results.size())));
                    flows.add(flow);
                    counter++;
                }
            }
        } catch (Exception e) {
            logger.debug(e.getMessage());
        }     
        return flows;
    }

    @Override
    public void preloadTracks(long startTs, long stopTs) {
        // align on aggregation boundaries
        long aggregationMs = TimeUnit.MILLISECONDS.convert(aggregationInterval, DB_TS_TIMEUNIT);
        startTs -= startTs % aggregationMs;
        stopTs += aggregationMs - (stopTs % aggregationMs);

        if (lastStartTs == startTs && lastStopTs == stopTs) {
            return;
        }

        if (!preloadLock.tryLock()) {
            synchronized (preloader) {
                preloader.preloadIntervalShouldChange = true;
                preloader.newPreloadStart = startTs;
                preloader.newPreloadStop = stopTs;
            }
            return;
        }

        try {
            long intervalLower = DB_TS_TIMEUNIT.convert(startTs, TimeUnit.MILLISECONDS);
            long intervalUpper = DB_TS_TIMEUNIT.convert(stopTs, TimeUnit.MILLISECONDS);
            long loadStartedAt = System.currentTimeMillis();
            long loadedTracks = 0;

            eventSupport.dispatch(EventType.LOADING_TRACKS_STARTED, new EventData(startTs, stopTs));

            for (long currentTs = intervalLower; currentTs < intervalUpper; currentTs += aggregationInterval) {
                String sql_query = "SELECT track_id, COUNT(*) as volume, "
                        + "AVG(delay) as avgDelay, MIN(start_ts) as minTs, "
                        + "MAX(stop_ts) as maxTs FROM track_data WHERE "
                        + "start_ts BETWEEN :startTsSQL AND :stopTsSQL "
                        + "GROUP BY track_id";
                SqlQuery query = eServer.createSqlQuery(sql_query).setParameter("startTsSQL", currentTs).setParameter("stopTsSQL", currentTs + aggregationInterval);
                List<SqlRow> results = query.findList();

                if (results.isEmpty()) {
                    // no results in this interval, skip to the next track start
                    currentTs = findNextTrack(currentTs);
                    if (currentTs == Long.MAX_VALUE) {
                        // no more tracks terminate loop;
                        currentTs = intervalUpper;
                    } else {
                        // align on interval boundary
                        currentTs -= (currentTs % aggregationInterval);
                    }
                    continue;
                }

                for (SqlRow row : results) {
                    long trackTs = row.getLong("minTs");
                    long trackId = row.getLong("track_id");

                    if (trackMap.get(trackTs) == null) {
                        List<MapMarker> markers;

                        markers = trackMarkerCache.get(trackId);
                        if (markers == null) {
                            markers = findMarkersOnTrack(trackId);
                            trackMarkerCache.put(trackId, markers);
                        }

                        if (markers.size() > 1) {
                            Track track = ObjectFactory.createAnimated(
                                    row.getInteger("track_id"),
                                    row.getLong("minTs"),
                                    row.getLong("maxTs"),
                                    row.getInteger("volume"),
                                    row.getLong("avgDelay"),
                                    markers, DB_TS_TIMEUNIT);
                            trackMap.put(track.getStartTs(), track);
                            loadedTracks++;
                        }
                    }
                } // end for track

                synchronized (preloader) {
                    if (preloader.preloadIntervalShouldChange) {
                        preloader.preloadIntervalShouldChange = false;
                        intervalLower = DB_TS_TIMEUNIT.convert(preloader.newPreloadStart, TimeUnit.MILLISECONDS);
                        intervalUpper = DB_TS_TIMEUNIT.convert(preloader.newPreloadStop, TimeUnit.MILLISECONDS);
                        currentTs = intervalLower;
                        logger.debug("Preloader: changed preloading interval to : " + preloader.newPreloadStart + " - " + preloader.newPreloadStop);
                        loadedTracks = 0;
                        loadStartedAt = System.currentTimeMillis();
                        eventSupport.dispatch(EventType.LOADING_TRACKS_STARTED, new EventData(preloader.newPreloadStart, preloader.newPreloadStop));
                    }
                }
            } // end for interval

            eventSupport.dispatch(EventType.LOADING_TRACKS_FINISHED, new EventData(startTs, stopTs, trackMap.size(), loadedTracks, System.currentTimeMillis() - loadStartedAt));

            lastStartTs = startTs;
            lastStopTs = stopTs;

        } catch (Exception e) {
            eventSupport.dispatch(EventType.LOADING_TRACKS_FAILED, new EventData(startTs, stopTs));
            logger.debug(e.getMessage());
            e.printStackTrace();
        } finally {
            preloadLock.unlock();
        }
    }

    private List<Long> findProbesOnTrack(long trackId) {
        List<Long> probes = new ArrayList<Long>();

        try {
            /* find a record for this track */
            SqlQuery rquery = eServer.createSqlQuery("SELECT MIN(uid) as record_id FROM track_data WHERE track_id = :trackId").setParameter("trackId", trackId);
            SqlRow row = rquery.findUnique();

            if (row.get("record_id") != null) {
                /* get order list of probeIds */
                long recordId = row.getLong("record_id");
                SqlQuery pquery = eServer.createSqlQuery("SELECT probe_id FROM raw_track_data WHERE record_id = :recordId ORDER BY hop_number").setParameter("recordId", recordId);
                List<SqlRow> prows = pquery.findList();

                for (SqlRow prow : prows) {
                    probes.add(prow.getLong("probe_id"));
                }
            }
        } catch (Exception e) {
            logger.warn(e.getMessage());
            e.printStackTrace();
        }

        // logger.info("== probes " + trackId +
        // "==============");
        // for(Long p : vProbes) {
        // 		logger.info("" + p);
        // }
        // logger.info("== probes =============================");

        return probes;
    }

    private List<MapMarker> findMarkersOnTrack(long trackId) {
        List<Long> probes = findProbesOnTrack(trackId);
        List<MapMarker> markers = new ArrayList<MapMarker>();
        MapMarker lastMarker = null;
        for (long probeId : probes) {
            MapMarker marker = probeIdMarkerMap.get(probeId);
            if (marker == null) {
                logger.warn("Missing marker for probeId: " + probeId);
                if (warnOnMissingMarkers) {
                    JCheckBox cbDisable = new JCheckBox("disable this warning");
                    Object[] options = {"Continue", cbDisable};
                    cbDisable.setSelected(false);
                    JOptionPane.showOptionDialog(null, "Can't find node/marker for probeId: " + probeId + ".", "Warning", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                    if (cbDisable.isSelected()) {
                        warnOnMissingMarkers = false;
                    }
                }
            } else if (marker != lastMarker) {
                markers.add(marker);
                lastMarker = marker;
            } else {
                // NOP / avoid duplications
            }
        }
        return markers;
    }

    public ArrayList<Node> findNodesOnTrack(long trackId) {
        List<MapMarker> markers = findMarkersOnTrack(trackId);
        ArrayList<Node> nodeList = new ArrayList<Node>();

        for (MapMarker m : markers) {
            Node n = m.getReference(Node.class);
            if (n != null) {
                nodeList.add(n);
            }
        }
        return nodeList;
    }

    /**
     * find record_ids of records with trackId and start <= startTs <= stop;
     * @param trackId
     * @param start timestamp in milliseconds
     * @param stop timestamp in milliseconds
     * @return
     */
    public ArrayList<Long> findTrackRecords(long trackId, long start, long stop) {
        ArrayList<Long> records = new ArrayList<Long>();
        try {
            SqlQuery query = eServer.createSqlQuery("SELECT uid FROM track_data WHERE track_id = :trackid AND start_ts BETWEEN :start AND :stop").setParameter("trackid", trackId).setParameter("start", DB_TS_TIMEUNIT.convert(start, TimeUnit.MILLISECONDS)).setParameter("stop", DB_TS_TIMEUNIT.convert(stop, TimeUnit.MILLISECONDS));
            List<SqlRow> rows = query.findList();
            for (SqlRow row : rows) {
                records.add(row.getLong("uid"));
            }
        } catch (Exception e) {
            logger.warn(e.getMessage());
            e.printStackTrace();
        }
        return records;
    }

    private long findNextTrack(long currentTs) {
        try {
            String sql_query = "SELECT MIN(start_ts) as timestamp FROM track_data WHERE start_ts > :now";
            SqlQuery query = eServer.createSqlQuery(sql_query).setParameter("now", currentTs);
            SqlRow row = query.findUnique();

            if (row.get("timestamp") != null) {
                return row.getLong("timestamp");
            }
        } catch (Exception e) {
            logger.warn(e.getMessage());
            e.printStackTrace();
        }
        return Long.MAX_VALUE;
    }

    private long findPrevTrack(long t) {
        try {
            String sql_query = "SELECT MAX(start_ts) as timestamp FROM track_data WHERE start_ts < :now";
            SqlQuery query = eServer.createSqlQuery(sql_query).setParameter("now", t);
            SqlRow row = query.findUnique();

            if (row.get("timestamp") != null) {
                return row.getLong("timestamp");
            }

        } catch (Exception e) {
            logger.warn(e.getMessage());
            e.printStackTrace();
        }
        return Long.MIN_VALUE;
    }

    @Override
    public void init() {
        logger.debug("initializing track repository");

    }

    @Override
    public void start() {
        // TODO Auto-generated method stub
    }

    @Override
    public void stop() {
        trackMap.clear();
    }

    public void addEventListener(EventType eventType, EventSupport.EventListener<EventData> lsn) {
        eventSupport.addEventListener(eventType, lsn);
    }

    public void removeEventListener(EventSupport.EventListener<EventData> lsn) {
        eventSupport.removeEventListener(lsn);
    }
        
    @Override
    public void addPacketTrackRecord(PacketTrackRecord record) {
        if (record == null) {
            logger.warn("could not create (Raw)TrackData object, returning null");
        } else {
            TrackData td = new TrackData(record);
            eServer.save(td);

            for (int i = 0; i < record.oids.length; i++) {
                RawTrackData rtd = new RawTrackData(record, i, td.getUid());
                eServer.save(rtd);
            }
        }
    }
    
    @Override
    public void addBearerInformation(PtBearerInformation record) {
        if(record == null) {
            logger.warn("could not create BearerInformation object, returning null");
        } else {
            final BearerInformation bi = new BearerInformation(record);
            eServer.save(bi);
        }
    }

    @Override
    public void addPtInterfaceStats(PtInterfaceStats sampling) {
        if (sampling == null) {
            logger.warn("could not create Sampling object, returning null");
        } else {
            final InterfaceStats sample = new InterfaceStats(sampling);
            eServer.save(sample);
        }
    }

    @Override
    public void addPtProbeStats(PtProbeStats probeStats) {
        if (probeStats == null) {
            logger.warn("could not create ProbeStats object, returning null");
        } else {
            final ProbeStats probe = new ProbeStats(probeStats);
            eServer.save(probe);
        }
    }

    @Override
    public void addPtProbeLocation(PtProbeLocation location) {
        if (location == null) {
            logger.warn("could not create ProbeStats object, returning null");
        } else {
            ProbeLocation loc = new ProbeLocation(location);
            eServer.save(loc);
        }
    }

    public List<SqlRow> getFirstAddresses(List<SqlRow> firstPositions) {
        List<SqlRow> extractedRows = new ArrayList<SqlRow>();
        List<Long> oids = new ArrayList<Long>();
        long currOid;
        for (SqlRow row : firstPositions) {
            currOid = row.getLong("oid");
            if (oids.isEmpty() || !(OidAlreadyReaded(oids, currOid))) {
                oids.add(currOid);
            }
        }
        List<SqlRow> rows;
        SqlQuery query;
        for (long a : oids) {
            query = eServer.createSqlQuery(
                    "SELECT oid, uid, address"
                    + " FROM Probe_Location"
                    + " WHERE oid = " + a
                    + " ORDER BY uid asc");
            rows = query.findList();
            //logger.debug("--- getFirstAddresses ---");
            for (SqlRow row : rows) {
                //logger.debug(row.toString());
                if (((String) row.get("address")).equals("unknown")
                        || ((String) row.get("address")).equals("Unknown")
                        || ((String) row.get("address")).equals("")) {
                    continue;
                }
                extractedRows.add(row);
                break;
            }
            //logger.debug("-------------------------");
        }
        //logger.debug("Returned Addr-Row: "+extractedRows.toString());
        //logger.debug("-------------------------");
        return extractedRows;
    }

    public List<SqlRow> getFirstNames(List<SqlRow> firstPositions) {
        List<SqlRow> extractedRows = new ArrayList<SqlRow>();
        List<Long> oids = new ArrayList<Long>();
        long currOid;
        for (SqlRow row : firstPositions) {
            currOid = row.getLong("oid");
            if (oids.isEmpty() || !(OidAlreadyReaded(oids, currOid))) {
                oids.add(currOid);
            }
        }
        List<SqlRow> rows;
        SqlQuery query;
        for (long a : oids) {
            query = eServer.createSqlQuery(
                    "SELECT oid, uid, name"
                    + " FROM Probe_Location"
                    + " WHERE oid = " + a
                    + " ORDER BY uid asc");
            rows = query.findList();
            //logger.debug("--- getFirstName ---");
            for (SqlRow row : rows) {
                //logger.debug(row.toString());
                if (((String) row.get("name")).equals("unknown")
                        || ((String) row.get("name")).equals("Unknown")
                        || ((String) row.get("name")).equals("")) {
                    continue;
                }
                extractedRows.add(row);
                break;
            }
            //logger.debug("-------------------------");
        }
        //logger.debug("Returned Name-Rows: "+extractedRows.toString());
        //logger.debug("-------------------------");
        return extractedRows;
    }

    public List<SqlRow> getFirstLocationNames(List<SqlRow> firstPositions) {
        List<SqlRow> extractedRows = new ArrayList<SqlRow>();
        List<Long> oids = new ArrayList<Long>();
        long currOid;
        for (SqlRow row : firstPositions) {
            currOid = row.getLong("oid");
            if (oids.isEmpty() || !(OidAlreadyReaded(oids, currOid))) {
                oids.add(currOid);
            }
        }
        List<SqlRow> rows;
        SqlQuery query;
        for (long a : oids) {
            query = eServer.createSqlQuery(
                    "SELECT oid, uid, location_name"
                    + " FROM Probe_Location"
                    + " WHERE oid = " + a
                    + " ORDER BY uid asc");
            rows = query.findList();
            //logger.debug("--- getFirstLocationName ---");
            for (SqlRow row : rows) {
                //logger.debug(row.toString());
                if (((String) row.get("location_name")).equals("unknown")
                        || ((String) row.get("location_name")).equals("Unknown")
                        || ((String) row.get("location_name")).equals("")) {
                    continue;
                }
                extractedRows.add(row);
                break;
            }
            //logger.debug("-------------------------");
        }
        //logger.debug("Returned LocName-Rows: "+extractedRows.toString());
        //logger.debug("-------------------------");
        return extractedRows;
    }

    public List<SqlRow> getFirstPositions() {
        SqlQuery query = eServer.createSqlQuery("SELECT uid, oid FROM Probe_Location ORDER BY uid asc");
        List<SqlRow> rows1 = query.findList();
        long currOid;
        List<Long> oids = new ArrayList<Long>();
        for (SqlRow row : rows1) {
            currOid = row.getLong("oid");
            if (oids.isEmpty() || !(OidAlreadyReaded(oids, currOid))) {
                oids.add(currOid);
            }
        }
        List<SqlRow> rows2;
        List<SqlRow> extractedRows = new ArrayList<SqlRow>();
        for (long a : oids) {
            query = eServer.createSqlQuery(
                    "SELECT oid, uid, latitude, longitude"
                    + " FROM Probe_Location"
                    + " WHERE oid = " + a
                    + " ORDER BY uid asc");
            rows2 = query.findList();
            //logger.debug("--- getFirstPositions ---");
            for (SqlRow row : rows2) {
                //logger.debug(row.toString());
                try {
                    Double.parseDouble((String) row.get("latitude"));
                    Double.parseDouble((String) row.get("longitude"));
                } catch (NumberFormatException e) {
                    continue;
                }
                extractedRows.add(row);
                break;
            }
            //logger.debug("-------------------------");
        }

        return extractedRows;
    }

    public void saveLocation(Node node) {
        String name;
        String locationName;
        if (node.getView().getLabel(node).contains("@")) {
            name = node.getView().getLabel().substring(0, node.getView().getLabel().indexOf("@"));
            locationName = node.getView().getLabel().substring(node.getView().getLabel().indexOf("@") + 1);
            logger.debug("name: " + name + " | locationName: " + locationName);
        } else {
            locationName = node.getView().getLabel(node);
            name = "Unknown";
        }
        ProbeLocation loc = new ProbeLocation(
                node.mp.getProbe().getProbeId(),
                new java.sql.Timestamp(new java.util.Date().getTime()).getTime(),
                String.valueOf(node.phy.getWaypoint().latitude),
                String.valueOf(node.phy.getWaypoint().longitude),
                name,
                locationName,
                node.mp.getProbe().getLabel());
        logger.debug("OOO saving: " + loc.toString());
        eServer.save(loc);

    }

    public boolean OidAlreadyReaded(List<Long> oids, long oid) {
        for (long currOid : oids) {
            if (currOid == oid) {
                return true;
            }
        }
        return false;
    }

    public String getLocationNameOverWebservice(long oid, double latitude, double longitude) {

        logger.info("The probe " + oid + " did not sent his location name. Connecting to ws.geonames.org to retreive the location name ...");
        PostalCodeSearchCriteria postalCodeSearchCriteria = new PostalCodeSearchCriteria();
        try {
            postalCodeSearchCriteria.setLatitude(latitude);
        } catch (InvalidParameterException ex) {
            logger.info("NetView wasn't able to set the latitude for the geonames request");
        }
        try {
            postalCodeSearchCriteria.setLongitude(longitude);
        } catch (InvalidParameterException ex) {
            logger.info("NetView wasn't able to set the longitude for the geonames request");
        }

        // Calling the webservice
        List<PostalCode> postalCodes = null;
        String locationName = "Unknown";
        for (int i = 0; i < 10; i++) {
            logger.info("attempt " + (i + 1) + " ...");
            try {
                postalCodes = WebService.findNearbyPostalCodes(postalCodeSearchCriteria);
            } catch (Exception ex) {
                logger.info("The webservice geonames.org is unreachable");
            }
            if (postalCodes != null && !postalCodes.isEmpty()) {
                locationName = postalCodes.get(0).getPlaceName();
                break;
            } else {
                List<Toponym> typonyms = null;
                try {
                    typonyms = WebService.findNearbyPlaceName(latitude, longitude);
                } catch (Exception ex) {
                    logger.info("The webservice geonames.org is unreachable");
                }
                if (typonyms != null) {
                    if (!typonyms.isEmpty()) {
                        locationName = typonyms.get(0).getCountryName();
                        break;
                    } else {
                        break;
                    }
                }
            }
        }
        logger.info("The webservice geonames.org sent the following location name for the Probe " + oid + ":" + locationName);
        return locationName;
    }

    public List<Node> getNodes() {
        List<Node> nodesToReturn = new ArrayList<Node>();
        List<SqlRow> firstPositions = getFirstPositions();
        List<SqlRow> firstNames = getFirstNames(firstPositions);
        List<SqlRow> firstLocationNames = getFirstLocationNames(firstPositions);
        List<SqlRow> firstAddresses = getFirstAddresses(firstPositions);

        Node node;
        NodeViewProperties view;
        NodePhysicalProperties phy;
        NodeMeasurementProperties mp;
        Probe probe;
        long oid = 0;
        String address;
        String name;
        String locationName;

        for (SqlRow row : firstPositions) {

            address = "Unknown IP";
            name = "Unknown";
            locationName = "Unknown";

            double lat = 0.0;
            double lon = 0.0;
            oid = (Long) row.get("oid");
            try {
                lat = Double.parseDouble((String) row.get("latitude"));
                lon = Double.parseDouble((String) row.get("longitude"));
            } catch (NumberFormatException e) {
                logger.error("No Location Received!");
            }

            for (SqlRow row2 : firstNames) {
                if ((Long) row2.get("oid") == oid) {
                    name = (String) row2.get("name");
                }
            }

            for (SqlRow row3 : firstLocationNames) {
                if ((Long) row3.get("oid") == oid) {
                    locationName = (String) row3.get("location_name");
                }
            }

            for (SqlRow row4 : firstAddresses) {
                if ((Long) row4.get("oid") == oid) {
                    address = (String) row4.get("address");
                }
            }

            node = new Node();
            node.setUid(oid);
            view = new NodeViewProperties();
            view.setLabel(name + "@" + locationName);
            node.setView(view);
            phy = new NodePhysicalProperties();
            phy.setWaypoint(new WayPoint(lat, lon));
            node.setPhy(phy);
            mp = new NodeMeasurementProperties();
            probe = new Probe(oid);
            probe.setLabel(address);
            mp.setProbe(probe);
            node.setMp(mp);

            nodesToReturn.add(node);
        }
        return nodesToReturn;
    }

    @Override
    public int numberOfPreloadedTracks() {
        return trackMap.size();
    }

    @Override
    public TimeUnit getTimestampUnit() {
        return DB_TS_TIMEUNIT;
    }

    @Override
    public long getNumberOfTracks(long startTs, long stopTs) {
        if (trackMap.size() == 0) {
            return 0;
        }
        long from = DB_TS_TIMEUNIT.convert(startTs, TimeUnit.MILLISECONDS);
        long to = DB_TS_TIMEUNIT.convert(stopTs, TimeUnit.MILLISECONDS);
        ConcurrentNavigableMap<Long, Track> tmp = trackMap.subMap(from, to);
        return tmp.size();
    }

    public void clearCache() {
        lastStartTs = 0;
        lastStopTs = 0;
        trackMap.clear();
    }

    @Override
    public void refresh() {
        try {
            dsc.getDB().importNodesAlternative(dsc.getModel(), nodes); // New Node Import
            dsc.getModel().refreshNetworks();
            dsc.updateNodesControls(dsc.getModel().network);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e + "");
            dsc.getView().message(e.getMessage());
        } finally {
            dsc.getView().getBusyIconAnimator().stop();
        }

        long f, t;
        f = lastStartTs;
        t = lastStopTs;
        lastStartTs = lastStopTs = 0; // force
        preloadTracks(f, t);
    }

    @Override
    public long nextTrackStart(long ts) {
        long t = DB_TS_TIMEUNIT.convert(ts, TimeUnit.MILLISECONDS);
        long r = findNextTrack(t);
        if (r == Long.MAX_VALUE) {
            return r;
        } else {
            return TimeUnit.MILLISECONDS.convert(r, DB_TS_TIMEUNIT);
        }
    }

    @Override
    public long previousTrackStart(long ts) {
        long t = DB_TS_TIMEUNIT.convert(ts, TimeUnit.MILLISECONDS);
        long r = findPrevTrack(t);
        if (r == Long.MIN_VALUE) {
            return r;
        } else {
            return TimeUnit.MILLISECONDS.convert(r, DB_TS_TIMEUNIT);
        }
    }

    /**
     * return the last Sampling send by the Node n before timestamp ts.
     * @param n
     * @param ts
     * @return last Sampling sent or null.
     */
    public InterfaceStats getLatestNodeSamplingStats(Node n, long ts) {
        InterfaceStats r = null;
        long oid = n.mp.getProbe().getProbeId();

        try {
            r = eServer.find(InterfaceStats.class).where().lt("timestamp", ts).eq("oid", oid).orderBy("timestamp desc").setMaxRows(1).findUnique();
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        return r;
    }

    public ProbeStats getLatestNodeSystemStats(Node n, long ts) {
        ProbeStats r = null;
        long oid = n.mp.getProbe().getProbeId();

        try {
            r = eServer.find(ProbeStats.class).where().lt("timestamp", ts).eq("oid", oid).orderBy("timestamp desc").setMaxRows(1).findUnique();
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        return r;
    }

    /**
     * return the last ProbeLocation send by the Node n before timestamp ts.
     * @param n
     * @param ts
     * @return last Location sent or null.
     */
    public ProbeLocation getLatestProbeLocation(Node n, long ts) {
        ProbeLocation r = null;
        long oid = n.mp.getProbe().getProbeId();

        try {
            r = eServer.find(ProbeLocation.class).where().lt("timestamp", ts).eq("oid", oid).orderBy("timestamp desc").setMaxRows(1).findUnique();
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        return r;
    }
}
