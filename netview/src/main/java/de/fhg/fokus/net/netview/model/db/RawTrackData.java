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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import de.fhg.fokus.net.ptapi.PacketTrackRecord;

@Entity
public class RawTrackData {

    public RawTrackData() {
    }

    public RawTrackData(int uID, int trackID, int packetID, int recordID, int probeID,
            long ts, int ttl, int nextHop, int delay, int hopNumber) {
        this.uid = uID;
        this.trackID = trackID;
        this.packetID = packetID;
        this.recordID = recordID;
        this.probeID = probeID;
        this.ts = ts;
        this.ttl = ttl;
        this.nextHop = nextHop;
        this.delay = delay;
        this.hopNumber = hopNumber;
    }

    public RawTrackData(PacketTrackRecord record, int i, long recordID) {
        this.trackID = record.trackid;
        this.packetID = record.pktid;
        this.probeID = record.oids[i];
        this.recordID = recordID;
        this.ruleID = record.ruleId;
        this.ttl = record.ttl[i];
        this.ts = record.ts[i];
        this.hopNumber = i;
        if (i < record.oids.length - 1) {
            this.nextHop = record.oids[i + 1];
            this.delay = record.ts[i + 1] - record.ts[i];
        }

        // Not all records contain source addresses
        if (record.sourceAddress != null) {
            this.sourceAddress = record.sourceAddress.toString().substring(1);
        } else {
            this.sourceAddress = "unkown source";
        }

        // Not all records contain destination addresses
        if (record.destinationAddress != null) {
            this.destinationAddress = record.destinationAddress.toString().substring(1);
        } else {
            this.destinationAddress = "unkown destination";
        }
        
        this.sourcePort = record.sourcePort;
        this.destinationPort = record.destinationPort;
        this.protocolIdentifier = record.protocolIdentifier;
    }
    /**
     * Unique Id
     */
    @Id
    private int uid;
    @Column
    private int trackID;
    @Column
    private int packetID;
    @Column
    private long recordID;
    @Column
    private long probeID;
    @Column
    private long ruleID;
    @Column
    private long ts;
    @Column
    private int ttl;
    @Column
    private long nextHop;
    @Column
    private long delay;
    @Column
    private int hopNumber;
    @Column
    private String sourceAddress;
    @Column
    private int sourcePort;
    @Column
    private String destinationAddress;
    @Column
    private int destinationPort;
    @Column
    private short protocolIdentifier;

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getTrackID() {
        return trackID;
    }

    public void setTrackID(int trackID) {
        this.trackID = trackID;
    }

    public int getPacketID() {
        return packetID;
    }

    public void setPacketID(int packetID) {
        this.packetID = packetID;
    }

    public long getRecordID() {
        return recordID;
    }

    public void setRecordID(long recordID) {
        this.recordID = recordID;
    }

    public long getProbeID() {
        return probeID;
    }

    public void setProbeID(long probeID) {
        this.probeID = probeID;
    }
    
    public long getRuleID() {
        return ruleID;
    }

    public void setRuleID(long ruleID) {
        this.ruleID = ruleID;
    }
    
    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public long getNextHop() {
        return nextHop;
    }

    public void setNextHop(long nextHop) {
        this.nextHop = nextHop;
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public int getHopNumber() {
        return hopNumber;
    }

    public void setHopNumber(int hopNumber) {
        this.hopNumber = hopNumber;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public int getDestinationPort() {
        return destinationPort;
    }

    public void setDestinationPort(int destinationPort) {
        this.destinationPort = destinationPort;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public void setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
    }

    public short getProtocolIdentifier() {
        return this.protocolIdentifier;
    }
    
    public void setProtocolIdentifier(short protocolIdentifier) {
        this.protocolIdentifier = protocolIdentifier;    
    }
}
