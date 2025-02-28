package org.example.Verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

public class CredentialProfileVerticle extends AbstractVerticle {

    @Override
    public void start() {

        System.out.println("CredentailProfileVerticle started!");

        vertx.eventBus().consumer("credential.profile.create", message -> {

            JsonObject requestBody = (JsonObject) message.body();

            if (requestBody == null || !requestBody.containsKey("credential.profileName")
                    || !requestBody.containsKey("credential.protocol") || !requestBody.containsKey("credential.username")
                    || !requestBody.containsKey("credential.password")) {

                message.fail(400, "Missing required fields");

                return;

            }

            String profileName = requestBody.getString("profileName");
            String protocol = requestBody.getString("protocol");
            String username = requestBody.getString("username");
            String password = requestBody.getString("password");

            // Create the insert query as a JsonObject
            JsonObject dbRequest = new JsonObject()
                    .put("query", "INSERT INTO credential_profile (profile_name, protocol, username, password) VALUES ($1, $2, $3, $4) RETURNING id")
                    .put("params", new JsonObject()
                            .put("1", profileName)
                            .put("2", protocol)
                            .put("3", username)
                            .put("4", password)
                    );

            vertx.eventBus().request("database.execute", dbRequest, dbResponse -> {
                if (dbResponse.succeeded()) {
                    JsonObject result = (JsonObject) dbResponse.result().body();
                    message.reply(new JsonObject()
                            .put("message", "✅ Credential Profile created successfully")
                            .put("id", result.getInteger("id"))
                            .put("profileName", profileName)
                            .put("protocol", protocol)
                            .put("username", username)
                    );
                } else {
                    message.fail(500, "❌ Database Error: " + dbResponse.cause().getMessage());
                }
            });

            JsonObject response = new JsonObject()
                    .put("message", "Credential Profile created successfully")
                    .put("profileName", profileName)
                    .put("username", username)
                    .put("password", password)
                    .put("protocol", protocol);

                     message.reply(response);
        });

    }

}
