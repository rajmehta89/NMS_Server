package org.nmssever.util;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * The type Response util.
 */
public class ResponseUtil {

    /**
     * Send a success response with the given status code and body
     *
     * @param ctx        The routing context
     * @param statusCode The HTTP status code
     * @param body       The response body
     */
    public static void sendSuccessResponse(RoutingContext ctx, int statusCode, Object body) {

        ctx.response()
                .setStatusCode(statusCode)
                .putHeader(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
                .end(body.toString());

    }

    /**
     * Send an error response with the given status code and error message
     *
     * @param ctx          The routing context
     * @param statusCode   The HTTP status code
     * @param errorMessage The error message
     */
    public static void sendErrorResponse(RoutingContext ctx, int statusCode, String errorMessage) {

        ctx.response()
                .setStatusCode(statusCode)
                .putHeader(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
                .end(new JsonObject().put("error", errorMessage).encodePrettily());

    }

    /**
     * Send success.
     *
     * @param message the message
     * @param data    the data
     */
    public static void sendSuccess(Message<?> message, Object data) {

        var response = data;

        message.reply(response);

    }

    /**
     * Send error.
     *
     * @param message  the message
     * @param code     the code
     * @param errorMsg the error msg
     */
    public static void sendError(Message<?> message, int code, String errorMsg) {

        var response = new JsonObject().put("error", errorMsg);

        message.fail(code, response.encode());

    }

}