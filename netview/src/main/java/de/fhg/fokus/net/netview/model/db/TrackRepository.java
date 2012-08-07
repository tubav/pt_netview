package de.fhg.fokus.net.netview.model.db;


import de.fhg.fokus.net.netview.control.DataSourcesController;
import de.fhg.fokus.net.netview.control.NoviDBInterface;
import de.fhg.fokus.net.ptapi.PacketTrackRecord;
import de.fhg.fokus.net.ptapi.PtProbeStats;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author FhG-FOKUS NETwork Research
 *
 */
public class TrackRepository {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final int binsize = 100;
    private final NoviDBInterface novidbinterface;

    public DataSourcesController dsc = null;

    public TrackRepository() {
        super();
        this.novidbinterface = new NoviDBInterface("127.0.0.1",
                "5432", "noviDB", "novi_user", "novi123");

    }

    private String getPath(long[] oids) {
        String path = "\'";
        for (int i = 0; i < oids.length - 1; i++) {
            path = path + oids[i] + "-";
        }
        path = path + oids[oids.length - 1] + "\'";

        return path;
    }

    public void addPacketTrackRecord(PacketTrackRecord record) {

        if (record == null) {
            logger.warn("could not create (Raw)TrackData object, returning null");
        } else {
            // ---- Save data in Novi database ----

            // ------------------
            // PATH DELAYS
            // ------------------
            long pathDelayTimestamp = record.ts[0];
            long pathDelayId = pathDelayTimestamp / (1000 * binsize);
            long pathDelaySrc = record.oids[0];
            long pathDelayDst = record.oids[record.oids.length - 1];
            long pathDelayNum = 1;
            long pathDelaySumDelay = Math.abs(record.ts[0] - record.ts[record.oids.length - 1]);
            long pathDelaySumbytes = (long) record.size;
            long pathDelayMindelay;
            long pathDelayMaxdelay;
            String pathDelayPath = getPath(record.oids);

            ResultSet content_path_delays = novidbinterface.getContent(
                    "pdid, id, ts, src, dst, num, path, sumdelay, sumbytes, mindelay, maxdelay",
                    " WHERE ((id = " + pathDelayId + ") AND (src = " + pathDelaySrc + ") AND (dst = " + pathDelayDst + "))",
                    "path_delays");

            try {
                if (content_path_delays.next()) {
                    // Update Row
                    try {
                        long pdid = content_path_delays.getLong("pdid");
                        long pathDelayOldSumOfDelays = content_path_delays.getLong("sumdelay");
                        long pathDelayOldNumberOfDelays = content_path_delays.getLong("num");
                        long pathDelayOldSumBytes = content_path_delays.getLong("sumbytes");
                        long pathDelayOldMinDelay = content_path_delays.getLong("mindelay");
                        long pathDelayOldMaxDelay = content_path_delays.getLong("maxdelay");
                        pathDelayMindelay = pathDelayOldMinDelay;
                        if (pathDelaySumDelay < pathDelayMindelay) {
                            pathDelayMindelay = pathDelaySumDelay;
                        }
                        pathDelayMaxdelay = pathDelayOldMaxDelay;
                        if (pathDelaySumDelay > pathDelayMaxdelay) {
                            pathDelayMaxdelay = pathDelaySumDelay;
                        }
                        novidbinterface.updateRow(
                                pdid,
                                pathDelaySumDelay + pathDelayOldSumOfDelays,
                                pathDelayNum + pathDelayOldNumberOfDelays,
                                0,
                                pathDelaySumbytes + pathDelayOldSumBytes,
                                pathDelayMindelay,
                                pathDelayMaxdelay,
                                "path_delays");
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    pathDelayMindelay = pathDelaySumDelay;
                    pathDelayMaxdelay = pathDelaySumDelay;
                    // Write new Row
                    novidbinterface.writeRow(
                            pathDelayId,
                            pathDelayTimestamp,
                            pathDelaySrc,
                            pathDelayDst,
                            pathDelayNum,
                            0,
                            pathDelayPath,
                            pathDelaySumDelay,
                            pathDelaySumbytes,
                            pathDelayMindelay,
                            pathDelayMaxdelay,
                            "path_delays");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            // ------------------
            // HOP DELAYS
            // ------------------
            float numberOfNodes = (float) record.oids.length;
            int currSrc;
            int currDst;
            long id;
            long timestamp;
            long srcNode;
            long dstNode;
            long sumOfDelays;
            long numberOfDelays;
            float hitcounter = 1 / (numberOfNodes - 1);
            long sumbytes;
            long mindelay;
            long maxdelay;

            for (currSrc = 0; currSrc < (numberOfNodes); currSrc++) {
                for (currDst = currSrc + 1; currDst < (numberOfNodes); currDst++) {

                    // --- 1) Setting the values ---
                    // Timestamp
                    timestamp = record.ts[currSrc];

                    // Source and Destination
                    srcNode = record.oids[currSrc];
                    dstNode = record.oids[currDst];

                    // Sum of delays and number of delays
                    sumOfDelays = 0;
                    int i = currSrc;
                    numberOfDelays = 1;
                    for (; i < (currDst); i++) {
                        sumOfDelays = sumOfDelays + Math.abs(record.ts[i + 1] - record.ts[i]);
                    }

                    // ID
                    id = timestamp / (1000 * binsize);

                    // SumBytes
                    //logger.debug("record.size = "+record.size);
                    sumbytes = (long) record.size;

                    // --- 2) Read out the current content ---
                    ResultSet content_hop_delays = novidbinterface.getContent(
                            "hdid, id, ts, src, dst, num, hits, sumdelay, sumbytes, mindelay, maxdelay",
                            " WHERE ((id = " + id + ") AND (src = " + srcNode + ") AND (dst = " + dstNode + "))",
                            "hop_delays");

                    // --- 3) Write the new content ---
                    try {
                        if (content_hop_delays.next()) {
                            // Update Row
                            try {
                                long hdid = content_hop_delays.getLong("hdid");
                                long oldSumOfDelays = content_hop_delays.getLong("sumdelay");
                                long oldNumberOfDelays = content_hop_delays.getLong("num");
                                float oldHitcounter = content_hop_delays.getFloat("hits");
                                long oldSumBytes = content_hop_delays.getLong("sumbytes");
                                long oldMinDelay = content_hop_delays.getLong("mindelay");
                                long oldMaxDelay = content_hop_delays.getLong("maxdelay");
                                mindelay = oldMinDelay;
                                if (sumOfDelays < mindelay) {
                                    mindelay = sumOfDelays;
                                }
                                maxdelay = oldMaxDelay;
                                if (sumOfDelays > maxdelay) {
                                    maxdelay = sumOfDelays;
                                }
                                novidbinterface.updateRow(
                                        hdid,
                                        sumOfDelays + oldSumOfDelays,
                                        numberOfDelays + oldNumberOfDelays,
                                        hitcounter + oldHitcounter,
                                        sumbytes + oldSumBytes,
                                        mindelay,
                                        maxdelay,
                                        "hop_delays");
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }
                        } else {
                            mindelay = sumOfDelays;
                            maxdelay = sumOfDelays;
                            // Write new Row
                            novidbinterface.writeRow(
                                    id,
                                    timestamp,
                                    srcNode,
                                    dstNode,
                                    numberOfDelays,
                                    hitcounter,
                                    "",
                                    sumOfDelays,
                                    sumbytes,
                                    mindelay,
                                    maxdelay,
                                    "hop_delays");
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public void addPtProbeStats(PtProbeStats probeStats) {
        if (probeStats == null) {
            logger.warn("could not create ProbeStats object, returning null");
        } else {
            // ---- Save data in Novi database ----
            novidbinterface.exportNodeStats(probeStats);
        }
    }
}
