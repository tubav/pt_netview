package de.fhg.fokus.net.worldmap.layers.track;

import org.apache.commons.lang3.builder.HashCodeBuilder;

public class MapObject {
    
    public long id;
    public Type type;

    public static enum Type {
        TRACK, FLOW, BEARER
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(id).append(type).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        
        if(id == ((MapObject)obj).id && type == ((MapObject)obj).type)
            return true;
        
        return false;
    }
}