package de.fhg.fokus.net.netview.control;

import de.fhg.fokus.net.ptapi.PtProbeStats;
import java.math.BigInteger;
import java.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoviDBInterface {

        private long uid;
        private long hdid;
        private long pdid;
        private String host;
        private String port;
        private String db;
        private String user;
        private String password;
        private final Logger logger = LoggerFactory.getLogger(getClass());

        public NoviDBInterface(String host, String port, String db, String user, String password){
                this.uid = 0;
                this.hdid = 0;
                this.pdid = 0;
                this.host = host;
                this.port = port;
                this.db = db;
                this.user = user;
                this.password = password;
                // Creating the SQL tables if they doesn't exist already.
                createHopDelaysTable();
                createPathDelaysTable();
                createNodeStatsTable();
        }

        private void createHopDelaysTable(){
                try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			logger.debug("createHopDelaysTable-Error: PostgreSQL JDBC Driver not found!");
		}
		Connection connection = null;
		try {
			connection = DriverManager.getConnection(
                                        "jdbc:postgresql://"+host+":"+port+"/"+db,user,password);
		} catch (SQLException e) {
			logger.debug("createHopDelaysTable-Error: Connection failed!");
		}

                if (connection != null) {
                        Statement stmt;
                        try {
                                stmt = connection.createStatement();

                                String query = "SELECT * FROM hop_delays;";
                                ResultSet res = stmt.executeQuery(query);
                                logger.debug("hop_delays-table exists.");

                                // Counting the rows of the table 'hop_delays'
                                int i = 0;
                                while(res.next()){
                                        i++;
                                }
                                //logger.debug("i = "+i);
                                hdid = i;

                        } catch (SQLException ex) {
                                logger.debug("Creating hop_delays-table ...");
                                
                                String update = "CREATE TABLE hop_delays (hdid bigint NOT NULL, "
                                        + "id bigint, ts bigint, src bigint, dst bigint, "
                                        + "num bigint, hits double precision, "
                                        + "sumdelay bigint, sumbytes bigint, "
                                        + "mindelay bigint, maxdelay bigint, "
                                        + "CONSTRAINT hop_delays_pkey PRIMARY KEY (hdid) );";
                                try {
                                        stmt = connection.createStatement();
                                        stmt.executeUpdate(update);
                                } catch (SQLException ex1) {
                                        logger.debug("createHopDelaysTable-Error: Wasn't able to create a statement or a query!");
                                }
                        }

                        try {
                                connection.close();
                        } catch (SQLException ex) {
                                logger.debug("createHopDelaysTable-Error: Wasn't able to close the connection!");
                        }

		} else {
			logger.debug("createHopDelaysTable-Error: Failed to set up the connection!");
		}
        }

        private void createPathDelaysTable(){
                try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			logger.debug("createPathDelaysTable-Error: PostgreSQL JDBC Driver not found!");
		}
		Connection connection = null;
		try {
			connection = DriverManager.getConnection(
                                        "jdbc:postgresql://"+host+":"+port+"/"+db,user,password);
		} catch (SQLException e) {
			logger.debug("createPathDelaysTable-Error: Connection failed!");
		}

                if (connection != null) {
                        Statement stmt;
                        try {
                                stmt = connection.createStatement();

                                String query = "SELECT * FROM path_delays;";
                                ResultSet res = stmt.executeQuery(query);
                                logger.debug("path_delays-table exists.");

                                // Counting the rows of the table 'hop_delays'
                                int i = 0;
                                while(res.next()){
                                        i++;
                                }
                                //logger.debug("i = "+i);
                                pdid = i;

                        } catch (SQLException ex) {
                                logger.debug("Creating path_delays-table ...");

                                String update = "CREATE TABLE path_delays (pdid bigint NOT NULL, "
                                        + "id bigint, ts bigint, src bigint, dst bigint, "
                                        + "num bigint, path text, "
                                        + "sumdelay bigint, sumbytes bigint, "
                                        + "mindelay bigint, maxdelay bigint, "
                                        + "CONSTRAINT path_delays_pkey PRIMARY KEY (pdid) );";
                                try {
                                        stmt = connection.createStatement();
                                        stmt.executeUpdate(update);
                                } catch (SQLException ex1) {
                                        logger.debug("createPathDelaysTable-Error: Wasn't able to create a statement or a query!");
                                }
                        }

                        try {
                                connection.close();
                        } catch (SQLException ex) {
                                logger.debug("createPathDelaysTable-Error: Wasn't able to close the connection!");
                        }

		} else {
			logger.debug("createPathDelaysTable-Error: Failed to set up the connection!");
		}
        }

        private void createNodeStatsTable(){
                try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			logger.debug("createNodeStatsTable-Error: PostgreSQL JDBC Driver not found!");
		}
		Connection connection = null;
		try {
			connection = DriverManager.getConnection(
                                        "jdbc:postgresql://"+host+":"+port+"/"+db,user,password);
		} catch (SQLException e) {
			logger.debug("createNodeStatsTable-Error: Connection failed!");
		}

                if (connection != null) {
                        Statement stmt;
                        try {
                                stmt = connection.createStatement();

                                String query = "SELECT * FROM node_stats;";
                                ResultSet res = stmt.executeQuery(query);
                                logger.debug("node_stats-table exists.");

                                // Counting the rows of the table 'delays'
                                int i = 0;
                                while(res.next()){
                                        i++;
                                }
                                //logger.debug("i = "+i);
                                uid = i;

                        } catch (SQLException ex) {
                                logger.debug("Creating node_stats-table ...");

                                String update = "CREATE TABLE node_stats ( "
                                        + "uid bigint NOT NULL, "
                                        + "oid bigint, "
                                        + "exporttime bigint,"
                                        + "observationtimemilliseconds bigint, "
                                        + "systemcpuidle double precision,"
                                        + "systemmemfree bigint, "
                                        + "processcpuuser double precision, "
                                        + "processcpusys double precision, "
                                        + "processmemvzs bigint,"
                                        + "processmemrss bigint, "
                                        + "CONSTRAINT node_stats_pkex PRIMARY KEY (uid) );";
                                try {
                                        stmt = connection.createStatement();
                                        stmt.executeUpdate(update);
                                } catch (SQLException ex1) {
                                        logger.debug("createNodeStatsTable-Error: Wasn't able to create a statement or a query!");
                                }
                        }

                        try {
                                connection.close();
                        } catch (SQLException ex) {
                                logger.debug("createNodeStatsTable-Error: Wasn't able to close the connection!");
                        }

		} else {
			logger.debug("createNodeStatsTable-Error: Failed to set up the connection!");
		}
        }

        public ResultSet getContent(String selection, String options, String table){
                ResultSet result = null;

                if(!(table.equals("hop_delays")) && !(table.equals("path_delays"))){
                        logger.debug("getContent-Error: parameter table has unexpected content");
                        return result;
                }

                try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			logger.debug("getContent-Error: PostgreSQL JDBC Driver not found!");
			return result;
		}
		Connection connection = null;
		try {
			connection = DriverManager.getConnection(
                                        "jdbc:postgresql://"+host+":"+port+"/"+db,user,password);
		} catch (SQLException e) {
			logger.debug("getContent-Error: Connection failed!");
			return result;
		}

                if (connection != null) {
                        Statement stmt;
                        try {
                                stmt = connection.createStatement();
                                String query = "SELECT " + selection + " FROM "+table+" "+ options + " ;";
                                //logger.debug("getContent-query: "+query);

                                result = stmt.executeQuery(query);
                        } catch (SQLException ex) {
                                ex.printStackTrace();
                                logger.debug("getContent-Error: Wasn't able to create a statement or a query!");
                        }

                        try {
                                connection.close();
                        } catch (SQLException ex) {
                                logger.debug("getContent-Error: Wasn't able to close the connection!");
                                return result;
                        }

		} else {
			logger.debug("getContent-Error: Failed to set up the connection!");
                        return result;
		}

                return result;
        }

        public void updateRow(long did, long sumdelay, long num, float hits,
                long sumbytes, long mindelay, long maxdelay, String table){

                if(!(table.equals("hop_delays")) && !(table.equals("path_delays"))){
                        logger.debug("updateRow-Error: parameter table has unexpected content");
                        return;
                }

                try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			logger.debug("updateRow-Error: PostgreSQL JDBC Driver not found!");
			return;
		}
		Connection connection = null;
		try {
			connection = DriverManager.getConnection(
                                        "jdbc:postgresql://"+host+":"+port+"/"+db,user,password);
		} catch (SQLException e) {
			logger.debug("updateRow-Error: Connection failed!");
			return;
		}
                if (connection != null) {
                        Statement stmt;
                        try {
                                stmt = connection.createStatement();

                                String update = "";
                                if(table.equals("hop_delays")){
                                        update = "UPDATE hop_delays "
                                        + "SET num = "+num
                                        + ", hits = "+hits
                                        + ", sumdelay = "+sumdelay
                                        + ", sumbytes = "+sumbytes
                                        + ", mindelay = "+mindelay
                                        + ", maxdelay = "+maxdelay
                                        + " WHERE hdid = "+did+" ;";
                                }
                                else if(table.equals("path_delays")){
                                        update = "UPDATE path_delays "
                                        + "SET num = "+num
                                        + ", sumdelay = "+sumdelay
                                        + ", sumbytes = "+sumbytes
                                        + ", mindelay = "+mindelay
                                        + ", maxdelay = "+maxdelay
                                        + " WHERE pdid = "+did+" ;";
                                }
                                logger.debug("updateRow-Update: "+update);

                                stmt.executeUpdate(update);
                        } catch (SQLException ex) {
                                logger.debug("updateRow-Error: Wasn't able to create a statement or a query!");
                        }

                        try {
                                connection.close();
                        } catch (SQLException ex) {
                                logger.debug("updateRow-Error: Wasn't able to close the connection!");
                                return;
                        }

		} else {
			logger.debug("updateRow-Error: Failed to set up the connection!");
                        return;
		}
        }

        public void writeRow(long id, long timestamp, long src, long dst,
                long numberOfDelays, float hitcounter, String path, long sumDelays, long sumBytes,
                long mindelay, long maxdelay, String table){

                if(table.equals("hop_delays")){
                    hdid++;
                }
                else if(table.equals("path_delays")){
                    pdid++;
                }
                else{
                        logger.debug("writeRow-Error: parameter table has unexpected content");
                        return;
                }
                
                try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			logger.debug("writeRow-Error: PostgreSQL JDBC Driver not found!");
			return;
		}
		Connection connection = null;
		try {
			connection = DriverManager.getConnection(
                                        "jdbc:postgresql://"+host+":"+port+"/"+db,user,password);
		} catch (SQLException e) {
			logger.debug("writeRow-Error: Connection failed!");
			return;
		}

                if (connection != null) {
                        Statement stmt;
                        try {
                                stmt = connection.createStatement();

                                String update = "";
                                
                                if(table.equals("hop_delays")){
                                        update = "INSERT INTO hop_delays "
                                        + "VALUES ( "
                                        + hdid + ", "
                                        + id +", "
                                        + timestamp +", "
                                        + src +", "
                                        + dst +", "
                                        + numberOfDelays +", "
                                        + hitcounter + ", "
                                        + sumDelays + ", "
                                        + sumBytes + ", "
                                        + mindelay + ", "
                                        + maxdelay + " "
                                        +");";
                                }
                                else if(table.equals("path_delays"))
                                {
                                        update = "INSERT INTO path_delays "
                                        + "VALUES ( "
                                        + pdid + ", "
                                        + id +", "
                                        + timestamp +", "
                                        + src +", "
                                        + dst +", "
                                        + numberOfDelays +", "
                                        + path + ", "
                                        + sumDelays + ", "
                                        + sumBytes + ", "
                                        + mindelay + ", "
                                        + maxdelay + " "
                                        +");";
                                }
                                logger.debug("writeRow-Update: "+update);

                                stmt.executeUpdate(update);
                        } catch (SQLException ex) {
                                logger.debug("writeRow-Error: Wasn't able to create a statement or a query!");
                        }

                        try {
                                connection.close();
                        } catch (SQLException ex) {
                                logger.debug("writeRow-Error: Wasn't able to close the connection!");
                                return;
                        }

		} else {
			logger.debug("writeRow-Error: Failed to set up the connection!");
                        return;
		}
        }

        public void exportNodeStats(PtProbeStats probeStats) {

                uid++;
                long oid = probeStats.getOid();
                long exportTime = probeStats.getExportTime();
                long observationTimeMilliseconds = probeStats.getObservationTimeMilliseconds();
                float systemCpuIdle = probeStats.getSystemCpuIdle();
                BigInteger systemMemFree = probeStats.getSystemMemFree();
                float processCpuUser = probeStats.getProcessCpuUser();
                float processCpuSys = probeStats.getProcessCpuSys();
                BigInteger processMemVzs = probeStats.getProcessMemVzs();
                BigInteger processMemRss = probeStats.getProcessMemRss();

		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			logger.debug("exportNodeStats-Error: PostgreSQL JDBC Driver not found!");
			return;
		}
		Connection connection = null;
		try {
			connection = DriverManager.getConnection(
                                        "jdbc:postgresql://"+host+":"+port+"/"+db,user,password);
		} catch (SQLException e) {
			logger.debug("exportNodeStats-Error: Connection failed!");
			return;
		}

		if (connection != null) {
                        Statement stmt;
                        try {
                                stmt = connection.createStatement();
                                String update = "INSERT INTO node_stats "
                                        + "VALUES ("
                                        + uid +", "
                                        + oid +", "
                                        + exportTime +", "
                                        + observationTimeMilliseconds +", "
                                        + systemCpuIdle +", "
                                        + systemMemFree +", "
                                        + processCpuUser +", "
                                        + processCpuSys +", "
                                        + processMemVzs +", "
                                        + processMemRss +
                                        ");";
                                logger.debug("exportNodeStats-Update: "+update);
                                stmt.executeUpdate(update);
                                
                        } catch (SQLException ex) {
                                logger.debug("exportNodeStats-Error: Wasn't able to create a statement or a query!");
                        }

                        try {
                                connection.close();
                        } catch (SQLException ex) {
                                logger.debug("exportNodeStats-Error: Wasn't able to close the connection!");
                                return;
                        }

		} else {
			logger.debug("exportNodeStats-Error: Failed to set up the connection!");
                        return;
		}
	}
}
