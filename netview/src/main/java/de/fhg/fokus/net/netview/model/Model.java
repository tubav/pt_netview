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
package de.fhg.fokus.net.netview.model;

import de.fhg.fokus.net.netview.control.DataSourcesController;
import de.fhg.fokus.net.netview.model.db.NetViewDB;
import de.fhg.fokus.net.netview.model.db.TrackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Model {

    private final static Logger logger = LoggerFactory.getLogger(Model.class);
    private final NetViewDB db;
    private DataSourcesController dsc = null;

    public Model() {
        this.db = new NetViewDB();
    }

    public TrackRepository getTrackRepository() {
        return db.getTrackRepository();
    }

    public NetViewDB getDb() {
        return db;
    }

    public void setDataSourcesController(DataSourcesController dsc) {
        this.dsc = dsc;
        this.db.getTrackRepository().dsc = dsc;
    }

    public DataSourcesController getDataSourcesController() {
        return this.dsc;
    }
}
