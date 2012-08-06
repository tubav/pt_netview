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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.ProgressMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.netview.control.MainController;
import de.fhg.fokus.net.netview.model.db.NetViewDB;
import de.fhg.fokus.net.netview.util.DelayCSVExporter;
import de.fhg.fokus.net.netview.view.charts.TrackDelayChart;
import de.fhg.fokus.net.netview.view.charts.TrackPerPacketDelayChart;
import de.fhg.fokus.net.worldmap.layers.track.TrackPlayer;
import de.fhg.fokus.net.worldmap.layers.track.Track;
import de.fhg.fokus.net.worldmap.layers.track.Bearer;

public class TrackPopupMenu {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Track track;
    private JPopupMenu popupMenu;
    private JMenuItem showDelayStatsItem;
    private JMenuItem showPerPacketDelayItem;
    private JMenuItem savePacketDelaysItem;
    private JMenuItem showFlowsItem;

    public TrackPopupMenu(Track track) {
        this.track = track;
        createPopupMenu();
    }

    private void createPopupMenu() {
        popupMenu = new JPopupMenu("Track");

        showDelayStatsItem = new JMenuItem("Delay stats");
        showDelayStatsItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                JFrame frame = new TrackDelayChart(
                        MainController.getApplication().getModel().getDb(),
                        "Track delay stats", track.getTrackId(),
                        MainController.getApplication().getTrackPlayer()
                        .getCurrentTimestamp() + 2000);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);
            }
        });

        showPerPacketDelayItem = new JMenuItem("Per packet delays");
        showPerPacketDelayItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                JFrame frame = new TrackPerPacketDelayChart(
                        MainController.getApplication().getModel().getDb(),
                        "Track per packet delay stats",
                        track.getTrackId(),
                        MainController.getApplication().getTrackPlayer()
                        .getCurrentTimestamp() + 2000);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);
            }
        });

        savePacketDelaysItem = new JMenuItem("Save packet delays");
        savePacketDelaysItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {

                long from, to;

                MainController mc = MainController.getApplication();
                TrackPlayer player = mc.getTrackPlayer();
                NetViewDB db = mc.getModel().getDb();

                from = player.getStartTimestamp();
                to = player.getStopTimestamp();

                File outFile = getSaveFile("delays-track-" + track.getTrackId() + "-" + from + "-" + to + ".csv");
                if (outFile != null) {
                    BufferedWriter out = null;
                    try {
                        out = new BufferedWriter(new FileWriter(outFile));
                    } catch (IOException ioe) {
                        logger.error(ioe.getMessage());
                        JOptionPane.showMessageDialog(null, ioe, "Error opening " + outFile.getAbsolutePath(), JOptionPane.ERROR_MESSAGE);
                    }

                    if (out != null) {
                        final ProgressMonitor progressMonitor = new ProgressMonitor(null, "CSV Export", "Completed ??%", 0, 100);
                        final DelayCSVExporter dce = new DelayCSVExporter(db, track.getTrackId(), from, to, out);
                        dce.addPropertyChangeListener(new PropertyChangeListener() {

                            @Override
                            public void propertyChange(PropertyChangeEvent evt) {
                                if ("progress".equals(evt.getPropertyName())) {
                                    Integer p = (Integer) evt.getNewValue();
                                    if (progressMonitor.isCanceled()) {
                                        if (!dce.isCancelled()) {
                                            dce.cancel(false);
                                            JOptionPane.showMessageDialog(null, "Export cancelled.");
                                        }
                                    } else {
                                        progressMonitor.setProgress(p);
                                        progressMonitor.setNote(String.format("Completed %02d%%", p));
                                    }
                                }
                            }
                        });
                        dce.execute();
                    }
                }
            }
        });

        showFlowsItem = new JMenuItem("Show bearers");
        showFlowsItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                ArrayList<Bearer> bearers = new ArrayList<Bearer>();

                MainController mc = MainController.getApplication();
                NetViewDB db = mc.getModel().getDb();
                
                bearers = db.getTrackRepository().getBearers(track);
                
                if (!bearers.isEmpty()) {
                    track.setBearerList(bearers);
                    track.setBearers(true);
                    mc.getModel().getSplineLayer().refresh();
                }     
            }
        });

        JLabel title = new JLabel("Track");
        Font tfont = title.getFont();
        title.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        title.setFont(tfont.deriveFont(tfont.getStyle() ^ Font.BOLD));

        popupMenu.add(title);
        popupMenu.add(new JSeparator());
        popupMenu.add(showDelayStatsItem);
        popupMenu.add(showPerPacketDelayItem);
        popupMenu.add(new JSeparator());
        popupMenu.add(savePacketDelaysItem);
        popupMenu.add(new JSeparator());
        popupMenu.add(showFlowsItem);
    }

    public void show(Component invoker, int x, int y) {
        popupMenu.show(invoker, x, y);
    }

    /* TODO: move to a sane place. */
    private static File getSaveFile(String suggestion) {
        JFileChooser fc = new JFileChooser() {

            @Override
            public void approveSelection() {
                File selected = getSelectedFile();
                if (selected.exists()) {
                    int option = JOptionPane.showConfirmDialog(
                            null,
                            "Overwrite " + selected.getAbsolutePath() + "?",
                            "File exists",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (option != JOptionPane.OK_OPTION) {
                        return;
                    }
                }
                super.approveSelection();
            }
        };

        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(false);
        if (suggestion != null) {
            fc.setSelectedFile(new File(suggestion));
        }

        if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFile();
        } else {
            return null;
        }
    }
}
