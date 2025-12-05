package com.billy.app;

import com.billy.api.CustomerHandler;
import com.billy.api.CustomerRouter;
import com.billy.api.ItemHandler;
import com.billy.api.ItemRouter;
import com.billy.database.CustomerDAO;
import com.billy.database.ItemDAO;
import com.billy.database.MapDbWrapper;
import com.billy.service.CustomerService;
import com.billy.service.ItemService;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.SetHeaderHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.billy.app.AppConstants.APPLICATION_JSON;
import static com.billy.app.AppConstants.COMMIT_SCHEDULER;
import static com.billy.app.AppConstants.CONTENT_TYPE;
import static com.billy.app.AppConstants.DB_FILE_NAME;
import static com.billy.app.AppConstants.DELAY;
import static com.billy.app.AppConstants.PERIOD;
import static com.billy.app.AppConstants.SERVER_HOST;
import static com.billy.app.AppConstants.SERVER_PORT;
import static com.billy.app.AppConstants.WORKER_THREADS;


public class AppContext {
    private static final Logger logger = Logger.getLogger(AppContext.class.getName());

    // Configuration and DB
    private final Config config;
    private final MapDbWrapper dbWrapper;

    // Persistence layer
    private final CustomerDAO customerDAO;
    private final ItemDAO itemDAO;

    // Services
    private final CustomerService customerService;
    private final ItemService itemService;

    // HTTP handlers
    private final CustomerHandler customerHandler;
    private final ItemHandler itemHandler;

    // Routing and scheduling
    private final RoutingHandler routingHandler;
    private final ScheduledExecutorService scheduler;

    private Undertow server;

    /**
     * Constructs the application context and wires all dependencies.
     *
     * @param config application configuration source
     */
    public AppContext(Config config) {
        this.config = config;
        String dbFileName = config.get(DB_FILE_NAME);
        if (dbFileName == null || dbFileName.isBlank()) {
            throw new IllegalArgumentException("DB file name is missing in configuration");
        }
        this.dbWrapper = new MapDbWrapper(dbFileName);
        this.customerDAO = new CustomerDAO(dbWrapper);
        this.itemDAO = new ItemDAO(dbWrapper);
        this.customerService = new CustomerService(customerDAO);
        this.itemService = new ItemService(itemDAO);
        this.customerHandler = new CustomerHandler(customerService);
        this.itemHandler = new ItemHandler(itemService);
        this.routingHandler = Handlers.routing();
        CustomerRouter.register(routingHandler, customerHandler);
        ItemRouter.register(routingHandler, itemHandler);

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName(COMMIT_SCHEDULER);
            return t;
        });
    }

    /**
     * Starts Undertow and the scheduled MapDB commit task.
     */
    public void start() {

        HttpHandler handler =
                new SetHeaderHandler(routingHandler, CONTENT_TYPE, APPLICATION_JSON);
        String host = config.get(SERVER_HOST);
        int port = config.getInt(SERVER_PORT);
        this.server = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(Handlers.path().addPrefixPath("/", handler))
                .setIoThreads(Runtime.getRuntime().availableProcessors())
                .setWorkerThreads(WORKER_THREADS)
                .build();
        server.start();
        logger.log(Level.INFO, String.format("Undertow started at http://%s:%d%n", host, port));
        scheduler.scheduleAtFixedRate(() -> {
            try {
                dbWrapper.commit();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Commit failed", e);
            }
        }, DELAY, PERIOD, TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    /**
     * Stops the server, shuts down the scheduler, and safely commits/closes the DB.
     */
    public void shutdown() {
        try {
            if (server != null) {
                server.stop();
            }

            scheduler.shutdownNow();
            if (!dbWrapper.db().isClosed()) {
                dbWrapper.commit();
                dbWrapper.close();
            }

            logger.log(Level.INFO, "MapDB committed and closed. Server stopped.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Shutdown error", e);
        }
    }

}



