package com.servlets;

import com.dao.AppDao;
import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            new AppDao().initialize();
        } catch (Exception exception) {
            event.getServletContext().log("Database initialization failed", exception);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        try {
            AbandonedConnectionCleanupThread.checkedShutdown();
        } catch (Exception exception) {
            event.getServletContext().log(
                    "Unable to stop the MySQL cleanup thread",
                    exception);
        }

        ClassLoader applicationClassLoader = getClass().getClassLoader();
        Enumeration<Driver> drivers = DriverManager.getDrivers();

        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getClassLoader() != applicationClassLoader) {
                continue;
            }

            try {
                DriverManager.deregisterDriver(driver);
            } catch (Exception exception) {
                event.getServletContext().log(
                        "Unable to unregister JDBC driver " + driver,
                        exception);
            }
        }
    }
}
