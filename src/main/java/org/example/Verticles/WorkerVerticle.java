package org.example.Verticles;

import io.vertx.core.AbstractVerticle;

public class WorkerVerticle extends AbstractVerticle {


    @Override
    public void start() {

        System.out.println("WorkerVerticle started!");

    }

}
