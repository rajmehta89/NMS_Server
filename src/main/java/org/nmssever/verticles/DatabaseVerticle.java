package org.nmssever.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import org.nmssever.database.DatabaseClient;
import org.nmssever.services.CredentialProfileService;
import org.nmssever.services.DiscoveryProfileService;
import org.nmssever.services.ProvisionService;
import org.nmssever.util.Constants;
import org.nmssever.util.ResponseUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDateTime;


/**
 * The type Database verticle.
 */
public class DatabaseVerticle extends AbstractVerticle {

    private CredentialProfileService credentialProfileService;

    private DiscoveryProfileService discoveryProfileService;

    private ProvisionService provisionService;

    private final Logger LOGGER = LoggerFactory.getLogger(DatabaseVerticle.class);

    private final Pool dbPool;

    private final JsonObject jsonObject = new JsonObject();

    /**
     * Instantiates a new Database verticle.
     *
     * @param databaseClient the database client
     */
    public DatabaseVerticle(DatabaseClient databaseClient) {

        this.dbPool = databaseClient.getPool();

    }

    @Override
    public void start(Promise<Void> startPromise) {

        credentialProfileService = new CredentialProfileService(dbPool);

        discoveryProfileService = new DiscoveryProfileService(dbPool, vertx.eventBus());

        provisionService = new ProvisionService(dbPool);

        registerEventBusConsumers();

        testDatabaseConnection(startPromise);

    }

    private void registerEventBusConsumers() {

        LOGGER.info("Registering Event Bus consumers...");

        vertx.eventBus().consumer(Constants.CREDENTIAL_PROFILE_CREATE, this::handleCreateCredentialProfile);
        LOGGER.info("Consumer registered for: " + Constants.CREDENTIAL_PROFILE_CREATE);


        vertx.eventBus().consumer(Constants.CREDENTIAL_PROFILE_READ, this::handleGetCredentialProfile);
        LOGGER.info("Consumer registered for: " + Constants.CREDENTIAL_PROFILE_READ);


        vertx.eventBus().consumer(Constants.CREDENTIAL_PROFILE_UPDATE, this::handleUpdateCredentialProfile);
        LOGGER.info("Consumer registered for: " + Constants.CREDENTIAL_PROFILE_UPDATE);


        vertx.eventBus().consumer(Constants.CREDENTIAL_PROFILE_DELETE, this::handleDeleteCredentialProfile);
        LOGGER.info("Consumer registered for: " + Constants.CREDENTIAL_PROFILE_DELETE);


        vertx.eventBus().consumer(Constants.DISCOVERY_PROFILE_CREATE, this::handleCreateDiscoveryProfile);
        LOGGER.info("Consumer registered for: " + Constants.DISCOVERY_PROFILE_CREATE);


        vertx.eventBus().consumer(Constants.DISCOVERY_PROFILE_READ, this::handleGetDiscoveryProfile);
        LOGGER.info("Consumer registered for: " + Constants.DISCOVERY_PROFILE_READ);


        vertx.eventBus().consumer(Constants.DISCOVERY_PROFILE_UPDATE, this::handleUpdateDiscoveryProfile);
        LOGGER.info("Consumer registered for: " + Constants.DISCOVERY_PROFILE_UPDATE);


        vertx.eventBus().consumer(Constants.DISCOVERY_PROFILE_DELETE, this::handleDeleteDiscoveryProfile);
        LOGGER.info("Consumer registered for: " + Constants.DISCOVERY_PROFILE_DELETE);


        vertx.eventBus().consumer(Constants.DISCOVERY_RUN, this::handleRunDiscovery);
        LOGGER.info("Consumer registered for: " + Constants.DISCOVERY_RUN);

        vertx.eventBus().consumer(Constants.CREDENTIAL_PROFILES_READ, this::handleGetCredentialsProfiles);
        LOGGER.info("Consumer registered for: " + Constants.CREDENTIAL_PROFILES_READ);

        vertx.eventBus().consumer(Constants.DISCOVERY_PROFILES_READ, this::handleGetDiscoveryProfiles);
        LOGGER.info("Consumer registered for: " + Constants.DISCOVERY_PROFILES_READ);


        vertx.eventBus().consumer(Constants.PROVISION, this::handleProvision);
        LOGGER.info("Consumer registered for: " + Constants.PROVISION);


        vertx.eventBus().consumer(Constants.PROVISIONEDDATA, this::handleGetProvisionData);
        LOGGER.info("Consumer registered for: " + Constants.PROVISIONEDDATA);


        vertx.eventBus().consumer(Constants.GET_CREDENTIAL_PROFILE, this::handleGetCredentialProfile);
        LOGGER.info("Consumer registered for: " + Constants.GET_CREDENTIAL_PROFILE);


        vertx.eventBus().consumer(Constants.UPDATE_DISCOVERY_STATUS, this::handleUpdateDiscoveryStatus);
        LOGGER.info("Consumer registered for: " + Constants.UPDATE_DISCOVERY_STATUS);


        vertx.eventBus().consumer(Constants.GET_DISCOVERY_PROFILE, this::handleGetDiscoveryProfile);
        LOGGER.info("Consumer registered for: " + Constants.GET_DISCOVERY_PROFILE);


        vertx.eventBus().consumer(Constants.GET_DISCOVERY_STATUS, this::handleGetDiscoveryStatus);
        LOGGER.info("Consumer registered for: " + Constants.GET_DISCOVERY_STATUS);


        vertx.eventBus().consumer(Constants.GET_PROVISIONED_PROFILES, this::handleGetProvisionedProfiles);
        LOGGER.info("Consumer registered for: " + Constants.GET_PROVISIONED_PROFILES);


        vertx.eventBus().consumer(Constants.ZMQ_POLLED_DATA, this::handleZmqPolledData);

        vertx.eventBus().consumer(Constants.PING_CHECK, (Message<JsonObject> message) -> {

            var request = message.body();

            var ipAddress = request.getString("ipAddress");

            if (ipAddress == null || ipAddress.isEmpty()) {

                message.fail(400, "Missing or invalid IP address");

                return;
            }

            checkPingReachability(ipAddress).onComplete(result -> {

                if (result.succeeded()) {

                    message.reply(new JsonObject().put("isReachable", result.result()));

                } else {

                    message.reply(new JsonObject().put("isReachable", false).put("error", result.cause().getMessage()));

                }

            });

        });


        LOGGER.info("All Event Bus consumers registered successfully.");

    }

    /**
     * Get json object json object.
     *
     * @return the json object
     */
    public JsonObject getJsonObject() {

        return this.jsonObject;

    }

    private void testDatabaseConnection(Promise<Void> startPromise) {

        credentialProfileService.testConnection()
                .onSuccess(res -> {

                    startPromise.complete();

                })

                .onFailure(err -> {

                    startPromise.fail(err);

                });
    }


    private Future<Boolean> checkPingReachability(String ipAddress) {

        Promise<Boolean> promise = Promise.promise();

        try {

            var isReachable = isHostReachable(ipAddress);

            if (isReachable) {

                LOGGER.info("Ping to " + ipAddress + " is successful");

                promise.complete(true);

            } else {

                LOGGER.warn("Ping to " + ipAddress + " failed");

                promise.fail("Ping to " + ipAddress + " is not reachable");

            }

        } catch (IOException | InterruptedException e) {

            LOGGER.error("Error while pinging " + ipAddress);

            promise.fail("Error while pinging " + ipAddress + ": " + e.getMessage());

        }

        return promise.future();

    }


    /**
     * Is host reachable boolean.
     *
     * @param ip the ip
     * @return the boolean
     * @throws IOException          the io exception
     * @throws InterruptedException the interrupted exception
     */
    public boolean isHostReachable(String ip) throws IOException, InterruptedException {

        ProcessBuilder processBuilder = new ProcessBuilder("ping", "-c", "4", ip);

        Process process = processBuilder.start();

        var isReachable = false;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

            String line;

            while ((line = reader.readLine()) != null) {

                if (line.contains("0% packet loss")) {

                    isReachable = true;

                }

            }
        }

        process.waitFor();

        return isReachable;

    }


    private void handleCreateCredentialProfile(Message<JsonObject> message) {

        var request = message.body();

        LOGGER.info("Received create credential profile request: " + request.encodePrettily());

        credentialProfileService.createCredentialProfile(request)

                .onSuccess(response -> {

                    LOGGER.info("Successfully created credential profile. Response: " + response.encodePrettily());

                    ResponseUtil.sendSuccess(message, response);

                })

                .onFailure(err -> {

                    LOGGER.error("Failed to create credential profile. Error: " + err.getMessage(), err);

                    ResponseUtil.sendError(message, 500, err.getMessage());

                });

    }


    private void handleGetDiscoveryProfiles(Message<JsonObject> message) {

        var request = message.body();

        discoveryProfileService.getdiscoveryProfiles(request)

                .onSuccess(response -> {

                    LOGGER.info("Successfully created credential profile. Response: " + response.encodePrettily());

                    ResponseUtil.sendSuccess(message, response);

                })

                .onFailure(err -> {

                    LOGGER.error("Failed to create credential profile. Error: " + err.getMessage(), err);

                    ResponseUtil.sendError(message, 500, err.getMessage());

                });

    }


    private void handleGetCredentialsProfiles(Message<JsonObject> message) {

        var request = message.body();

        credentialProfileService.getCredentialsProfile(request)

                .onSuccess(response -> {

                    LOGGER.info("Successfully created credential profile. Response: " + response.encodePrettily());

                    ResponseUtil.sendSuccess(message, response);

                })

                .onFailure(err -> {

                    LOGGER.error("Failed to create credential profile. Error: " + err.getMessage(), err);

                    ResponseUtil.sendError(message, 500, err.getMessage());

                });

    }


    private void handleUpdateCredentialProfile(Message<JsonObject> message) {

        var request = message.body();

        LOGGER.info("Received update credential profile request: " + request.encodePrettily());

        credentialProfileService.updateCredentialProfile(request)

                .onSuccess(response -> {

                    LOGGER.info("Successfully updated credential profile. Response: " + response.encodePrettily());

                    ResponseUtil.sendSuccess(message, response);

                })

                .onFailure(err -> {

                    LOGGER.error("Failed to update credential profile. Error: " + err.getMessage(), err);

                    ResponseUtil.sendError(message, 500, err.getMessage());

                });

    }


    private void handleDeleteCredentialProfile(Message<JsonObject> message) {

        var request = message.body();

        var id = request.getInteger("id");

        LOGGER.info("Received delete credential profile request for ID: " + id);

        credentialProfileService.deleteCredentialProfile(id)

                .onSuccess(response -> {

                    LOGGER.info("Successfully deleted credential profile with ID: " + id);

                    ResponseUtil.sendSuccess(message, response);

                })
                .onFailure(err -> {

                    LOGGER.error("Failed to delete credential profile with ID: " + id + ". Error: " + err.getMessage(), err);

                    ResponseUtil.sendError(message, 500, err.getMessage());

                });
    }

    private void handleCreateDiscoveryProfile(Message<JsonObject> message) {

        var request = message.body();

        LOGGER.info("Received create discovery profile request: " + request.encodePrettily());

        discoveryProfileService.createDiscoveryProfile(request)

                .onSuccess(response -> {

                    LOGGER.info("Successfully created discovery profile. Response: " + response.encodePrettily());

                    ResponseUtil.sendSuccess(message, response);

                })

                .onFailure(err -> {

                    LOGGER.error("Failed to create discovery profile. Error: " + err.getMessage(), err);

                    ResponseUtil.sendError(message, 400, err.getMessage());

                });

    }

    private void handleUpdateDiscoveryProfile(Message<JsonObject> message) {

        var request = message.body();

        LOGGER.info("Received update discovery profile request: " + request.encodePrettily());

        discoveryProfileService.updateDiscoveryProfile(request)

                .onSuccess(response -> {

                    LOGGER.info("Successfully updated discovery profile. Response: " + response.encodePrettily());

                    ResponseUtil.sendSuccess(message, response);

                })

                .onFailure(err -> {

                    LOGGER.error("Failed to update discovery profile. Error: " + err.getMessage(), err);

                    ResponseUtil.sendError(message, 500, err.getMessage());

                });

    }

    private void handleDeleteDiscoveryProfile(Message<JsonObject> message) {

        var request = message.body();

        var profileID = request.getInteger("discovery_profile_id");

        LOGGER.info("Received delete discovery profile request for profile: " + profileID);

        discoveryProfileService.deleteDiscoveryProfile(profileID)

                .onSuccess(response -> {

                    LOGGER.info("Successfully deleted discovery profile: " + profileID);

                    ResponseUtil.sendSuccess(message, response);

                })

                .onFailure(err -> {

                    LOGGER.error("Failed to delete discovery profile: " + profileID + ". Error: " + err.getMessage(), err);

                    ResponseUtil.sendError(message, 500, err.getMessage());

                });

    }

    private void handleRunDiscovery(Message<JsonObject> message) {

        var request = message.body();

        LOGGER.info("Received run discovery request: " + request.encodePrettily());

        discoveryProfileService.runDiscovery(request)

                .onSuccess(response -> {

                    LOGGER.info("Successfully ran discovery. Response: " + response.encodePrettily());

                    ResponseUtil.sendSuccess(message, response);

                })

                .onFailure(err -> {

                    LOGGER.error("Failed to run discovery. Error: " + err.getMessage(), err);

                    ResponseUtil.sendError(message, 500, err.getMessage());

                });

    }

    private void handleProvision(Message<JsonObject> message) {

        var request = message.body();

        LOGGER.info("Received provision device request: " + request.encodePrettily());

        provisionService.provisionDevice(request)

                .onSuccess(response -> {

                    LOGGER.info("Successfully provisioned device. Response: " + response.encodePrettily());

                    ResponseUtil.sendSuccess(message, response);

                })
                .onFailure(err -> {

                    LOGGER.error("Failed to provision device. Error: " + err.getMessage(), err);

                    ResponseUtil.sendError(message, 500, err.getMessage());

                });

    }


    private void handleGetProvisionData(Message<JsonObject> message) {

        var request = message.body();

        LOGGER.info("Received provision device request: " + request.encodePrettily());

        provisionService.getprovisionDeviceData(request)

                .onSuccess(response -> {

                    LOGGER.info("Successfully provisioned device. Response: " + response.encodePrettily());

                    ResponseUtil.sendSuccess(message, response);

                })
                .onFailure(err -> {

                    LOGGER.error("Failed to provision device. Error: " + err.getMessage(), err);

                    ResponseUtil.sendError(message, 500, err.getMessage());

                });

    }


    private void handleGetProvisionedProfiles(Message<Object> message) {

        LOGGER.info("Received request to get provisioned profiles.");

        provisionService.getProvisionedProfiles()

                .onSuccess(response -> {

                    LOGGER.info("Successfully retrieved provisioned profiles. Response: " + response.encodePrettily());

                    ResponseUtil.sendSuccess(message, response);

                })

                .onFailure(err -> {

                    LOGGER.error("Failed to get provisioned profiles. Error: " + err.getMessage(), err);

                    ResponseUtil.sendError(message, 500, err.getMessage());

                });

    }

    private void handleGetCredentialProfile(Message<Object> message) {

        var profileId = ((JsonObject) message.body()).getInteger("id");

        LOGGER.info("Received request to get credential profile: " + profileId);

        credentialProfileService.getCredentialProfile(profileId)

                .onSuccess(response -> {

                    LOGGER.info("Successfully retrieved credential profile. Response: " + response.encodePrettily());

                    ResponseUtil.sendSuccess(message, response);

                })

                .onFailure(err -> {

                    LOGGER.error("Failed to get credential profile. Error: " + err.getMessage(), err);

                    ResponseUtil.sendError(message, 404, err.getMessage());

                });

    }

    private void handleUpdateDiscoveryStatus(Message<JsonObject> message) {

        var request = message.body();

        var discoveryProfileName = request.getString("discovery_profile_name");

        var discoveryStatus = true;

        LOGGER.info("Received request to update discovery status. Profile Name: " + discoveryProfileName + ", Status: " + discoveryStatus);

        discoveryProfileService.updateDiscoveryStatus(1, 1)
                .onSuccess(response -> {

                    LOGGER.info("Successfully updated discovery status. Response: " + response.encodePrettily());

                    ResponseUtil.sendSuccess(message, response);

                })
                .onFailure(err -> {

                    LOGGER.error("Failed to update discovery status. Error: " + err.getMessage(), err);

                    ResponseUtil.sendError(message, 500, err.getMessage());

                });

    }

    private void handleGetDiscoveryProfile(Message<Object> message) {

        var body = (JsonObject) message.body();

        var discoveryProfileId = body.getInteger("discovery_profile_id");

        LOGGER.info("Received request to get discovery profile: " + discoveryProfileId);

        discoveryProfileService.getDiscoveryProfile(discoveryProfileId)

                .onSuccess(response -> {

                    LOGGER.info("Successfully retrieved discovery profile. Response: " + response.encodePrettily());

                    ResponseUtil.sendSuccess(message, response);

                })
                .onFailure(err -> {

                    LOGGER.error("Failed to get discovery profile. Error: " + err.getMessage(), err);

                    ResponseUtil.sendError(message, 404, err.getMessage());

                });

    }

    private void handleGetDiscoveryStatus(Message<Object> message) {

        var discoveryProfileName = (String) message.body();

        LOGGER.info("Received request to get discovery status for profile: " + discoveryProfileName);

        discoveryProfileService.getDiscoveryStatus(discoveryProfileName)
                .onSuccess(response -> {

                    LOGGER.info("Successfully retrieved discovery status. Response: " + response.encodePrettily());

                    ResponseUtil.sendSuccess(message, response);

                })
                .onFailure(err -> {

                    LOGGER.error("Failed to get discovery status. Error: " + err.getMessage(), err);

                    ResponseUtil.sendError(message, 500, err.getMessage());

                });

    }

    private void handleZmqPolledData(Message<Object> message) {

        var request = (JsonObject) message.body();

        var discoveryProfileID = request.getInteger("discovery_profile_id");

        JsonObject systemData;

        if (request.containsKey("data")) {

            systemData = request.getJsonObject("data").put("timestamp", LocalDateTime.now().toString());

        } else if (request.containsKey("error")) {

            systemData = request.getJsonObject("error").put("timestamp", LocalDateTime.now().toString());

        } else {

            systemData = new JsonObject().put("message", "Unknown data format").put("timestamp", LocalDateTime.now().toString());

        }


        var insertSql = "INSERT INTO systemdata (discovery_profile_id, system_info,timestamp) VALUES ($1, $2, $3)";

        dbPool.preparedQuery(insertSql).execute(Tuple.of(discoveryProfileID, systemData, LocalDateTime.now()), insertAr -> {

            if (insertAr.failed()) {

                LOGGER.error("❌ Failed to insert system_data for discovery_profile_id: " + discoveryProfileID, insertAr.cause());

            } else {

                LOGGER.info("✅ Successfully inserted system_data for discovery_profile_id: " + discoveryProfileID);

            }

        });

    }


    @Override
    public void stop() {

    }

}