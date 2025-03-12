package org.nmssever.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import org.nmssever.handlers.ZmqHandler;
import org.nmssever.services.ZmqService;

/**
 * The type Zmq verticle.
 */
public class ZmqVerticle extends AbstractVerticle {

    private ZmqService zmqService;

    private ZmqHandler zmqHandler;

    private final Logger LOGGER = LoggerFactory.getLogger(ZmqVerticle.class);

    @Override
    public void start(Promise<Void> startPromise) {

        zmqService = new ZmqService(vertx);

        zmqHandler = new ZmqHandler(vertx, zmqService);

        zmqHandler.registerHandlers();

        LOGGER.info("ZmqHandler registered successfully.");

        startPromise.complete();

    }

    @Override
    public void stop() {

        LOGGER.info("Stopping ZmqVerticle...");

        if (zmqService != null) {

            zmqService.close();

            LOGGER.info("ZmqService closed successfully.");

        }

        LOGGER.info("ZmqVerticle stopped.");

    }

}
