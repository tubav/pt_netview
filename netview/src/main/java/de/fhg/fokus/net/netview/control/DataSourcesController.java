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
package de.fhg.fokus.net.netview.control;

import de.fhg.fokus.net.netview.model.Model;
import de.fhg.fokus.net.netview.model.db.NetViewDB;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data source controller
 *
 * @author FhG-FOKUS NETwork Research
 *
 */
public class DataSourcesController {
    // sys

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Timer timer = new Timer("Datasources");
    private final Preferences prefs = Preferences.userNodeForPackage(getClass());

    private static enum PrefKeys {

        LAST_VISITED_NODES_DIRECTORY,
        LAST_VISITED_TRACKS_DIRECTORY,
        COLLECTOR_PORT,
        START_COLLECTOR
    }
    private final ExecutorService executor;

    public static enum EventType {

        CONTROLLER_STARTED
    }
    // ctrl
    private int collectorPort;
    // view
    private static SimpleDateFormat iso8601s = new SimpleDateFormat(
            "yyyy-MM-dd  HH:mm:ss.S");
    // --

    public static class EventData {
    }
    private final NetViewDB db;
    private final PacketTrackCollector collector;
    private final Model model;

    public DataSourcesController(ExecutorService executor, Model model) {
        super();

        this.model = model;
        this.executor = executor;
        this.collector = new PacketTrackCollector(model.getTrackRepository(), this.executor);
        this.db = this.model.getDb();
    }

    private void updateCollectorStatus() {
        if (collector.isBound()) {
            logger.debug(String.format("Bound to:\t%s \n", collector.getLocalAddress()));
            logger.debug(String.format("Started at:\t%s \n", iso8601s.format(new Date(collector.getStartedAt()))));
            logger.debug(String.format("Records:\t%d\n", collector.getNumberOfRecords()));
            if (collector.getClients().size() > 0) {
                logger.debug(String.format("Packet Track Exporters: %d ", collector.getClients().size()));
                int i = 1;
                for (Socket socket : collector.getClients()) {
                    logger.debug(String.format("\n - %d: %s ", i++, socket));
                }
            }
        } else {
            logger.debug("Not running!");
        }
    }
    
    public void init() {
        setupCollector();
    }
    private final TimerTask timerTaskUpdateCollectorStatus = new TimerTask() {

        @Override
        public void run() {
            if (collector.isBound()) {
                updateCollectorStatus();
            }
        }
    };

    private void setupCollector() {
        try {
            collectorPort = Integer.parseInt("40123");
        } catch (Exception e) {
            logger.error("Invalid port format!");
        }
        logger.debug("Starting collector..");
        collector.bind(collectorPort);
    }

    public NetViewDB getDB() {
        return this.db;
    }

    public Model getModel() {
        return this.model;
    }
}
