package org.nmssever.handlers;

import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.nmssever.util.Constants;
import org.nmssever.util.ResponseUtil;

/**
 * The type Provision handler.
 */
public class ProvisionHandler {

    private final EventBus eventBus;

    private final Logger Logger = LoggerFactory.getLogger(ProvisionHandler.class);

    /**
     * Instantiates a new Provision handler.
     *
     * @param eventBus the event bus
     */
    public ProvisionHandler(EventBus eventBus) {

        this.eventBus = eventBus;

    }

    /**
     * Provision.
     *
     * @param ctx the ctx
     */
    public void provision(RoutingContext ctx) {

        Logger.info("Received request for provisioning");

        var request = ctx.body().asJsonObject();

        if (!request.containsKey("discovery_profile_id") || request.getValue("discovery_profile_id") == null || !(request.getValue("discovery_profile_id") instanceof Integer)) {

            Logger.warn("Invalid or missing discovery profile ID");

            ResponseUtil.sendErrorResponse(ctx, 400, "Invalid or missing discovery profile ID");

            return;

        }

        if (!request.containsKey("provision_status") || request.getValue("provision_status") == null || !(request.getValue("provision_status") instanceof Integer)) {

            Logger.warn("Invalid or missing provision status");

            ResponseUtil.sendErrorResponse(ctx, 400, "Invalid or missing provision status");

            return;

        }

        var discoveryProfileID = request.getInteger("discovery_profile_id");

        Logger.info("Received request to provision device for profile ID: " + discoveryProfileID);

        Logger.info("Sending provisioning request to EventBus");

        eventBus.request(Constants.PROVISION, request, reply -> {

            if (reply.succeeded()) {

                Logger.info("Provisioning successful");

                ResponseUtil.sendSuccessResponse(ctx, 200, reply.result().body());

            }

            else {

                Logger.warn("Provisioning failed with provided credentials. Error: {}", reply.cause());

                ResponseUtil.sendErrorResponse(ctx, 500, "Failed to provision with provided credentials");

            }

        });

    }


    /**
     * Get provisioned data.
     *
     * @param ctx the ctx
     */
    public void getProvisionedData(RoutingContext ctx) {

        Logger.info("Received request for provisioning Data Get");

        var idParam = ctx.pathParam("id");

        if (idParam == null || idParam.trim().isEmpty()) {

            Logger.warn("Missing or invalid discovery_profile_id in path");

            ResponseUtil.sendErrorResponse(ctx, 400, "Missing or invalid discovery_profile_id");

            return;

        }

        try {

            var discoveryProfileID = Integer.parseInt(idParam);

            var request = new JsonObject().put("discovery_profile_id", discoveryProfileID);

            Logger.info("Sending provisioning Data Get request to EventBus");

            eventBus.request(Constants.PROVISIONEDDATA, request, reply -> {

                if (reply.succeeded()) {

                    Logger.info("Provisioning Data successfully retrieved");

                    ResponseUtil.sendSuccessResponse(ctx, 200, reply.result().body());

                }

                else {

                    Logger.warn("Provisioning failed for discovery_profile_id: " + discoveryProfileID);

                    ResponseUtil.sendErrorResponse(ctx, 404, "Failed to retrieve provisioned data for the provided discovery_profile_id");

                }

            });

        }

        catch (NumberFormatException e) {

            Logger.warn("Invalid discovery_profile_id format: " + idParam);

            ResponseUtil.sendErrorResponse(ctx, 400, "Invalid discovery_profile_id format");

        }

    }

}