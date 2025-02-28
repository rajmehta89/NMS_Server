import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class MainVerticle extends AbstractVerticle {

    @Override
    public void start() {

        ZContext context = new ZContext();

        ZMQ.Socket socket = context.createSocket(ZMQ.REQ);

        socket.connect("tcp://localhost:5555");

        JsonObject request = new JsonObject();

        request.put("ip", "10.20.41.58");

        request.put("username", "admin");

        request.put("password", "motadata");

        request.put("RequestType","provisioning");

        request.put("SystemType","windows");


        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {

                socket.send(request.encode());

                // Receive response from the server
                String jsonResponse = socket.recvStr();

                if (jsonResponse != null) {

//                    JsonObject json = new JsonObject(jsonResponse);

                    System.out.println("Received Response: " + jsonResponse);

                }


                try {
                    Thread.sleep(50000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle());
    }
}


//import io.vertx.core.*;
//import org.example.Verticles.*;
//
//public class MainVerticle extends AbstractVerticle {
//
//    @Override
//    public void start(Promise<Void> startPromise) {
//
//        Future<String> verticle1 = vertx.deployVerticle(new WorkerVerticle());
//
//        Future<String> verticle2 = vertx.deployVerticle(new HttpServerVerticle());
//
//        Future<String> verticle3 = vertx.deployVerticle(new DatabaseVerticle());
//
//        Future<String> verticle4 = vertx.deployVerticle(new EventBusVerticle());
//
//        Future<String> verticle5 = vertx.deployVerticle(new CredentialProfileVerticle());
//
//
//
//        CompositeFuture.all(verticle1, verticle2, verticle3, verticle4,verticle5).onComplete(ar -> {
//
//            if (ar.succeeded()) {
//
//                System.out.println("All Verticles deployed successfully!");
//
//                startPromise.complete();
//
//            } else {
//
//                System.err.println("Failed to deploy Verticles: " + ar.cause().getMessage());
//
//                startPromise.fail(ar.cause());
//
//            }
//
//        });
//    }
//
//
//    public static void main(String[] args) {
//
//        Vertx.vertx().deployVerticle(new MainVerticle());
//
//    }
//}
