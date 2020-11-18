package fr.cnes.regards.modules.indexer.dao.mapping.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.vavr.control.Try;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.client.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public class Query {

    private static final Logger LOGGER = LoggerFactory.getLogger(Query.class);
    static final JsonParser PARSER = new JsonParser();

    private final String name;
    private final String method;
    private final String pathTemplate;
    private final JsonObject entity;

    private Query(String name, String method, String pathTemplate, JsonObject entity) {
        this.name = name;
        this.method = method;
        this.pathTemplate = pathTemplate;
        this.entity = entity;
    }

    public static Try<Query> fromFile(String name) {
        return Try.of(() -> {
            String filePath = "mapping/queries/" + name + ".json";
            InputStream is = ClassLoader.getSystemResourceAsStream(filePath);
            String content = IOUtils.toString(is, "UTF-8");
            String firstLine = content.split("\n")[0];
            String method = firstLine.split(" ")[0];
            String path = firstLine.replaceFirst("^\\S+\\s", "");
            String rest = content.replaceFirst("^.*\n", "");
            return new Query(name, method, path, PARSER.parse(rest).getAsJsonObject());
        })
        .onFailure(t -> LOGGER.error(t.getMessage(), t));
    }

    public Request toRequestForIndex(String index) {
        Request request = new Request(method, pathTemplate.replace("%index", index));
        request.setJsonEntity(entity.toString());
        return request;
    }

    public String getName() {
        return name;
    }

    public String getMethod() {
        return method;
    }

    public String getPathTemplate() {
        return pathTemplate;
    }

    public JsonObject getEntity() {
        return entity;
    }
}
