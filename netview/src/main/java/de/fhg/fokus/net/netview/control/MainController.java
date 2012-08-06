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
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.netview.model.Model;
import de.fhg.fokus.net.netview.model.db.TrackRepository;
import de.fhg.fokus.net.netview.model.db.TrackRepository.EventType;
import de.fhg.fokus.net.netview.model.ui.TabbedPaneModel;
import de.fhg.fokus.net.netview.sys.NetViewConfig;
import de.fhg.fokus.net.netview.view.ViewMain;
import de.fhg.fokus.net.worldmap.layers.track.DefaultTrackPlayer;
import de.fhg.fokus.net.worldmap.layers.track.TrackPlayer;
import de.fhg.fokus.net.worldmap.util.EventSupport;

/**
 * The main class of the application.
 * 
 * @author FhG-FOKUS NETwork Research
 * 
 * 
 */
public final class MainController extends SingleFrameApplication {
    //==[ external services / utilities ]===

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    // configuration settings (loaded from NETVIEW_HOME/netview.xml)
    private static NetViewConfig config;
    //==[ sub-controllers  ]==
    private MainTabbedPaneController tabbedPaneMainCtrl;
    private TopToolBarController toolbarTopCtrl;
    private MapController mapCtrl;
    //	private Console console;
    private TrackPlayer trackPlayer;
    private DataSourcesController dataSourcesController;
    private Model model;
    //	private CSPController cspCtrl;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

    @Override
    protected void initialize(String[] args) {
        logger.debug("=== Initializing NetView === ");
        config = new NetViewConfig(new File("."));
        model = new Model(executor, config);
    }

    /**
     * At startup create and show the main frame of the application.
     */
    @Override
    protected void startup() {
        // FIXME there should be a view init before show
        show(new ViewMain(MainController.this));

        // avoiding using AWT-Thread
        executor.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    initializeControllers();
                    dataSourcesController.start();
                    model.loadPreferences();

                    EventQueue.invokeLater(new Runnable() {

                        @Override
                        public void run() {

                            getMainView().getBusyIconAnimator().start();

                            getMainView().message("NetView initialized.", 7);
                            scheduler.schedule(new Runnable() {

                                @Override
                                public void run() {
                                    trackPlayer.start();
                                    trackPlayer.updateStatus();
                                }
                            }, 2000, TimeUnit.MILLISECONDS);

                        }
                    });
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
                EventQueue.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        getMainView().getBusyIconAnimator().stop();
                    }
                });
            }
        });
//		show();

    }

    /**
     * Initialize controllers. Object dependencies are solved here and in the subsequent initialization
     * methods. 
     * 
     * @throws RuntimeException if resources can't be found
     */
    private void initializeControllers() {
        // starting up ..
        final ViewMain view = getMainView();
        if (view == null) {
            String error = "could not get main view";
            logger.error(error);
            throw new RuntimeException(error);
        }


        final ResourceMap resourceMap = view.getResourceMap();
        if (resourceMap == null) {
            String error = "could not get resource map";
            logger.error(error);
            throw new RuntimeException(error);
        }

        //== 1. MainTabbedPane Controller  ==
        initMainTabbedPaneController(view, resourceMap);

        // == map controller ==
        this.mapCtrl = new MapController(view, model, config);
        this.mapCtrl.init();

        // == Data sources controller
        this.dataSourcesController = new DataSourcesController(view,
                model,
                executor);
        this.dataSourcesController.init();

        if (this.dataSourcesController != null) {
            //logger.debug("DSC beim MainController!");
        }

        this.model.setDataSourcesController(this.dataSourcesController);

        if (this.model.getDataSourcesController() != null) {
            //logger.debug("DSC beim Model!");
        }


        // == track player ==
        initTrackPlayer(view);
        model.getNodeStatsLayer().setTrackPlayer(getTrackPlayer());

        //== . ToolBar Top Controller
        initToolBarTopController(view, resourceMap);

    }

    /**
     * Initialize track player
     * @param view
     */
    private void initTrackPlayer(final ViewMain view) {
        final SimpleDateFormat iso8601s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");
        TrackRepository repo = model.getTrackRepository();
        this.trackPlayer = new DefaultTrackPlayer(
                mapCtrl.getWorldmap().getToolsLayer().getTrackPlayerPanel(),
                model.getSplineLayer(), model.getTrackRepository(), scheduler);
        // Attaching notification in for the user interface 
        repo.addEventListener(EventType.LOADING_TRACKS_STARTED,
                new EventSupport.EventListener<TrackRepository.EventData>() {

                    public void onEvent(TrackRepository.EventData e) {
                        view.getBusyIconAnimator().start();
                        view.message(String.format("Loading packet tracks, interval: %s - %s", iso8601s.format(new Date(e.startTs)),
                                iso8601s.format(new Date(e.stopTs))));
                    }
                ;
        });
		repo.addEventListener(EventType.LOADING_TRACKS_FINISHED,
                new EventSupport.EventListener<TrackRepository.EventData>() {

                    public void onEvent(TrackRepository.EventData e) {
                        view.getBusyIconAnimator().stop();
                        view.message(String.format("Finished loading %d (%d new) tracks in %d ms, interval: %s - %s",
                                e.numberOfTracks,
                                e.numberOfNewTracks,
                                e.elapsedTime,
                                iso8601s.format(new Date(e.startTs)),
                                iso8601s.format(new Date(e.stopTs))));
                        trackPlayer.updateStatus();
                    }
                ;
        });
		repo.addEventListener(EventType.LOADING_TRACKS_FAILED,
                new EventSupport.EventListener<TrackRepository.EventData>() {

                    public void onEvent(TrackRepository.EventData e) {
                        view.getBusyIconAnimator().stop();
                        view.message(String.format("Could not load packet tracks for interval: %s - %s",
                                iso8601s.format(new Date(e.startTs)),
                                iso8601s.format(new Date(e.stopTs))));
                    }
                ;
        });
		this.trackPlayer.init();
    }

    private void initMainTabbedPaneController(ViewMain view, ResourceMap resourceMap) {
        TabbedPaneModel model = new TabbedPaneModel(view.getJTabbedPaneMain(), resourceMap);
        tabbedPaneMainCtrl = new MainTabbedPaneController(model);
        tabbedPaneMainCtrl.init();
    }

    /**
     * Initializes toolbar top controller
     *  - setup events
     * @param view
     * @param resourceMap
     */
    private void initToolBarTopController(final ViewMain view, ResourceMap resourceMap) {
        //== Toolbar Top  ==

        toolbarTopCtrl = new TopToolBarController(view, mapCtrl, tabbedPaneMainCtrl);
        toolbarTopCtrl.init();

    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override
    protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of DesktopApplication2
     */
    public static MainController getApplication() {
        return Application.getInstance(MainController.class);
    }

    public Model getModel() {
        return model;
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {

        launch(MainController.class, args);
    }

    @Override
    public ViewMain getMainView() {

        return (ViewMain) super.getMainView();
    }

    /**
     * Shuts controllers down. The order in which controllers are stopped _is_ important and
     * depends on who controllers of same level interacts with each other.
     */
    @Override
    protected void shutdown() {
        super.shutdown();

        // view
        getMainView().stop();

        // controllers
        trackPlayer.stop();
        toolbarTopCtrl.stop();
        tabbedPaneMainCtrl.stop();
        mapCtrl.stop();

        dataSourcesController.stop();
        // model
        model.savePreferences();

        // system
        executor.shutdown();
        scheduler.shutdown();


    }

    public TrackPlayer getTrackPlayer() {
        return this.trackPlayer;
    }
}
