package de.fhg.fokus.net.worldmap.layers.track;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bearer {
    private static Logger logger = LoggerFactory.getLogger(Bearer.class);
    
    private ArrayList<Flow> flowList = new ArrayList<Flow>();
    private boolean flowView = false;
    private long bearerId;
    private long ruleId;
    private String apn;
    private String ruleName;
    private String imsi;
    private long qci;
    private long maxDownload;
    private long maxUpload;
    private long guaDownload;
    private long guaUpload;
    private long apnDownload;
    private long apnUpload;
    private String srcIp;
    private String dstIp;
    private int srcPort;
    private int dstPort;
            
    private Track track;
    public View view;

    public String getApn() {
        return apn;
    }

    public void setApn(String apn) {
        this.apn = apn;
    }
    
    public Track getTrack() {
        return track;
    }
    
    public void setTrack(Track track) {
        this.track = track;
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

    public long getBearerId() {
        return bearerId;
    }

    public void setBearerId(long bearerId) {
        this.bearerId = bearerId;
    }

    public String getDstIp() {
        return dstIp;
    }

    public void setDstIp(String dstIp) {
        this.dstIp = dstIp;
    }

    public int getDstPort() {
        return dstPort;
    }

    public void setDstPort(int dstPort) {
        this.dstPort = dstPort;
    }

    public long getGuaDownload() {
        return guaDownload;
    }

    public void setGuaDownload(long guaDownload) {
        this.guaDownload = guaDownload;
    }

    public long getGuaUpload() {
        return guaUpload;
    }

    public void setGuaUpload(long guaUpload) {
        this.guaUpload = guaUpload;
    }

    public String getImsi() {
        return imsi;
    }

    public void setImsi(String imsi) {
        this.imsi = imsi;
    }

    public long getMaxDownload() {
        return maxDownload;
    }

    public void setMaxDownload(long maxDownload) {
        this.maxDownload = maxDownload;
    }

    public long getMaxUpload() {
        return maxUpload;
    }

    public void setMaxUpload(long maxUpload) {
        this.maxUpload = maxUpload;
    }

    public long getQci() {
        return qci;
    }

    public void setQci(long qci) {
        this.qci = qci;
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

    public String getSrcIp() {
        return srcIp;
    }

    public void setSrcIp(String srcIp) {
        this.srcIp = srcIp;
    }

    public int getSrcPort() {
        return srcPort;
    }

    public void setSrcPort(int srcPort) {
        this.srcPort = srcPort;
    }

    public void clearFlowList() {
        flowList.clear();
    }

    public void setFlowList(ArrayList<Flow> flows) {
        flowList = flows;
    }
    
    public boolean isFlowId(long fid) {
        if(this.hasFlows()) {
            for(int i=0;i<flowList.size();i++) {
                if(flowList.get(i).getFlowId() == fid)
                    return true;
            }
        }
        return false;
    }
    
    public ArrayList<Flow> getFlows() {
        return flowList;
    }
    
    public void setFlows(boolean flowView) {
        this.flowView = flowView;
    }
    
    public boolean hasFlows() {
        return flowView;
    }

    public Flow getFlow(long fid) {
        for(Flow flow : flowList) {
            if(flow.getFlowId() == fid)
                return flow;
        }
        return null;
    }
}
