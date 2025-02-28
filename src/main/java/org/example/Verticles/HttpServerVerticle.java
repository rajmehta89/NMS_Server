package org.example.Verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class HttpServerVerticle extends AbstractVerticle {

    @Override
    public void start() {

        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());


        router.get("/api").handler(ctx -> ctx.response().end("Hello from Vert.x HTTP Server!"));

        router.post("/api/credential-profile").handler(ctx -> {

            JsonObject requestBody = ctx.body().asJsonObject();


            vertx.eventBus().request("credential.profile.create", requestBody, reply -> {

                if (reply.succeeded()) {

                    ctx.response().setStatusCode(200).end(reply.result().body().toString());

                } else {

                    ctx.response().setStatusCode(500).end(new JsonObject().put("error", "Internal Server Error").encodePrettily());

                }
            });

        });


        vertx.createHttpServer().requestHandler(router).listen(8000, http -> {

            if (http.succeeded()) {

                System.out.println("HTTP Server running on port 8000");

            } else {

                System.err.println("Failed to start HTTP Server: " + http.cause());

            }

        });
    }
}
