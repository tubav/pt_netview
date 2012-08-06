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

package de.fhg.fokus.net.netview.view.charts;

import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;

import de.fhg.fokus.net.netview.model.Node;
import de.fhg.fokus.net.netview.view.util.ColorSequence;


public class NodeSamplingStatsChart extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private Node   node;
	private String chartTitle;
	private long timestamp;
	private Vector<Long> probeIds = new Vector<Long>();
	
	private ChartPanel chartPanel;
	private JFreeChart chart;
	private final EbeanServer eServer;
	
	private String[] names = { "ASF","pcap_Stat_Recv", "pcap_Stat_Drop"};

	private ColorSequence colorSequence;
	
	
	public NodeSamplingStatsChart(EbeanServer eServer, String title, Node node, long timestamp) {
		super(title);
		this.chartTitle = title;
		this.node = node;
		this.timestamp = timestamp;

		this.eServer = eServer;
		
		probeIds.add(node.mp.getProbe().getProbeId());
		
		setupChart();
	
		chartPanel = new ChartPanel(chart);
		setContentPane(chartPanel);
	}
	
	private void setupChart() {
		TimeSeriesCollection[] data = fetchData();
		colorSequence = new ColorSequence();
		
		chart = ChartFactory.createTimeSeriesChart(chartTitle, "ts", "", null, true, false, false);
        
		XYPlot plot = (XYPlot)chart.getPlot();
        NumberAxis kbAxis = new NumberAxis("ASF");
        NumberAxis pcAxis = new NumberAxis("pcap");

        // add datasets to the chart
        for(int i = 0; i < data.length; i++)
        	plot.setDataset(i, data[i]);

        // add separate axis kb vs %
        plot.setRangeAxis(0, kbAxis);
        plot.setRangeAxis(1, pcAxis);

        // assign datasets to axis
        for (int i=0; i< data.length; i=i+3){
	        plot.mapDatasetToRangeAxis(i+0, 0);
	        plot.mapDatasetToRangeAxis(i+1, 1);
	        plot.mapDatasetToRangeAxis(i+2, 1);
        }
        
        // assign colors
        for(int i = 0; i < data.length; i++) {
            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
            renderer.setSeriesPaint(0, colorSequence.next());
            plot.setRenderer(i, renderer);
        }
	}


	private TimeSeriesCollection[] fetchData() {
		// TODO fetch data from the db
		
		long probeId = node.mp.getProbe().getProbeId();
		int numberOfResults = 100;
		String interfaceName;
		int count = 0;
		int i =0;
		
		String sql_query = "SELECT DISTINCT(interface_name) FROM interface_stats " +
		"WHERE oid = :probeId"; 
		SqlQuery query = eServer.createSqlQuery(sql_query)
			.setParameter("probeId", probeId);
		List<SqlRow> interf = query.findList();
		int interfaces = interf.size();
		TimeSeriesCollection[] data = new TimeSeriesCollection[names.length*interfaces];
		TimeSeries[] ts = new TimeSeries[names.length*interfaces];

		for (SqlRow row : interf) {
			interfaceName = row.getString("interface_name");

			for (int init=0; init< names.length; init++){
				ts[init+count]= new TimeSeries(names[init]+"_"+interfaceName);
			}
			i++;
			
			sql_query = "SELECT * FROM interface_stats " +
			"WHERE oid = :probeId AND " +
			"timestamp < :timestamp " +
			"ORDER BY timestamp DESC LIMIT :limit";
			query = eServer.createSqlQuery(sql_query)
				.setParameter("probeId", probeId)
				.setParameter("timestamp", timestamp)
				.setParameter("limit", numberOfResults);
			List<SqlRow> results = query.findList();

			for (SqlRow result : results) {
				long samplingSize = result.getLong("sampling_size");
				long packetDeltaCount = result.getLong("packet_delta_count");
				long pcapStatRecv = result.getLong("pcap_stat_recv");
				long pcapStatDrop = result.getLong("pcap_stat_drop");
				long timestamp = result.getLong("timestamp");
				FixedMillisecond ms = new FixedMillisecond(timestamp);
				if (packetDeltaCount != 0){
					ts[0+count].addOrUpdate(ms,(float)samplingSize/packetDeltaCount);
				}
				ts[1+count].addOrUpdate(ms,pcapStatRecv);
				ts[2+count].addOrUpdate(ms,pcapStatDrop);
			}
			count=count+names.length;
		}		

		for (i=0;i<names.length*interfaces;i++){
			data[i] = new TimeSeriesCollection(ts[i]);
		}
		return data;
	}

}
