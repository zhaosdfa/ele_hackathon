package com.sunsky.server;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
 
import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

public class DBHelper {

    public static BoneCP connectionPool;

    public static void main(String[] args) {
        connectionPool = null;
        Connection connection = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception e) {
            e.printStackTrace();
            return ;
        }

        try {
            BoneCPConfig config = new BoneCPConfig();
            config.setJdbcUrl("jdbc:mysql://127.0.0.1:4306/eleme");
            config.setUsername("root");
            config.setPassword("toor");
            config.setMinConnectionsPerPartition(5);
			config.setMaxConnectionsPerPartition(10);
			config.setPartitionCount(1);
			connectionPool = new BoneCP(config); // setup the connection pool
			
			connection = connectionPool.getConnection(); // fetch a connection
			
			if (connection != null) {
				System.out.println("Connection successful!");
				Statement stmt = connection.createStatement();
                String sql = "select * from food limit 0, 10";
				ResultSet rs = stmt.executeQuery(sql); // do something with the connection.
				while(rs.next()) {
					System.out.println(rs.getString(1)); // should print out "1"'
				}
			}
            connectionPool.shutdown(); // shutdown connection pool.
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
