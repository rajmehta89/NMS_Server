package org.nmssever.services;

import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import org.nmssever.util.Constants;
import org.nmssever.util.DBConstants;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import org.zeromq.ZMQ;
import java.util.HashMap;
import java.util.Map;




public class UnifiedProfileService {

    private final Pool pool;

    private final EventBus eventBus;

    private final ZMQ.Context context;

    private final ZMQ.Socket socket;

    private static final String BIND_ADDRESS = Constants.ZMQ_BIND_ADDRESS;

    private final Map<String, Message<JsonObject>> routingTable = new HashMap<>();

    private final Logger logger = LoggerFactory.getLogger(UnifiedProfileService.class);

    public UnifiedProfileService(Pool pool, EventBus eventBus) {

        this.pool = pool;

        this.eventBus = eventBus;

        this.context = ZMQ.context(1);

        this.socket = context.socket(ZMQ.DEALER);
        
        this.socket.connect(BIND_ADDRESS);

        logger.info("ZMQ socket connected to: " + BIND_ADDRESS);

    }

    

    private Future<JsonObject> executeUpdate(String sql, Tuple params) {
        return pool.preparedQuery(sql)
                .execute(params)
                .map(res -> {
                    if (res.rowCount() > 0) {
                        logger.info("Operation successful: " + sql);
                        return new JsonObject().put("status", "success");
                    } else {
                        logger.warn("Operation failed: No rows affected for SQL: " + sql);
                        return new JsonObject().put("status", "fail").put("error", "No rows affected");
                    }
                })
                .onFailure(err -> {
                    logger.error("Error executing SQL: " + sql, err);
                    return Future.failedFuture(new JsonObject().put("status", "fail").put("error", err.getMessage()));
                });
    }



    private Future<JsonArray> executeQuery(String sql, Tuple params) {
        return pool.preparedQuery(sql)
                .execute(params)
                .map(rows -> {
                    JsonArray responseArray = new JsonArray();
                    rows.forEach(row -> responseArray.add(rowToJson(row)));
                    logger.info("Retrieved " + responseArray.size() + " records for SQL: " + sql);
                    return responseArray;
                })
                .onFailure(err -> {
                    logger.error("Error executing query: " + sql, err);
                    return Future.failedFuture(new JsonObject().put("status", "fail").put("error", err.getMessage()));
                });
    }
     


    public Future<JsonObject> createCredentialProfile(JsonObject request) {
        String sql = "INSERT INTO CredentialProfiles (credential_profile_name, system_type, credentialconfig) VALUES ($1, $2, $3) RETURNING id";
        return pool.preparedQuery(sql)
                .execute(Tuple.of(
                        request.getString("credential_profile_name"),
                        request.getString("system_type"),
                        request.getJsonObject("credentialconfig").encode()
                ))
                .map(res -> {
                    if (res.rowCount() > 0) {
                        var generatedId = res.iterator().next().getInteger("id");
                        logger.info("Credential profile created with ID: " + generatedId);
                        return new JsonObject().put("status", "success").put("id", generatedId);
                    } else {
                        logger.error("Failed to create credential profile: No ID returned");
                        return new JsonObject().put("status", "fail").put("error", "No ID returned");
                    }
                })
                .onFailure(err -> {
                    logger.error("Error creating credential profile", err);
                    return Future.failedFuture(new JsonObject().put("status", "fail").put("error", err.getMessage()));
                });
    }


    public Future<JsonArray> getCredentialProfiles() {
        return executeQuery("SELECT * FROM CredentialProfiles", Tuple.tuple());
    }


    public Future<JsonObject> updateCredentialProfile(JsonObject request) {
        String sql = "UPDATE CredentialProfiles SET credential_profile_name = $1, system_type = $2, credentialconfig = $3 WHERE id = $4";
        return executeUpdate(sql, Tuple.of(
                request.getString("credential_profile_name"),
                request.getString("system_type"),
                request.getJsonObject("credentialconfig").encode(),
                request.getInteger("id")
        ));
    }
     


    public Future<JsonObject> deleteCredentialProfile(Integer id) {
        String sql = "DELETE FROM CredentialProfiles WHERE id = $1";
        return executeUpdate(sql, Tuple.of(id));
    }

    

    public Future<JsonObject> createDiscoveryProfile(JsonObject request) {
        String sql = "INSERT INTO DiscoveryProfiles (discovery_profile_name, ip, credential_profile_id) VALUES ($1, $2, $3) RETURNING id";
        return pool.preparedQuery(sql)
                .execute(Tuple.of(
                        request.getString("discovery_profile_name"),
                        request.getString("ip"),
                        request.getInteger("credential_profile_id")
                ))
                .map(res -> {
                    if (res.rowCount() > 0) {
                        var generatedId = res.iterator().next().getInteger("id");
                        logger.info("Discovery profile created with ID: " + generatedId);
                        return new JsonObject().put("status", "success").put("id", generatedId);
                    } else {
                        logger.error("Failed to create discovery profile: No ID returned");
                        return new JsonObject().put("status", "fail").put("error", "No ID returned");
                    }
                })
                .onFailure(err -> {
                    logger.error("Error creating discovery profile", err);
                    return Future.failedFuture(new JsonObject().put("status", "fail").put("error", err.getMessage()));
                });
    }

    public Future<JsonArray> getDiscoveryProfiles() {
        return executeQuery("SELECT * FROM DiscoveryProfiles", Tuple.tuple());
    }

    public Future<JsonObject> updateDiscoveryProfile(JsonObject request) {
        String sql = "UPDATE DiscoveryProfiles SET ip = $1, credential_profile_id = $2 WHERE id = $3";
        return executeUpdate(sql, Tuple.of(
                request.getString("ip"),
                request.getInteger("credential_profile_id"),
                request.getInteger("id")
        ));
    }

    public Future<JsonObject> deleteDiscoveryProfile(Integer id) {
        String sql = "DELETE FROM DiscoveryProfiles WHERE id = $1";
        return executeUpdate(sql, Tuple.of(id));
    }

    public Future<JsonObject> getDiscoveryProfile(Integer profileID) {
        return executeQuery("SELECT * FROM DiscoveryProfiles WHERE id = $1", Tuple.of(profileID))
                .map(rows -> rows.rowCount() > 0 ? rowToJson(rows.iterator().next()) : new JsonObject().put("error", "Discovery profile not found"));
    }

    public Future<JsonObject> updateDiscoveryStatus(Integer discoveryProfileID, Integer discoveryStatus) {
        String sql = "UPDATE " + DBConstants.TABLE_DISCOVERY_PROFILES + " SET " + DBConstants.COL_DISCOVERY_STATUS + " = $1 WHERE " + DBConstants.COL_ID + " = $2";
        return executeUpdate(sql, Tuple.of(discoveryStatus, discoveryProfileID));
    }

   
    public Future<JsonObject> getProvisionDeviceData(JsonObject request) {
        var discoveryProfileID = request.getInteger("discovery_profile_id");
        var selectSql = "SELECT discovery_profile_id, json_agg(system_info) AS system_info_array FROM systemdata WHERE discovery_profile_id = $1 GROUP BY discovery_profile_id;";

        return pool.preparedQuery(selectSql)
                .execute(Tuple.of(discoveryProfileID))
                .map(result -> {
                    if (result.rowCount() > 0) {
                        var row = result.iterator().next();
                        var systemInfoArray = row.getJsonArray("system_info_array");
                        return new JsonObject()
                                .put("success", "System info retrieved successfully")
                                .put("discovery_profile_id", discoveryProfileID)
                                .put("system_info", systemInfoArray);
                    } else {
                        return new JsonObject().put("error", "No system info found for discovery_profile_id: " + discoveryProfileID);
                    }
                })
                .onFailure(err -> new JsonObject().put("error", "Database query failed: " + err.getMessage()));
    }

    public Future<JsonObject> provisionDevice(JsonObject request) {
        var discoveryProfileID = request.getInteger("discovery_profile_id");
        var provisionStatus = request.getInteger("provision_status");
        var updateSql = "UPDATE " + DBConstants.TABLE_DISCOVERY_PROFILES + " SET " + DBConstants.COL_PROVISION_STATUS + " = $1 WHERE id = $2 AND discovery_status = 1 RETURNING id;";

        return pool.preparedQuery(updateSql)
                .execute(Tuple.of(provisionStatus, discoveryProfileID))
                .map(rows -> rows.rowCount() > 0 ? new JsonObject().put("success", "Provision status updated successfully") : new JsonObject().put("error", "Update failed: No matching profile found with discovery_status = 1"))
                .onFailure(err -> new JsonObject().put("error", "Database update failed: " + err.getMessage()));
    }

    public Future<JsonArray> getProvisionedProfiles() {
        var sql = "SELECT * FROM " + DBConstants.TABLE_DISCOVERY_PROFILES + " WHERE " + DBConstants.COL_PROVISION_STATUS + " = true";

        return pool.preparedQuery(sql)
                .execute()
                .map(rows -> {
                    var profiles = new JsonArray();
                    rows.forEach(row -> profiles.add(rowToJson(row)));
                    return profiles;
                });
    }

    private JsonObject rowToJson(Row row) {
        return new JsonObject()
                .put(DBConstants.COL_DISCOVERY_PROFILE_NAME, row.getString(DBConstants.COL_DISCOVERY_PROFILE_NAME))
                .put(DBConstants.COL_IP, row.getString(DBConstants.COL_IP))
                .put(DBConstants.COL_CREDENTIAL_PROFILE_NAME, row.getString(DBConstants.COL_CREDENTIAL_PROFILE_NAME))
                .put("discovery_status", row.getBoolean(DBConstants.COL_DISCOVERY_STATUS))
                .put("provision_status", row.getBoolean(DBConstants.COL_PROVISION_STATUS));
    }

 
    public void send(String requestId, Message<JsonObject> message) {
        logger.info("Sending message with requestId: {}", requestId);

        var response = new JsonObject();

        try {
            routingTable.put(requestId, message);
            boolean sent = socket.sendMore(requestId) && socket.sendMore("") && socket.send(message.body().toString());

            if (sent) {
                logger.info("Message sent successfully for requestId: {}", requestId);
                handleProvisioningReply(message, response, "ZMQ request sent successfully");
            } else {
                logger.error("Failed to send message for requestId: {}", requestId);
                handleProvisioningReply(message, response, "Failed to send ZMQ request");
            }
        } catch (Exception e) {
            logger.error("Error while sending message for requestId: {}", requestId, e);
            handleProvisioningReply(message, response, "Error occurred: " + e.getMessage());
        }
    }

    private void checkResponse() {
        String clientID;
        while ((clientID = socket.recvStr(ZMQ.DONTWAIT)) != null) {
            var message = routingTable.get(clientID);
            socket.recvStr(); // Discard empty string
            var responseMessage = socket.recvStr();

            try {
                var cleanedResponse = Json.decodeValue(responseMessage, String.class);
                var response = new JsonObject(cleanedResponse);

                if (message != null) {
                    processResponse(clientID, message, response);
                }
            } catch (Exception e) {
                logger.error("Error processing response for clientID: {}", clientID, e);
            }
        }
    }

    private void processResponse(String clientID, Message<JsonObject> message, JsonObject response) {
        var requestType = response.getString("RequestType");

        if ("provisioning".equalsIgnoreCase(requestType)) {
            handleProvisioningResponse(clientID, response);
            logger.info("Provisioning response processed for clientID: {}", clientID);
        } else if ("discovery".equalsIgnoreCase(requestType)) {
            message.reply(response);
            logger.info("Discovery response replied for clientID: {}", clientID);
        }

        routingTable.remove(clientID);
    }

    private void handleProvisioningResponse(String clientID, JsonObject responseJson) {
        logger.info("Handling provisioning response for clientID: {}", clientID);

        var status = responseJson.getString("status");
        var discoveryProfileId = responseJson.getInteger("discovery_profile_id");
        var request = new JsonObject().put("clientID", clientID).put("discovery_profile_id", discoveryProfileId);

        if ("success".equalsIgnoreCase(status)) {
            var result = responseJson.getJsonObject("result", new JsonObject());
            request.put("data", result);
            eventBus.send(Constants.ZMQ_POLLED_DATA, request);
        } else {
            var errors = responseJson.getJsonObject("errors", new JsonObject());
            request.put("error", errors);
            eventBus.send(Constants.ZMQ_POLLED_DATA, request);
        }
    }


    public void close() {

        logger.info("Closing ZMQ socket and terminating context");

        socket.close();

        context.term();

        logger.info("Socket closed and context terminated successfully");

    }


}