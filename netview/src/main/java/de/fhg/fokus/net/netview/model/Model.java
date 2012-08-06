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
package de.fhg.fokus.net.netview.model;

import de.fhg.fokus.net.netview.control.DataSourcesController;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.netview.model.db.NetViewDB;
import de.fhg.fokus.net.netview.model.db.TrackRepository;
import de.fhg.fokus.net.netview.sys.NetViewConfig;
import de.fhg.fokus.net.netview.view.map.NodeStatsLayer;
import de.fhg.fokus.net.worldmap.MapMarker;
import de.fhg.fokus.net.worldmap.layers.track.SplineLayer;
import de.fhg.fokus.net.worldmap.util.EventSupport;
import de.fhg.fokus.net.worldmap.util.EventSupport.EventListener;

// 1. model a network -> model.network.NodeSet (network, overlay)
// how to synchronize jtree model with MainModel
public final class Model implements PersistentPreferences {

    public final Network network = new Network("the network");
    // sys
    private final static Logger logger = LoggerFactory.getLogger(Model.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    // properties
    private final NetViewDB db;
    private final NetViewConfig config;
    private DataSourcesController dsc = null;
    // cache
    private final Set<Network> networks = new ConcurrentSkipListSet<Network>();
    private final ConcurrentMap<Long, MapMarker> probeIdMarkerMap = new ConcurrentHashMap<Long, MapMarker>();

    /**
     * A container for passing mode event data. 
     *
     */
    public static final class ModelEventData {

        public Node node;
        public Network network;

        public ModelEventData(Network network, Node node) {
            this.node = node;
            this.network = network;
        }

        public ModelEventData(Node node) {
            this.node = node;
        }

        public ModelEventData(Network network) {
            this.network = network;
        }
    }

    public static enum ModelEventType {

        /**
         * Node added. Fields e.node and e.network set. 
         */
        NODE_ADDED,
        /**
         * Node removed. Fields e.node and e.network set. 
         */
        NODE_REMOVED,
        /**
         * Network loaded. Field code e.network set.
         */
        NETWORK_LOADED,
        /**
         * Network unloaded. Field e.network set.
         */
        NETWORK_UNLOADED
    }
    private final EventSupport<ModelEventType, ModelEventData> eventSupport;
    private final ExecutorService executor;
    //	private final List<Network> networks = new CopyOnWriteArrayList<Network>();
    // layers
    // TODO get config from NetViewConfig
    private final SplineLayer splineLayer;
    private final NodeStatsLayer nodeStatsLayer;
    //	private final TrackLayer<NodeOld> cspLayer = new TrackLayer<NodeOld>("CSP Overlay");
    private final Preferences prefs = Preferences.userNodeForPackage(getClass());

    private static enum PrefKeys {

        TRACK_LAYER_VISIBLE,
        CSP_LAYER_VISIBLE
    }

    public Model(ExecutorService executor, NetViewConfig cfg) {
        // FIXME setting a static uid = 1
        network.setUid(1);

        this.executor = executor;
        this.eventSupport = new EventSupport<ModelEventType, ModelEventData>(this.executor);

        this.db = new NetViewDB(this, scheduler, cfg);
        this.config = cfg;

        // setup layers
        this.splineLayer = new SplineLayer("Network Tracks", 100);
        this.nodeStatsLayer = new NodeStatsLayer("Node Stats", getTrackRepository());
        splineLayer.setLevel(10);
        nodeStatsLayer.setLevel(20);
        //		cspLayer.setLevel(15);
    }

    public SplineLayer getSplineLayer() {
        return splineLayer;
    }

    public NodeStatsLayer getNodeStatsLayer() {
        return nodeStatsLayer;
    }

    @Override
    public void loadPreferences() {
        splineLayer.setVisible(prefs.getBoolean(PrefKeys.TRACK_LAYER_VISIBLE + "", true));
        executor.execute(new Runnable() {

            @Override
            public void run() {
                loadModel();
            }
        });
    }

    /**
     * Load network, and referenced nodes into model
     * @param network
     */
    private boolean loadNetwork(Network network) {
        if (network == null) {
            logger.warn("Network should not be null, ignoring it!");
            return false;
        }
        logger.debug("Loading network {}", network);
        if (networks.contains(network)) {
            logger.debug("Network  \"{}\" already loaded, I'll try to unload it first", network);
            if (!unloadNetwork(network)) {
                logger.warn("Could not unload network: {}", network);
                return false;
            }
        }
        networks.add(network);
        for (Node node : network.getNodes()) {
            eventSupport.dispatch(ModelEventType.NODE_ADDED, new ModelEventData(network, node));
        }
        eventSupport.dispatch(ModelEventType.NETWORK_LOADED, new ModelEventData(network));
        return true;
    }

    /**
     * Unload network 
     * 
     * @param network
     * @return true in case of success
     */
    public boolean unloadNetwork(Network network) {
        if (networks.contains(network)) {
            logger.debug("Unloading network {}", network);

            networks.remove(network);
            for (Node node : network.getNodes()) {
                eventSupport.dispatch(ModelEventType.NODE_REMOVED, new ModelEventData(network, node));
            }
            eventSupport.dispatch(ModelEventType.NETWORK_UNLOADED, new ModelEventData(network));
            return true;
        } else {
            logger.warn("Network \"{}\" not loaded, ignoring it.", network);
        }
        return false;
    }

    /**
     * Load all networks from database
     */
    public void refreshNetworks() {
        networks.clear();
        for (Network net : db.getNetworks()) {
            loadNetwork(net);
        }

    }

    /**
     * Load model data from database
     */
    private void loadModel() {
        refreshNetworks();

    }

    @Override
    public void savePreferences() {
        logger.debug("saving main model");
        //		prefs.putBoolean(PrefKeys.CSP_LAYER_VISIBLE+"", cspLayer.isVisible());
        prefs.putBoolean(PrefKeys.TRACK_LAYER_VISIBLE + "", splineLayer.isVisible());


    }

    public void addNodeEventListener(ModelEventType evt, EventListener<ModelEventData> lsn) {
        eventSupport.addEventListener(evt, lsn);
    }

    public void removeNodeEventListener(EventListener<ModelEventData> lsn) {
        eventSupport.removeEventListener(lsn);
    }

    public TrackRepository getTrackRepository() {
        return db.getTrackRepository();
    }

    public NetViewDB getDb() {
        return db;
    }

    public NetViewConfig getConfig() {
        return config;
    }

    public Set<Network> getNetworks() {
        return networks;
    }

    public void unloadNetworks() {
        for (Network net : networks) {
            unloadNetwork(net);
        }

    }

    public ConcurrentMap<Long, MapMarker> getProbeIdMarkerMap() {
        return probeIdMarkerMap;
    }

    public void setDataSourcesController(DataSourcesController dsc) {
        this.dsc = dsc;
        this.db.getTrackRepository().dsc = dsc;
    }

    public DataSourcesController getDataSourcesController() {
        return this.dsc;
    }
}
