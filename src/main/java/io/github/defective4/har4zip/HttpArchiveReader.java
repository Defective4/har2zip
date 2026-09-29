package io.github.defective4.har4zip;

import java.io.IOException;
import java.io.Reader;
import java.util.function.Consumer;

import com.google.gson.stream.JsonReader;

import io.github.defective4.har4zip.codec.model.BrowserInfo;
import io.github.defective4.har4zip.codec.model.Entry;
import io.github.defective4.har4zip.codec.model.HttpArchiveInfo;
import io.github.defective4.har4zip.codec.model.PageInfo;

public class HttpArchiveReader implements AutoCloseable {
    private final JsonReader reader;

    public HttpArchiveReader(Reader reader) {
        this.reader = new JsonReader(reader);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    public HttpArchiveInfo readHttpArchive(Consumer<Entry> entryCallback) throws IOException {
        reader.beginObject();
        if (!reader.nextName().equals("log")) throw new IOException("Expected \"log\" as the first object");
        reader.beginObject();

        BrowserInfo creator = null;
        BrowserInfo browser = null;
        PageInfo[] pages = new PageInfo[0];

        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case "creator" -> creator = BrowserInfo.CODEC.read(reader);
                case "browser" -> browser = BrowserInfo.CODEC.read(reader);
                case "pages" -> pages = PageInfo.CODEC.read(reader);
                case "entries" -> {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        Entry entry = Entry.CODEC.read(reader);
                        entryCallback.accept(entry);
                    }
                    reader.endArray();
                }
                default -> reader.skipValue();
            }
        }

        reader.endObject();
        reader.endObject();

        return new HttpArchiveInfo(creator, browser, pages);
    }
}
