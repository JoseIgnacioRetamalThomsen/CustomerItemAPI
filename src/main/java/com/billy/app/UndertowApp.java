package com.billy.app;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.billy.app.AppConstants.CONFIG_PROPERTIES;

public class UndertowApp {

    private static final Logger logger = Logger.getLogger(UndertowApp.class.getName());

    public static void main(String[] args) {
        try {
            logger.info("Starting Undertow application.");
            Config config = new Config(CONFIG_PROPERTIES);
            AppContext ctx = new AppContext(config);
            ctx.start();
            logger.info("Undertow application started successfully.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to start Undertow application", e);
            System.exit(1);
        }
    }
}
