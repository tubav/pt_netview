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



public class NodeSystemStatsChart extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private Node   node;
	private String chartTitle;
	private long timestamp;
	
	private ChartPanel chartPanel;
	private JFreeChart chart;
	
	private String[] names = { "cpu_free", "cpu_probe", "ram_free", "ram_probe" };

    private final EbeanServer eServer;
	private ColorSequence colorSequence;

	
	
	public NodeSystemStatsChart(EbeanServer eServer, String title, Node node, long timestamp) {
		super(title);
		this.chartTitle = title;
		this.node = node;
		this.timestamp = timestamp;
		
		this.eServer = eServer;
//		this.ef = eServer.getExpressionFactory();
		
		setupChart();
	
		chartPanel = new ChartPanel(chart);
		setContentPane(chartPanel);
	}
	
	private void setupChart() {
		TimeSeriesCollection[] data = fetchData();
		colorSequence = new ColorSequence();
		
		chart = ChartFactory.createTimeSeriesChart(chartTitle, "ts", "kb", null, true, false, false);
        
		XYPlot plot = (XYPlot)chart.getPlot();
        NumberAxis kbAxis = new NumberAxis("kb");
        NumberAxis pcAxis = new NumberAxis("ratio");

        // add datasets to the chart
        for(int i = 0; i < data.length; i++)
        	plot.setDataset(i, data[i]);
        
        // add separate axis kb vs %
        plot.setRangeAxis(0, pcAxis);
        plot.setRangeAxis(1, kbAxis);

        // assign datasets to axis
        plot.mapDatasetToRangeAxis(0, 0);
        plot.mapDatasetToRangeAxis(1, 0);
        plot.mapDatasetToRangeAxis(2, 1);
        plot.mapDatasetToRangeAxis(3, 1);
        
        // assign colors
        for(int i = 0; i < data.length; i++) {
            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
            renderer.setSeriesPaint(0, colorSequence.next());
            plot.setRenderer(i, renderer);
        }
	}


	private TimeSeriesCollection[] fetchData() {
		long probe = node.mp.getProbe().getProbeId();
		// TODO fetch data from the db
		long fixed_ts = timestamp;
//		long fixed_ts = DB_TS_TIMEUNIT.convert(timestamp, TimeUnit.MILLISECONDS);
		int numberOfResults = 100;
		String sql_query = "SELECT * FROM probe_stats " +
			"WHERE oid = :probeId AND " +
			"timestamp < :playerts " +
			"ORDER BY timestamp DESC LIMIT :limit";
		SqlQuery query = eServer.createSqlQuery(sql_query)
			.setParameter("playerts", fixed_ts)
			.setParameter("probeId", probe)
			.setParameter("limit", numberOfResults);
		List<SqlRow> results = query.findList();

		logger.debug(sql_query);
		logger.debug("SELECT * FROM probe_stats WHERE oid = "+probe+ " AND timestamp < "+fixed_ts +" ORDER BY timestamp DESC LIMIT "+ numberOfResults);
		
		TimeSeriesCollection[] data = new TimeSeriesCollection[names.length];
		TimeSeries[] ts = new TimeSeries[names.length];
		for (int init=0; init< names.length; init++){
			ts[init]= new TimeSeries(names[init]);
		}
		for (SqlRow row : results) {
			float cpu_free = row.getFloat("system_cpu_idle");
			float cpu_probe = row.getFloat("process_cpu_user") + row.getFloat("process_cpu_sys");
			long ram_free = row.getLong("system_mem_free");
			long ram_probe = row.getLong("process_mem_vzs") + row.getLong("process_mem_rss");
			long timestamp = row.getLong("timestamp");
			logger.debug("RAM_FREE "+ram_free + "    TIMESATMP: "+ timestamp);
			FixedMillisecond ms = new FixedMillisecond(timestamp);
			
			if(cpu_free >= 0)
				ts[0].addOrUpdate(ms,cpu_free);
			if(cpu_probe >= 0)
				ts[1].addOrUpdate(ms,cpu_probe);
			if(ram_free >= 0)
				ts[2].addOrUpdate(ms,ram_free);
			if(ram_probe >= 0)
				ts[3].addOrUpdate(ms,ram_probe/1000.);
		}
			
		data[0] = new TimeSeriesCollection(ts[0]);
		data[1] = new TimeSeriesCollection(ts[1]);
		data[2] = new TimeSeriesCollection(ts[2]);
		data[3] = new TimeSeriesCollection(ts[3]);
			
		return data;
	}

}
