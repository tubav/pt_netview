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

/*
 * Netview Main View
 */
package de.fhg.fokus.net.netview.view;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.Timer;

import org.jdesktop.application.Action;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.TaskMonitor;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.netview.control.MainController;

/**
 * The application's main frame.
 * 
 * @author FhG-FOKUS NETwork Research
 */
public class ViewMain extends FrameView {
	// sys
	private final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());
	// --
	private java.util.Timer timer = new java.util.Timer(true);
	private SimpleDateFormat sdfMessage = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	/**
	 * Write a message to used with a timeout 
	 * @param message 
	 * @param timeout in seconds
	 */
	public void message( String msg, int timeout ){
		message(msg);
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				// clean message in 5 seconds
				getStatusMessageLabel().setText("");
			}
		}, timeout * 1000);
	}
	public void message(String message ){
		String msgOut = String.format("%s %s ",sdfMessage.format(new Date()),message);
		getStatusMessageLabel().setText(msgOut);
		logger.debug(message);
		writeMessageToConsole(msgOut+"\n");
	}

	public void messageHide() {
		getStatusMessageLabel().setText("");
	}

	private void writeMessageToConsole( String message ){
		jTextAreaConsole.append(message);
		jTextAreaConsole.setCaretPosition(jTextAreaConsole.getText().length());
		
	}
	/**
	 * Stop internal threads (e.g. timer)
	 * 
	 */
	public void stop(){
		timer.cancel();
	}
	
    // == constants ==
	public static enum Events {
      jButtonConfig,
		
	}

   public JToggleButton getjToggleNavigator() {
      return jToggleNavigator;
   }

   public JToggleButton getjToggleButtonCsp() {
      return jToggleButtonCsp;
   }
	
	private final BusyIconAnimator busyIconAnimator = new BusyIconAnimator() {

      @Override
      public void start() {
         getBusyIconTimer().start();
      }

      @Override
      public void stop() {
         getBusyIconTimer().stop();
         getStatusAnimationLabel().setIcon(getIdleIcon());

      }
   };
    public ViewMain(SingleFrameApplication app) {
        super(app);
        initComponents();
         JFrame mainFrame = getFrame();

         BufferedImage icon;
        try {
            icon = ImageIO.read(this.getClass().getResource("resources/icons/netview-icon.png"));
            mainFrame.setIconImage(icon);

        } catch (Exception ex) {
            Logger.getLogger(ViewMain.class.getName()).log(Level.SEVERE, null, ex);
        }
      
        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                getStatusMessageLabel().setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (getBusyIconIndex() + 1) % getBusyIcons().length;
                getStatusAnimationLabel().setIcon(getBusyIcons()[getBusyIconIndex()]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        getStatusAnimationLabel().setIcon(getBusyIcons()[0]);
                        busyIconIndex = 0;
                        getBusyIconTimer().start();
                    }
                    getProgressBar().setVisible(true);
                    getProgressBar().setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    getBusyIconTimer().stop();
                    getStatusAnimationLabel().setIcon(getIdleIcon());
                    getProgressBar().setVisible(false);
                    getProgressBar().setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String) (evt.getNewValue());
                    getStatusMessageLabel().setText((text == null) ? "" : text);
                    getMessageTimer().restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer) (evt.getNewValue());
                    getProgressBar().setVisible(true);
                    getProgressBar().setIndeterminate(false);
                    getProgressBar().setValue(value);
                }
            }
        });
    }

   public BusyIconAnimator getBusyIconAnimator() {
      return busyIconAnimator;
   }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = MainController.getApplication().getMainFrame();
            aboutBox = new ViewAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        MainController.getApplication().show(aboutBox);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        jPanelMap = new javax.swing.JPanel();
        jLayeredPane1 = new javax.swing.JLayeredPane();
        jPanelDataSources = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jButtonNodesPurge = new javax.swing.JButton();
        jButtonNodesImport = new javax.swing.JButton();
        jLabelNodesTotal = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jTextFieldPacketTrackCollectorPort = new javax.swing.JTextField();
        jButtonStartStop = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextAreaCollectorStatus = new javax.swing.JTextArea();
        jButtonTracksPurge = new javax.swing.JButton();
        jButtonTracksImport = new javax.swing.JButton();
        jScrollPaneConsole = new javax.swing.JScrollPane();
        jTextAreaConsole = new javax.swing.JTextArea();
        jToolBarTop = new javax.swing.JToolBar();
        jToggleNavigator = new javax.swing.JToggleButton();
        jToggleButtonMap = new javax.swing.JToggleButton();
        jToggleButtonCsp = new javax.swing.JToggleButton();
        jToggleButtonDataSources = new javax.swing.JToggleButton();
        jToggleButtonConsole = new javax.swing.JToggleButton();
        jButtonConfig = new javax.swing.JButton();
        jButtonHelp = new javax.swing.JButton();
        jButtonExit = new javax.swing.JButton();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();

        mainPanel.setName("mainPanel"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(de.fhg.fokus.net.netview.control.MainController.class).getContext().getResourceMap(ViewMain.class);
        jTabbedPaneMain.setToolTipText(resourceMap.getString("jTabbedPaneMain.toolTipText")); // NOI18N
        jTabbedPaneMain.setDoubleBuffered(true);
        jTabbedPaneMain.setName("jTabbedPaneMain"); // NOI18N

        jPanelMap.setName("jPanelMap"); // NOI18N

        jLayeredPane1.setName("jLayeredPane1"); // NOI18N

        javax.swing.GroupLayout jPanelMapLayout = new javax.swing.GroupLayout(jPanelMap);
        jPanelMap.setLayout(jPanelMapLayout);
        jPanelMapLayout.setHorizontalGroup(
            jPanelMapLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayeredPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 863, Short.MAX_VALUE)
        );
        jPanelMapLayout.setVerticalGroup(
            jPanelMapLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayeredPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 424, Short.MAX_VALUE)
        );

        jTabbedPaneMain.addTab(resourceMap.getString("jPanelMap.TabConstraints.tabTitle"), jPanelMap); // NOI18N

        jPanelDataSources.setName("jPanelDataSources"); // NOI18N
        jPanelDataSources.setLayout(new javax.swing.BoxLayout(jPanelDataSources, javax.swing.BoxLayout.LINE_AXIS));

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, resourceMap.getString("jPanel1.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 12), resourceMap.getColor("jPanel1.border.titleColor"))); // NOI18N
        jPanel1.setMaximumSize(new java.awt.Dimension(200, 32767));
        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setPreferredSize(new java.awt.Dimension(200, 406));

        jButtonNodesPurge.setForeground(resourceMap.getColor("jButtonNodesPurge.foreground")); // NOI18N
        jButtonNodesPurge.setIcon(resourceMap.getIcon("jButtonNodesPurge.icon")); // NOI18N
        jButtonNodesPurge.setText(resourceMap.getString("jButtonNodesPurge.text")); // NOI18N
        jButtonNodesPurge.setToolTipText(resourceMap.getString("jButtonNodesPurge.toolTipText")); // NOI18N
        jButtonNodesPurge.setName("jButtonNodesPurge"); // NOI18N
        jButtonNodesPurge.setPreferredSize(new java.awt.Dimension(88, 28));
        jButtonNodesPurge.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);

        jButtonNodesImport.setForeground(resourceMap.getColor("jButtonNodesImport.foreground")); // NOI18N
        jButtonNodesImport.setIcon(resourceMap.getIcon("jButtonNodesImport.icon")); // NOI18N
        jButtonNodesImport.setText(resourceMap.getString("jButtonNodesImport.text")); // NOI18N
        jButtonNodesImport.setName("jButtonNodesImport"); // NOI18N

        jLabelNodesTotal.setForeground(resourceMap.getColor("jLabelNodesTotal.foreground")); // NOI18N
        jLabelNodesTotal.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelNodesTotal.setText(resourceMap.getString("jLabelNodesTotal.text")); // NOI18N
        jLabelNodesTotal.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jLabelNodesTotal.setName("jLabelNodesTotal"); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabelNodesTotal, javax.swing.GroupLayout.DEFAULT_SIZE, 190, Short.MAX_VALUE)
            .addComponent(jButtonNodesPurge, javax.swing.GroupLayout.DEFAULT_SIZE, 190, Short.MAX_VALUE)
            .addComponent(jButtonNodesImport, javax.swing.GroupLayout.DEFAULT_SIZE, 190, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jButtonNodesImport)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonNodesPurge, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 322, Short.MAX_VALUE)
                .addComponent(jLabelNodesTotal, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jPanelDataSources.add(jPanel1);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, resourceMap.getString("jPanel2.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 12), resourceMap.getColor("jPanel2.border.titleColor"))); // NOI18N
        jPanel2.setName("jPanel2"); // NOI18N

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(null, resourceMap.getString("jPanel3.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 1, 12), resourceMap.getColor("jPanel3.border.titleColor"))); // NOI18N
        jPanel3.setName("jPanel3"); // NOI18N

        jLabel2.setForeground(resourceMap.getColor("jLabel2.foreground")); // NOI18N
        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        jTextFieldPacketTrackCollectorPort.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldPacketTrackCollectorPort.setText(resourceMap.getString("jTextFieldPacketTrackCollectorPort.text")); // NOI18N
        jTextFieldPacketTrackCollectorPort.setName("jTextFieldPacketTrackCollectorPort"); // NOI18N

        jButtonStartStop.setText(resourceMap.getString("jButtonStartStop.text")); // NOI18N
        jButtonStartStop.setName("jButtonStartStop"); // NOI18N

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        jTextAreaCollectorStatus.setColumns(20);
        jTextAreaCollectorStatus.setEditable(false);
        jTextAreaCollectorStatus.setFont(resourceMap.getFont("jTextAreaCollectorStatus.font")); // NOI18N
        jTextAreaCollectorStatus.setForeground(resourceMap.getColor("jTextAreaCollectorStatus.foreground")); // NOI18N
        jTextAreaCollectorStatus.setRows(5);
        jTextAreaCollectorStatus.setBorder(javax.swing.BorderFactory.createTitledBorder(new javax.swing.border.LineBorder(resourceMap.getColor("jTextAreaCollectorStatus.border.border.lineColor"), 1, true), resourceMap.getString("jTextAreaCollectorStatus.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, resourceMap.getFont("jTextAreaCollectorStatus.border.titleFont"), resourceMap.getColor("jTextAreaCollectorStatus.border.titleColor"))); // NOI18N
        jTextAreaCollectorStatus.setName("jTextAreaCollectorStatus"); // NOI18N
        jScrollPane1.setViewportView(jTextAreaCollectorStatus);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 616, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldPacketTrackCollectorPort, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonStartStop, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 271, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonStartStop)
                    .addComponent(jTextFieldPacketTrackCollectorPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addContainerGap())
        );

        jButtonTracksPurge.setIcon(resourceMap.getIcon("jButtonTracksPurge.icon")); // NOI18N
        jButtonTracksPurge.setText(resourceMap.getString("jButtonTracksPurge.text")); // NOI18N
        jButtonTracksPurge.setName("jButtonTracksPurge"); // NOI18N
        jButtonTracksPurge.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);

        jButtonTracksImport.setIcon(resourceMap.getIcon("jButtonTracksImport.icon")); // NOI18N
        jButtonTracksImport.setText(resourceMap.getString("jButtonTracksImport.text")); // NOI18N
        jButtonTracksImport.setName("jButtonTracksImport"); // NOI18N
        jButtonTracksImport.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(15, 15, 15))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(319, Short.MAX_VALUE)
                .addComponent(jButtonTracksImport, javax.swing.GroupLayout.PREFERRED_SIZE, 158, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButtonTracksPurge, javax.swing.GroupLayout.PREFERRED_SIZE, 158, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButtonTracksPurge, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonTracksImport, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        jPanelDataSources.add(jPanel2);

        jTabbedPaneMain.addTab(resourceMap.getString("jPanelDataSources.TabConstraints.tabTitle"), jPanelDataSources); // NOI18N

        jScrollPaneConsole.setName("jScrollPaneConsole"); // NOI18N

        jTextAreaConsole.setBackground(resourceMap.getColor("jTextAreaConsole.background")); // NOI18N
        jTextAreaConsole.setColumns(20);
        jTextAreaConsole.setEditable(false);
        jTextAreaConsole.setFont(resourceMap.getFont("jTextAreaConsole.font")); // NOI18N
        jTextAreaConsole.setForeground(resourceMap.getColor("jTextAreaConsole.foreground")); // NOI18N
        jTextAreaConsole.setRows(5);
        jTextAreaConsole.setText(resourceMap.getString("jTextAreaConsole.text")); // NOI18N
        jTextAreaConsole.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jTextAreaConsole.setName("jTextAreaConsole"); // NOI18N
        jScrollPaneConsole.setViewportView(jTextAreaConsole);

        jTabbedPaneMain.addTab(resourceMap.getString("jScrollPaneConsole.TabConstraints.tabTitle"), jScrollPaneConsole); // NOI18N

        jToolBarTop.setFloatable(false);
        jToolBarTop.setRollover(true);
        jToolBarTop.setName("jToolBarTop"); // NOI18N

        jToggleNavigator.setIcon(resourceMap.getIcon("jToggleNavigator.icon")); // NOI18N
        jToggleNavigator.setText(resourceMap.getString("jToggleNavigator.text")); // NOI18N
        jToggleNavigator.setToolTipText(resourceMap.getString("jToggleNavigator.toolTipText")); // NOI18N
        jToggleNavigator.setEnabled(false);
        jToggleNavigator.setFocusable(false);
        jToggleNavigator.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jToggleNavigator.setName("jToggleNavigator"); // NOI18N
        jToggleNavigator.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToggleNavigator.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleNavigatorActionPerformed(evt);
            }
        });
        jToolBarTop.add(jToggleNavigator);

        jToggleButtonMap.setIcon(resourceMap.getIcon("jToggleButtonMap.icon")); // NOI18N
        jToggleButtonMap.setText(resourceMap.getString("jToggleButtonMap.text")); // NOI18N
        jToggleButtonMap.setToolTipText(resourceMap.getString("jToggleButtonMap.toolTipText")); // NOI18N
        jToggleButtonMap.setFocusable(false);
        jToggleButtonMap.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jToggleButtonMap.setName("jToggleButtonMap"); // NOI18N
        jToggleButtonMap.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToggleButtonMap.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonMapActionPerformed(evt);
            }
        });
        jToolBarTop.add(jToggleButtonMap);

        jToggleButtonCsp.setIcon(resourceMap.getIcon("jToggleButtonCsp.icon")); // NOI18N
        jToggleButtonCsp.setText(resourceMap.getString("jToggleButtonCsp.text")); // NOI18N
        jToggleButtonCsp.setToolTipText(resourceMap.getString("jToggleButtonCsp.toolTipText")); // NOI18N
        jToggleButtonCsp.setEnabled(false);
        jToggleButtonCsp.setFocusable(false);
        jToggleButtonCsp.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jToggleButtonCsp.setName("jToggleButtonCsp"); // NOI18N
        jToggleButtonCsp.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToggleButtonCsp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonCspActionPerformed(evt);
            }
        });
        jToolBarTop.add(jToggleButtonCsp);

        jToggleButtonDataSources.setIcon(resourceMap.getIcon("jToggleButtonDataSources.icon")); // NOI18N
        jToggleButtonDataSources.setText(resourceMap.getString("jToggleButtonDataSources.text")); // NOI18N
        jToggleButtonDataSources.setToolTipText(resourceMap.getString("jToggleButtonDataSources.toolTipText")); // NOI18N
        jToggleButtonDataSources.setFocusable(false);
        jToggleButtonDataSources.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jToggleButtonDataSources.setName("jToggleButtonDataSources"); // NOI18N
        jToggleButtonDataSources.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToggleButtonDataSources.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonDataSourcesActionPerformed(evt);
            }
        });
        jToolBarTop.add(jToggleButtonDataSources);

        jToggleButtonConsole.setIcon(resourceMap.getIcon("jToggleButtonConsole.icon")); // NOI18N
        jToggleButtonConsole.setText(resourceMap.getString("jToggleButtonConsole.text")); // NOI18N
        jToggleButtonConsole.setToolTipText(resourceMap.getString("jToggleButtonConsole.toolTipText")); // NOI18N
        jToggleButtonConsole.setFocusable(false);
        jToggleButtonConsole.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jToggleButtonConsole.setName("jToggleButtonConsole"); // NOI18N
        jToggleButtonConsole.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToggleButtonConsole.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonConsoleActionPerformed(evt);
            }
        });
        jToolBarTop.add(jToggleButtonConsole);

        jButtonConfig.setIcon(resourceMap.getIcon("jButtonConfig.icon")); // NOI18N
        jButtonConfig.setText(resourceMap.getString("jButtonConfig.text")); // NOI18N
        jButtonConfig.setToolTipText(resourceMap.getString("jButtonConfig.toolTipText")); // NOI18N
        jButtonConfig.setFocusable(false);
        jButtonConfig.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonConfig.setName("jButtonConfig"); // NOI18N
        jButtonConfig.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonConfigActionPerformed(evt);
            }
        });
        jToolBarTop.add(jButtonConfig);

        jButtonHelp.setIcon(resourceMap.getIcon("jButtonHelp.icon")); // NOI18N
        jButtonHelp.setText(resourceMap.getString("jButtonHelp.text")); // NOI18N
        jButtonHelp.setEnabled(false);
        jButtonHelp.setFocusable(false);
        jButtonHelp.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonHelp.setName("jButtonHelp"); // NOI18N
        jButtonHelp.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBarTop.add(jButtonHelp);

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(de.fhg.fokus.net.netview.control.MainController.class).getContext().getActionMap(ViewMain.class, this);
        jButtonExit.setAction(actionMap.get("quit")); // NOI18N
        jButtonExit.setIcon(resourceMap.getIcon("jButtonExit.icon")); // NOI18N
        jButtonExit.setText(resourceMap.getString("jButtonExit.text")); // NOI18N
        jButtonExit.setToolTipText(resourceMap.getString("jButtonExit.toolTipText")); // NOI18N
        jButtonExit.setFocusable(false);
        jButtonExit.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonExit.setName("jButtonExit"); // NOI18N
        jButtonExit.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBarTop.add(jButtonExit);

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jToolBarTop, javax.swing.GroupLayout.DEFAULT_SIZE, 868, Short.MAX_VALUE)
            .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jTabbedPaneMain, javax.swing.GroupLayout.DEFAULT_SIZE, 868, Short.MAX_VALUE))
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addComponent(jToolBarTop, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(469, Short.MAX_VALUE))
            .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(mainPanelLayout.createSequentialGroup()
                    .addGap(31, 31, 31)
                    .addComponent(jTabbedPaneMain, javax.swing.GroupLayout.DEFAULT_SIZE, 451, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        jTabbedPaneMain.getAccessibleContext().setAccessibleParent(jTabbedPaneMain);

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setIcon(resourceMap.getIcon("exitMenuItem.icon")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setFont(resourceMap.getFont("statusMessageLabel.font")); // NOI18N
        statusMessageLabel.setForeground(resourceMap.getColor("statusMessageLabel.foreground")); // NOI18N
        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 868, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 844, Short.MAX_VALUE)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, statusPanelLayout.createSequentialGroup()
                .addContainerGap(821, Short.MAX_VALUE)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3))
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents
    /**
     * Dispatches sync tab event
     * @param evt
     */
    private void dispatchButtonStateChanged(java.awt.event.ActionEvent evt) {
        if (evt.getSource() instanceof JToggleButton) {
            Component btn = (Component) evt.getSource();
            firePropertyChange(btn.getName(), false, true);
        } else if(evt.getSource() instanceof JButton) {
            Component btn = (Component) evt.getSource();
            firePropertyChange(btn.getName(), false, true);
        }
    }
    private void jToggleButtonMapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButtonMapActionPerformed
        dispatchButtonStateChanged(evt);
}//GEN-LAST:event_jToggleButtonMapActionPerformed

    private void jToggleButtonConsoleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButtonConsoleActionPerformed
        dispatchButtonStateChanged(evt);
    }//GEN-LAST:event_jToggleButtonConsoleActionPerformed

    private void jToggleButtonDataSourcesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButtonDataSourcesActionPerformed
        dispatchButtonStateChanged(evt);
    }//GEN-LAST:event_jToggleButtonDataSourcesActionPerformed

    private void jButtonConfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonConfigActionPerformed
        dispatchButtonStateChanged(evt);
    }//GEN-LAST:event_jButtonConfigActionPerformed

    private void jToggleButtonCspActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButtonCspActionPerformed
        dispatchButtonStateChanged(evt);
    }//GEN-LAST:event_jToggleButtonCspActionPerformed

    private void jToggleNavigatorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleNavigatorActionPerformed
        dispatchButtonStateChanged(evt);
    }//GEN-LAST:event_jToggleNavigatorActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonConfig;
    private javax.swing.JButton jButtonExit;
    private javax.swing.JButton jButtonHelp;
    private javax.swing.JButton jButtonNodesImport;
    private javax.swing.JButton jButtonNodesPurge;
    private javax.swing.JButton jButtonStartStop;
    private javax.swing.JButton jButtonTracksImport;
    private javax.swing.JButton jButtonTracksPurge;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabelNodesTotal;
    private javax.swing.JLayeredPane jLayeredPane1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanelDataSources;
    private javax.swing.JPanel jPanelMap;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPaneConsole;
    private final javax.swing.JTabbedPane jTabbedPaneMain = new javax.swing.JTabbedPane();
    private javax.swing.JTextArea jTextAreaCollectorStatus;
    private javax.swing.JTextArea jTextAreaConsole;
    private javax.swing.JTextField jTextFieldPacketTrackCollectorPort;
    private javax.swing.JToggleButton jToggleButtonConsole;
    private javax.swing.JToggleButton jToggleButtonCsp;
    private javax.swing.JToggleButton jToggleButtonDataSources;
    private javax.swing.JToggleButton jToggleButtonMap;
    private javax.swing.JToggleButton jToggleNavigator;
    private javax.swing.JToolBar jToolBarTop;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    // End of variables declaration//GEN-END:variables
    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;
    private JDialog aboutBox;

    /**
     * @return the jPanelMap
     */
    public javax.swing.JPanel getJPanelMap() {
        return jPanelMap;
    }

    /**
     * @return the jTabbedPaneMain
     */
    public javax.swing.JTabbedPane getJTabbedPaneMain() {
        return jTabbedPaneMain;
    }

    /**
     * @return the jButtonConfig
     */
    public javax.swing.JButton getJButtonConfig() {
        return jButtonConfig;
    }

    /**
     * @return the jButtonExit
     */
    public javax.swing.JButton getJButtonExit() {
        return jButtonExit;
    }

    /**
     * @return the jPanelDataSources
     */
    public javax.swing.JPanel getJPanelDataSources() {
        return jPanelDataSources;
    }

    /**
     * @return the jScrollPaneConsole
     */
    public javax.swing.JScrollPane getJScrollPaneConsole() {
        return jScrollPaneConsole;
    }

    /**
     * @return the jTextAreaConsole
     */
    public javax.swing.JTextArea getJTextAreaConsole() {
        return jTextAreaConsole;
    }

    /**
     * @return the jToggleButtonConsole
     */
    public javax.swing.JToggleButton getJToggleButtonConsole() {
        return jToggleButtonConsole;
    }

    /**
     * @return the jToolBar1
     */
    public javax.swing.JToolBar getJToolBarTop() {
        return jToolBarTop;
    }

    /**
     * @return the mainPanel
     */
    public javax.swing.JPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * @return the progressBar
     */
    public javax.swing.JProgressBar getProgressBar() {
        return progressBar;
    }

    /**
     * @return the statusAnimationLabel
     */
    public javax.swing.JLabel getStatusAnimationLabel() {
        return statusAnimationLabel;
    }

    /**
     * @return the statusMessageLabel
     */
    public javax.swing.JLabel getStatusMessageLabel() {
        return statusMessageLabel;
    }

    /**
     * @return the statusPanel
     */
    public javax.swing.JPanel getStatusPanel() {
        return statusPanel;
    }

    /**
     * @return the messageTimer
     */
    public Timer getMessageTimer() {
        return messageTimer;
    }

    /**
     * @return the busyIconTimer
     */
    public Timer getBusyIconTimer() {
        return busyIconTimer;
    }

    /**
     * @return the idleIcon
     */
    public Icon getIdleIcon() {
        return idleIcon;
    }

    /**
     * @return the busyIcons
     */
    public Icon[] getBusyIcons() {
        return busyIcons;
    }

    /**
     * @return the busyIconIndex
     */
    public int getBusyIconIndex() {
        return busyIconIndex;
    }

   public JButton getjButtonNodesImport() {
      return jButtonNodesImport;
   }


   public JTextField getjTextFieldPacketTrackCollectorPort() {
      return jTextFieldPacketTrackCollectorPort;
   }

   public JLabel getjLabelNodesTotal() {
      return jLabelNodesTotal;
   }

   public JButton getjButtonNodesPurge() {
      return jButtonNodesPurge;
   }

   public JButton getjButtonTracksPurge() {
      return jButtonTracksPurge;
   }

   public JButton getjButtonTracksImport() {
      return jButtonTracksImport;
   }

   public JButton getjButtonStartStop() {
      return jButtonStartStop;
   }

   public JTextArea getjTextAreaCollectorStatus() {
      return jTextAreaCollectorStatus;
   }

   
}
