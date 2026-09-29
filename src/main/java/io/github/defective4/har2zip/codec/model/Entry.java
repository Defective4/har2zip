package io.github.defective4.har2zip.codec.model;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;

import io.github.defective4.har2zip.codec.Codec;
import io.github.defective4.har2zip.codec.model.Entry.Response.Content;

public record Entry(LocalDateTime startedDateTime, Response response, String serverIPAddress, String connection,
        String pageref, Request request) {

    public String urlEncodedPageref() {
        return URLEncoder.encode(pageref(), StandardCharsets.UTF_8);
    }

    public byte[] decodeContent() {
        Content content = response.content();
        if (content == null || content.text() == null) return new byte[0];
        if ("base64".equals(content.encoding())) return Base64.getDecoder().decode(content.text());
        return content.text().getBytes(StandardCharsets.UTF_8);
    }

    public record Request(URI url, String method) {
        public static Codec<Request> CODEC = reader -> {
            reader.beginObject();

            URI url = null;
            String method = "GET";

            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case "url" -> {
                        try {
                            url = URI.create(reader.nextString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    case "method" -> method = reader.nextString();
                    default -> reader.skipValue();
                }
            }
            reader.endObject();
            return new Request(url, method);
        };
    }

    public record Response(int status, Content content) {
        public record Content(String mimeType, long size, String encoding, String text) {
            public static final Codec<Content> CODEC = reader -> {
                reader.beginObject();

                String mimeType = null;
                long size = 0;
                String text = null;
                String encoding = null;

                while (reader.hasNext()) {
                    String name = reader.nextName();
                    switch (name) {
                        case "mimeType" -> mimeType = reader.nextString();
                        case "size" -> size = reader.nextLong();
                        case "text" -> text = reader.nextString();
                        case "encoding" -> encoding = reader.nextString();
                        default -> reader.skipValue();
                    }
                }
                reader.endObject();
                return new Content(mimeType, size, encoding, text);
            };
        }

        public static final Codec<Response> CODEC = reader -> {
            reader.beginObject();

            int status = -1;
            Content content = null;

            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case "content" -> content = Content.CODEC.read(reader);
                    case "status" -> status = reader.nextInt();
                    default -> reader.skipValue();
                }
            }
            reader.endObject();
            return new Response(status, content);
        };
    }

    public static final Codec<Entry> CODEC = reader -> {
        reader.beginObject();

        LocalDateTime startedDateTime = null;
        Response response = null;
        String serverIPAddress = null;
        String connection = null;
        String pageref = null;
        Request request = null;

        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "request" -> request = Request.CODEC.read(reader);
                case "startedDateTime" -> {
                    try {
                        startedDateTime = LocalDateTime
                                .from(DateTimeFormatter.ISO_DATE_TIME.parse(reader.nextString()));
                    } catch (DateTimeParseException e) {
                        e.printStackTrace();
                    }
                }
                case "response" -> response = Response.CODEC.read(reader);
                case "serverIPAddress" -> serverIPAddress = reader.nextString();
                case "connection" -> connection = reader.nextString();
                case "pageref" -> pageref = reader.nextString();
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        return new Entry(startedDateTime, response, serverIPAddress, connection, pageref, request);
    };
}
