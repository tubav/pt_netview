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

package de.fhg.fokus.net.netview.view;

import de.fhg.fokus.net.geo.WayPoint.Format;
import de.fhg.fokus.net.netview.model.Node;
import de.fhg.fokus.net.netview.model.NodePhysicalProperties;
import de.fhg.fokus.net.netview.model.NodeViewProperties;

public class NodeViewUtil  {
	/**
	 * Renders html used in tool tips.
	 * @param node
	 * @return html as string
	 */
	public static String renderHtml( Node node ){
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append(String.format("<table >" +
				"<tr><td colspan=\"2\"><b>%s</b></td></tr>" +
				"<tr><td><small>WGS84</small></td><td><small>%s</small></td></tr>"+
				"</table>", NodeViewProperties.getLabel(node),NodePhysicalProperties.getWaypoint(node).getString(Format.WGS89_DECIMAL)));
		sb.append(String.format("<p><small>Id: " + node.mp.getProbe().getProbeId() + " ("+ node.mp.getProbe().getLabel() + ")</small></p>"));
		sb.append("</html>");
		return sb.toString();
	}

}
