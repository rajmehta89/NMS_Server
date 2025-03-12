package org.nmssever;

import io.vertx.core.*;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import org.nmssever.database.DatabaseClient;
import org.nmssever.verticles.DatabaseVerticle;
import org.nmssever.verticles.HttpServerVerticle;
import org.nmssever.verticles.PollingVerticle;
import org.nmssever.verticles.ZmqVerticle;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        Vertx vertx = Vertx.vertx();

        logger.info("Starting application...");

        DatabaseClient databaseClient = new DatabaseClient(vertx);

        vertx.deployVerticle(new HttpServerVerticle()).compose(httpRes -> {

            logger.info("HttpServerVerticle deployed successfully!");

            return vertx.deployVerticle(new DatabaseVerticle(databaseClient));

        }).compose(dbRes -> {

            logger.info("DatabaseVerticle deployed successfully!");

            return vertx.deployVerticle(new ZmqVerticle());

        }).compose(zmqRes -> {
            logger.info("ZmqVerticle deployed successfully!");

            return vertx.deployVerticle(new PollingVerticle(databaseClient));

        }).onSuccess(pollingRes -> {

            logger.info("PollingVerticle deployed successfully!");

            logger.info("All verticles deployed successfully!");

        }).onFailure(err -> {

            logger.error("Failed to deploy verticles: " + err.getMessage());

            vertx.close();

        });

    }
}
