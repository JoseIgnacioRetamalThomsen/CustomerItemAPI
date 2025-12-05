package com.billy.api;

import com.billy.common.JsonUtils;
import com.billy.factory.ObjectMapperFactory;
import com.billy.objects.Item;
import com.billy.objects.ItemUpdateRequest;
import com.billy.objects.Response;
import com.billy.service.ItemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.billy.api.HandlersConstants.BAD_FORMAT_IN_REQUEST;
import static com.billy.api.HandlersConstants.DELETING_WITH_ID;
import static com.billy.api.HandlersConstants.EXCEPTION_SENDING_RESPONSE;
import static com.billy.api.HandlersConstants.FAILED_TO_STREAM;
import static com.billy.api.HandlersConstants.INTERNAL_SERVER_ERROR;
import static com.billy.api.HandlersConstants.INVALID_OR_MISSING_REQUEST_BODY;
import static com.billy.api.HandlersConstants.MISSING_OR_INVALID_ID;
import static com.billy.api.HandlersConstants.NOT_FOUND_ID_D;
import static com.billy.common.HandlerUtils.async;
import static com.billy.common.HandlerUtils.parseIdFromRequest;
import static com.billy.common.HandlerUtils.parseRequest;
import static com.billy.common.HandlerUtils.parseRequestForUpdate;
import static com.billy.common.HandlerUtils.sendErrorResponse;
import static com.billy.common.HandlerUtils.sendResponse;

/**
 * HTTP handler for managing {@link Item} entities.
 * Provides REST-style endpoints for CRUD operations.
 */

public class ItemHandler {
    private static final Logger logger = Logger.getLogger(ItemHandler.class.getName());
    public static final String MISSING_REQUIRED_FIELD_NAME = "Missing required field: name";
    private final ItemService itemService;

    public ItemHandler(ItemService itemService) {
        this.itemService = itemService;
    }

    /**
     * Streams all items as a JSON array.
     * <p>
     * Responses:
     * <ul>
     *   <li>200 OK – Returns a JSON array of all items.</li>
     *   <li>500 Internal Server Error – For unexpected errors.</li>
     * </ul>
     *
     * @param exchange the HTTP exchange containing request and response data
     */
    public void getAllItems(HttpServerExchange exchange) {
        async(exchange, () -> {
            final ObjectMapper mapper = ObjectMapperFactory.get();
            exchange.setStatusCode(StatusCodes.OK);
            Iterator<Item> it = itemService.iteratorAllItems();

            exchange.getResponseSender().send("[", new IoCallback() {
                boolean shouldSendObjectNext = true;

                @Override
                public void onComplete(HttpServerExchange ex, Sender sender) {
                    try {
                        if (!it.hasNext()) {
                            sender.send("]", IoCallback.END_EXCHANGE);
                            return;
                        }
                        if (!shouldSendObjectNext) {
                            sender.send(",", this);
                            shouldSendObjectNext = true;
                            return;
                        }
                        shouldSendObjectNext = false;
                        Item item = it.next();
                        byte[] json = mapper.writeValueAsBytes(item);
                        sender.send(ByteBuffer.wrap(json), this);

                    } catch (Exception e) {
                        logger.log(Level.SEVERE, FAILED_TO_STREAM, e);
                        safeAbort(ex);
                    }
                }

                @Override
                public void onException(HttpServerExchange ex, Sender sender, IOException e) {
                    logger.log(Level.SEVERE, EXCEPTION_SENDING_RESPONSE, e);
                    safeAbort(ex);
                }

                private void safeAbort(HttpServerExchange ex) {
                    try {
                        ex.endExchange();
                    } catch (Exception ignored) {
                    }
                }
            });
        }, logger);
    }

    /**
     * Retrieves a single item by its ID from the path or query parameter.
     * <p>
     * Responses:
     * <ul>
     *   <li>200 OK – Returns the item object as JSON.</li>
     *   <li>400 Bad Request – If the ID is missing or invalid.</li>
     *   <li>404 Not Found – If no item exists with the given ID.</li>
     *   <li>500 Internal Server Error – For unexpected errors.</li>
     * </ul>
     *
     * @param exchange the HTTP exchange containing request and response data
     */
    public void getItemById(final HttpServerExchange exchange) {
        async(exchange, () -> {
            final OptionalLong idLongOptional = parseIdFromRequest(exchange);
            if (idLongOptional.isEmpty()) {
                sendErrorResponse(exchange, StatusCodes.BAD_REQUEST, MISSING_OR_INVALID_ID);
                return;
            }
            final long idToFind = idLongOptional.getAsLong();
            final Optional<Item> itemWithId = itemService.getItemById(idToFind);
            if (itemWithId.isEmpty()) {
                sendErrorResponse(exchange, StatusCodes.NOT_FOUND,
                        String.format(NOT_FOUND_ID_D, idToFind), idToFind);
                return;
            }
            final String itemJson = JsonUtils.toJson(itemWithId.get());
            sendResponse(exchange, StatusCodes.OK, itemJson);
        }, logger);
    }

    /**
     * Creates a new item from the JSON request body.
     * <p>
     * Responses:
     * <ul>
     *   <li>200 OK – Returns the created item object as JSON.</li>
     *   <li>400 Bad Request – If required fields are missing or request body is invalid.</li>
     *   <li>500 Internal Server Error – For unexpected errors.</li>
     * </ul>
     *
     * @param exchange the HTTP exchange containing request and response data
     */
    public void createItem(HttpServerExchange exchange) {
        async(exchange, () -> {
            exchange.getRequestReceiver().receiveFullBytes((ex, data) -> {
                parseRequest(data, Item.class).ifPresentOrElse(item -> {
                    if (item.name() == null || item.name().isBlank()) {
                        sendErrorResponse(exchange, StatusCodes.BAD_REQUEST, MISSING_REQUIRED_FIELD_NAME);
                        return;
                    }
                    itemService.createItem(item).ifPresentOrElse(savedItem -> {
                        String json = JsonUtils.toJson(savedItem);
                        sendResponse(ex, StatusCodes.CREATED, json);
                    }, () -> sendErrorResponse(ex, StatusCodes.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR));

                }, () -> sendErrorResponse(exchange, StatusCodes.BAD_REQUEST, BAD_FORMAT_IN_REQUEST));
            });
        }, logger);
    }

    /**
     * Deletes an item by its ID from the path or query parameter.
     * <p>
     * Responses:
     * <ul>
     *   <li>200 OK – Returns the deleted item ID as JSON.</li>
     *   <li>400 Bad Request – If the ID is missing or invalid.</li>
     *   <li>404 Not Found – If no item exists with the given ID.</li>
     *   <li>500 Internal Server Error – For unexpected errors.</li>
     * </ul>
     *
     * @param exchange the HTTP exchange containing request and response data
     */
    public void deleteItem(HttpServerExchange exchange) {
        async(exchange, ()->{
                OptionalLong idOpt = parseIdFromRequest(exchange);
                idOpt.ifPresentOrElse(id -> {
                    if (logger.getLevel() == Level.FINE) {
                        logger.fine(String.format(DELETING_WITH_ID, id));
                    }
                    boolean deleted = itemService.deleteItem(id);
                    if (deleted) {
                        sendResponse(exchange, StatusCodes.OK, JsonUtils.toJson(Response.success(id)));
                    } else {
                        sendErrorResponse(exchange, StatusCodes.NOT_FOUND,
                                String.format(NOT_FOUND_ID_D, id), id);
                    }
                }, () -> sendErrorResponse(exchange, StatusCodes.BAD_REQUEST, MISSING_OR_INVALID_ID));
        },logger);
    }

    /**
     * Updates an existing item by its ID using the JSON request body.
     * <p>
     * Responses:
     * <ul>
     *   <li>200 OK – Returns the updated item object as JSON.</li>
     *   <li>400 Bad Request – If the ID is missing/invalid or request body is invalid.</li>
     *   <li>404 Not Found – If no item exists with the given ID.</li>
     *   <li>500 Internal Server Error – For unexpected errors.</li>
     * </ul>
     *
     * @param exchange the HTTP exchange containing request and response data
     */
    public void updateItem(HttpServerExchange exchange) {
        async(exchange, ()->{
            exchange.getRequestReceiver().receiveFullBytes((ex, data) -> {
                    OptionalLong idOpt = parseIdFromRequest(exchange);
                    idOpt.ifPresentOrElse(id -> {
                        parseRequestForUpdate(data, ItemUpdateRequest.class).ifPresentOrElse(itemUpdateRequest -> {
                            itemService.updateItem(id, itemUpdateRequest)
                                    .ifPresentOrElse(savedItem -> {
                                        sendResponse(exchange, StatusCodes.OK, JsonUtils.toJson(savedItem));
                                    }, () -> sendErrorResponse(exchange, StatusCodes.NOT_FOUND,
                                            String.format(NOT_FOUND_ID_D, id), id));
                        }, () -> sendErrorResponse(exchange, StatusCodes.BAD_REQUEST, INVALID_OR_MISSING_REQUEST_BODY));
                    }, () -> sendErrorResponse(exchange, StatusCodes.BAD_REQUEST, MISSING_OR_INVALID_ID));
            });
        },logger);
    }

}
