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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.netview.model.ui.TabbedPaneModel;

/**
 *  Controls a tabbed pane. It also extends the default tabbed model a bit so we do
 *  things like hide/show a tab without losing the original model.
 *  @apiviz.stereotype control
 *  //apiviz.uses de.fhg.fokus.net.netview.model.ui.TabbedPaneModel
 *  
 *  @author FhG-FOKUS NETwork Research
 */
public class TabbedPaneController implements Controllable {
	//== External services ==
	/**
	 * Logger
	 */
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	//== controlled model ==
	protected final TabbedPaneModel model;
	
	public TabbedPaneController(TabbedPaneModel model ) {
		super();
		this.model = model;
	}
	


	@Override
	public void start() {
		// TODO Auto-generated method stub

	}
	@Override
	public void stop() {
		logger.debug("stopping tabbed pane controller");
		
		// TODO Auto-generated method stub

	}
	/**
	 * Show/Hide tab
	 * @param name tab name
	 * @param show true to show, false to hide
	 */
	public void setTabVisible( String name, boolean show ){
		model.setTabVisible(name, show);

	}


	@Override
	public void init() {
		model.loadPreferences();
		
		
	}




}
