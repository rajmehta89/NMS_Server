package org.nmssever.handlers;

import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.nmssever.util.Constants;
import org.nmssever.util.DBConstants;
import org.nmssever.util.ResponseUtil;

import java.util.regex.Pattern;

/**
 * The type Discovery profile handler.
 */
public class DiscoveryProfileHandler {

    private final EventBus eventBus;

    private final Logger Logger = LoggerFactory.getLogger(DiscoveryProfileHandler.class);

    /**
     * Instantiates a new Discovery profile handler.
     *
     * @param eventBus the event bus
     */
    public DiscoveryProfileHandler(EventBus eventBus) {

        this.eventBus = eventBus;

    }


    /**
     * Gets discovery profiles.
     *
     * @param ctx the ctx
     */
    public void getDiscoveryProfiles(RoutingContext ctx) {

        Logger.info("Received request to get discovery profiles");

        Logger.info("Sending request to EventBus at address:");

        eventBus.request(Constants.DISCOVERY_PROFILES_READ, new JsonObject(), reply -> {

            if (reply.succeeded() && reply.result() != null) {

                var responseBody = reply.result().body();

                if (responseBody instanceof JsonArray credentials) {

                    if (credentials.isEmpty()) {

                        Logger.warn("No discovery profiles found.");

                        ResponseUtil.sendErrorResponse(ctx, 404, "No discovery profiles available.");

                        return;

                    }

                    Logger.info("Successfully retrieved discovery profiles." + credentials.size());

                    ResponseUtil.sendSuccessResponse(ctx, 200, credentials);


                } else {

                    Logger.warn("Unexpected response type from EventBus:" + responseBody);

                    ResponseUtil.sendErrorResponse(ctx, 500, "Invalid response from server.");

                }

            } else {

                var cause = (reply.cause() != null) ? reply.cause() : new Exception("Unknown EventBus error");

                Logger.error("Failed to retrieve discovery profiles.", cause);

                ResponseUtil.sendErrorResponse(ctx, 500, "Failed to fetch discovery profiles: " + cause.getMessage());

            }

        });

    }


    /**
     * Gets discovery profile.
     *
     * @param ctx the ctx
     */
    public void getDiscoveryProfile(RoutingContext ctx) {

        var discoveryProfileId = ctx.pathParam("id");

        if (discoveryProfileId == null || discoveryProfileId.trim().isEmpty()) {

            Logger.warn("Discovery profile name is missing in the request");

            ResponseUtil.sendErrorResponse(ctx, 400, "Missing discovery profile name");

            return;
        }

        Logger.info("Received request to fetch discovery profile: " + discoveryProfileId);

        var request = new JsonObject().put("discovery_profile_id", Integer.parseInt(discoveryProfileId));

        Logger.info("Sending request to EventBus for discovery profile retrieval");

        eventBus.request(Constants.DISCOVERY_PROFILE_READ, request, reply -> {

            if (reply.succeeded()) {

                Logger.info("Discovery profile retrieved successfully: " + discoveryProfileId);

                ResponseUtil.sendSuccessResponse(ctx, 200, reply.result().body());

            } else {

                Logger.warn("Discovery profile not found: " + discoveryProfileId + ", Error: " + reply.cause());

                ResponseUtil.sendErrorResponse(ctx, 404, "Discovery profile not found");

            }

        });

    }

    /**
     * Run discovery.
     *
     * @param ctx the ctx
     */
    public void runDiscovery(RoutingContext ctx) {

        Logger.info("Received request to run discovery");

        var request = ctx.body().asJsonObject();

        if (!request.containsKey(DBConstants.COL_ID) || request.getValue(DBConstants.COL_ID) == null) {

            Logger.warn("Missing or invalid required field: " + DBConstants.COL_ID);

            ResponseUtil.sendErrorResponse(ctx, 400, "Missing or invalid required field: " + DBConstants.COL_ID);

            return;

        }

        Logger.info("Sending discovery run request to EventBus");

        eventBus.request(Constants.DISCOVERY_RUN, request, reply -> {

            if (reply.succeeded()) {

                Logger.info("Discovery run successful");

                ResponseUtil.sendSuccessResponse(ctx, 200, reply.result().body());

            }

            else {

                Logger.warn("Failed to run discovery");

                ResponseUtil.sendErrorResponse(ctx, 404, "Failed to run discovery");

            }

        });

    }


    /**
     * Create discovery profile.
     *
     * @param ctx the ctx
     */
    public void createDiscoveryProfile(RoutingContext ctx) {

        Logger.info("Received request to create discovery profile");

        var request = ctx.body().asJsonObject();

        if (!request.containsKey("discovery_profile_name") || request.getString("discovery_profile_name") == null || request.getString("discovery_profile_name").trim().isEmpty()) {

            Logger.warn("Missing or invalid required field: discovery_profile_name");

            ResponseUtil.sendErrorResponse(ctx, 400, "Missing or invalid required field: discovery_profile_name");

            return;

        }

        if (!request.containsKey("ip") || request.getString("ip") == null || request.getString("ip").trim().isEmpty()) {

            Logger.warn("Missing or invalid required field: ip");

            ResponseUtil.sendErrorResponse(ctx, 400, "Missing or invalid required field: ip");

            return;

        }

        var ip = request.getString("ip");

        if (!isValidIPAddress(ip)) {

            Logger.warn("Invalid IP address format: " + ip);

            ResponseUtil.sendErrorResponse(ctx, 400, "Invalid IP address format");

            return;

        }

        if (!request.containsKey("credential_profile_id") || request.getValue("credential_profile_id") == null) {

            Logger.warn("Missing or invalid required field: credential_profile_id");

            ResponseUtil.sendErrorResponse(ctx, 400, "Missing or invalid required field: credential_profile_id");

            return;

        }

        Logger.info("Sending discovery profile creation request to EventBus");

        eventBus.request(Constants.DISCOVERY_PROFILE_CREATE, request, reply -> {

            if (reply.succeeded()) {

                Logger.info("Discovery profile created successfully");

                ResponseUtil.sendSuccessResponse(ctx, 201, reply.result().body());

            } else {

                Logger.error("Failed to create discovery profile: " + reply.cause());

                ResponseUtil.sendErrorResponse(ctx, 400, reply.cause() != null ? reply.cause().getMessage() : "Failed to create discovery profile");

            }

        });

    }


    private boolean isValidIPAddress(String ip) {

        var ipv4Regex = "^(25[0-5]|2[0-4][0-9]|1?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|1?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|1?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|1?[0-9][0-9]?)$";

        var ipv6Regex = "^(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9])?[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9])?[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9])?[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9])?[0-9]))$";

        return Pattern.matches(ipv4Regex, ip) || Pattern.matches(ipv6Regex, ip);

    }

    /**
     * Update discovery profile.
     *
     * @param ctx the ctx
     */
    public void updateDiscoveryProfile(RoutingContext ctx) {

        Logger.info("Received request to update discovery profile");

        var request = ctx.body().asJsonObject();

        if (!request.containsKey("id") || request.getValue("id") == null) {

            Logger.warn("Missing or invalid required field: id");

            ResponseUtil.sendErrorResponse(ctx, 400, "Missing or invalid required field: id");

            return;

        }

        if (!request.containsKey("ip") || request.getString("ip") == null || request.getString("ip").trim().isEmpty()) {

            Logger.warn("Missing or invalid required field: ip");

            ResponseUtil.sendErrorResponse(ctx, 400, "Missing or invalid required field: ip");

            return;

        }

        var ip = request.getString("ip");

        if (!isValidIPAddress(ip)) {

            Logger.warn("Invalid IP address format: " + ip);

            ResponseUtil.sendErrorResponse(ctx, 400, "Invalid IP address format");

            return;

        }

        request.put("ip", ip);

        if (!request.containsKey("credential_profile_id") || request.getValue("credential_profile_id") == null) {

            Logger.warn("Missing or invalid required field: credential_profile_id");

            ResponseUtil.sendErrorResponse(ctx, 400, "Missing or invalid required field: credential_profile_id");

            return;

        }

        Logger.info("Sending discovery profile update request to EventBus");

        eventBus.request(Constants.DISCOVERY_PROFILE_UPDATE, request, reply -> {

            if (reply.succeeded()) {

                Logger.info("Discovery profile updated successfully");

                ResponseUtil.sendSuccessResponse(ctx, 200, reply.result().body());

            }

            else {

                Logger.warn("Discovery profile not found or update failed. Error: {}", reply.cause());

                ResponseUtil.sendErrorResponse(ctx, 404, "Discovery profile not found or update failed");

            }

        });

    }


    /**
     * Delete discovery profile.
     *
     * @param ctx the ctx
     */
    public void deleteDiscoveryProfile(RoutingContext ctx) {

        Logger.info("Received request to delete discovery profile");

        var discoveryProfileID = ctx.pathParam("id");

        if (discoveryProfileID == null || discoveryProfileID.isEmpty()) {

            Logger.warn("Missing required parameter: discovery_profile_name");

            ResponseUtil.sendErrorResponse(ctx, 400, "Missing required parameter: discovery_profile_name");

            return;

        }

        Logger.info("Sending delete request to EventBus for discovery profile: " + discoveryProfileID);

        var request = new JsonObject().put("discovery_profile_id", Integer.parseInt(discoveryProfileID));

        eventBus.request(Constants.DISCOVERY_PROFILE_DELETE, request, reply -> {

            if (reply.succeeded()) {

                Logger.info("Discovery profile deleted successfully");

                ResponseUtil.sendSuccessResponse(ctx, 200, reply.result().body());

            } else {

                Logger.warn("Discovery profile not found or deletion failed");

                ResponseUtil.sendErrorResponse(ctx, 404, "Discovery profile not found or deletion failed");

            }

        });

    }

}