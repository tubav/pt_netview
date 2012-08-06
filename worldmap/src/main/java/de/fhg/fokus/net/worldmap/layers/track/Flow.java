package de.fhg.fokus.net.worldmap.layers.track;

public class Flow {
    private String srcIp;
    private String dstIp;
    private int srcPort;
    private int dstPort;
    private long flowId;
    
    private Bearer bearer;
    public View view;
            
    @Override
    public String toString() {
        return String.format("src: %s:%d, dst: %s:%d",
                srcIp, srcPort, dstIp, dstPort);
    }

    public Bearer getBearer() {
        return bearer;
    }
    
    public void setBearer(Bearer bearer) {
        this.bearer = bearer;
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

    public long getFlowId() {
        return flowId;
    }

    public void setFlowId(long flowId) {
        this.flowId = flowId;
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
}
