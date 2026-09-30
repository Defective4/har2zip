package io.github.defective4.har2zip;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import io.github.defective4.har2zip.codec.model.Entry.Response;
import io.github.defective4.har2zip.codec.model.HttpArchiveInfo;
import io.github.defective4.har2zip.codec.model.PageInfo;

public class Main {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Option HELP_OPTION = Option.builder("h").desc("Show this help").longOpt("help").get();
    private static final Options OPTIONS;

    private static final Option REQ_METADATA_OPTION = Option.builder("m").desc("Write request metadata")
            .longOpt("request-metadata").get();

    static {
        OPTIONS = new Options().addOption(HELP_OPTION).addOption(REQ_METADATA_OPTION);
    }

    public static void main(String[] args) throws IOException {
        try {
            CommandLine cli = new DefaultParser().parse(OPTIONS, args);
            if (cli.hasOption(HELP_OPTION)) {
                printHelp(null);
                return;
            }
            String[] subargs = cli.getArgs();

            if (subargs.length < 2) {
                printHelp("Missing parameters");
                System.exit(1);
                return;
            }

            try (HttpArchiveReader reader = new HttpArchiveReader(new FileReader(subargs[0]));
                    ZipOutputStream output = new ZipOutputStream(
                            Files.newOutputStream(Path.of(subargs[1]), StandardOpenOption.CREATE))) {
                List<String> paths = new ArrayList<>();

                Map<String, JsonObject> metadata = new HashMap<>();
                boolean writeReqMetadata = cli.hasOption(REQ_METADATA_OPTION);

                System.err.println("Reading HAR file...");
                HttpArchiveInfo info = reader.readHttpArchive(entry -> {
                    String file;
                    try {
                        try {
                            file = entry.request().url().toURL().getFile();
                        } catch (MalformedURLException ex) {
                            file = "/" + URLEncoder.encode(entry.request().url().toString(), StandardCharsets.UTF_8);
                        }
                        int index = file.indexOf('?');
                        if (index > 0) {
                            file = file.substring(0, index);
                        }
                        if (file.endsWith("/")) file += "index.html";

                        String path = entry.urlEncodedPageref() + file;
                        if (paths.contains(path)) {
                            String np;
                            int i = 1;
                            do {
                                np = path + " (%s)".formatted(i++);
                            } while (paths.contains(np));
                            path = np;
                        }

                        output.putNextEntry(new ZipEntry(path));
                        output.write(entry.decodeContent());
                        output.closeEntry();
                        paths.add(path);

                        if (writeReqMetadata) {
                            JsonObject root = metadata.computeIfAbsent(entry.urlEncodedPageref(),
                                    t -> new JsonObject());

                            JsonObject req = new JsonObject();
                            req.addProperty("path", entry.request().url().toString());
                            JsonObject responseObj = new JsonObject();

                            Response response = entry.response();
                            responseObj.addProperty("status", response.status());
                            responseObj.addProperty("mime", response.content().mimeType());

                            req.add("response", responseObj);

                            root.add(path, req);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                System.err.println("Writing site summary...");
                output.putNextEntry(new ZipEntry("/pages.json"));
                JsonObject root = new JsonObject();
                for (PageInfo page : info.pages()) {
                    root.addProperty(page.id(), page.title());
                }

                output.write(GSON.toJson(root).getBytes(StandardCharsets.UTF_8));
                output.closeEntry();

                if (writeReqMetadata) {
                    System.err.println("Writing requests metadata...");
                    metadata.forEach((page, obj) -> {
                        try {
                            output.putNextEntry(new ZipEntry("%s.json".formatted(page)));
                            output.write(GSON.toJson(obj).getBytes(StandardCharsets.UTF_8));
                            output.closeEntry();
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.exit(2);
                        }
                    });
                }
                System.err.println("All done!");
                System.err.println("Result archive saved to %s".formatted(subargs[1]));
            }

        } catch (ParseException e) {
            printHelp(e.getMessage());
            System.exit(1);
        }
    }

    private static void printHelp(String message) throws IOException {
        if (message != null) System.err.println(message);
        String name = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getName();
        HelpFormatter.builder().setShowSince(false).get().printHelp(
                "%s [options...] [input har file] [output zip file]".formatted(name), null, OPTIONS, null, false);
    }
}
