package io.github.defective4.har4zip.codec;

import java.io.IOException;

import com.google.gson.stream.JsonReader;

public interface Codec<T> {
    T read(JsonReader reader) throws IOException;
}
