package me.armar.plugins.autorank.mysql.wrapper;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.*;

import me.armar.plugins.autorank.Autorank;
import me.armar.plugins.autorank.config.ConfigHandler;
import me.armar.plugins.autorank.config.ConfigHandler.MySQLOptions;
import me.armar.plugins.autorank.data.SQLDataStorage;
import me.armar.plugins.autorank.playtimes.Playtimes;

/**
 * This class keeps all incoming and outgoing under control.
 * It sends MySQL queries and can locate the database.
 * Previously, {@link me.armar.plugins.autorank.playtimes.Playtimes} kept all
 * MySQL, but it wasn't neatly organised.
 * MySQLWrapper class is (hopefully) fail-prove and organised.
 * 
 * This also has a fail-safe when two queries are altering at the same time.
 * 
 * @author Staartvin
 * 
 */
public class MySQLWrapper {

	String hostname, username, password, database, table;
	// Keeps track of when a call to the database was for this player
	private final HashMap<UUID, Long> lastChecked = new HashMap<UUID, Long>();
	// Stores the last received global time for a player
	private final HashMap<UUID, Integer> lastReceivedTime = new HashMap<UUID, Integer>();
	// Thread pool for saving and retrieving data.
	private ExecutorService executor = Executors.newFixedThreadPool(1);

	private SQLDataStorage mysql;
	private final Autorank plugin;

	public MySQLWrapper(final Autorank instance) {
		plugin = instance;

		sqlSetup();

		if (mysql != null) {
			setupTable();
		}
	}

	/**
	 * Get the cached value of the global time.
	 * 
	 * @param uuid UUID to get the time for
	 * @return cached global time or -1 if nothing was cached.
	 */
	public Integer getCachedGlobalTime(final UUID uuid) {
		if (!lastReceivedTime.containsKey(uuid)) {
			return -1;
		}

		final int cached = lastReceivedTime.get(uuid);

		// Weird cached
		if (cached <= 0) {
			return -1;
		}

		return cached;
	}

	public String getDatabaseName() {
		return database;
	}

	/**
	 * Gets the database time of player <br>
	 * Run this ASYNC, because it will block the thread it's on.
	 * <p>
	 * This will return an updated value every 5 minutes. Calling it every
	 * minute isn't smart, as it will only update every 5 minutes.
	 * 
	 * @param uuid UUID to get the time of
	 * @return time player has played across all servers
	 */
	public int getDatabaseTime(final UUID uuid) {

		// Do not make a call to the database every time.
		// Instead, only call once every 5 minutes.
		if (!isOutOfDate(uuid)) {
			return getCachedGlobalTime(uuid);
		}

		// Mysql is not enabled
		if (!isMySQLEnabled())
			return -1;

		// Check if connection is still alive
		if (mysql.isClosed()) {
			mysql.connect();
		}

		// Retrieve database time

		// Initialise new callable class
		final Callable<Integer> callable = new GrabDatabaseTimeTask(mysql,
				uuid, table);

		// Sumbit callable
		final Future<Integer> futureValue = executor.submit(callable);

		// Grab value (will block thread, but there is no other way)
		// That's why you need to run this async.
		int value = -1;

		try {
			value = futureValue.get();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		} catch (final ExecutionException e) {
			e.printStackTrace();
		}

		// Store last received time and last received value
		lastChecked.put(uuid, System.currentTimeMillis());
		lastReceivedTime.put(uuid, value);

		return value;
	}

	/**
	 * Get the database time of a certain UUID.
	 * <p>
	 * This will always return the result that is currently in the database and
	 * is never a cached value.
	 * <p>
	 * A new request will always be made to get the value, therefor this should
	 * be run async.
	 * 
	 * @param uuid UUID of the player to get the time for.
	 * @return fresh value of database time for UUID.
	 */
	public int getFreshDatabaseTime(final UUID uuid) {
		// Mysql is not enabled
		if (!isMySQLEnabled())
			return -1;

		// Check if connection is still alive
		if (mysql.isClosed()) {
			mysql.connect();
		}

		// Retrieve database time

		// Initialise new callable class
		final Callable<Integer> callable = new GrabDatabaseTimeTask(mysql,
				uuid, table);

		// Sumbit callable
		final Future<Integer> futureValue = executor.submit(callable);

		// Grab value (will block thread, but there is no other way)
		// That's why you need to run this async.
		int value = -1;

		try {
			value = futureValue.get();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		} catch (final ExecutionException e) {
			e.printStackTrace();
		}

		// Store last received time and last received value
		lastChecked.put(uuid, System.currentTimeMillis());
		lastReceivedTime.put(uuid, value);

		return value;
	}

	public boolean isMySQLEnabled() {
		return mysql != null;
	}

	public boolean isOutOfDate(final UUID uuid) {
		// Checks whether the last check was five minutes ago.
		// When the last check was more than five minutes ago,
		// the database time is 'outdated'

		// Never checked
		if (!lastChecked.containsKey(uuid)) {
			return true;
		}

		final long currentTime = System.currentTimeMillis();

		final long lastCheckedTime = lastChecked.get(uuid);

		// Weird time received.
		if (lastCheckedTime <= 0) {
			return true;
		}

		// Get the difference in minutes
		if ((currentTime - lastCheckedTime) / 60000 >= Playtimes.INTERVAL_MINUTES) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Sets the time of a player
	 * 
	 * @param uuid UUID to set the time of
	 * @param time Time to change to
	 */
	public void setGlobalTime(final UUID uuid, final int time) {

		if (!isMySQLEnabled())
			return;

		// Check if connection is still alive
		if (mysql.isClosed()) {
			mysql.connect();
		}

		final String statement = "INSERT INTO " + table + " VALUES ('"
				+ uuid.toString() + "', " + time + ", CURRENT_TIMESTAMP) "
				+ "ON DUPLICATE KEY UPDATE " + "time=" + time;

		// Run async to prevent load issues.
		plugin.getServer().getScheduler()
				.runTaskAsynchronously(plugin, new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						mysql.execute(statement);
					}
				});

		// Update cache records
		this.lastChecked.put(uuid, System.currentTimeMillis());
		this.lastReceivedTime.put(uuid, time);
	}

	public void setupTable() {
		// Check if connection is still alive
		if (mysql.isClosed()) {
			mysql.connect();
		}

		final String statement = "CREATE TABLE  IF NOT EXISTS " + table + " "
				+ "(uuid VARCHAR(255) not NULL, " + " time INTEGER not NULL, "
				+ " modified TIMESTAMP not NULL, " + " PRIMARY KEY ( uuid ))";

		// Run async to prevent load issues.
		plugin.getServer().getScheduler()
				.runTaskAsynchronously(plugin, new Runnable() {

					@Override
					public void run() {
						mysql.execute(statement);
					}
				});

	}

	public void sqlSetup() {

		final ConfigHandler configHandler = plugin.getConfigHandler();
		/*final ConfigurationSection s = config.getConfigurationSection("sql");

		if (s == null) {
			plugin.getLogger().warning(
					"MySQL options are missing in the advancedconfig.yml!");
			return;
		}*/

		if (configHandler.useMySQL()) {

			hostname = configHandler.getMySQLSettings(MySQLOptions.HOSTNAME);
			username = configHandler.getMySQLSettings(MySQLOptions.USERNAME);
			password = configHandler.getMySQLSettings(MySQLOptions.PASSWORD);
			database = configHandler.getMySQLSettings(MySQLOptions.DATABASE);
			table = configHandler.getMySQLSettings(MySQLOptions.TABLE);

			mysql = new SQLDataStorage(hostname, username, password, database);
			if (!mysql.connect()) {
				mysql = null;
				plugin.getLogger().severe("Could not connect to " + hostname);
			} else {
				plugin.getLogger().info(
						"Successfully established connection to " + hostname);
			}
		}
	}

	/**
	 * Disconnect from database manually.
	 */
	public void disconnectDatabase() {
		executor.shutdown();
		plugin.getLogger().info("Awaiting termination of MySQL thread...");
		try {
			executor.awaitTermination(10, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
			plugin.getLogger().warning("Failed to await termination of thread pool. Interrupted.");
		}

		if (mysql != null) {
			mysql.closeConnection();
		}
	}
}
