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

import de.fhg.fokus.net.netview.model.Model;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    
    private DataSourcesController dataSourcesController;
    private Model model;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

    @Override
    protected void initialize(String[] args) {
        logger.debug("=== Initializing NetView === ");
        model = new Model();
    }

 
    @Override
    protected void startup() {

        executor.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    initializeControllers();
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        });
    }

  
    private void initializeControllers() {
       
        this.dataSourcesController = new DataSourcesController(executor, model);
        this.dataSourcesController.init();

        if (this.dataSourcesController != null) {
            //logger.debug("DSC beim MainController!");
        }

        this.model.setDataSourcesController(this.dataSourcesController);

        if (this.model.getDataSourcesController() != null) {
            //logger.debug("DSC beim Model!");
        }

    }

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
    protected void shutdown() {
        super.shutdown();
 
        executor.shutdown();
        scheduler.shutdown();


    }
}
