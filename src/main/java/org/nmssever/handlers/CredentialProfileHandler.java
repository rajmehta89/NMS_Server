package org.nmssever.handlers;


import io.vertx.core.eventbus.EventBus;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.nmssever.util.Constants;
import org.nmssever.util.ResponseUtil;

/**
 * The type Credential profile handler.
 */
public class CredentialProfileHandler {

    private final EventBus eventBus;

    private final Logger Logger = LoggerFactory.getLogger(CredentialProfileHandler.class);

    /**
     * Instantiates a new Credential profile handler.
     *
     * @param eventBus the event bus
     */
    public CredentialProfileHandler(EventBus eventBus) {

        this.eventBus = eventBus;

    }

    /**
     * Gets credential profile.
     *
     * @param ctx the ctx
     */
    public void getCredentialProfile(RoutingContext ctx) {

        var profileId = ctx.pathParam("id");

        Logger.info("Received request to get credential profile by ID: " + profileId);

        if (profileId == null || profileId.trim().isEmpty()) {

            Logger.warn("Invalid credential profile ID provided");

            ResponseUtil.sendErrorResponse(ctx, 400, "Invalid credential profile ID");

            return;
        }

        try {

            var id = Integer.parseInt(profileId);

            var request = new JsonObject().put("id", id);

            Logger.info("Sending request to EventBus for credential profile: " + profileId);

            eventBus.request(Constants.CREDENTIAL_PROFILE_READ, request, reply -> {

                if (reply.succeeded()) {

                    Logger.info("Successfully retrieved credential profile: " + profileId);

                    ResponseUtil.sendSuccessResponse(ctx, 200, reply.result().body());

                } else {

                    Logger.warn("Credential profile not found: " + profileId);

                    ResponseUtil.sendErrorResponse(ctx, 404, "Credential profile not found");

                }

            });

        } catch (NumberFormatException e) {

            Logger.warn("Invalid credential profile ID format");

            ResponseUtil.sendErrorResponse(ctx, 400, "Invalid credential profile ID format");

        }
    }

    /**
     * Create credential profile.
     *
     * @param ctx the ctx
     */
    public void createCredentialProfile(RoutingContext ctx) {

        var requestBody = ctx.body().asJsonObject();

        Logger.info("Received request to create credential profile");

        if (!requestBody.containsKey("credential_profile_name") || requestBody.getString("credential_profile_name") == null || requestBody.getString("credential_profile_name").trim().isEmpty()) {

            Logger.warn("Missing or empty required field: credential_profile_name");

            ResponseUtil.sendErrorResponse(ctx, 400, "Missing or empty required field: credential_profile_name");

            return;

        }

        if (!requestBody.containsKey("system_type") || requestBody.getString("system_type") == null || requestBody.getString("system_type").trim().isEmpty()) {

            Logger.warn("Missing or empty required field: system_type");

            ResponseUtil.sendErrorResponse(ctx, 400, "Missing or empty required field: system_type");

            return;

        }

        if (!requestBody.containsKey("credentialconfig") || requestBody.getJsonObject("credentialconfig") == null || requestBody.getJsonObject("credentialconfig").isEmpty()) {

            Logger.warn("Missing or empty required field: credentialconfig");

            ResponseUtil.sendErrorResponse(ctx, 400, "Missing or empty required field: credentialconfig");

            return;

        }

        Logger.info("Sending request to EventBus for credential profile creation");

        eventBus.request(Constants.CREDENTIAL_PROFILE_CREATE, requestBody, reply -> {

            if (reply.succeeded()) {

                Logger.info("Credential profile created successfully");

                ResponseUtil.sendSuccessResponse(ctx, 201, reply.result().body());

            } else {

                Logger.error("Failed to create credential profile: " + reply.cause());

                ResponseUtil.sendErrorResponse(ctx, 500, reply.cause() != null ?
                        reply.cause().getMessage() : "Failed to create credential profile");

            }

        });

    }


    /**
     * Update credential profile.
     *
     * @param ctx the ctx
     */
    public void updateCredentialProfile(RoutingContext ctx) {

        JsonObject requestBody = ctx.body().asJsonObject();

        Logger.info("Received request to update credential profile");

        if (!requestBody.containsKey("id") || requestBody.getInteger("id") == null ||
                !requestBody.containsKey("credential_profile_name") || requestBody.getString("credential_profile_name") == null || requestBody.getString("credential_profile_name").trim().isEmpty() ||
                !requestBody.containsKey("system_type") || requestBody.getString("system_type") == null || requestBody.getString("system_type").trim().isEmpty() ||
                (requestBody.containsKey("credentialconfig") && !(requestBody.getValue("credentialconfig") instanceof JsonObject))) {

            Logger.warn("Missing or invalid required fields: id, credential_profile_name, system_type, credentialconfig");

            ResponseUtil.sendErrorResponse(ctx, 400, "Missing or invalid required fields: id, credential_profile_name, system_type, credentialconfig");

            return;

        }

        var profileId = requestBody.getInteger("id");

        Logger.info("Sending update request to EventBus for credential profile: " + profileId);

        eventBus.request(Constants.CREDENTIAL_PROFILE_UPDATE, requestBody, reply -> {

            if (reply.succeeded()) {

                Logger.info("Credential profile updated successfully for ID: " + profileId);

                ResponseUtil.sendSuccessResponse(ctx, 200, reply.result().body());

            }

            else {

                Logger.error("Profile update failed for ID: " + profileId, reply.cause());

                ResponseUtil.sendErrorResponse(ctx, 500, "Profile update failed: " + reply.cause().getMessage());

            }

        });

    }


    /**
     * Gets credentials.
     *
     * @param ctx the ctx
     */
    public void getCredentials(RoutingContext ctx) {

        Logger.info("Received request to get credential profiles");

        Logger.info("Sending request to EventBus at address:");

        eventBus.request(Constants.CREDENTIAL_PROFILES_READ, new JsonObject(), reply -> {

            if (reply.succeeded() && reply.result() != null) {

                var responseBody = reply.result().body();

                if (responseBody instanceof JsonArray credentials) {

                    if (credentials.isEmpty()) {

                        Logger.warn("No credential profiles found.");

                        ResponseUtil.sendErrorResponse(ctx, 404, "No credential profiles available.");

                        return;

                    }

                    Logger.info("Successfully retrieved credential profiles." + credentials.size());

                    ResponseUtil.sendSuccessResponse(ctx, 200, credentials);


                } else {

                    Logger.warn("Unexpected response type from EventBus:" + responseBody);

                    ResponseUtil.sendErrorResponse(ctx, 500, "Invalid response from server.");

                }

            } else {

                var cause = (reply.cause() != null) ? reply.cause() : new Exception("Unknown EventBus error");

                Logger.error("Failed to retrieve credential profiles.", cause);

                ResponseUtil.sendErrorResponse(ctx, 500, "Failed to fetch credential profiles: " + cause.getMessage());

            }

        });

    }


    /**
     * Delete credential profile.
     *
     * @param ctx the ctx
     */
    public void deleteCredentialProfile(RoutingContext ctx) {

        var idParam = ctx.pathParam("id");

        if (idParam == null || idParam.trim().isEmpty()) {

            Logger.warn("Delete failed: ID is missing");

            ResponseUtil.sendErrorResponse(ctx, 400, "ID is required");

            return;
        }

        int id;

        try {

            id = Integer.parseInt(idParam);

        } catch (NumberFormatException e) {

            Logger.warn("Delete failed: Invalid ID format");

            ResponseUtil.sendErrorResponse(ctx, 400, "Invalid ID format");

            return;
        }

        Logger.info("Received request to delete credential profile with ID: " + id);

        var request = new JsonObject().put("id", id);

        Logger.info("Sending delete request to EventBus for credential profile");

        eventBus.request(Constants.CREDENTIAL_PROFILE_DELETE, request, reply -> {

            if (reply.succeeded()) {

                Logger.info("Credential profile deleted successfully");

                ResponseUtil.sendSuccessResponse(ctx, 200, reply.result().body());

            } else {

                Logger.warn("Credential profile not found or deletion failed");

                ResponseUtil.sendErrorResponse(ctx, 404, "Credential profile not found");

            }
        });
    }

}