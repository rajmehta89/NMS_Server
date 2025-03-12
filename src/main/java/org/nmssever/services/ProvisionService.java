package org.nmssever.services;

import io.vertx.core.Future;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import org.nmssever.util.DBConstants;

/**
 * The type Provision service.
 */
public class ProvisionService {

    private final Pool pool;

    private final Logger Logger = LoggerFactory.getLogger(ProvisionService.class);

    /**
     * Instantiates a new Provision service.
     *
     * @param pool the pool
     */
    public ProvisionService(Pool pool) {

        this.pool = pool;

    }


    /**
     * Gets provision device data.
     *
     * @param request the request
     * @return the provision device data
     */
    public Future<JsonObject> getprovisionDeviceData(JsonObject request) {

        var response = new JsonObject();

        var discoveryProfileID = request.getInteger("discovery_profile_id");

        Logger.info("Received request to get provision device data for profile ID: " + discoveryProfileID);

        var selectSql = "SELECT discovery_profile_id, json_agg(system_info) AS system_info_array " +
                "FROM systemdata WHERE discovery_profile_id = $1 GROUP BY discovery_profile_id;";

        Logger.info("Executing query to fetch system_info: " + selectSql);

        return pool.preparedQuery(selectSql)
                .execute(Tuple.of(discoveryProfileID))
                .map(result -> {

                    if (result.rowCount() > 0) {

                        var row = result.iterator().next();

                        var systemInfoArray = row.getJsonArray("system_info_array");

                        response.put("success", "System info retrieved successfully");

                        response.put("discovery_profile_id", discoveryProfileID);

                        response.put("system_info", systemInfoArray);

                    } else {

                        response.put("error", "No system info found for discovery_profile_id: " + discoveryProfileID);

                    }

                    return response;
                })
                .onFailure(err -> {

                    Logger.error("Database query failed for profile ID: " + discoveryProfileID, err);

                    response.put("error", "Database query failed: " + err.getMessage());

                });

    }


    /**
     * Provision device future.
     *
     * @param request the request
     * @return the future
     */
    public Future<JsonObject> provisionDevice(JsonObject request) {

        var response = new JsonObject();

        var discoveryProfileID = request.getInteger("discovery_profile_id");

        var provisionStatus = request.getInteger("provision_status");

        Logger.info("Received request to provision device for profile ID: " + discoveryProfileID);

        var updateSql = "UPDATE " + DBConstants.TABLE_DISCOVERY_PROFILES +
                " SET " + DBConstants.COL_PROVISION_STATUS + " = $1 " +
                " WHERE id = $2 AND discovery_status = 1 " +
                " RETURNING id;";


        Logger.info("Executing update query: " + updateSql);

        Logger.info("Parameters: provision_status = " + provisionStatus + ", ID = " + discoveryProfileID);

        return pool.preparedQuery(updateSql)
                .execute(Tuple.of(provisionStatus, discoveryProfileID))
                .map(rows -> {
                    if (rows.rowCount() > 0) {

                        Logger.info("Provision status updated successfully for profile ID: " + discoveryProfileID);

                        response.put("success", "Provision status updated successfully");

                    } else {

                        Logger.warn("Update failed: No matching profile found with discovery_status = 1 for ID: " + discoveryProfileID);

                        response.put("error", "Update failed: No matching profile found with discovery_status = 1");

                    }

                    return response;

                })
                .onFailure(err -> {

                    Logger.error("Database update failed for profile ID: " + discoveryProfileID, err);

                    response.put("error", "Database update failed: " + err.getMessage());

                });

    }


    /**
     * Gets provisioned profiles.
     *
     * @return the provisioned profiles
     */
    public Future<JsonArray> getProvisionedProfiles() {

        var sql = "SELECT * FROM " + DBConstants.TABLE_DISCOVERY_PROFILES + " " +
                "WHERE " + DBConstants.COL_PROVISION_STATUS + " = true";

        Logger.info("Fetching provisioned discovery profiles");

        return pool.preparedQuery(sql)
                .execute()
                .map(rows -> {

                    var profiles = new JsonArray();

                    rows.forEach(row -> profiles.add(rowToJson(row)));

                    Logger.info("Fetched " + profiles.size() + " provisioned profiles");

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

}