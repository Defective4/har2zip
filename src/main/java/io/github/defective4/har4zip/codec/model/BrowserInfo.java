package io.github.defective4.har4zip.codec.model;

import io.github.defective4.har4zip.codec.Codec;

public record BrowserInfo(String name, String version) {
    public static final Codec<BrowserInfo> CODEC = reader -> {
        reader.beginObject();
        String name = null;
        String version = null;
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "name" -> name = reader.nextString();
                case "version" -> version = reader.nextString();
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        return new BrowserInfo(name, version);
    };
}
