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
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.netview.control.MainController;
import de.fhg.fokus.net.netview.model.db.NetViewDB;
import de.fhg.fokus.net.netview.view.charts.BearerDelayChart;
import de.fhg.fokus.net.netview.view.charts.BearerPerPacketDelayChart;
import de.fhg.fokus.net.worldmap.layers.track.TrackPlayer;
import de.fhg.fokus.net.worldmap.layers.track.Track;
import de.fhg.fokus.net.worldmap.layers.track.Bearer;
import de.fhg.fokus.net.worldmap.layers.track.Flow;

public class BearerPopupMenu {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Bearer bearer;
    private JPopupMenu popupMenu;
    private JMenuItem showTracksItem;
    private JMenuItem showFlowsItem;
    private JMenuItem showDelayStatsItem;
    private JMenuItem showPerPacketDelayItem;

    public BearerPopupMenu(Bearer bearer) {
        this.bearer = bearer;
        createPopupMenu();
    }

    private void createPopupMenu() {
        popupMenu = new JPopupMenu("Bearer");

        showTracksItem = new JMenuItem("Show tracks");
        showTracksItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {

                MainController mc = MainController.getApplication();
                Track track = bearer.getTrack();
                
                track.clearBearerList();
                track.setBearers(false);
                mc.getModel().getSplineLayer().refresh();
            }
        });
        
        showDelayStatsItem = new JMenuItem("Delay stats");
        showDelayStatsItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                JFrame frame = new BearerDelayChart(MainController.getApplication()
                        .getModel().getDb(), "Bearer delay stats",
                        bearer, MainController.getApplication().getTrackPlayer().getCurrentTimestamp() + 2000);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);
            }
        });
        
        showPerPacketDelayItem = new JMenuItem("Per packet delays");
        showPerPacketDelayItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                JFrame frame = new BearerPerPacketDelayChart(
                        MainController.getApplication().getModel().getDb(),
                        "Bearer per packet delay stats", bearer,
                        MainController.getApplication().getTrackPlayer()
                        .getCurrentTimestamp() + 2000);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);
            }
        });
        
        showFlowsItem = new JMenuItem("Show flows");
        showFlowsItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                ArrayList<Flow> flows = new ArrayList<Flow>();

                MainController mc = MainController.getApplication();
                NetViewDB db = mc.getModel().getDb();
                   
                flows = db.getTrackRepository().getFlows(bearer.getTrack(), bearer);
                
                if (!flows.isEmpty()) {
                    bearer.setFlowList(flows);
                    bearer.setFlows(true);
                    mc.getModel().getSplineLayer().refresh();
                }  
            }
        });

        JLabel title = new JLabel("Bearer");
        Font tfont = title.getFont();
        title.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        title.setFont(tfont.deriveFont(tfont.getStyle() ^ Font.BOLD));

        popupMenu.add(title);
        popupMenu.add(new JSeparator());
        popupMenu.add(showDelayStatsItem);
        popupMenu.add(showPerPacketDelayItem);
        popupMenu.add(new JSeparator());
        popupMenu.add(showFlowsItem);
        popupMenu.add(showTracksItem);
    }

    public void show(Component invoker, int x, int y) {
        popupMenu.show(invoker, x, y);
    }
}
