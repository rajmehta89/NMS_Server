package org.nmssever.verticles;

import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.nmssever.handlers.CredentialProfileHandler;
import org.nmssever.handlers.DiscoveryProfileHandler;
import org.nmssever.handlers.ProvisionHandler;
import org.nmssever.util.Constants;

/**
 * The type Http server verticle.
 */
public class HttpServerVerticle extends AbstractVerticle {

    private static final int DEFAULT_PORT = 8000;

    private int port;

    private final Logger Logger = LoggerFactory.getLogger(HttpServerVerticle.class);

    @Override
    public void start(Promise<Void> startPromise) {

        Dotenv dotenv = Dotenv.load();

        var portStr = dotenv.get("HTTP_PORT");

        if (portStr != null) {

            try {

                port = Integer.parseInt(portStr);

            } catch (NumberFormatException e) {

                Logger.warn("Invalid HTTP_PORT value; using default port " + DEFAULT_PORT);

                port = DEFAULT_PORT;

            }

        } else {

            port = DEFAULT_PORT;

        }

        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        registerCredentialProfileRoutes(router);

        registerDiscoveryProfileRoutes(router);

        registerProvisioningRoutes(router);

        HttpServer server = vertx.createHttpServer();

        server.requestHandler(router)

                .listen(port, result -> {

                    if (result.succeeded()) {

                        Logger.info("HTTP Server running on port " + port);

                        startPromise.complete();

                    } else {

                        Logger.info("Failed to start HTTP Server: " + result.cause().getMessage());

                        startPromise.fail(result.cause());

                    }

                });

    }

    private void registerCredentialProfileRoutes(Router router) {

        CredentialProfileHandler handler = new CredentialProfileHandler(vertx.eventBus());

        router.get(Constants.GET_CREDENTIAL_PROFILE_API).handler(handler::getCredentialProfile);

        router.get(Constants.GET_CREDENTIALS).handler(handler::getCredentials);

        router.post(Constants.CREATE_CREDENTIAL_PROFILE).handler(handler::createCredentialProfile);

        router.put(Constants.UPDATE_CREDENTIAL_PROFILE).handler(handler::updateCredentialProfile);

        router.delete(Constants.DELETE_CREDENTIAL_PROFILE).handler(handler::deleteCredentialProfile);

    }

    private void registerDiscoveryProfileRoutes(Router router) {

        DiscoveryProfileHandler handler = new DiscoveryProfileHandler(vertx.eventBus());

        router.get(Constants.GET_DISCOVERY_PROFILE_API).handler(handler::getDiscoveryProfile);

        router.get(Constants.GET_DISCOVERYPROFILES).handler(handler::getDiscoveryProfiles);

        router.get(Constants.GET_DISCOVERY_RUN).handler(handler::runDiscovery);

        router.post(Constants.CREATE_DISCOVERY_PROFILE).handler(handler::createDiscoveryProfile);

        router.put(Constants.UPDATE_DISCOVERY_PROFILE).handler(handler::updateDiscoveryProfile);

        router.delete(Constants.DELETE_DISCOVERY_PROFILE).handler(handler::deleteDiscoveryProfile);

    }

    private void registerProvisioningRoutes(Router router) {

        ProvisionHandler handler = new ProvisionHandler(vertx.eventBus());

        router.post(Constants.GET_PROVISION).handler(handler::provision);

        router.get(Constants.GET_PROVISIONED_DATA).handler(handler::getProvisionedData);

    }

}