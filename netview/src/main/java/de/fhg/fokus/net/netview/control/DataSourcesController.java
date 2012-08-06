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

package de.fhg.fokus.net.netview.control;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.netview.control.PacketTrackCollector.PtcEventData;
import de.fhg.fokus.net.netview.control.PacketTrackCollector.PtcEventType;
import de.fhg.fokus.net.netview.model.Model;
import de.fhg.fokus.net.netview.model.Model.ModelEventData;
import de.fhg.fokus.net.netview.model.Model.ModelEventType;
import de.fhg.fokus.net.netview.model.Network;
import de.fhg.fokus.net.netview.model.PersistentPreferences;
import de.fhg.fokus.net.netview.model.db.NetViewDB;
import de.fhg.fokus.net.netview.view.ViewMain;
import de.fhg.fokus.net.worldmap.util.EventSupport;
import de.fhg.fokus.net.worldmap.util.EventSupport.EventListener;

/**
 * Data source controller
 * 
 * @author FhG-FOKUS NETwork Research
 * 
 */
public class DataSourcesController implements Controllable, PersistentPreferences  {
	// sys
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Timer timer = new Timer("Datasources");
	private final Preferences prefs = Preferences.userNodeForPackage(getClass());
	private static enum PrefKeys {
		LAST_VISITED_NODES_DIRECTORY,
		LAST_VISITED_TRACKS_DIRECTORY,
		COLLECTOR_PORT,
		START_COLLECTOR
	}
	private final ExecutorService executor;
	private final EventSupport<EventType, EventData> eventSupport;
	public static enum EventType {
		CONTROLLER_STARTED
	}
	// ctrl
	private int collectorPort;
	// view
	private static SimpleDateFormat iso8601s = new SimpleDateFormat(
	"yyyy-MM-dd  HH:mm:ss.S");
	// --
	public static class EventData {
		// nothing yet
	}
	//--
	private final ViewMain view;
	private final Model model;
	private final NetViewDB db;
	private final PacketTrackCollector collector;
	//	private final PacketTrackCollectorTest packetTrackCollector;
	// view
	private final JFileChooser fileChooser;
	private File lastVisitedNodesDirectory; 
	private File lastVisitedTracksDirectory;
	public DataSourcesController(ViewMain view, Model model,
			ExecutorService executor  ) {
		super();
		this.view = view;
		this.model = model;
		this.executor = executor;
		this.collector = new PacketTrackCollector(model.getTrackRepository(),this.executor);
		this.db = this.model.getDb();
		this.eventSupport = new EventSupport<EventType, EventData>(this.executor);
		this.fileChooser = new JFileChooser(model.getConfig().getNetviewHome());


	}
	private void updateCollectorStatus(){
		StringBuffer sbuf = new StringBuffer();
		if( collector.isBound() ){
			view.getjButtonStartStop().setText("Stop");
			sbuf.append(String.format("Bound to:\t%s \n",collector.getLocalAddress()));
			sbuf.append(String.format("Started at:\t%s \n",iso8601s.format(new Date(collector.getStartedAt()))));
			sbuf.append(String.format("Records:\t%d\n", collector.getNumberOfRecords()));
			if( collector.getClients().size() > 0 ){
				sbuf.append(String.format("Packet Track Exporters: %d ",collector.getClients().size() ));
				int i =1;
				for( Socket socket: collector.getClients() ){
					sbuf.append(String.format("\n - %d: %s ",i++, socket ));
				}
			}
		} else {
			view.getjButtonStartStop().setText("Start");
			sbuf.append("Not running!");
		}
		view.getjTextAreaCollectorStatus().setText(sbuf.toString());
		view.getjTextFieldPacketTrackCollectorPort().setText(collectorPort+"");
	}

	@Override
	public void init() {
		setupNodes();
		setupTracks();
		setupModelBindings();
		setupCollector();
	}

	private final TimerTask timerTaskUpdateCollectorStatus = new TimerTask() {
		@Override
		public void run() {
			if( collector.isBound()){
				updateCollectorStatus();
			}
		}
	};
	private void setupCollector() {
		// refresh status
		timer.scheduleAtFixedRate(timerTaskUpdateCollectorStatus, 5000, 5000);

		collector.addEventListener(PtcEventType.STARTED, new EventListener<PtcEventData>() {
			public void onEvent(PtcEventData e) {
				updateCollectorStatus();
				view.message("Collector started.");
			};
		});
		collector.addEventListener(PtcEventType.STOPPED, new EventListener<PtcEventData>() {
			public void onEvent(PtcEventData e) {
				updateCollectorStatus();
				view.message("Collector stopped.");
			};
		});
		collector.addEventListener(PtcEventType.CLIENT_CONNECTED, new EventListener<PtcEventData>() {
			public void onEvent(PtcEventData e) {
				updateCollectorStatus();
				view.message("New client connected from "+e.clientAddress);
			};
		});
		collector.addEventListener(PtcEventType.CLIENT_DISCONNECTED, new EventListener<PtcEventData>() {
			public void onEvent(PtcEventData e) {
				updateCollectorStatus();
				view.message("New client disconnected from "+e.clientAddress);
			};
		});


		// Start / Stop button
		view.getjButtonStartStop().addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				executor.execute(new Runnable() {

					@Override
					public void run() {
						if(collector.isBound()){
							view.message("Stopping collector..");
							collector.stop();
						} else {
							try {
								try {
									collectorPort = Integer.parseInt(view.getjTextFieldPacketTrackCollectorPort().getText());
								} catch (Exception eport ){
									throw new Exception("Invalid port format!");
								}
								view.message("Starting collector..");
								collector.bind(collectorPort);

							} catch (Exception e) {
								view.message(e.getMessage());
							}
						}
					}
				});


			}
		});
	}

	/**
	 * Setup nodes data source functions
	 */
	public void updateNodesControls(Network net){
		if(net==null){
			return;
		}
		JLabel label  =  view.getjLabelNodesTotal();
		int total = 0;
		if(net.getNodes()!=null){
			total = net.getNodes().size();
		}

		label.setText(String.format("%d node(s) in %s", total,net.getLabel()));

	}

	private void setupModelBindings(){
		model.addNodeEventListener(ModelEventType.NETWORK_LOADED, new EventListener<ModelEventData>() {
			@Override
			public void onEvent(ModelEventData e) {
				updateNodesControls(e.network);
				view.message("Loaded network: "+e.network);
			}
		});
		model.addNodeEventListener(ModelEventType.NETWORK_UNLOADED, new EventListener<ModelEventData>() {
			@Override
			public void onEvent(ModelEventData e) {
				updateNodesControls(e.network);
				view.message("Unloaded network: "+e.network);
			}
		});

	}

	/**
	 * Setup nodes data source
	 */
	private void setupNodes(){
		setupNodesPurge();
		setupNodesImportExport();
	}
	/**
	 * Setup tracks data source
	 */
	private void setupTracks(){
		setupTracksPurge();
		setupTracksImport();
	}

	private void setupTracksPurge() {
		view.getjButtonTracksPurge().addActionListener(
				new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						executor.execute(new Runnable() {
							@Override
							public void run() {

								if( JOptionPane.showConfirmDialog(view.getFrame(),
										"Are you sure you want to purge tracks from the database?",
										"Purge Packet Tracks",
										JOptionPane.YES_NO_OPTION)== JOptionPane.NO_OPTION ){
									view.message("Purge packet tracks canceled.",5);
									return;
								}
								view.getBusyIconAnimator().start();
								view.message("Purging packet tracks ...");
								try {
									db.purgePacketTracks();
									model.getTrackRepository().clearCache();
									model.getSplineLayer().reset();
									EventQueue.invokeLater(new Runnable() {
										
										@Override
										public void run() {
											model.getSplineLayer().repaint();
										}
									});
									view.message("Packet tracks purged from database!",4);
								} catch (Exception e) {
									view.message("Could not purge tracks from database!");
									view.message(e.getLocalizedMessage());
									logger.debug(e.getMessage());
								} finally {
									view.getBusyIconAnimator().stop();
								}

							}
						});

					}
				}	
		);
	}
	
	private void setupTracksImport() {
		view.getjButtonTracksImport().addActionListener(
				new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						executor.execute(new Runnable() {
							@Override
							public void run() {
								JFileChooser fc = new JFileChooser();
								fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
								if(fc.showOpenDialog(view.getFrame()) == JFileChooser.APPROVE_OPTION) {
									view.getBusyIconAnimator().start();
									File f = fc.getSelectedFile();
									logger.debug("importing tracks from " + f.toString());
									view.message("Importing objects from " + f.toString());
									int count = db.importTracks(f);
									JOptionPane.showMessageDialog(view.getFrame(), 
											"Imported " + Integer.toString(count) + " objects.",
											"Import",
											JOptionPane.INFORMATION_MESSAGE);
									logger.debug("Imported " + Integer.toString(count) + " objects");
									view.message("Imported " + Integer.toString(count) + " objects", 5);
									view.getBusyIconAnimator().stop();
								}
							}
						});

					}
				}	
		);
	}

	private void setupNodesPurge() {
		view.getjButtonNodesPurge().addActionListener(
				new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						executor.execute(new Runnable() {
							@Override
							public void run() {

								if( JOptionPane.showConfirmDialog(view.getFrame(),
										"Are you sure you want to delete all nodes from the database?",
										"Purge Nodes",
										JOptionPane.YES_NO_OPTION)== JOptionPane.NO_OPTION ){
									view.message("Purge nodes canceled.");
									return;
								}
								try {
									model.unloadNetworks();
									db.purgeNodes();
									view.message("All nodes where successfully removed from the database!");
									// TODO review
									updateNodesControls(model.network);
								} catch (Exception e) {
									view.message("Could not purge nodes from db!",5);
									view.message(e.getLocalizedMessage());
									logger.debug(e.getMessage());
								}

							}
						});

					}
				}	
		);

	}
	private final FileFilter nodesFileFilter = new FileFilter() {
		@Override
		public boolean accept(File file) {
			String filename = file.getName();
			return filename.endsWith(".xml") || file.isDirectory();
		}
		@Override
		public String getDescription() {
			return "NetView nodes (*.xml)";
		}
	};
	private void setupNodesImportExport(){
		view.getjButtonNodesImport().addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if( lastVisitedNodesDirectory!= null){
					fileChooser.setCurrentDirectory(lastVisitedNodesDirectory);
				}
				fileChooser.setFileFilter(nodesFileFilter);
				fileChooser.showOpenDialog(view.getFrame());
				lastVisitedNodesDirectory = fileChooser.getCurrentDirectory();
				if(fileChooser.getSelectedFile()==null){
					view.message("Import nodes aborted!",5);
					return;
				}
				executor.execute(new Runnable() {
					@Override
					public void run() {
						view.getBusyIconAnimator().start();
						try {
							db.importNodes(model.network, fileChooser.getSelectedFile());
                                                        //db.importNodesAlternative(model); // New Node Import
							model.refreshNetworks();
							updateNodesControls(model.network);
							view.message("imported "+fileChooser.getSelectedFile().getName());
						} catch (Exception e) {
							e.printStackTrace();
							logger.error(e+"");
							view.message(e.getMessage());
						} finally {
							view.getBusyIconAnimator().stop();
						}

					}
				});

			}
		});
	}
	@Override
	public void start() {
		loadPreferences();
		eventSupport.dispatch(EventType.CONTROLLER_STARTED, null);
	}

	@Override
	public void stop() {
		timerTaskUpdateCollectorStatus.cancel();
		timer.cancel();
		savePreferences();

	}


	@Override
	public void loadPreferences() {
		lastVisitedNodesDirectory=new File(
				prefs.get(PrefKeys.LAST_VISITED_NODES_DIRECTORY+"", model.getConfig().getNetviewHome().getAbsolutePath()));
		lastVisitedTracksDirectory=new File(
				prefs.get(PrefKeys.LAST_VISITED_TRACKS_DIRECTORY+"", model.getConfig().getNetviewHome().getAbsolutePath()));
		collectorPort = prefs.getInt(PrefKeys.COLLECTOR_PORT+"", 40123);
		// start collector 
		if( prefs.getBoolean(PrefKeys.START_COLLECTOR+"", true)){
			executor.execute(new Runnable() {

				@Override
				public void run() {
					collector.bind(collectorPort);
				}
			});
		}

		updateCollectorStatus();
	}


	@Override
	public void savePreferences() {
		prefs.put(PrefKeys.LAST_VISITED_NODES_DIRECTORY+"", lastVisitedNodesDirectory+"");
		prefs.put(PrefKeys.LAST_VISITED_TRACKS_DIRECTORY+"", lastVisitedTracksDirectory+"");
		prefs.put(PrefKeys.COLLECTOR_PORT+"", collectorPort+"");
		prefs.put(PrefKeys.START_COLLECTOR+"", collector.isBound()+"");
	}
	public void addEventListener( EventType evt, EventSupport.EventListener<EventData> lsn ){
		eventSupport.addEventListener(evt, lsn);
	}
	public void removeEventListener( EventSupport.EventListener<EventData> lsn ){
		eventSupport.removeEventListener(lsn);
	}

        public NetViewDB getDB(){
                return this.db;
        }

        public Model getModel(){
                return this.model;
        }

        public ViewMain getView(){
                return this.view;
        }

}
