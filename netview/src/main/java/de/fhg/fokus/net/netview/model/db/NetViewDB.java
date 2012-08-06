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

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.h2.tools.RunScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import com.avaje.ebean.AdminLogging.TxLogLevel;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.SqlRow;
import com.avaje.ebean.config.AutofetchConfig;
import com.avaje.ebean.config.AutofetchMode;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebeaninternal.server.lib.ShutdownManager;
import com.thoughtworks.xstream.XStream;

import de.fhg.fokus.net.geo.WayPoint;
import de.fhg.fokus.net.netview.model.Model;
import de.fhg.fokus.net.netview.model.Network;
import de.fhg.fokus.net.netview.model.Node;
import de.fhg.fokus.net.netview.model.NodeMeasurementProperties;
import de.fhg.fokus.net.netview.model.NodePhysicalProperties;
import de.fhg.fokus.net.netview.model.NodeViewProperties;
import de.fhg.fokus.net.netview.model.Probe;
import de.fhg.fokus.net.netview.sys.NetViewConfig;
import de.fhg.fokus.net.ptapi.PacketTrackRecord;
import de.fhg.fokus.net.ptapi.PtInterfaceStats;
import de.fhg.fokus.net.ptapi.PtProbeStats;
import de.fhg.fokus.net.ptapi.PtBearerInformation;
import java.util.ArrayList;
import java.util.Set;

/**
 * 
 * @author FhG-FOKUS NETwork Research
 * 
 */
public class NetViewDB {
    // sys

    private final Logger logger = LoggerFactory.getLogger(getClass());
    // xml import
    private final XStream xstream;
    private final Class<?>[] aliases = {Node.class, Probe.class};
    private final Class<?>[] omitUidClasses = {NodeViewProperties.class,
        NodePhysicalProperties.class,
        NodeMeasurementProperties.class,
        Probe.class,
        WayPoint.class
    };
    // --
    private final EbeanServer eServer;
    private final Model model;
    private final ScheduledExecutorService scheduler;
    private final TrackRepository trackRepository;
    private final NetViewConfig config;
    private final ServerConfig serverConfig;

    public NetViewDB(Model model, ScheduledExecutorService scheduler, NetViewConfig config) {
        this.scheduler = scheduler;
        this.model = model;
        this.config = config;
        //
        // Database config
        //

        // ### Configuration Objects ###
        this.serverConfig = new ServerConfig();
        DataSourceConfig dataSourceConfig = new DataSourceConfig();

        // ### Configuration Settings ###
        // -> data source
        dataSourceConfig.setDriver(config.getDbDriver());
        dataSourceConfig.setUsername(config.getDbUser());
        dataSourceConfig.setPassword(config.getDbPass());
        dataSourceConfig.setUrl(config.getDbUrl());
        // -> server
        serverConfig.setName("default");
        serverConfig.setDataSourceConfig(dataSourceConfig);
//        serverConfig.setTransactionLogging(TxLogLevel.NONE);
//       serverConfig.setTransactionLogDirectory(config.getNetviewHome().getAbsolutePath() + File.separator + "ebean.logs");

        //    -> entities
        serverConfig.addClass(ProbeStats.class);
        serverConfig.addClass(ProbeLocation.class);
        serverConfig.addClass(InterfaceStats.class);
        serverConfig.addClass(RawTrackData.class);
        serverConfig.addClass(TrackData.class);
        serverConfig.addClass(BearerInformation.class);
        serverConfig.addClass(Network.class);
        serverConfig.addClass(Node.class);
        serverConfig.addClass(NodeViewProperties.class);
        serverConfig.addClass(Probe.class);
        serverConfig.addClass(NodeMeasurementProperties.class);
        serverConfig.addClass(NodePhysicalProperties.class);
        serverConfig.addClass(WayPoint.class);

        // autofetch
        AutofetchConfig autofetchConfig = new AutofetchConfig();
        autofetchConfig.setMode(AutofetchMode.DEFAULT_OFF);
        autofetchConfig.setQueryTuning(false);
        serverConfig.setAutofetchConfig(autofetchConfig);


        //  auto create db if does not exist
        if (!(config.getDbFile()).exists()) {
            serverConfig.setDdlGenerate(true);
            serverConfig.setDdlRun(true);

            this.eServer = EbeanServerFactory.create(serverConfig);

            // create indices
            this.eServer.beginTransaction();
            this.eServer.createSqlUpdate("create index ix_pkt_ts on raw_track_data ( ts );").execute();
            this.eServer.createSqlUpdate("create index ix_pkt_record on raw_track_data (record_id);").execute();
            this.eServer.createSqlUpdate("create index ix_pkt_probe on raw_track_data (probe_id);").execute();
            this.eServer.createSqlUpdate("create index ix_pkt_track on raw_track_data (track_id);").execute();
            this.eServer.createSqlUpdate("create index ix_track_start on track_data (start_ts);").execute();
            this.eServer.createSqlUpdate("create index ix_track_time on track_data (start_ts, stop_ts);").execute();
            this.eServer.createSqlUpdate("create index ix_track_id on track_data (track_id);").execute();
            this.eServer.createSqlUpdate("create index ix_interface_oid on interface_stats (oid);").execute();
            this.eServer.createSqlUpdate("create index ix_interface_timestamp on interface_stats (timestamp);").execute();
            this.eServer.createSqlUpdate("create index ix_probe_oid on probe_stats (oid);").execute();
            this.eServer.createSqlUpdate("create index ix_probe_timestamp on probe_stats (timestamp);").execute();
            this.eServer.endTransaction();

        } else {
            this.eServer = EbeanServerFactory.create(serverConfig);
        }
        //
        // Xml import / export
        //
        this.xstream = new XStream();
        //		xstream.alias("CopyOnWriteA", CopyOnWriteArrayList.class);
        for (Class<?> klass : aliases) {
            this.xstream.alias(klass.getSimpleName().toLowerCase(), klass);
        }
        for (Class<?> klass : omitUidClasses) {
            this.xstream.omitField(klass, "uid");
        }


        this.trackRepository = new TrackRepository(this.eServer,
                this.scheduler, this.model.getProbeIdMarkerMap());

    }

    public void shutdown() {
        // FIXME we shouldn't need to shutdown Ebean manually 

        ShutdownManager.shutdown();
    }

    public EbeanServer getEbeanServer() {
        return eServer;
    }

    public TrackRepository getTrackRepository() {
        return trackRepository;
    }

    public List<Network> getNetworks() {
        return eServer.find(Network.class).join("nodes").join("nodes.view").join("nodes.phy").join("nodes.phy.waypoint").join("nodes.mp").orderBy("label").findList();
    }

    public boolean save(Object obj) {
        eServer.save(obj);
        // TODO check if saved worked ok
        return true;
    }

    /**
     * Remove network 
     * @param network
     * @return
     */
    public boolean deleteNetwork(Network network) {
        logger.debug("remove network " + network);
        // TODO review
        try {
            eServer.beginTransaction();
            eServer.createSqlUpdate("delete from network_node where network_uid = "
                    + "(select uid from network where label = :label)").setParameter("label", network.getLabel()).execute();
            eServer.createSqlUpdate("delete from network where label = :label").setParameter("label", network.getLabel()).execute();
            // manyToMany delete cascade
            eServer.createSqlUpdate("delete from network_node where network_uid=:uid").setParameter("uid", network.getUid()).execute();


            eServer.commitTransaction();
        } catch (Exception e) {
            logger.debug(e.getMessage());
            return false;
        } finally {
            eServer.endTransaction();
        }
        return true;
    }

    public boolean locationAlreadyRegistered(Node node, List<SqlRow> receivedLocations) {
        for (SqlRow row : receivedLocations) {
            if ((Long) row.get("oid") == node.getMp().getProbe().getProbeId()) {
                return true;
            }
        }
        return false;
    }

    public void setPreviousEntries(Node node) {

        String name;
        String locationName;
        if (node.getView().getLabel(node).contains("@")) {
            name = node.getView().getLabel().substring(0, node.getView().getLabel().indexOf("@"));
            locationName = node.getView().getLabel().substring(node.getView().getLabel().indexOf("@") + 1);
            model.getNodeStatsLayer().previousNames.put(
                    node.getMp().getProbe().getProbeId(),
                    name);
            model.getNodeStatsLayer().previousLocationNames.put(
                    node.getMp().getProbe().getProbeId(),
                    locationName);
        } else {
            locationName = node.getView().getLabel(node);
            model.getNodeStatsLayer().previousLocationNames.put(
                    node.getMp().getProbe().getProbeId(),
                    locationName);
        }

        model.getNodeStatsLayer().previousAddresses.put(
                node.getMp().getProbe().getProbeId(),
                node.getMp().getProbe().getLabel());

    }

    /**
     * Import nodes and assign them to a network
     * 
     * Currently only XML data is supported.
     * 
     * 
     * @param network
     * @param file
     * @return
     * @throws FileNotFoundException 
     */
    @SuppressWarnings("unchecked")
    public void importNodes(Network network, File file) throws FileNotFoundException {
        FileReader fr = new FileReader(file);
        List<Node> nodes = (List<Node>) xstream.fromXML(fr);

        List<SqlRow> receivedLocations = trackRepository.getFirstPositions();
        List<Node> newNodes = new ArrayList<Node>();
        Set<Node> RepoNodes = trackRepository.getNodesHashSet();
        for (Node node : nodes) {

            // Setting the new Names and the IP-Address immediately
            setPreviousEntries(node);
            for (Node node2 : RepoNodes) {
                if (node2.getMp().getProbe().getProbeId() == node.getMp().getProbe().getProbeId()) {
                    node2.getView().setLabel(node.getView().getLabel());
                    node2.getMp().getProbe().setLabel(node.getMp().getProbe().getLabel());
                }
            }

            if (!locationAlreadyRegistered(node, receivedLocations)) {
                newNodes.add(node);
                trackRepository.saveLocation(node);
            }
        }
        for (Node node : newNodes) {

            logger.debug("Importing " + NodeViewProperties.getLabel(node));
            Node nodeBean = getNodeFromUid(node.uid);
            if (nodeBean == null) {
                nodeBean = node;
            } else {
                // TODO cascade probe label 
                Node.deepcopy(node, nodeBean);
            }
            try {
                logger.debug("importing into net: " + network.getLabel());

                eServer.beginTransaction();
                eServer.save(nodeBean);
                // insert or update

                // FIXME: hack for adding a dummy network. 
                // there will be only one network.
                if (getNetworks().size() == 0) {
                    addNetwork(network);
                }
                if (eServer.createSqlQuery("select * from network_node where network_uid=:net and node_uid= :node").setParameter("net", network.getUid()).setParameter("node", nodeBean.getUid()).findList().size() == 0) {
//					logger.debug(arg0)//
                    eServer.createSqlUpdate("insert into network_node values (:net, :node)").setParameter("net", network.getUid()).setParameter("node", nodeBean.getUid()).execute();

                }

                eServer.commitTransaction();
            } finally {
                eServer.endTransaction();
            }

        }
        model.getNodeStatsLayer().updateLocation(model.getNodeStatsLayer().getCurrentTs());
    }

    public List<Node> getNewNodes(Set<Node> nodeSet) {
        List<Long> uids = new ArrayList<Long>();
        for (Node tempNode : nodeSet) {
            uids.add(tempNode.getUid());
        }
        List<Node> repoNodes = trackRepository.getNodes();
        List<Node> newNodes = new ArrayList<Node>();
        for (Node tempNode : repoNodes) {
            if (!uidAlreadyReaded(uids, tempNode)) {
                newNodes.add(tempNode);
            }
        }
        return newNodes;
    }

    /**
     * Import nodes and assign them to a network
     *
     * Uses the entries of the database
     *
     *
     * @param network
     * @return
     */
    public void importNodesAlternative(Model model, Set<Node> nodeSet) {
        List<Node> newNodes = getNewNodes(nodeSet);
        for (Node node : newNodes) {
            if (node.getView().getLabel().substring(node.getView().getLabel().indexOf("@") + 1).equals("Unknown")) {
                String locationName = trackRepository.getLocationNameOverWebservice(
                        node.getMp().getProbe().getProbeId(),
                        node.getPhy().getWaypoint().getLatitude(),
                        node.getPhy().getWaypoint().getLongitude());
                node.getView().setLabel(node.getView().getLabel().substring(0, node.getView().getLabel().indexOf("@") + 1) + locationName);
            }
            model.getNodeStatsLayer().previousNames.put(
                    node.getMp().getProbe().getProbeId(),
                    node.getView().getLabel().substring(0, node.getView().getLabel().indexOf("@")));
            model.getNodeStatsLayer().previousLocationNames.put(
                    node.getMp().getProbe().getProbeId(),
                    node.getView().getLabel().substring(node.getView().getLabel().indexOf("@") + 1));
            model.getNodeStatsLayer().previousAddresses.put(
                    node.getMp().getProbe().getProbeId(),
                    node.getMp().getProbe().getLabel());
            logger.debug("Importing " + NodeViewProperties.getLabel(node));
            Node nodeBean = getNodeFromUid(node.uid);
            if (nodeBean == null) {
                nodeBean = node;
            } else {
                // TODO cascade probe label
                Node.deepcopy(node, nodeBean);
            }
            try {
                logger.debug("importing into net: " + model.network.getLabel());

                eServer.beginTransaction();
                eServer.save(nodeBean);
                // insert or update

                // FIXME: hack for adding a dummy network.
                // there will be only one network.
                if (getNetworks().size() == 0) {
                    addNetwork(model.network);
                }
                if (eServer.createSqlQuery("select * from network_node where network_uid=:net and node_uid= :node").setParameter("net", model.network.getUid()).setParameter("node", nodeBean.getUid()).findList().size() == 0) {
//					logger.debug(arg0)//
                    eServer.createSqlUpdate("insert into network_node values (:net, :node)").setParameter("net", model.network.getUid()).setParameter("node", nodeBean.getUid()).execute();

                }

                eServer.commitTransaction();
            } finally {
                eServer.endTransaction();
            }
        }
    }

    public boolean uidAlreadyReaded(List<Long> uids, Node node) {
        for (long uid : uids) {
            if (node.getUid() == uid) {
                return true;
            }
        }
        return false;
    }

    public Node getNodeFromUid(long uid) {
        // eServer.createSqlQuery("SELECT * FROM node, node_measurement_properties, node_physical_properties, probe, network,  WHERE uid=:")
        return eServer.find(Node.class).join("view").join("phy").join("mp").join("networks").where().idEq(uid).findUnique();
    }

    public void deleteNode(Node node) {
        logger.debug("delete node " + node);
        //		eServer.delete(node);
        Node nodeBean = getNodeFromUid(node.uid);
        if (nodeBean != null) {
            logger.debug(xstream.toXML(nodeBean));

            eServer.delete(nodeBean);
        }

        //		if( nodeBean!=null){
        //			eServer.createSqlUpdate("delete from NodeViewProperties where uid")
        //			.execute();
        //		}

    }

    /**
     * Add a new network
     * @param network
     * @return true if succeed
     */
    public boolean addNetwork(Network network) {
        if (network == null) {
            logger.warn("network is null, ignoring it.");
            return false;
        }
        try {
            Network net = eServer.find(Network.class) //			.join("nodes")
                    .where().eq("label", network.getLabel()).findUnique();
            if (net == null) {
                eServer.save(network);
            }
            return true;
        } catch (RuntimeException e) {
            logger.debug(e.getMessage());
        }
        return false;
    }

    /**
     * Purge all packet tracks/stats from the database.
     * 
     */
    public void purgePacketTracks() {
        try {
            eServer.beginTransaction();
            eServer.createSqlUpdate("delete from RAW_TRACK_DATA").execute();
            eServer.createSqlUpdate("delete from TRACK_DATA").execute();
            eServer.createSqlUpdate("delete from INTERFACE_STATS").execute();
            eServer.createSqlUpdate("delete from PROBE_STATS").execute();
            eServer.commitTransaction();
        } finally {
            eServer.endTransaction();
        }

    }

    /**
     * Import tracks/stats from a File (object stream).
     * @param f
     * @return the number of objects imported from the file.
     */
    public int importTracks(File file) {
        ObjectInputStream ois = null;
        Object obj = null;
        int count = 0;

        try {
            ois = new ObjectInputStream(new FileInputStream(file));

            while (true) {
                obj = ois.readObject();
                if (obj != null) {
                    if (obj instanceof PacketTrackRecord) {
                        trackRepository.addPacketTrackRecord((PacketTrackRecord) obj);
                        count++;
                    } else if (obj instanceof PtInterfaceStats) {
                        trackRepository.addPtInterfaceStats((PtInterfaceStats) obj);
                        count++;
                    } else if (obj instanceof PtProbeStats) {
                        trackRepository.addPtProbeStats((PtProbeStats) obj);
                        count++;
                    } else if (obj instanceof PtBearerInformation) {
                        trackRepository.addBearerInformation((PtBearerInformation) obj);
                        count++;
                    } else {
                        logger.warn("Unexpected object of class " + obj.getClass() + " in object stream.");
                    }
                }

            }
        } catch (EOFException eof) {
            /* NOP */
        } catch (IOException ioe) {
            logger.warn("IO error while reading " + file.getAbsolutePath());
            ioe.printStackTrace();
        } catch (ClassNotFoundException cnfe) {
            logger.warn("Unknown  class in object stream.");
            cnfe.printStackTrace();
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    /* NOP */
                }
            }
        }
        return count;
    }

    /**
     * Purge all nodes from database. This should be use with care!
     * 
     */
    public void purgeNodes() {
        try {
            eServer.beginTransaction();
            eServer.createSqlUpdate("SET REFERENTIAL_INTEGRITY FALSE").execute();
            eServer.createSqlUpdate("delete from network_node").execute();
            eServer.createSqlUpdate("delete from node").execute();
            eServer.createSqlUpdate("delete from way_point").execute();
            eServer.createSqlUpdate("delete from node_physical_properties").execute();
            eServer.createSqlUpdate("delete from node_view_properties").execute();
            eServer.createSqlUpdate("delete from node_measurement_properties").execute();
            eServer.createSqlUpdate("delete from probe").execute();
            eServer.createSqlUpdate("delete from PROBE_STATS").execute();
            eServer.createSqlUpdate("delete from INTERFACE_STATS").execute();
            eServer.createSqlUpdate("delete from NETWORK").execute();


            eServer.createSqlUpdate("SET REFERENTIAL_INTEGRITY TRUE").execute();
            eServer.commitTransaction();
        } finally {
            eServer.endTransaction();
        }
    }

    public long getPacketTrackCount() {
        SqlRow row = eServer.createSqlQuery("select count(*) as n from track_data").findUnique();
        return row.getLong("n");
    }

    /**
     * Run sql script 
     *  
     * @param file sql script file
     * @return true if successfull
     */
    public boolean runSqlScript(File script) {
        try {
            RunScript.execute(
                    config.getDbUrl(),
                    config.getDbUser(),
                    config.getDbPass(),
                    script.getAbsolutePath(),
                    config.getDbCharsetName(),
                    config.isDbContinueOnError());
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return false;
    }
}
