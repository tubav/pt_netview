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

package de.fhg.fokus.net.worldmap.util;

import java.awt.BorderLayout;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.toedter.calendar.JCalendar;

/**
 * Non modal time stamp selection dialog. Updates are received
 * via time stamp listener.
 * 
 * @author FhG-FOKUS NETwork Research
 */
public class TimestampSelectionDialog extends javax.swing.JDialog {
   // sys

   private static final long serialVersionUID = 1L;
   private final Logger logger = LoggerFactory.getLogger(getClass());
   private final JCalendar calendar = new JCalendar();

   /**
    * Asynchronous notification for timestamp updates
    * 
    */
   public static interface TimestampListener {

      /**
       * Called whenever timestamp has changed.
       * 
       * @param timestamp
       * @param iso8601 formated timestamp
       */
      public void onChange(long timestamp, String iso8601);
   }
   // utils
   private static SimpleDateFormat sdfIso8601 =
           new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
   // properties
   private long timestamp;
   private String iso8601;
   private volatile TimestampListener timestampListener;

   /**
    * Update timestamp
    */
   private void fireUpdateTs() {
      if (timestampListener != null) {
         timestampListener.onChange(timestamp, iso8601);
      }
   }

   private void setViewFromTimestamp(long timestamp) {
      this.timestamp = timestamp;
      updateIso8601();
      DateTime date = new DateTime(timestamp);
      calendar.setDate(date.toDate());
      jSliderHours.getModel().setValue(date.getHourOfDay());
      jSliderMinutes.getModel().setValue(date.getMinuteOfHour());
      jSliderSeconds.getModel().setValue(date.getSecondOfMinute());
      jTextFieldIso8601.setText(iso8601);
      fireUpdateTs();
   }

   private void updateIso8601() {
      iso8601 = sdfIso8601.format(new Date(timestamp));
   }

   public long getTimestamp() {
      return timestamp;
   }

   /** Creates new form TimestampSelectionDialog */
   public TimestampSelectionDialog(java.awt.Frame parent, boolean modal) {
      super(parent, modal);
      initComponents();

      try {
         setIconImage(
                 ImageIO.read(
                 getClass().getResource(
                 "/de/fhg/fokus/net/worldmap/view/resources/date.png")));
      } catch (Exception e) {
         logger.error("Could not find image: /view/resources/date.png");
         logger.error(e.toString());
      }
      
      setupSliders();
      setupJCalendar();
      setupIso8601Editor();

   }


   private void setupIso8601Editor() {
      // TODO make it editable
      jTextFieldIso8601.setEditable(false);
   }

   private void setupJCalendar() {
      jPanelCalendar.setLayout(new BorderLayout());
      jPanelCalendar.add(calendar, BorderLayout.NORTH);
      PropertyChangeListener lsn = new PropertyChangeListener() {

         @Override
         public void propertyChange(PropertyChangeEvent evt) {
            updateTsFromView();
         }
      };
      Font font10 = Font.decode("Dialog Plain 10");
      Font font11 = Font.decode("Dialog Plain 11");

      calendar.setFont(font11);
      calendar.getDayChooser().setFont(font10);

      calendar.getDayChooser().addPropertyChangeListener("day", lsn);
      calendar.getMonthChooser().addPropertyChangeListener("month", lsn);
      calendar.getYearChooser().addPropertyChangeListener("year", lsn);


   }

   private void updateTsFromView() {
      DateTime date = new DateTime(calendar.getDate());
      DateTime fullDate = new DateTime(date.getYear(),
              date.getMonthOfYear(),
              date.getDayOfMonth(),
              jSliderHours.getModel().getValue(),
              jSliderMinutes.getModel().getValue(),
              jSliderSeconds.getModel().getValue(),
              date.getMillisOfSecond());
      timestamp = fullDate.getMillis();
      updateIso8601();
      jTextFieldIso8601.setText(iso8601);
      fireUpdateTs();

   }

   private void setupSliders() {
      jSliderHours.getModel().addChangeListener(new ChangeListener() {

         @Override
         public void stateChanged(ChangeEvent arg0) {
            updateTsFromView();
         }
      });
      jSliderMinutes.getModel().addChangeListener(new ChangeListener() {

         @Override
         public void stateChanged(ChangeEvent arg0) {
            updateTsFromView();
         }
      });
      jSliderSeconds.getModel().addChangeListener(new ChangeListener() {

         @Override
         public void stateChanged(ChangeEvent arg0) {
            updateTsFromView();
         }
      });


   }

   /** This method is called from within the constructor to
    * initialize the form.
    * WARNING: Do NOT modify this code. The content of this method is
    * always regenerated by the Form Editor.
    */
   @SuppressWarnings("unchecked")
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      jButtonClose = new javax.swing.JButton();
      jTextFieldIso8601 = new javax.swing.JTextField();
      jPanelSliders = new javax.swing.JPanel();
      jPanelCalendar = new javax.swing.JPanel();
      jSliderHours = new javax.swing.JSlider();
      jSliderMinutes = new javax.swing.JSlider();
      jSliderSeconds = new javax.swing.JSlider();
      jButtonCurrentTime = new javax.swing.JButton();
      jTextFieldUnixMilliseconds = new javax.swing.JTextField();
      jLabelMilliseconds = new javax.swing.JLabel();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
      setFont(new java.awt.Font("Monospaced", 1, 12));
      setForeground(java.awt.Color.darkGray);

      jButtonClose.setText("Close");
      jButtonClose.setPreferredSize(new java.awt.Dimension(123, 26));
      jButtonClose.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButtonCloseActionPerformed(evt);
         }
      });

      jTextFieldIso8601.setBackground(java.awt.Color.lightGray);
      jTextFieldIso8601.setFont(new java.awt.Font("DejaVu Sans", 1, 11));
      jTextFieldIso8601.setHorizontalAlignment(javax.swing.JTextField.CENTER);
      jTextFieldIso8601.setText("2009-10-28 12:30.2345");
      jTextFieldIso8601.setToolTipText("ISO 8601");
      jTextFieldIso8601.setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.darkGray));

      jPanelSliders.setLayout(new javax.swing.BoxLayout(jPanelSliders, javax.swing.BoxLayout.PAGE_AXIS));

      jPanelCalendar.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

      javax.swing.GroupLayout jPanelCalendarLayout = new javax.swing.GroupLayout(jPanelCalendar);
      jPanelCalendar.setLayout(jPanelCalendarLayout);
      jPanelCalendarLayout.setHorizontalGroup(
         jPanelCalendarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGap(0, 354, Short.MAX_VALUE)
      );
      jPanelCalendarLayout.setVerticalGroup(
         jPanelCalendarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGap(0, 193, Short.MAX_VALUE)
      );

      jPanelSliders.add(jPanelCalendar);

      jSliderHours.setFont(new java.awt.Font("DejaVu Sans", 0, 9));
      jSliderHours.setForeground(java.awt.Color.gray);
      jSliderHours.setMajorTickSpacing(2);
      jSliderHours.setMaximum(23);
      jSliderHours.setMinorTickSpacing(1);
      jSliderHours.setPaintLabels(true);
      jSliderHours.setPaintTicks(true);
      jSliderHours.setSnapToTicks(true);
      jSliderHours.setToolTipText("Hours");
      jSliderHours.setValue(12);
      jSliderHours.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1), "Hour", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("DejaVu Sans", 0, 10), java.awt.Color.gray)); // NOI18N
      jSliderHours.setPreferredSize(new java.awt.Dimension(200, 80));
      jPanelSliders.add(jSliderHours);

      jSliderMinutes.setFont(new java.awt.Font("DejaVu Sans", 0, 9)); // NOI18N
      jSliderMinutes.setForeground(java.awt.Color.gray);
      jSliderMinutes.setMajorTickSpacing(5);
      jSliderMinutes.setMaximum(59);
      jSliderMinutes.setPaintLabels(true);
      jSliderMinutes.setPaintTicks(true);
      jSliderMinutes.setToolTipText("Minutes");
      jSliderMinutes.setValue(30);
      jSliderMinutes.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1), "Minute", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("DejaVu Sans", 0, 10), java.awt.Color.gray)); // NOI18N
      jSliderMinutes.setPreferredSize(new java.awt.Dimension(200, 80));
      jPanelSliders.add(jSliderMinutes);

      jSliderSeconds.setFont(new java.awt.Font("DejaVu Sans", 0, 9));
      jSliderSeconds.setForeground(java.awt.Color.gray);
      jSliderSeconds.setMajorTickSpacing(5);
      jSliderSeconds.setMaximum(59);
      jSliderSeconds.setPaintLabels(true);
      jSliderSeconds.setPaintTicks(true);
      jSliderSeconds.setValue(30);
      jSliderSeconds.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1), "Seconds", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("DejaVu Sans", 0, 10), java.awt.Color.gray))); // NOI18N
      jSliderSeconds.setPreferredSize(new java.awt.Dimension(200, 80));
      jPanelSliders.add(jSliderSeconds);

      jButtonCurrentTime.setText("Now");
      jButtonCurrentTime.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButtonCurrentTimeActionPerformed(evt);
         }
      });

      jTextFieldUnixMilliseconds.setForeground(java.awt.Color.gray);
      jTextFieldUnixMilliseconds.setHorizontalAlignment(javax.swing.JTextField.CENTER);
      jTextFieldUnixMilliseconds.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            jTextFieldUnixMillisecondsActionPerformed(evt);
         }
      });

      jLabelMilliseconds.setFont(new java.awt.Font("DejaVu Sans", 0, 10)); // NOI18N
      jLabelMilliseconds.setForeground(java.awt.Color.gray);
      jLabelMilliseconds.setText("ms");
      jLabelMilliseconds.setToolTipText("Unix milliseconds");

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(jPanelSliders, javax.swing.GroupLayout.DEFAULT_SIZE, 356, Short.MAX_VALUE)
               .addComponent(jTextFieldIso8601, javax.swing.GroupLayout.DEFAULT_SIZE, 356, Short.MAX_VALUE)
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addComponent(jLabelMilliseconds)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(jTextFieldUnixMilliseconds, javax.swing.GroupLayout.DEFAULT_SIZE, 171, Short.MAX_VALUE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(jButtonCurrentTime, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(jButtonClose, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(jTextFieldIso8601, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jPanelSliders, javax.swing.GroupLayout.DEFAULT_SIZE, 435, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(layout.createSequentialGroup()
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 12, Short.MAX_VALUE)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(jButtonClose, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(jTextFieldUnixMilliseconds, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addComponent(jLabelMilliseconds))
                  .addContainerGap())
               .addGroup(layout.createSequentialGroup()
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(jButtonCurrentTime)
                  .addContainerGap())))
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

    private void jButtonCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCloseActionPerformed
       // TODO add your handling code here:
       setVisible(false);
    }//GEN-LAST:event_jButtonCloseActionPerformed

    private void jButtonCurrentTimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCurrentTimeActionPerformed
    	setViewFromTimestamp(System.currentTimeMillis());
    }//GEN-LAST:event_jButtonCurrentTimeActionPerformed

    private void jTextFieldUnixMillisecondsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldUnixMillisecondsActionPerformed
      String txt = jTextFieldUnixMilliseconds.getText();
      try {
      setViewFromTimestamp(Long.parseLong(txt));
      } catch (Exception e ){
         logger.warn("Invalid format for unix timestamp: {}",txt);
      }
    }//GEN-LAST:event_jTextFieldUnixMillisecondsActionPerformed

   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JButton jButtonClose;
   private javax.swing.JButton jButtonCurrentTime;
   private javax.swing.JLabel jLabelMilliseconds;
   private javax.swing.JPanel jPanelCalendar;
   private javax.swing.JPanel jPanelSliders;
   private javax.swing.JSlider jSliderHours;
   private javax.swing.JSlider jSliderMinutes;
   private javax.swing.JSlider jSliderSeconds;
   private javax.swing.JTextField jTextFieldIso8601;
   private javax.swing.JTextField jTextFieldUnixMilliseconds;
   // End of variables declaration//GEN-END:variables

   public void setTimestampListener(TimestampListener timestampListener) {
      this.timestampListener = timestampListener;
   }

   /**
    * Reset timestamp
    * 
    * @param title dialog title
    * @param lsn timestamp listener (for receiving updates)
    * @param timestamp initial timestamp
    */
   public synchronized void reset(String title, TimestampListener lsn, long timestamp) {
      setTitle(title);
      // avoid triggering old listener
      timestampListener = null;
      setViewFromTimestamp(timestamp);
      timestampListener = lsn;
      setVisible(true);
   }
}
