package org.nmssever.database;

import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

/**
 * The type Database client.
 */
public class DatabaseClient {

    private final Pool pool;

    private static final Logger Logger = LoggerFactory.getLogger(DatabaseClient.class);

    /**
     * Instantiates a new Database client.
     *
     * @param vertx the vertx
     */
    public DatabaseClient(Vertx vertx) {

        Logger.info("Initializing DatabaseClient");

        Dotenv dotenv = Dotenv.load();

        PgConnectOptions connectOptions = new PgConnectOptions()
                .setHost("localhost")
                .setPort(5432)
                .setDatabase("network_monitoring")
                .setUser("username")
                .setPassword("secure_password")
                .setReconnectAttempts(5)
                .setReconnectInterval(2000);

        Logger.info("PostgreSQL connection options configured");

        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(10)
                .setMaxWaitQueueSize(50)
                .setIdleTimeout(30000);

        Logger.info("Database connection pool options configured");

        this.pool = PgPool.pool(vertx, connectOptions, poolOptions);

        Logger.info("Database pool initialized successfully");

        createTables();

    }

    /**
     * Gets pool.
     *
     * @return the pool
     */
    public Pool getPool() {

        return pool;

    }

    /**
     * Close.
     */
    public void close() {

        Logger.info("Closing database connection pool");

        pool.close();

    }

    private void createTables() {

        var sql = """
                CREATE TABLE IF NOT EXISTS CredentialProfiles (
                    id SERIAL PRIMARY KEY,
                    credential_profile_name VARCHAR(255) NOT NULL UNIQUE,
                    system_type VARCHAR(100) NOT NULL,  -- Added system_type column
                    CredentialConfig JSONB NOT NULL
                );
                
                CREATE TABLE IF NOT EXISTS DiscoveryProfiles (
                    id SERIAL PRIMARY KEY,
                    discovery_profile_name VARCHAR(255) NOT NULL UNIQUE,
                    credential_profile_id INT NOT NULL,
                     ip VARCHAR(45) NOT NULL,  -- Changed from INET to VARCHAR(45)
                     provision_status INT NOT NULL DEFAULT 0,
                    discovery_status INT NOT NULL DEFAULT 0,
                    FOREIGN KEY (credential_profile_id) REFERENCES CredentialProfiles(id) ON DELETE RESTRICT
                );
                
                CREATE TABLE IF NOT EXISTS SystemData (
                    id SERIAL PRIMARY KEY,
                    discovery_profile_id INT NOT NULL,
                    system_info JSONB NOT NULL
                );
                """;

        pool.getConnection(ar -> {

            if (ar.succeeded()) {

                var connection = ar.result();

                connection.query(sql).execute(res -> {

                    if (res.succeeded()) {

                        Logger.info("Tables ensured successfully");

                    } else {

                        Logger.error("Failed to ensure tables", res.cause());

                    }

                    connection.close();

                });

            } else {

                Logger.error("Failed to obtain database connection", ar.cause());

            }

        });

    }

}
