package cl.camodev.wosbot.main;

import cl.camodev.wosbot.almac.jpa.BotPersistence;
import cl.camodev.wosbot.web.server.WebDashboardServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cl.camodev.wosbot.logging.ProfileLogger;

public class Main {
	private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final long programStart = System.currentTimeMillis();
	public static void main(String[] args) {
		try {
            // Silence logback's internal status messages
			System.setProperty("logback.statusListenerClass", "ch.qos.logback.core.status.NopStatusListener");

            suppressJavaFXWarnings();

            // Initialize Log4j configuration
            logger.info("Starting Log4j initialization at {} ms", System.currentTimeMillis() - programStart);
            long log4jInitStart = System.currentTimeMillis();
            configureLog4j();
            long log4jInitEnd = System.currentTimeMillis();
            logger.info("Log4j initialization completed in " + (log4jInitEnd - log4jInitStart) + "ms");

			logger.info("Starting WosBot application");
			logger.info("Logging configured. Check target/log/bot.log for detailed logs.");
			logger.info("Profile-specific logs will be created in target/log/profile_*.log files");

            // Start initializing the database
            Thread.ofVirtual().start(Main::initializeDatabase).setName("DatabaseInitializer");

            // Start the log web server
            Thread.ofVirtual().start(Main::startLogWebServer).setName("LogWebServerInitializer");

            // Add shutdown hook to close log files and web server
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Application shutting down, closing log files...");
                ProfileLogger.closeAllLogWriters();
                WebDashboardServer.getInstance().stop();
            }));

			// Launch JavaFX application
            logger.info("Starting JavaFX loading at {} ms", System.currentTimeMillis() - programStart);
			FXApp.main(args);

		} catch (Exception e) {
			logger.error("Failed to start application: " + e.getMessage(), e);
			ProfileLogger.closeAllLogWriters();
			System.exit(1);
		}
	}

	/**
	 * Configure Log4j programmatically
	 */
	private static void configureLog4j() {
		try {
			// Configure java.util.logging to suppress JavaFX warnings directly
			java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
			rootLogger.setLevel(java.util.logging.Level.WARNING);
			
			// Set specific loggers to SEVERE level
			java.util.logging.Logger.getLogger("javafx").setLevel(java.util.logging.Level.SEVERE);
			java.util.logging.Logger.getLogger("com.sun.javafx").setLevel(java.util.logging.Level.SEVERE);
			java.util.logging.Logger.getLogger("javax.swing").setLevel(java.util.logging.Level.SEVERE);
			
			logger.info("Log4j configuration loaded successfully");
		} catch (Exception e) {
			System.err.println("Failed to configure Log4j: " + e.getMessage());
			e.printStackTrace();
		}
	}
    private static void suppressJavaFXWarnings() {
        // Configure java.util.logging to suppress JavaFX warnings
        java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
        rootLogger.setLevel(java.util.logging.Level.WARNING);

        // Set specific loggers to SEVERE level
        java.util.logging.Logger.getLogger("javafx").setLevel(java.util.logging.Level.SEVERE);
        java.util.logging.Logger.getLogger("com.sun.javafx").setLevel(java.util.logging.Level.SEVERE);
        java.util.logging.Logger.getLogger("javax.swing").setLevel(java.util.logging.Level.SEVERE);

        logger.info("JavaFX warnings suppressed");
    }

    private static void initializeDatabase() {
        long initDbStart = System.currentTimeMillis();
        logger.info("Starting to initialize database at {} ms", initDbStart - programStart);
        BotPersistence.getInstance();
        logger.info("Finished initializing database, took {} ms", System.currentTimeMillis() - initDbStart);

    }

	/**
	 * Starts the web dashboard server for real-time log viewing and bot control
	 */
	private static void startLogWebServer() {
        try {
            logger.info("Starting web dashboard server at {} ms", System.currentTimeMillis() - programStart);
            long webServerInitStart = System.currentTimeMillis();
            WebDashboardServer webDashboardServer = WebDashboardServer.getInstance();
            webDashboardServer.start(); // Starts on default port 8080
            long webServerInitEnd = System.currentTimeMillis();
            logger.info("Web dashboard server started in " + (webServerInitEnd - webServerInitStart) + "ms");
        } catch (Exception e) {
            logger.error("Failed to start web dashboard server: " + e.getMessage(), e);
            // Don't fail the application if the web server can't start
        }
	}
}
