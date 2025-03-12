package org.nmssever.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import org.nmssever.database.DatabaseClient;
import org.nmssever.util.Constants;

/**
 * The type Polling verticle.
 */
public class PollingVerticle extends AbstractVerticle {

    private final Logger LOGGER = LoggerFactory.getLogger(PollingVerticle.class);

    private final Pool pool;

    private final JsonObject responseObject = new JsonObject();

    /**
     * Instantiates a new Polling verticle.
     *
     * @param databaseClient the database client
     */
    public PollingVerticle(DatabaseClient databaseClient) {

        this.pool = databaseClient.getPool();

    }

    @Override
    public void start(Promise<Void> startPromise) {

        vertx.setPeriodic(120000, id -> fetchProvisionedProfiles(startPromise));

    }

    private void fetchProvisionedProfiles(Promise<Void> startPromise) {

        LOGGER.info("Fetching provisioned profiles...");

        var sql = """
                    SELECT dp.ip, dp.id, dp.credential_profile_id, cp.credentialconfig,cp.system_type
                    FROM discoveryprofiles dp
                    JOIN credentialprofiles cp ON dp.credential_profile_id = cp.id
                    WHERE dp.provision_status = 1
                """;

        pool.preparedQuery(sql).execute(ar -> {

            if (ar.failed()) {

                LOGGER.error("Failed to execute query: " + sql, ar.cause());

                return;

            }

            var rows = ar.result();

            if (rows.size() == 0) {

                LOGGER.info("No provisioned profiles found.");

                return;

            }

            rows.forEach(row -> {

                var discoveryProfileID = row.getInteger("id");

                var ip = row.getString("ip");

                var systemtype = row.getString("system_type");

                JsonObject credentialConfig;

                try {

                    credentialConfig = new JsonObject(row.getString("credentialconfig"));

                } catch (Exception e) {

                    LOGGER.error("Invalid JSON in credential_config for profile: " + discoveryProfileID, e);

                    return;

                }

                var username = credentialConfig.getString("username");

                var password = credentialConfig.getString("password");

                var responseObject = new JsonObject().put("RequestType", "provisioning").put("ip", ip).put("username", username).put("password", password).put("SystemType", systemtype).put("discovery_profile_id", discoveryProfileID);

                LOGGER.info("Sending ZMQ request: " + responseObject.encodePrettily());

                vertx.eventBus().request(Constants.ZMQ_POLLING_REQUEST, responseObject, new DeliveryOptions().setSendTimeout(120000), zmqResponse -> {

                    if (zmqResponse.failed()) {

                        LOGGER.error("ZMQ request failed for profile: " + discoveryProfileID, zmqResponse.cause());

                    }

                });

            });

        });

    }


    @Override
    public void stop(Promise<Void> stopPromise) {

        stopPromise.complete();

    }

}


