package com.billy.api;

import com.billy.common.JsonUtils;
import com.billy.factory.ObjectMapperFactory;
import com.billy.objects.Customer;
import com.billy.objects.CustomerUpdateRequest;
import com.billy.objects.Response;
import com.billy.service.CustomerService;
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
import static com.billy.app.AppConstants.MAX_REQUEST_LEN;
import static com.billy.common.HandlerUtils.async;
import static com.billy.common.HandlerUtils.parseIdFromRequest;
import static com.billy.common.HandlerUtils.parseRequest;
import static com.billy.common.HandlerUtils.parseRequestForUpdate;
import static com.billy.common.HandlerUtils.sendErrorResponse;
import static com.billy.common.HandlerUtils.sendResponse;


public class CustomerHandler {
    private static final Logger logger = Logger.getLogger(CustomerHandler.class.getName());
    public static final String MISSING_REQUIRED_FIELD_EMAIL = "Missing required field: email";
    public static final String EXCEPTION_AT_SAFE_ABORT = "Exception at safe abort";
    public static final String REQUEST_TOO_LARGE = "Request too large";
    private final CustomerService customerService;


    public CustomerHandler(CustomerService service) {
        this.customerService = service;
    }

    /**
     * Streams all customers as a JSON array to the HTTP client using non-blocking I/O.
     * Each user is sent individually to avoid loading the entire dataset into memory.
     * <p>
     * Responses:
     * <ul>
     *   <li>200 OK – Returns a JSON array of all users.</li>
     *   <li>500 Internal Server Error – If an error occurs while streaming.</li>
     * </ul>
     *
     * @param exchange the HTTP exchange containing request and response data
     */
    public void getAllCustomers(HttpServerExchange exchange) {
        async(exchange, () -> {
            final ObjectMapper mapper = ObjectMapperFactory.get();
            exchange.setStatusCode(StatusCodes.OK);
            Iterator<Customer> it = customerService.iteratorAllCustomers();

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


                        Customer c = it.next();
                        byte[] json = mapper.writeValueAsBytes(c);
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
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, EXCEPTION_AT_SAFE_ABORT, e);
                    }
                }
            });

        }, logger);
    }

    /**
     * Retrieves a single customer by their ID from the path parameter.
     * <p>
     * Responses:
     * <ul>
     *   <li>200 OK – Returns the customer object as JSON.</li>
     *   <li>400 Bad Request – If the ID is missing or invalid.</li>
     *   <li>404 Not Found – If no customer exists with the given ID.</li>
     *   <li>500 Internal Server Error – For unexpected errors.</li>
     * </ul>
     *
     * @param exchange the HTTP exchange containing request and response data
     */
    public void getCustomerById(final HttpServerExchange exchange) {
        async(exchange, () -> {

            final OptionalLong idLongOptional = parseIdFromRequest(exchange);
            if (idLongOptional.isEmpty()) {
                sendErrorResponse(exchange, StatusCodes.BAD_REQUEST, MISSING_OR_INVALID_ID);
                return;
            }
            final long idToFind = idLongOptional.getAsLong();
            final Optional<Customer> customerWithId = customerService.getCustomerById(idToFind);
            if (customerWithId.isEmpty()) {
                sendErrorResponse(exchange, StatusCodes.NOT_FOUND, String.format(NOT_FOUND_ID_D, idToFind), idToFind);
                return;
            }
            final String customerWithIdJson = JsonUtils.toJson(customerWithId.get());
            sendResponse(exchange, StatusCodes.OK, customerWithIdJson);

        }, logger);
    }


    /**
     * Creates a new customer from the request body.
     * <p>
     * Responses:
     * <ul>
     *   <li>200 OK – Returns the newly created customer object as JSON.</li>
     *   <li>400 Bad Request – If the request body is missing or badly formatted.</li>
     *   <li>500 Internal Server Error – If customer creation fails unexpectedly.</li>
     * </ul>
     *
     * @param exchange the HTTP exchange containing request and response data
     */
    public void createCustomer(HttpServerExchange exchange) {
        async(exchange, () -> {
            exchange.getRequestReceiver().receiveFullBytes((ex, data) -> {
                if (data.length > MAX_REQUEST_LEN) {
                    sendErrorResponse(ex, StatusCodes.REQUEST_ENTITY_TOO_LARGE, REQUEST_TOO_LARGE);
                    return;
                }
                parseRequest(data, Customer.class).ifPresentOrElse(customer -> {
                    if (customer.email() == null || customer.email().isBlank()) {
                        sendErrorResponse(exchange, StatusCodes.BAD_REQUEST, MISSING_REQUIRED_FIELD_EMAIL);
                        return;
                    }
                    customerService.createCustomer(customer)
                            .ifPresentOrElse(savedCustomer -> {
                                String json = JsonUtils.toJson(savedCustomer);
                                sendResponse(ex, StatusCodes.CREATED, json);
                            }, () -> sendErrorResponse(ex, StatusCodes.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR));
                }, () -> sendErrorResponse(ex, StatusCodes.BAD_REQUEST, BAD_FORMAT_IN_REQUEST));
            });
        }, logger);
    }

    /**
     * Deletes a customer identified by the ID in the path parameter.
     * <p>
     * Responses:
     * <ul>
     *   <li>200 OK – If the customer was successfully deleted.</li>
     *   <li>400 Bad Request – If the ID is missing or invalid.</li>
     *   <li>404 Not Found – If no customer exists with the given ID.</li>
     *   <li>500 Internal Server Error – For unexpected errors.</li>
     * </ul>
     *
     * @param exchange the HTTP exchange containing request and response data
     */
    public void deleteCustomer(HttpServerExchange exchange) {
        async(exchange, () -> {
            OptionalLong idOpt = parseIdFromRequest(exchange);
            idOpt.ifPresentOrElse(id -> {
                if (logger.getLevel() == Level.FINE) {
                    logger.fine(String.format(DELETING_WITH_ID, id));
                }
                boolean deleted = customerService.deleteCustomer(id);
                if (deleted) {
                    sendResponse(exchange, StatusCodes.OK, JsonUtils.toJson(Response.success(id)));
                } else {
                    sendErrorResponse(exchange, StatusCodes.NOT_FOUND,
                            String.format(NOT_FOUND_ID_D, id), id);
                }
            }, () -> sendErrorResponse(exchange, StatusCodes.BAD_REQUEST, MISSING_OR_INVALID_ID));

        }, logger);
    }

    /**
     * Updates a customer based on the request body.
     * <p>
     * Responses:
     * <ul>
     *   <li>200 OK – Returns the updated customer object as JSON.</li>
     *   <li>400 Bad Request – If the request body is invalid or missing.</li>
     *   <li>404 Not Found – If no customer exists with the given ID.</li>
     *   <li>500 Internal Server Error – For unexpected errors.</li>
     * </ul>
     *
     * @param exchange the HTTP exchange containing request and response data
     */
    public void updateCustomer(HttpServerExchange exchange) {
        async(exchange, () -> {
            exchange.getRequestReceiver().receiveFullBytes((ex, data) -> {

                OptionalLong idOpt = parseIdFromRequest(exchange);
                idOpt.ifPresentOrElse(id -> {
                    parseRequestForUpdate(data, CustomerUpdateRequest.class).ifPresentOrElse(customerUpdateRequest -> {
                        customerService.updateCustomer(id, customerUpdateRequest)
                                .ifPresentOrElse(savedCustomer -> {
                                    sendResponse(exchange, StatusCodes.OK, JsonUtils.toJson(savedCustomer));
                                }, () -> sendErrorResponse(exchange, StatusCodes.NOT_FOUND, String.format(NOT_FOUND_ID_D, id), id));
                    }, () -> sendErrorResponse(exchange, StatusCodes.BAD_REQUEST, INVALID_OR_MISSING_REQUEST_BODY));
                }, () -> sendErrorResponse(exchange, StatusCodes.BAD_REQUEST, MISSING_OR_INVALID_ID));
            });
        }, logger);
    }
}
