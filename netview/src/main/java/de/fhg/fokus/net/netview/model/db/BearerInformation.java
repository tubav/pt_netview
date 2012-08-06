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

import java.io.Serializable;

import de.fhg.fokus.net.ptapi.PtBearerInformation;

/**
 * Bearer information.
 * 
 * @author FhG-FOKUS NETwork Research
 * 
 */
@Entity
public class BearerInformation implements Serializable {
    
    @Id
    private long uid;
    
    @Column
    private short ruleFlag;
    
    @Column
    private long ruleId;

    @Column
    private String apn;
    
    @Column
    private String ruleName;
    
    @Column
    private String imsi;
    
    @Column
    private long qci;
    
    @Column
    private long maximumDownload;
    
    @Column
    private long maximumUpload;
    
    @Column
    private long guaranteedDownload;
    
    @Column
    private long guaranteedUpload;
    
    @Column
    private long apnDownload;
    
    @Column
    private long apnUpload;
        
    @Column
    private long timestamp;
      
    @Column
    private String sourceAddress;
    
    @Column 
    private String destinationAddress;
    
    @Column
    private int sourcePort;
    
    @Column
    private int destinationPort;

    public BearerInformation() {
    }
    
    public BearerInformation(String apn, String bearerClass,
            String imsi, String srcIp, int srcPort, String dstIp,
            int dstPort, long observationTimeMilliseconds) {
        this.apn = apn;
        this.ruleName = ruleName;
        this.destinationAddress = dstIp;
        this.destinationPort = dstPort;
        this.imsi = imsi;
        this.sourceAddress = srcIp;
        this.sourcePort = srcPort;
        this.timestamp = observationTimeMilliseconds;
    }
    
    public BearerInformation(PtBearerInformation record) {
        this.ruleFlag = record.ruleFlag;
        this.ruleId = record.ruleId;
        this.apn = record.apn;
        this.ruleName = record.ruleName;
        this.imsi = record.imsi;
        this.maximumDownload = record.maxDl;
        this.maximumUpload = record.maxUl;
        this.guaranteedDownload = record.guaDl;
        this.guaranteedUpload = record.guaUl;
        this.apnDownload = record.apnDl;
        this.apnUpload = record.apnUl;
        this.destinationAddress = record.dstIp.toString().substring(1);
        this.destinationPort = record.dstPort;
        this.sourceAddress = record.srcIp.toString().substring(1);
        this.sourcePort = record.srcPort;
        this.timestamp = record.observationTimeMilliseconds;
        
    }

    public String getApn() {
        return apn;
    }

    public void setApn(String apn) {
        this.apn = apn;
    }

    public long getApnDownload() {
        return apnDownload;
    }

    public void setApnDownload(long apnDownload) {
        this.apnDownload = apnDownload;
    }

    public long getApnUpload() {
        return apnUpload;
    }

    public void setApnUpload(long apnUpload) {
        this.apnUpload = apnUpload;
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

    public long getGuaranteedDownload() {
        return guaranteedDownload;
    }

    public void setGuaranteedDownload(long guaranteedDownload) {
        this.guaranteedDownload = guaranteedDownload;
    }

    public long getGuaranteedUpload() {
        return guaranteedUpload;
    }

    public void setGuaranteedUpload(long guaranteedUpload) {
        this.guaranteedUpload = guaranteedUpload;
    }

    public String getImsi() {
        return imsi;
    }

    public void setImsi(String imsi) {
        this.imsi = imsi;
    }

    public long getMaximumDownload() {
        return maximumDownload;
    }

    public void setMaximumDownload(long maximumDownload) {
        this.maximumDownload = maximumDownload;
    }

    public long getMaximumUpload() {
        return maximumUpload;
    }

    public void setMaximumUpload(long maximumUpload) {
        this.maximumUpload = maximumUpload;
    }

    public long getQci() {
        return qci;
    }

    public void setQci(long qci) {
        this.qci = qci;
    }

    public short getRuleFlag() {
        return ruleFlag;
    }

    public void setRuleFlag(short ruleFlag) {
        this.ruleFlag = ruleFlag;
    }

    public long getRuleId() {
        return ruleId;
    }

    public void setRuleId(long ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }
    
}
