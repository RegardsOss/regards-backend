package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.exception;

import java.util.List;

import org.springframework.http.MediaType;

@SuppressWarnings("serial")
public class UnsupportedMediaTypesException extends Exception {

    private final List<MediaType> mediaTypes;

    public UnsupportedMediaTypesException(List<MediaType> mediaTypes) {
        this.mediaTypes = mediaTypes;
    }

    @Override
    public String getMessage() {
        String mediaTypesStr = mediaTypes.stream().reduce("", (r, m) -> String.format("%s, %s", r, m.getType()),
                                                          (s1, s2) -> String.format("%s, %s", s1, s2));
        return String.format("Unsupported media type %s", mediaTypesStr);
    }

}
