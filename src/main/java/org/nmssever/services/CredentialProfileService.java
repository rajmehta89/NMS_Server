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
 * The type Credential profile service.
 */
public class CredentialProfileService {

    private final Pool pool;

    private final Logger Logger = LoggerFactory.getLogger(CredentialProfileService.class);

    private JsonObject jsonObject;

    /**
     * Instantiates a new Credential profile service.
     *
     * @param pool the pool
     */
    public CredentialProfileService(Pool pool) {

        this.pool = pool;

    }

    /**
     * Create credential profile future.
     *
     * @param request the request
     * @return the future
     */
    public Future<JsonObject> createCredentialProfile(JsonObject request) {

        var response = new JsonObject();

        Logger.info("Inserting credential profile: " + request.encodePrettily());

        return pool.preparedQuery("""
                            INSERT INTO CredentialProfiles (credential_profile_name, system_type, credentialconfig) 
                            VALUES ($1, $2, $3) 
                            RETURNING id
                        """)
                .execute(Tuple.of(

                        request.getString(DBConstants.COL_CREDENTIAL_PROFILE_NAME),

                        request.getString(DBConstants.COL_SYSTEM_TYPE),

                        request.getJsonObject(DBConstants.COL_CREDENTIAL_CONFIG).encode()

                ))
                .map(res -> {

                    if (res.rowCount() > 0) {

                        var generatedId = res.iterator().next().getInteger("id");

                        Logger.info("Credential profile inserted successfully with ID: " + generatedId);

                        return response.put("status", "success").put("id", generatedId);

                    } else {

                        Logger.error("Failed to insert credential profile: No ID returned.");

                        return response.put("status", "fail").put("error", "No ID returned");

                    }

                })
                .recover(err -> {

                    Logger.error("Error inserting credential profile", err);

                    return Future.succeededFuture(response.put("error", err.getMessage()));

                });
    }


    /**
     * Gets credentials profile.
     *
     * @param jsonObject the json object
     * @return the credentials profile
     */
    public Future<JsonArray> getCredentialsProfile(JsonObject jsonObject) {

        var responseArray = new JsonArray();

        Logger.info("Fetching all credential profiles...");

        return pool.preparedQuery("SELECT * FROM credentialprofiles")
                .execute()
                .map(rows -> {

                    for (var row : rows) {

                        responseArray.add(rowToJson(row));

                    }

                    Logger.info("Fetched " + responseArray.size() + " credential profiles.");

                    return responseArray;

                })
                .recover(err -> {

                    Logger.error("Error fetching credential profiles", err);

                    return Future.succeededFuture(new JsonArray().add(new JsonObject().put("error", err.getMessage())));

                });
    }


    /**
     * Gets credential profile.
     *
     * @param profileId the profile id
     * @return the credential profile
     */
    public Future<JsonObject> getCredentialProfile(int profileId) {

        var response = new JsonObject();

        Logger.info("Fetching credential profile for ID: " + profileId);

        return pool.preparedQuery(DBConstants.SELECT_CREDENTIAL_PROFILE)

                .execute(Tuple.of(profileId))

                .map(rows -> {

                    if (rows.rowCount() > 0) {

                        var profile = rowToJson(rows.iterator().next());

                        Logger.info("Credential profile found: " + profile.encodePrettily());

                        return profile;

                    } else {

                        Logger.warn("Credential profile not found for ID: " + profileId);

                        return response.put("error", "Credential profile not found");

                    }
                })
                .recover(err -> {

                    Logger.error("Error fetching credential profile", err);

                    return Future.succeededFuture(response.put("error", err.getMessage()));

                });
    }

    /**
     * Update credential profile future.
     *
     * @param request the request
     * @return the future
     */
    public Future<JsonObject> updateCredentialProfile(JsonObject request) {

        var response = new JsonObject();

        var id = request.getInteger(DBConstants.COL_ID);

        var profileName = request.getString(DBConstants.COL_CREDENTIAL_PROFILE_NAME);

        var systemType = request.getString(DBConstants.COL_SYSTEM_TYPE);

        var credentialConfig = request.getJsonObject(DBConstants.COL_CREDENTIALCONFIG);

        Logger.info("Updating credential profile for ID: " + id);

        Tuple params = Tuple.of(
                profileName,
                systemType,
                credentialConfig.encode(),
                id
        );

        return pool.preparedQuery(DBConstants.UPDATE_CREDENTIAL_PROFILE)
                .execute(params)
                .map(rows -> {

                    if (rows.rowCount() > 0) {

                        Logger.info("Credential profile updated successfully for ID: " + id);

                        response.put("success", "Credential profile updated successfully for ID: " + id);

                    } else {

                        Logger.warn("Update failed: Credential profile not found for ID: " + id);

                        response.put("error", "Update failed: Profile not found");

                    }

                    return response;

                })
                .recover(err -> {

                    Logger.error("Error updating credential profile", err);

                    return Future.succeededFuture(response.put("error", err.getMessage()));

                });
    }


    /**
     * Delete credential profile future.
     *
     * @param id the id
     * @return the future
     */
    public Future<JsonObject> deleteCredentialProfile(Integer id) {

        var response = new JsonObject();

        Logger.info("Attempting to delete credential profile ID: " + id);

        return pool.preparedQuery(DBConstants.DELETE_CREDENTIAL_PROFILE)
                .execute(Tuple.of(id))
                .map(deleteRes -> {

                    if (deleteRes.rowCount() > 0) {

                        Logger.info("Credential profile deleted successfully: " + id);

                        response.put("success", "Credential profile deleted successfully: " + id);

                    } else {

                        Logger.warn("Deletion failed: Credential profile not found for ID: " + id);

                        response.put("error", "Credential profile not found");

                    }

                    return response;

                })
                .recover(err -> {

                    if (err.getMessage().contains("violates foreign key constraint")) {

                        Logger.warn("Deletion blocked: Credential profile ID " + id + " is referenced in discovery profiles");

                        response.put("error", "Credential profile is assigned to a discovery profile and cannot be deleted");

                    } else {

                        Logger.error("Failed to delete credential profile ID: " + id + ". Error: " + err.getMessage(), err);

                        response.put("error", "Deletion failed: " + err.getMessage());

                    }

                    return Future.succeededFuture(response);

                });

    }


    private JsonObject rowToJson(Row row) {

        return new JsonObject()
                .put("id", row.getInteger("id"))
                .put("credential_profile_name", row.getString("credential_profile_name"))
                .put("system_type", row.getString("system_type"))
                .put("credentialconfig", new JsonObject(row.getString("credentialconfig")));

    }


    /**
     * Test connection future.
     *
     * @return the future
     */
    public Future<Void> testConnection() {

        return pool.query("SELECT 1").execute().mapEmpty();

    }

}