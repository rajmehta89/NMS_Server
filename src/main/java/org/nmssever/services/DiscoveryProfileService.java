package org.nmssever.services;

import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import org.nmssever.util.Constants;
import org.nmssever.util.DBConstants;

/**
 * The type Discovery profile service.
 */
public class DiscoveryProfileService {

    private final Pool pool;

    private final EventBus eventBus;

    private final Logger Logger = LoggerFactory.getLogger(DiscoveryProfileService.class);

    /**
     * Instantiates a new Discovery profile service.
     *
     * @param pool     the pool
     * @param eventBus the event bus
     */
    public DiscoveryProfileService(Pool pool, EventBus eventBus) {

        this.pool = pool;

        this.eventBus = eventBus;

    }


    /**
     * Getdiscovery profiles future.
     *
     * @param jsonObject the json object
     * @return the future
     */
    public Future<JsonArray> getdiscoveryProfiles(JsonObject jsonObject) {

        var responseArray = new JsonArray();

        Logger.info("Fetching all discovery profiles...");

        return pool.preparedQuery("SELECT * FROM discoveryprofiles")
                .execute()
                .map(rows -> {

                    for (var row : rows) {

                        responseArray.add(rowToJson(row));

                    }

                    Logger.info("Fetched " + responseArray.size() + " discovery profiles.");

                    return responseArray;

                })
                .recover(err -> {

                    Logger.error("Error fetching discovery profiles", err);

                    return Future.succeededFuture(new JsonArray().add(new JsonObject().put("error", err.getMessage())));

                });

    }


    /**
     * Create discovery profile future.
     *
     * @param request the request
     * @return the future
     */
    public Future<JsonObject> createDiscoveryProfile(JsonObject request) {

        var discoveryProfileName = request.getString(DBConstants.COL_DISCOVERY_PROFILE_NAME);

        var ip = request.getString(DBConstants.COL_IP);

        var credentialProfileId = request.getInteger(DBConstants.COL_CREDENTIAL_PROFILE_ID);

        var response = new JsonObject();

        return pool.preparedQuery(DBConstants.INSERT_DISCOVERY_PROFILE)
                .execute(Tuple.of(discoveryProfileName, ip, credentialProfileId))
                .map(res -> {
                    if (res.rowCount() > 0) {

                        var generatedId = res.iterator().next().getInteger(0);

                        Logger.info("Discovery profile created successfully with ID: " + generatedId);

                        response.put("success", "Discovery profile created");

                        response.put("id", generatedId);

                    } else {

                        Logger.warn("Credential profile not found for ID: " + credentialProfileId);

                        response.put("error", "Credential profile not found");

                    }

                    return response;

                })
                .recover(err -> {

                    Logger.error("Failed to create discovery profile: " + err.getMessage(), err);

                    response.put("error", "Failed to create discovery profile: " + err.getMessage());

                    return Future.succeededFuture(response);

                });

    }


    /**
     * Gets discovery profile.
     *
     * @param profileID the profile id
     * @return the discovery profile
     */
    public Future<JsonObject> getDiscoveryProfile(Integer profileID) {

        var response = new JsonObject();

        Logger.info("Fetching discovery profile for: " + profileID);

        return pool.preparedQuery(DBConstants.SELECT_DISCOVERY_PROFILE)

                .execute(Tuple.of(profileID))

                .map(rows -> {

                    if (rows.rowCount() > 0) {

                        var profile = rowToJson(rows.iterator().next());

                        Logger.info("Discovery profile found: " + profile.encodePrettily());

                        return profile;

                    } else {

                        Logger.warn("Discovery profile not found for: " + profileID);

                        return response.put("error", "Discovery profile not found");

                    }

                });

    }


    /**
     * Update discovery profile future.
     *
     * @param request the request
     * @return the future
     */
    public Future<JsonObject> updateDiscoveryProfile(JsonObject request) {

        var response = new JsonObject();

        var id = request.getInteger(DBConstants.COL_ID);

        var newIp = request.getString(DBConstants.COL_IP);

        var newCredentialProfileId = request.getInteger(DBConstants.COL_CREDENTIAL_PROFILE_ID);

        var sql = "UPDATE " + DBConstants.TABLE_DISCOVERY_PROFILES + " " +
                "SET " + DBConstants.COL_IP + " = CASE WHEN $1 IS NOT NULL THEN $1 ELSE " + DBConstants.COL_IP + " END, " +
                DBConstants.COL_CREDENTIAL_PROFILE_ID + " = $2 " +
                "WHERE " + DBConstants.COL_ID + " = $3";


        Logger.info("Updating discovery profile with ID: " + id);

        return pool.preparedQuery(sql)
                .execute(Tuple.of(newIp, newCredentialProfileId, id))
                .map(rows -> {

                    if (rows.rowCount() > 0) {

                        Logger.info("Discovery profile updated successfully for ID: " + id);

                        return response.put("success", id);

                    } else {

                        Logger.warn("Update failed: Discovery profile not found for ID: " + id);

                        return response.put("error", "Update failed: Discovery profile not found");

                    }

                });
    }

    /**
     * Delete discovery profile future.
     *
     * @param ProfileID the profile id
     * @return the future
     */
    public Future<JsonObject> deleteDiscoveryProfile(Integer ProfileID) {

        var response = new JsonObject();

        var sql = "DELETE FROM " + DBConstants.TABLE_DISCOVERY_PROFILES + " " +
                "WHERE " + DBConstants.COL_ID + " = $1";

        Logger.info("Deleting discovery profile: " + ProfileID);

        return pool.preparedQuery(sql)

                .execute(Tuple.of(ProfileID))

                .map(rows -> {

                    if (rows.rowCount() > 0) {

                        Logger.info("Discovery profile deleted successfully: " + ProfileID);

                        return response.put("success", ProfileID);

                    } else {

                        Logger.warn("Deletion failed: Discovery profile not found for: " + ProfileID);

                        return response.put("error", "delete failed: discovery profile not found");

                    }

                });

    }

    /**
     * Update discovery status future.
     *
     * @param discoveryProfileID the discovery profile id
     * @param discoveryStatus    the discovery status
     * @return the future
     */
    public Future<JsonObject> updateDiscoveryStatus(Integer discoveryProfileID, Integer discoveryStatus) {

        var response = new JsonObject();

        Logger.info("Updating discovery status for profile ID: " + discoveryProfileID);

        var sql = "UPDATE " + DBConstants.TABLE_DISCOVERY_PROFILES + " " +
                "SET " + DBConstants.COL_DISCOVERY_STATUS + " = $1 " +
                "WHERE " + DBConstants.COL_ID + " = $2";

        return pool.preparedQuery(sql)
                .execute(Tuple.of(discoveryStatus, discoveryProfileID))
                .map(rows -> {
                    if (rows.rowCount() > 0) {

                        Logger.info("Discovery status updated successfully for profile ID: " + discoveryProfileID);

                        return response.put("success", discoveryProfileID);

                    } else {

                        Logger.warn("Update failed: Discovery profile not found for ID: " + discoveryProfileID);

                        return response.put("error", "Update failed: Discovery profile not found");

                    }

                });

    }


    public Future<JsonObject> getDiscoveryStatus(String discoveryProfileName) {

        var response = new JsonObject();

        Logger.info("Fetching discovery status for profile: " + discoveryProfileName);

        var sql = "SELECT " + DBConstants.COL_DISCOVERY_STATUS + " FROM " + DBConstants.TABLE_DISCOVERY_PROFILES + " " +
                "WHERE " + DBConstants.COL_DISCOVERY_PROFILE_NAME + " = $1";

        return pool.preparedQuery(sql)

                .execute(Tuple.of(discoveryProfileName))

                .map(rows -> {

                    if (rows.rowCount() > 0) {

                        var row = rows.iterator().next();

                        Logger.info("Discovery status retrieved successfully for profile: " + discoveryProfileName);

                        return response.put("success", row.getBoolean(DBConstants.COL_DISCOVERY_STATUS));

                    } else {

                        Logger.warn("Discovery profile not found for: " + discoveryProfileName);

                        return response.put("error", "discovery profile not found");

                    }

                });

    }

    public Future<JsonObject> runDiscovery(JsonObject request) {

        var response = new JsonObject();

        var discoveryProfileID = request.getInteger(DBConstants.COL_ID);

        var sql = """
                    SELECT dp.discovery_profile_name, dp.ip::TEXT AS ip, 
                           cp.credential_profile_name, cp.credentialconfig, cp.system_type
                    FROM discoveryprofiles dp
                    JOIN credentialprofiles cp ON dp.credential_profile_id = cp.id
                    WHERE dp.id = $1
                """;

        return pool.preparedQuery(sql).execute(Tuple.of(discoveryProfileID))

                .compose(rows -> {

                    if (!rows.iterator().hasNext()) {

                        return Future.succeededFuture(response.put("error", "Discovery profile not found for ID: " + discoveryProfileID));

                    }

                    var row = rows.iterator().next();

                    var discoveryProfileName = row.getString("discovery_profile_name");

                    var ipAddress = row.getString("ip");

                    var credentialConfigString = row.getString("credentialconfig");

                    var systemType = row.getString("system_type");

                    var credentialConfig = credentialConfigString != null ? new JsonObject(credentialConfigString) : new JsonObject();

                    var username = credentialConfig.getString("username", "");

                    var password = credentialConfig.getString("password", "");

                    var pingRequest = new JsonObject().put("ipAddress", ipAddress);

                    return eventBus.<JsonObject>request(Constants.PING_CHECK, pingRequest, new DeliveryOptions().setSendTimeout(10000))
                            .compose(pingReply -> {

                                boolean isReachable = pingReply.body().getBoolean("isReachable", false);

                                if (!isReachable) {

                                    return Future.succeededFuture(response.put("error", "Ping to " + ipAddress + " is not reachable"));

                                }

                                response.put(DBConstants.COL_IP, ipAddress)
                                        .put("password", password)
                                        .put("username", username)
                                        .put("SystemType", systemType)
                                        .put("RequestType", "discovery");

                                return eventBus.<JsonObject>request(Constants.ZMQ_DISCOVERY_RUN_REQUEST, response,
                                                new DeliveryOptions().addHeader("discovery_run_zmq_query", discoveryProfileName))

                                        .compose(reply -> {

                                            var replyJson = reply.body();

                                            var status = replyJson.getString("status", "failed");

                                            if ("success".equalsIgnoreCase(status)) {

                                                return updateDiscoveryStatus(discoveryProfileID, 1)
                                                        .map(updated -> response.put("success", "completed")
                                                                .put("status", status)
                                                                .put("result", replyJson.getJsonObject("result", new JsonObject())));
                                            } else {

                                                return Future.succeededFuture(response.put("error", "Failed to get response from discovery profile: " + discoveryProfileName));

                                            }

                                        });
                            });
                });
    }


    private JsonObject rowToJson(Row row) {

        return new JsonObject()
                .put(DBConstants.COL_ID, row.getInteger(DBConstants.COL_ID))
                .put(DBConstants.COL_DISCOVERY_PROFILE_NAME, row.getString(DBConstants.COL_DISCOVERY_PROFILE_NAME))
                .put(DBConstants.COL_CREDENTIAL_PROFILE_ID, row.getInteger(DBConstants.COL_CREDENTIAL_PROFILE_ID))
                .put(DBConstants.COL_IP, row.getValue(DBConstants.COL_IP).toString())
                .put(DBConstants.COL_PROVISION_STATUS, row.getInteger(DBConstants.COL_PROVISION_STATUS))
                .put(DBConstants.COL_DISCOVERY_STATUS, row.getInteger(DBConstants.COL_DISCOVERY_STATUS));
    }


}
