package edu.ezip.commons.connectionpool.config.impl;

import edu.ezip.commons.connectionpool.config.DatabaseConnectionBasicConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class ConnectionPoolImpl {

    private final static String jdbc = "jdbc";
    private final static String LoggingLabel = "C o n n - p o o l";



    // Singleton Pattern
    public static ConnectionPoolImpl inst = null;


    private final Logger logger = LoggerFactory.getLogger(LoggingLabel);
    private static String url;
    private  static String databaseURL;



    // LinkedBlockingDeque = Multithreading Safe take() / put()
    private static final BlockingDeque<Connection> connections = new LinkedBlockingDeque<Connection>(
                                                                DatabaseConnectionBasicConfiguration.getInstance().
                                                                        getPoolSize());


    public static ConnectionPoolImpl getInstance(final String dbEditor) throws SQLException, UnsupportedEncodingException {
        if(inst == null) {
            inst = new ConnectionPoolImpl(dbEditor);
        }
        return inst;
    }

    // JDBC URL Construction
    private ConnectionPoolImpl(final String dbEditor) throws SQLException, UnsupportedEncodingException {
        final DatabaseConnectionBasicConfiguration config =  DatabaseConnectionBasicConfiguration.getInstance();
        final StringBuffer letsBuildUrl = new StringBuffer();
        letsBuildUrl.append(jdbc).append(":")
                .append(dbEditor).append("://")
                .append(config.getHost()).append(":")
                .append(config.getPort()).append("/")
                .append(config.getDatabaseName()).append("?")
                .append("user=").append(config.getUsername()).append("&")
                .append("password=").append(URLEncoder.encode(config.getPassword(), StandardCharsets.UTF_8.toString())).append("&")
                .append("ssl=false");
        logger.debug("URL = {}", (this.url = letsBuildUrl.toString()));

        initConnections();
        showConnections();
        // terminatePool();
    }


    private final Connection createConnection() throws SQLException {
        return DriverManager.getConnection(this.url);
    }

    private void initConnections() throws SQLException {
        int i = 0;
        while ( 0 < connections.remainingCapacity()) {
            connections.addLast(createConnection());
            i++;
        }
        logger.debug("{} created, pool size = {}", i, DatabaseConnectionBasicConfiguration.
                getInstance().getPoolSize());
    }

    public void terminatePool() throws SQLException {
        int i = 0;
        while ( !connections.isEmpty()) {
            final Connection c = connections.pollFirst();
            i++;
            if ( null != c) c.close();
        }
        logger.debug("{} released, pool size = {}", i, DatabaseConnectionBasicConfiguration.
                getInstance().getPoolSize());
    }



    private void showConnections() {
        int i = 0;
        final StringBuffer toShowConnections = new StringBuffer();
        toShowConnections.append("{");
        final Iterator<Connection> trtr = connections.iterator();
        while (trtr.hasNext()) {
            if (i++ > 0) toShowConnections.append(" ★ ");
            final String toStringConnection = trtr.next().toString();
            toShowConnections.append(toStringConnection.replace("org.postgresql.jdbc.", ""));
        }
        toShowConnections.append("}");
        logger.debug("Connections = {}", toShowConnections.toString());
    }

    public int available() {
        return connections.size();
    }
    public final Connection get() {
        return connections.pollFirst();
    }
    public void release(Connection connection) throws InterruptedException {connections.offerLast(connection);}
    private void closeConnection(final Connection c) throws SQLException {c.close();}
}
