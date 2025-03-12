package org.nmssever.services;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.nmssever.util.Constants;
import org.zeromq.ZMQ;

import java.util.HashMap;
import java.util.Map;

/**
 * The type Zmq service.
 */
public class ZmqService {
    private final Vertx vertx;

    private final ZMQ.Context context;

    private final ZMQ.Socket socket;

    private static final String BIND_ADDRESS = Constants.ZMQ_BIND_ADDRESS;

    private final Map<String, Message<JsonObject>> routingTable = new HashMap<>();

    private final Logger Logger = LoggerFactory.getLogger(ZmqService.class);

    /**
     * Instantiates a new Zmq service.
     *
     * @param vertx the vertx
     */
    public ZmqService(Vertx vertx) {

        this.vertx = vertx;

        Logger.info("Initializing ZMQ context and socket");

        context = ZMQ.context(1);

        if (context == null) {

            Logger.warn("Failed to initialize ZMQ context");

            socket = null;

            return;

        }

        socket = context.socket(ZMQ.DEALER);

        if (socket == null) {

            Logger.warn("Failed to create ZMQ socket");

            return;

        }

        socket.connect(BIND_ADDRESS);

        Logger.info("ZMQ socket connected to: " + BIND_ADDRESS);

        vertx.setPeriodic(100, id -> checkResponse());

        Logger.info("Scheduled periodic check for responses");

    }

    /**
     * Send.
     *
     * @param requestId the request id
     * @param message   the message
     */
    public void send(String requestId, Message<JsonObject> message) {

        var response = new JsonObject();

        Logger.info("Sending message with requestId: " + requestId);

        try {

            routingTable.put(requestId, message);

            var sent = socket.sendMore(requestId) &&
                    socket.sendMore("") &&
                    socket.send(message.body().toString());

            if (sent) {

                Logger.info("Message sent successfully for requestId: " + requestId);

                if ("provisioning".equalsIgnoreCase(message.body().getString("RequestType"))) {

                    message.reply(response.put("result", "ZMQ request sent successfully").put("status", "success"));

                }

            } else {

                Logger.error("Failed to send message for requestId: " + requestId);

                if ("provisioning".equalsIgnoreCase(message.body().getString("RequestType"))) {

                    message.reply(response.put("error", "Failed to send ZMQ request").put("status", "fail"));

                }

            }

        } catch (Exception e) {

            Logger.error("Error while sending message for requestId: " + requestId, e);

            if ("provisioning".equalsIgnoreCase(message.body().getString("RequestType"))) {

                message.reply(response.put("error", "Error occurred: " + e.getMessage()).put("status", "fail"));

            }

        }

    }


    private void checkResponse() {

        String clientID;

        while ((clientID = socket.recvStr(ZMQ.DONTWAIT)) != null) {

            var message = routingTable.get(clientID);

            socket.recvStr();

            var responseMessage = socket.recvStr();

            try {

                var cleanedResponse = Json.decodeValue(responseMessage, String.class);

                var response = new JsonObject(cleanedResponse);

                if (message != null) {

                    var requestType = response.getString("RequestType");

                    if ("provisioning".equalsIgnoreCase(requestType)) {

                        handleProvisioningResponse(clientID, response);

                        Logger.info("Provisioning response processed for clientID: " + clientID);

                    } else if ("discovery".equalsIgnoreCase(requestType)) {

                        message.reply(response);

                        Logger.info("Discovery response replied for clientID: " + clientID);

                    }

                    routingTable.remove(clientID);

                }

            } catch (Exception e) {

                Logger.error("Error processing response for clientID: " + clientID, e);

            }

        }

    }

    private void handleProvisioningResponse(String clientID, JsonObject responseJson) {

        Logger.info("Handling provisioning response for clientID: " + clientID);

        var status = responseJson.getString("status");

        var discovery_profile_id = responseJson.getInteger("discovery_profile_id");

        var Request = new JsonObject().put("clientID", clientID).put("discovery_profile_id", discovery_profile_id);

        if ("success".equalsIgnoreCase(status)) {

            var result = responseJson.getJsonObject("result", new JsonObject());

            Request.put("data", result);

            vertx.eventBus().send(Constants.ZMQ_POLLED_DATA, Request);

        } else {

            var errors = responseJson.getJsonObject("errors", new JsonObject());

            Request.put("error", errors);

            vertx.eventBus().send(Constants.ZMQ_POLLED_DATA, Request);

        }
    }


    /**
     * Close.
     */
    public void close() {

        Logger.info("Closing ZMQ socket and terminating context");

        socket.close();

        context.term();

        Logger.info("Socket closed and context terminated successfully");

    }

}
