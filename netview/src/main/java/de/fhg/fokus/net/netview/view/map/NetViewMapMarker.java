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
package de.fhg.fokus.net.netview.view.map;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import de.fhg.fokus.net.netview.model.Node;
import de.fhg.fokus.net.netview.sys.NetViewConfig;
import de.fhg.fokus.net.netview.view.NodeViewUtil;
import de.fhg.fokus.net.worldmap.MapMarkerImage;
import de.fhg.fokus.net.worldmap.view.ViewUtil;

/**
 * 
 * @author FhG-FOKUS NETwork Research
 *
 */
public class NetViewMapMarker extends MapMarkerImage {

    private static final long serialVersionUID = 1L;
    private static BufferedImage disabledImage;
    private Node node;
    private boolean showTraffic = true;

    static {
        try {
            disabledImage = ViewUtil.toCompatibleImage(ImageIO.read(NetViewMapMarker.class.getResource("resources/icons/marker_disabled.png")));
        } catch (IOException e) {
            logger.warn("Can't load disabled marker image.");
            e.printStackTrace();
        }
    }

    public NetViewMapMarker(Node node, NetViewConfig config) {
        super(node.phy.waypoint, node.getUid());
        setReference(node);
        this.node = node;
        setToolTipText(NodeViewUtil.renderHtml(node));
    }

    public boolean getShowTraffic() {
        return showTraffic;
    }

    public void setShowTraffic(boolean flag) {
        if (flag != showTraffic) {
            this.showTraffic = flag;
            if (showTraffic) {
                setImage(null, node.getUid()); // reset to default image
            } else {
                setImage(disabledImage, node.getUid());
            }
        }
    }

    @Override
    public void setImage(BufferedImage markerImage, long uid) {
        if (markerImage != null) {
            super.setImage(ViewUtil.toCompatibleImage(markerImage), uid);
        } else {
            super.setImage(null, uid);
        }
    }

    @Override
    public void setShadow(BufferedImage markerShadow) {
        if (markerShadow != null) {
            super.setShadow(ViewUtil.toCompatibleImage(markerShadow));
        } else {
            super.setShadow(null);
        }
    }
}
