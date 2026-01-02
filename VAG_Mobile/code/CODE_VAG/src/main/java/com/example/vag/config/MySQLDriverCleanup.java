package com.example.vag.config;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

@Component
public class MySQLDriverCleanup implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Не нужно ничего делать при старте
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Deregister MySQL JDBC drivers
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getName().contains("mysql")) {
                try {
                    DriverManager.deregisterDriver(driver);
                    System.out.println("MySQL JDBC Driver deregistered successfully");
                } catch (SQLException e) {
                    System.err.println("Error deregistering MySQL JDBC Driver: " + e.getMessage());
                }
            }
        }

        // Stop the abandoned connection cleanup thread
        try {
            AbandonedConnectionCleanupThread.checkedShutdown();
            System.out.println("MySQL AbandonedConnectionCleanupThread stopped successfully");
        } catch (Exception e) {
            System.err.println("Error stopping MySQL AbandonedConnectionCleanupThread: " + e.getMessage());
        }
    }
}