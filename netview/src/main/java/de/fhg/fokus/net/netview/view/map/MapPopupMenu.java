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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.netview.control.MainController;
import de.fhg.fokus.net.netview.model.ViewStyle;
import de.fhg.fokus.net.worldmap.MapMarker;
import de.fhg.fokus.net.worldmap.WorldMap;
import de.fhg.fokus.net.worldmap.layers.track.TrackPlayer;

public class MapPopupMenu {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final JLabel label  = new JLabel();
	private final ViewStyle style;
	private final WorldMap worldmap;
	private final JPopupMenu popupMenu = new JPopupMenu();
	private final JMenuItem showHiddenMarkers = new JMenuItem();
	private final JMenuItem preferences = new JMenuItem();
	private final JMenuItem search = new JMenuItem();
	private final JMenuItem enableTraffic = new JMenuItem();
	private final JMenuItem disableTraffic = new JMenuItem();
	
	public MapPopupMenu(ViewStyle style, WorldMap map) {
		this.style = style;
		this.worldmap = map;
		this.label.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		//
		// label
		//		
		label.setText(this.style.getString("txt.map"));
		this.style.applyLabelStyle(label);
		setupSearch();
		setupPreferences();
		setupShowHiddenMarkers();
		setupEnableTraffic();
		setupDisableTraffic();
		
		// building popup
		popupMenu.add(label);
		popupMenu.add(new JSeparator());
		//popupMenu.add(showHiddenMarkers);
		//popupMenu.add(search);
		popupMenu.add(enableTraffic);
		popupMenu.add(disableTraffic);
		popupMenu.add(new JSeparator());
		popupMenu.add(preferences);
	}

	private void setupDisableTraffic() {
		enableTraffic.setText(this.style.getString("txt.enableTraffic"));
		enableTraffic.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				for(MapMarker marker : worldmap.getMapMarkers()) {
					if(marker instanceof NetViewMapMarker) {
						NetViewMapMarker nvm = (NetViewMapMarker)marker;
						nvm.setShowTraffic(true);
					}
				}
				/* refresh tracks */
				TrackPlayer player = MainController.getApplication().getTrackPlayer();
				if(player != null)
					player.reloadCurrentTracks();
			}
		});
		
	}

	private void setupEnableTraffic() {
		disableTraffic.setText(this.style.getString("txt.disableTraffic"));
		disableTraffic.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				for(MapMarker marker : worldmap.getMapMarkers()) {
					if(marker instanceof NetViewMapMarker) {
						NetViewMapMarker nvm = (NetViewMapMarker)marker;
						nvm.setShowTraffic(false);
					}
				}
				/* refresh tracks */
				TrackPlayer player = MainController.getApplication().getTrackPlayer();
				if(player != null)
					player.reloadCurrentTracks();
			}
		});
	}

	private void setupSearch() {
		search.setIcon(this.style.getIcon("icon.search"));
		search.setText(this.style.getString("txt.search"));
		search.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				logger.debug("TODO");
			}
		});
	}

	private void setupPreferences() {
		preferences.setIcon(this.style.getIcon("icon.preferences"));
		preferences.setText(this.style.getString("txt.preferences"));
		preferences.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				worldmap.showConfigDialog();
			}
		});
	}
	
	private void setupShowHiddenMarkers() {
		showHiddenMarkers.setIcon(style.getIcon("icon.showHiddenMarkers"));
		showHiddenMarkers.setText(style.getString("txt.showHiddenMarkers"));
		showHiddenMarkers.setHorizontalAlignment(SwingConstants.LEFT);
		showHiddenMarkers.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				worldmap.showHiddenMarkers();
			}
		});

		
	}

	public void show(Component invoker, int x, int y){
		popupMenu.show(invoker, x, y);
	}
	

}
