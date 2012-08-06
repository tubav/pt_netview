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

import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.netview.model.db.TrackRecordRepository;
import de.fhg.fokus.net.worldmap.util.EventSupport;
import de.fhg.fokus.net.ptapi.PacketTrackRecord;
import de.fhg.fokus.net.ptapi.PtInterfaceStats;
import de.fhg.fokus.net.ptapi.PtProbeLocation;
import de.fhg.fokus.net.ptapi.PtProbeStats;
import de.fhg.fokus.net.ptapi.PtBearerInformation;

/**
 * Default packet track collector implementation
 * 
 * @author FhG-FOKUS NETwork Research
 *
 */
public class PacketTrackCollector {

    public static enum PtcEventType {

        /**
         * eventData: clientAddress
         */
        CLIENT_CONNECTED,
        /**
         * eventData: null
         */
        CLIENT_DISCONNECTED,
        /**
         * eventData: null
         */
        STARTED,
        STOPPED
    }

    public static class PtcEventData {

        public SocketAddress clientAddress;

        public PtcEventData() {
        }

        public PtcEventData(SocketAddress clientAddress) {
            this.clientAddress = clientAddress;
        }
    }
    private final EventSupport<PtcEventType, PtcEventData> eventSupport;
    // sys
    private final ExecutorService executor;
    private final TrackRecordRepository db;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    // model
    private final List<Socket> clients = new CopyOnWriteArrayList<Socket>();
    private long numberOfRecords = 0;
    private long startedAt = 0;

    public PacketTrackCollector(TrackRecordRepository db, ExecutorService executor) {
        this.executor = executor;
        this.db = db;
        this.eventSupport = new EventSupport<PtcEventType, PtcEventData>(executor);
    }

    /**
     * Handle incoming clients
     * 
     * @param socket
     */
    private void handleConnection(final Socket socket) {
        final SocketAddress remote = socket.getRemoteSocketAddress();
        clients.add(socket);
        eventSupport.dispatch(PtcEventType.CLIENT_CONNECTED, new PtcEventData(remote));
        try {
            InputStream inStream = socket.getInputStream();
            ObjectInput inObjectStream = new ObjectInputStream(inStream);

            Object obj;
//			socket.getRemoteSocketAddress();
            while (true) {
                obj = inObjectStream.readObject();
                if (obj != null) {
                    if (obj instanceof PacketTrackRecord) {
                        db.addPacketTrackRecord((PacketTrackRecord) obj);
                        numberOfRecords++;
                    } else if (obj instanceof PtInterfaceStats) {
                        db.addPtInterfaceStats((PtInterfaceStats) obj);
                    } else if (obj instanceof PtProbeStats) {
                        db.addPtProbeStats((PtProbeStats) obj);
                    } else if (obj instanceof PtProbeLocation) {
                        db.addPtProbeLocation((PtProbeLocation) obj);
                    } else if (obj instanceof PtBearerInformation) {
                        db.addBearerInformation((PtBearerInformation) obj);
                    } else {
                        logger.warn("Unexpected object of class " + obj.getClass() + " received.");
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.debug(e.getMessage());
            e.printStackTrace();
            try {
                socket.getInputStream().close();
                socket.close();
            } catch (Exception e1) {
                logger.debug(e1.getMessage());
            }
            clients.remove(socket);
            eventSupport.dispatch(PtcEventType.CLIENT_DISCONNECTED, new PtcEventData(remote));
        }
    }
    private ServerSocket serverSocket;

    public boolean isBound() {
        if (serverSocket == null) {
            return false;
        }
        return serverSocket.isBound();
    }

    public String getLocalAddress() {
        if (isBound()) {
            return serverSocket.getLocalSocketAddress().toString();
        }
        return "";
    }
    private boolean shouldRun = true;

    /**
     * Bind collector to a port
     * 
     * @param port
     * @throws Exception
     */
    public void bind(int port) {
        shouldRun = true;
        numberOfRecords = 0;
        try {
            serverSocket = new ServerSocket(port);
            startedAt = System.currentTimeMillis();
            eventSupport.dispatch(PtcEventType.STARTED, null);
            while (shouldRun) {
                final Socket client = serverSocket.accept();
                executor.execute(new Runnable() {

                    @Override
                    public void run() {
                        handleConnection(client);
                    }
                });
            }
        } catch (Exception e) {
            logger.debug(e.getMessage());
        } finally {
            eventSupport.dispatch(PtcEventType.STOPPED, new PtcEventData());
            serverSocket = null;
        }
    }

    public long getStartedAt() {
        return startedAt;
    }

    public long getNumberOfRecords() {
        return numberOfRecords;
    }

    /**
     * Stop collector
     */
    public void stop() {
        logger.debug("=> stopping collector...");
        shouldRun = false;
        for (Socket socket : clients) {
            logger.debug("Closing client: {}", socket);
            try {
                socket.getInputStream().close();
                socket.close();
            } catch (Exception e) {
                logger.debug(e.getMessage());
            }
        }
        try {
            serverSocket.close();
        } catch (Exception e) {
            logger.debug(e.getMessage());
        }
    }

    /**
     * Remove event listener
     * 
     * @param evt
     * @param lsn
     */
    public void addEventListener(PtcEventType evt, EventSupport.EventListener<PtcEventData> lsn) {
        eventSupport.addEventListener(evt, lsn);
    }

    /**
     * Add event listener;
     * @param lsn
     */
    public void removeEventListener(EventSupport.EventListener<PtcEventData> lsn) {
        eventSupport.removeEventListener(lsn);
    }

    public List<Socket> getClients() {
        return clients;
    }
}
