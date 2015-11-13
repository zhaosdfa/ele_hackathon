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
		connectionPool = null;
		Connection connection = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			BoneCPConfig config = new BoneCPConfig();
			config.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/eleme");
			config.setUsername("root");
			config.setPassword("toor");
			config.setMinConnectionsPerPartition(5);
			config.setMaxConnectionsPerPartition(10);
			config.setPartitionCount(1);
			connectionPool = new BoneCP(config); // setup the connection pool
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static Connection getConnection() throws Exception {
		if (connectionPool == null) return null;
		return connectionPool.getConnection();
	}

}
