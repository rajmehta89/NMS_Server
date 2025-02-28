package org.example.Verticles;

import io.vertx.core.AbstractVerticle;

public class EventBusVerticle extends AbstractVerticle {

    @Override
    public void start() {

        vertx.eventBus().consumer("event.address", message -> {

            System.out.println("Received message: " + message.body());

            message.reply("Message received!");

        });

        System.out.println("EventBusVerticle started!");

    }
}

