package com.billy.common;

import com.billy.objects.Response;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.PathTemplateMatch;

import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.billy.api.HandlersConstants.FAILED_TO_STREAM;
import static com.billy.api.HandlersConstants.INTERNAL_SERVER_ERROR;

public class HandlerUtils {
    public static final String ID = "id";
    public static void sendResponse(HttpServerExchange exchange, int status, String message) {
        exchange.setStatusCode(status);
        exchange.getResponseSender().send(message);
    }

    public static void sendErrorResponse(HttpServerExchange exchange, int statusCode, String message) {
        sendResponse(exchange, statusCode, JsonUtils.toJson(Response.error(message)));
    }

    public static void sendErrorResponse(HttpServerExchange exchange, int statusCode, String message, long id) {
        sendResponse(exchange, statusCode, JsonUtils.toJson(Response.error(message, id)));
    }

    public  static <T> Optional<T> parseRequest(byte[] data, Class<T> clazz) {
        try {
            T obj = JsonUtils.fromJson(data, clazz);
            return Optional.of(obj);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static OptionalLong parseIdFromRequest(HttpServerExchange exchange) {
        PathTemplateMatch match = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
        if (match == null) {
            return OptionalLong.empty();
        }
        String idStr = match.getParameters().get(ID);
        if (idStr == null || idStr.isEmpty()) {
            return OptionalLong.empty();
        }
        try {
            return OptionalLong.of(Long.parseLong(idStr));
        } catch (NumberFormatException e) {
            return OptionalLong.empty();
        }
    }

    public static <T> Optional<T> parseRequestForUpdate(byte[] data, Class<T> clazz) {
        try {
            T customer = JsonUtils.fromJson(data, clazz);
            return Optional.of(customer);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static void async(HttpServerExchange ex, Runnable task, Logger logger) {
        ex.dispatch(() -> {
            try {
                task.run();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Unhandled handler exception", e);
                sendErrorResponse(ex, 500, INTERNAL_SERVER_ERROR);
                try {
                    ex.endExchange();
                } catch (Exception ignore) {}
            }
        });
    }

}
