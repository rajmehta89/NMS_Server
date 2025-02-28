package org.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Tuple;

public class PostgresInsertVerticle extends AbstractVerticle {
    private Pool client;

    @Override
    public void start(Promise<Void> startPromise) {
        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(5432)
                .setHost("localhost")
                .setDatabase("testdb")
                .setUser("username")
                .setPassword("password");

        PoolOptions poolOptions = new PoolOptions().setMaxSize(5);
        client = Pool.pool(vertx, connectOptions, poolOptions);

        insertData("Jon Doe", "jondoe@example.com");
    }

    private void insertData(String name, String email) {
        String query = "INSERT INTO users(name, email) VALUES ($1, $2) RETURNING id";

        client.preparedQuery(query)
                .execute(Tuple.of(name, email))
                .onSuccess(rowSet -> {
                    System.out.println("Inserted with ID: " + rowSet.iterator().next().getInteger("id"));
                })
                .onFailure(err -> {
                    System.err.println("Insert failed: " + err.getMessage());
                });
    }

    public static void main(String[] args) {
        Launcher.executeCommand("run", PostgresInsertVerticle.class.getName());
    }
}

