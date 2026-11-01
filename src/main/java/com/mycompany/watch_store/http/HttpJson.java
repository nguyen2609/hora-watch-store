package com.mycompany.watch_store.http;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.sun.net.httpserver.HttpExchange;

public final class HttpJson {
    public static final int MAX_BODY_BYTES = 65536;
    private static final Gson GSON = new GsonBuilder().serializeNulls().setStrictness(Strictness.STRICT).create();

    private HttpJson() {
    }

    public record Body<T>(T value, Set<String> fields) {
        public Body {
            fields = Set.copyOf(fields);
        }
    }

    /** Request DTOs are flat records. Reject unknown, duplicate, nested and incorrectly typed fields. */
    public static <T> Body<T> read(HttpExchange exchange, Class<T> type, Set<String> required) throws IOException {
        requireJson(exchange);
        String text = bodyText(exchange);
        Map<String, Class<?>> types = new HashMap<>();
        for (var field : type.getRecordComponents()) {
            types.put(field.getName(), field.getType());
        }
        JsonObject object = new JsonObject();
        Set<String> seen = new HashSet<>();
        try (JsonReader reader = new JsonReader(new StringReader(text))) {
            reader.setStrictness(Strictness.STRICT);
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                Class<?> fieldType = types.get(name);
                if (fieldType == null || !seen.add(name)) {
                    throw badRequest();
                }
                JsonToken token = reader.peek();
                if (token == JsonToken.NULL) {
                    reader.nextNull();
                    if (required.contains(name) || !(name.equals("phone") || name.equals("description")
                            || name.equals("imageUrl") || name.equals("movementType") || name.equals("batteryLifeHours"))) {
                        throw badRequest();
                    }
                    object.add(name, null);
                } else if (fieldType == String.class && token == JsonToken.STRING) {
                    object.addProperty(name, reader.nextString());
                } else if (fieldType == Boolean.class && token == JsonToken.BOOLEAN) {
                    object.addProperty(name, reader.nextBoolean());
                } else if ((fieldType == Integer.class || fieldType == BigDecimal.class) && token == JsonToken.NUMBER) {
                    String number = reader.nextString();
                    if (number.length() > 64) {
                        throw badRequest();
                    }
                    BigDecimal value = new BigDecimal(number);
                    if (fieldType == Integer.class) {
                        value.intValueExact();
                    }
                    // Bound exponent/scale to prevent tiny inputs causing enormous money output.
                    if (Math.abs((long) value.scale()) > 12 || value.precision() > 24) {
                        throw badRequest();
                    }
                    object.add(name, new JsonPrimitive(value));
                } else {
                    throw badRequest();
                }
            }
            reader.endObject();
            if (reader.peek() != JsonToken.END_DOCUMENT || !seen.containsAll(required)) {
                throw badRequest();
            }
            return new Body<>(GSON.fromJson(object, type), seen);
        } catch (ApiException exception) {
            throw exception;
        } catch (IOException | IllegalArgumentException | IllegalStateException | ArithmeticException exception) {
            throw badRequest();
        }
    }

    public static void emptyBody(HttpExchange exchange) throws IOException {
        String text = bodyText(exchange).strip();
        if (!text.isEmpty()) {
            requireJson(exchange);
            try (JsonReader reader = new JsonReader(new StringReader(text))) {
                reader.setStrictness(Strictness.STRICT);
                reader.beginObject();
                if (reader.hasNext()) throw badRequest();
                reader.endObject();
                if (reader.peek() != JsonToken.END_DOCUMENT) throw badRequest();
            } catch (IOException | IllegalStateException exception) {
                throw badRequest();
            }
        }
    }

    public static void send(HttpExchange exchange, int status, Object dto) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        if (status == 204) {
            exchange.sendResponseHeaders(status, -1);
            return;
        }
        byte[] bytes = GSON.toJson(dto).getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    public static ApiException badRequest() {
        return new ApiException(400, "BAD_REQUEST", "Invalid JSON, missing fields, or invalid field values.");
    }

    private static void requireJson(HttpExchange exchange) {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.split(";", 2)[0].strip().equalsIgnoreCase("application/json")) {
            throw new ApiException(415, "UNSUPPORTED_MEDIA_TYPE", "Use application/json with UTF-8 encoding.");
        }
        for (String parameter : contentType.split(";")) {
            String[] pair = parameter.strip().split("=", 2);
            if (pair[0].equalsIgnoreCase("charset") && (pair.length != 2
                    || !pair[1].replace("\"", "").strip().equalsIgnoreCase("UTF-8"))) {
                throw new ApiException(415, "UNSUPPORTED_MEDIA_TYPE", "Use application/json with UTF-8 encoding.");
            }
        }
        String encoding = exchange.getRequestHeaders().getFirst("Content-Encoding");
        if (encoding != null && !encoding.equalsIgnoreCase("identity")) {
            throw new ApiException(415, "UNSUPPORTED_MEDIA_TYPE", "Encoded request bodies are not supported.");
        }
    }

    private static String bodyText(HttpExchange exchange) throws IOException {
        String length = exchange.getRequestHeaders().getFirst("Content-Length");
        if (length != null) {
            try {
                if (Long.parseLong(length) > MAX_BODY_BYTES) {
                    throw new ApiException(413, "BODY_TOO_LARGE", "Request body exceeds 64 KiB.");
                }
            } catch (NumberFormatException exception) {
                throw badRequest();
            }
        }
        byte[] bytes = exchange.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
        if (bytes.length > MAX_BODY_BYTES) {
            throw new ApiException(413, "BODY_TOO_LARGE", "Request body exceeds 64 KiB.");
        }
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException exception) {
            throw badRequest();
        }
    }
}
