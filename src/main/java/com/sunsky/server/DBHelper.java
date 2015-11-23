package com.sunsky.server;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

public class DBHelper {

    private static BoneCP connectionPool = null;

    static {
	try {
	    Class.forName("com.mysql.jdbc.Driver");
	    BoneCPConfig config = new BoneCPConfig();
	    String connStr = "jdbc:mysql://" + System.getenv("DB_HOST")
		+ ":" + System.getenv("DB_PORT") + "/" + System.getenv("DB_NAME");
	    config.setJdbcUrl(connStr);
	    config.setUsername(System.getenv("DB_USER"));
	    config.setPassword(System.getenv("DB_PASS"));
	    config.setMinConnectionsPerPartition(5);
	    config.setMaxConnectionsPerPartition(10);
	    config.setAcquireIncrement(2);
	    config.setPartitionCount(1);
	    connectionPool = new BoneCP(config); // setup the connection pool
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public static Connection getConnection() throws Exception {
	if (connectionPool == null) return null;
	return connectionPool.getConnection();
    }

    //	public static void releaseConnection(Connection connection) {
    //		if (connection == null) return ;
    //		connectionPool.releaseConnection(connection);
    //	}

}
