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

import java.awt.Color;
import java.awt.Font;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Icon;
import javax.swing.JLabel;

import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.ResourceMap;

import de.fhg.fokus.net.netview.model.ViewStyle;
/**
 * Used to minimize code dependency on application application framework and
 * apply common styles.
 *  
 * @author FhG-FOKUS NETwork Research
 */
public class ResourceView {
	private static final Map<Class<?>, ViewStyle> cache = new ConcurrentHashMap<Class<?>, ViewStyle>();
	private static final ApplicationContext ctx = Application.getInstance().getContext();
	public static ViewStyle getViewStyle(final Class<?> klass) {
		ViewStyle rloc = cache.get(klass);
		if (rloc == null) {
			rloc = new ViewStyle() {
				ResourceMap resouceMap = ctx.getResourceMap(klass);

				@Override
				public String getString(String key, Object... args) {
					String str = resouceMap.getString(key, args);
					if(str ==null ){
						str = key;
					}
					return str;
				}

				@Override
				public Icon getIcon(String key) {
					return resouceMap.getIcon(key);
				}

				@Override
				public void applyLabelStyle(JLabel label) {
					Font font = label.getFont();
					label.setFont(font.deriveFont(font.getStyle() ^ Font.BOLD ));
					label.setForeground(Color.DARK_GRAY);
				}
			};
		}
		cache.put(klass, rloc);
		return rloc;
	}

}
