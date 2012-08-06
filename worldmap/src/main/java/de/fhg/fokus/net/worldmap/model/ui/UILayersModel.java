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

package de.fhg.fokus.net.worldmap.model.ui;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.worldmap.layers.Layer;
import de.fhg.fokus.net.worldmap.layers.map.MapLayer;
import de.fhg.fokus.net.worldmap.model.LayerModel;

/**
 * Adapts layer model for using in different UI Components (currently only JTable)
 * 
 * @author FhG-FOKUS NETwork Research
 *
 */
public class UILayersModel {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final LayerModel layerModel;
	private final UILayersTableModel tablemodel;
	private final int COL_STACK_LEVEL=0;
	private final int COL_LAYER_ID=1;
	private final int COL_VISIBLE =2;

	private String[] columnNames = { "Stack Level", "Layer ID", "visible" };
	private Class<?>[] columnClasses = { String.class,
			Integer.class,
			Boolean.class };

	/**
	 * UI Layers table model
	 * 
	 * <pre>
	 *   +-----------------------------------------+
	 *   | Stack Level   | Layer ID  | Visibility  |  
	 *   +---------------+-----------+-------------+
	 *   |               |           |             |
	 * </pre>
	 * 
	 */
	private class UILayersTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;
		@Override
		public String getColumnName(int column) {
			return columnNames[column];
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return columnClasses[columnIndex];
		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public int getRowCount() {
			return layerModel.size();
		}
		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			if(columnIndex==COL_VISIBLE){
				return true;
			}
			return false;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if( columnIndex==COL_VISIBLE){
				Layer layer = layerModel.getByOrder(rowIndex);
				boolean value = (Boolean)aValue;
				// a map layer jcomponent cannot be hidden, otherwise we
				// won't be able to drag the background
				if (layer instanceof MapLayer) {
					MapLayer mapLayer = (MapLayer) layer;
					mapLayer.hideMap(!value);
				} else {
					layer.setVisible(value);
				}
			}
			fireTableCellUpdated(rowIndex, columnIndex);
		}
		@Override
		public Object getValueAt(int row, int col) {
			Layer layer = layerModel.getByOrder(row);
			if(layer==null){
				// TODO i18n
				return "invalid";
			}
			if (col == COL_STACK_LEVEL) {
				return layer.getLevel();
			} else if (col == COL_LAYER_ID) {
				return layer.getLayerId();
			} else if (col == COL_VISIBLE) {
				if (layer instanceof MapLayer) {
					MapLayer mapLayer = (MapLayer) layer;
					return !mapLayer.isHidden();
				} 
				return layer.isVisible();
			}
			return "";
		}

	}

	public UILayersModel(LayerModel layermodel) {
		this.tablemodel = new UILayersTableModel();
		this.layerModel = layermodel;
	}
	public TableModel getTableModel() {
		return tablemodel;
	}

}
