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

package de.fhg.fokus.net.netview.model.ui;

import java.awt.Component;
import java.util.prefs.Preferences;

import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.netview.model.PersistentPreferences;

/**
 *  Extends toolbar for application specific requirements.
 *  
 *  @author FhG-FOKUS NETwork Research
 */
public class ToolBarModel implements PersistentPreferences {
	// == external services ==
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	// == properties ==
	private final JToolBar toolBar;
	private final Preferences prefs = Preferences.userNodeForPackage(this.getClass());
	// == enums ==
	private enum SavedStates {
		isSelected
	}
	public ToolBarModel(JToolBar toolBar) {
		this.toolBar = toolBar;
	}
	public JToolBar getToolBar() {
		return toolBar;
	}
	/**
	 * Get button by name
	 * @param name
	 * @return
	 */
	public JToggleButton getToggleButton( String name){
		if(name!=null){
			for( Component c: toolBar.getComponents() ){
				if (c instanceof JToggleButton) {
					JToggleButton btn = (JToggleButton) c;
					if( name.contentEquals(btn.getName() )){
						return btn;
					}
				}
			}
		} else {
			logger.error("button name is null");
		}
		return null;
	}
	@Override
	public void loadPreferences() {
		String toolBarName = toolBar.getName();
		for( Component c: toolBar.getComponents() ){
			if (c instanceof JToggleButton) {
				JToggleButton btn = (JToggleButton) c;
				boolean isSelected =prefs.getBoolean(String.format("%s.%s.%s",toolBarName, btn.getName(),
						SavedStates.isSelected), false);
				btn.setSelected(isSelected);
			}
		}
	}
	@Override
	public void savePreferences() {
		String toolBarName = toolBar.getName();
		for( Component c: toolBar.getComponents() ){
			if (c instanceof JToggleButton) {
				JToggleButton btn = (JToggleButton) c;
				prefs.putBoolean(String.format("%s.%s.%s",toolBarName, btn.getName(),SavedStates.isSelected), btn.isSelected());
			}
		}
	}
}
