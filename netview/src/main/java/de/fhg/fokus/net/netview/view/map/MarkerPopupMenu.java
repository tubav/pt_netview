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

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.netview.control.MainController;
import de.fhg.fokus.net.netview.model.Node;
import de.fhg.fokus.net.netview.model.NodeViewProperties;
import de.fhg.fokus.net.netview.model.ViewStyle;
import de.fhg.fokus.net.netview.view.NodeDialog;
import de.fhg.fokus.net.netview.view.charts.NodeSamplingStatsChart;
import de.fhg.fokus.net.netview.view.charts.NodeSystemStatsChart;
import de.fhg.fokus.net.worldmap.layers.track.TrackPlayer;

public class MarkerPopupMenu {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final JPopupMenu popupMenu;
    private final JLabel label = new JLabel();
    private final JMenuItem propertiesItem = new JMenuItem();
    private final JMenuItem hideItem = new JMenuItem();
    private final JMenuItem removeNodeItem = new JMenuItem();
    private final JMenuItem showSystemStatsItem = new JMenuItem();
    private final JMenuItem showSamplingStatsItem = new JMenuItem();
    private final JMenuItem toggleTrafficItem = new JMenuItem();
    private final NodeDialog dialog;
    private final ViewStyle style;
    private NetViewMapMarker marker;

    public MarkerPopupMenu(ViewStyle style, NodeDialog dialog) {
        this.dialog = dialog;
        this.style = style;
        this.label.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        this.style.applyLabelStyle(label);

        setupHideItem();
        setupRemoveNodeItem();
        setupPropertiesItem();
        setupShowSystemStatsItem();
        setupShowSamplingStatsItem();
        setupToggleTrafficItem();

        this.popupMenu = new JPopupMenu();
        this.popupMenu.add(label);
        this.popupMenu.add(new JSeparator());
        //this.popupMenu.add(this.hideItem);
        //this.popupMenu.add(this.removeNodeItem);
        this.popupMenu.add(toggleTrafficItem);
        this.popupMenu.add(new JSeparator());
        this.popupMenu.add(this.showSystemStatsItem);
        this.popupMenu.add(this.showSamplingStatsItem);
//		this.popupMenu.add(new JSeparator());
//		this.popupMenu.add(this.propertiesItem);
    }

    private void setupToggleTrafficItem() {
        toggleTrafficItem.setText(style.getString("txt.toggleTraffic"));
        toggleTrafficItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                marker.setShowTraffic(!marker.getShowTraffic());
                /* refresh tracks */
                TrackPlayer player = MainController.getApplication().getTrackPlayer();
                if (player != null) {
                    player.reloadCurrentTracks();
                }
            }
        });
    }

    private void setupRemoveNodeItem() {
        removeNodeItem.setIcon(style.getIcon("menu.removeNodeIcon"));
        removeNodeItem.setText(style.getString("txt.removeNode"));
        removeNodeItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Node node = marker.getReference(Node.class);
                if (node != null) {
                    logger.debug("TODO: remove node: " + node);
                }
            }
        });


    }

    private void setupHideItem() {
        hideItem.setText(style.getString("txt.hideMarker"));
        hideItem.setIcon(style.getIcon("menu.hideMarkerIcon"));

        hideItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                marker.setVisible(false);
            }
        });

    }

    private void setupPropertiesItem() {
        propertiesItem.setText(style.getString("txt.properties"));
//		propertiesItem.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				Node node = marker.getReference(Node.class);
//				if( node != null ){
//					//TODO node properties
//					//dialog.setupAndShow(node);
//				}				
//			}
//		});

    }

    private void setupShowSystemStatsItem() {
        showSystemStatsItem.setText(style.getString("txt.showSystemStats"));
        showSystemStatsItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Node node = marker.getReference(Node.class);
                if (node != null) {
                    JFrame frame = new NodeSystemStatsChart(
                            MainController.getApplication().getModel().getDb()
                            .getEbeanServer(), "Node system stats", node, 
                            MainController.getApplication().getTrackPlayer()
                            .getCurrentTimestamp() + 2000);
                    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    frame.pack();
                    frame.setVisible(true);
                }
            }
        });
    }

    private void setupShowSamplingStatsItem() {
        showSamplingStatsItem.setText(style.getString("txt.showSamplingStats"));
        showSamplingStatsItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Node node = marker.getReference(Node.class);
                if (node != null) {
                    JFrame frame = new NodeSamplingStatsChart(MainController.getApplication().getModel().getDb().getEbeanServer(), "Node sampling stats", node, MainController.getApplication().getTrackPlayer().getCurrentTimestamp() + 2000);
                    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    frame.pack();
                    frame.setVisible(true);
                }
            }
        });
    }

    public MarkerPopupMenu setMarker(NetViewMapMarker marker) {
        this.marker = marker;
        Node node = this.marker.getReference(Node.class);
        if (node == null) {
            propertiesItem.setEnabled(false);
        } else {
            label.setText(NodeViewProperties.getLabel(node));
            propertiesItem.setEnabled(true);
        }
        return this;
    }

    public void show(Component invoker, int x, int y) {
        popupMenu.show(invoker, x, y);
    }
}
