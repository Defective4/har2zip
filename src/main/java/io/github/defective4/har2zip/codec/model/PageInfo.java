package io.github.defective4.har2zip.codec.model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import io.github.defective4.har2zip.codec.Codec;

public record PageInfo(String id, LocalDateTime startTime, String title) {
    public static final Codec<PageInfo[]> CODEC = reader -> {
        List<PageInfo> list = new ArrayList<>();
        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            String id = null;
            LocalDateTime startTime = null;
            String title = null;

            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case "id" -> id = reader.nextString();
                    case "title" -> title = reader.nextString();
                    case "startedDateTime" -> {
                        try {
                            startTime = LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(reader.nextString()));
                        } catch (DateTimeParseException e) {
                            e.printStackTrace();
                        }
                    }
                    default -> reader.skipValue();
                }
            }
            reader.endObject();
            list.add(new PageInfo(id, startTime, title));
        }
        reader.endArray();
        return list.toArray(new PageInfo[0]);
    };

    public String urlEncodedId() {
        return URLEncoder.encode(id, StandardCharsets.UTF_8);
    }

    public String urlEncodedTitle() {
        return URLEncoder.encode(title, StandardCharsets.UTF_8);
    }
}
