package org.nmssever.handlers;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import org.nmssever.services.ZmqService;
import org.nmssever.util.Constants;

import java.util.UUID;

/**
 * The type Zmq handler.
 */
public class ZmqHandler {

    private final Vertx vertx;

    private final ZmqService zmqService;

    private final Logger Logger = LoggerFactory.getLogger(ZmqHandler.class);

    /**
     * Instantiates a new Zmq handler.
     *
     * @param vertx      the vertx
     * @param zmqService the zmq service
     */
    public ZmqHandler(Vertx vertx, ZmqService zmqService) {

        this.vertx = vertx;

        this.zmqService = zmqService;

    }

    /**
     * Register handlers.
     */
    public void registerHandlers() {

        vertx.eventBus().consumer(Constants.ZMQ_DISCOVERY_RUN_REQUEST, this::handleDiscoveryRun);

        vertx.eventBus().consumer(Constants.ZMQ_POLLING_REQUEST, this::handlePollingRequest);

    }

    private void handleDiscoveryRun(Message<JsonObject> message) {

        Logger.info("Handling discovery run request");

        var requestId = UUID.randomUUID().toString();

        Logger.info("Generated request ID for discovery run: " + requestId);

        zmqService.send(requestId, message);

    }

    private void handlePollingRequest(Message<JsonObject> message) {

        Logger.info("Handling polling request");

        var requestId = UUID.randomUUID().toString();

        Logger.info("Generated request ID for polling request: " + requestId);

        zmqService.send(requestId, message);

    }

}
