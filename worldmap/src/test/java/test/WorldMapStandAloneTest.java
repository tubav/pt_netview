/**
 * worldmap - an extension to JMapViewer which provides additional
 *            functionality. New functions allow setting markers,
 *            adding layers, and printing tracks on the map. (see
 *            http://wiki.openstreetmap.org/wiki/JMapViewer for more
 *            information on JMapViewer)
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

package test;

import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.assertEquals;
import java.lang.InterruptedException;
import de.fhg.fokus.net.worldmap.WorldMap;

/**
 *
 * @author jve
 */
public class WorldMapStandAloneTest extends javax.swing.JFrame {

    private WorldMap worldmap;
    private static WorldMapStandAloneTest testClass = null;
    
    public WorldMapStandAloneTest() {
        initComponents();
        
        /* And this is where the magic happens */
        this.worldmap = new WorldMap(this.WorldMapPanel, "filecache");
        this.testClass = this;
    }

    private static WorldMapStandAloneTest getInstance() {
        if (WorldMapStandAloneTest.testClass == null) {
            WorldMapStandAloneTest.testClass = new WorldMapStandAloneTest();
        }
        return WorldMapStandAloneTest.testClass;
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        WorldMapPanel = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(800, 600));
        setName("WorldMapFrame"); // NOI18N

        javax.swing.GroupLayout WorldMapPanelLayout = new javax.swing.GroupLayout(WorldMapPanel);
        WorldMapPanel.setLayout(WorldMapPanelLayout);
        WorldMapPanelLayout.setHorizontalGroup(
            WorldMapPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 800, Short.MAX_VALUE)
        );
        WorldMapPanelLayout.setVerticalGroup(
            WorldMapPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 600, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(WorldMapPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(WorldMapPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new WorldMapStandAloneTest().setVisible(true);
            }
        });
    }

    @Test
    public void testStandAlone() 
    {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                WorldMapStandAloneTest.getInstance().setVisible(true);
            }
        });
        try {
            Thread.currentThread().sleep(20000);        
            assertEquals(true, true);
        } catch (InterruptedException e) {}
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel WorldMapPanel;
    // End of variables declaration//GEN-END:variables
}
