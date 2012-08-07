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

import de.fhg.fokus.net.ptapi.PacketTrackRecord;
import de.fhg.fokus.net.ptapi.PtProbeStats;
import java.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author FhG-FOKUS NETwork Research
 * 
 */
public class NetViewDB {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final TrackRepository trackRepository;
    
    public NetViewDB() {
        this.trackRepository = new TrackRepository();
    }

    public void shutdown() {
        // NoviDB CLOSE!!!
    }

    public TrackRepository getTrackRepository() {
        return trackRepository;
    }

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
                    } else if (obj instanceof PtProbeStats) {
                        trackRepository.addPtProbeStats((PtProbeStats) obj);
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
}
