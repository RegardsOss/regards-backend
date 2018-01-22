package fr.cnes.regards.modules.catalog.services.plugins;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.GsonBuilder;

public class CatalogPluginResponseFactory {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogPluginResponseFactory.class);

    private static final String INLINE_FILE_CONTENT_DISPOSITION_FORMAT = "inline; filename=sampleServiceResults.%s";

    public enum CatalogPluginResponseType {
        XML,
        JSON,
        FILE_IMG_PNG,
        FILE_IMG_JPG,
        FILE_DOWNLOAD;
    }

    private static String getFileName(String extension) {
        return String.format(INLINE_FILE_CONTENT_DISPOSITION_FORMAT, extension);
    }

    public static ResponseEntity<StreamingResponseBody> createStreamSuccessResponse(HttpServletResponse response,
            StreamingResponseBody responseContent, String fileName) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, fileName);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        return new ResponseEntity<>(responseContent, headers, HttpStatus.OK);
    }

    public static ResponseEntity<StreamingResponseBody> createSuccessResponse(HttpServletResponse response,
            CatalogPluginResponseType type, Object responseContent) {
        HttpHeaders headers = new HttpHeaders();
        GsonBuilder builder = new GsonBuilder();
        switch (type) {
            case XML:
                XmlMapper xmlMapper = new XmlMapper();
                try {
                    String xml = xmlMapper.writeValueAsString(responseContent);
                    headers.set(HttpHeaders.CONTENT_DISPOSITION, getFileName("xml"));
                    headers.setContentType(MediaType.APPLICATION_XML);
                    response.setContentType(MediaType.APPLICATION_XML_VALUE);
                    return new ResponseEntity<>(toStreamingResponseBody(xml), headers, HttpStatus.OK);
                } catch (JsonProcessingException e) {
                    LOGGER.error("Error serializing object to xml", e);
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            case JSON:
                headers.set(HttpHeaders.CONTENT_DISPOSITION, getFileName("json"));
                headers.setContentType(MediaType.APPLICATION_JSON);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                return new ResponseEntity<>(toStreamingResponseBody(builder.create().toJson(responseContent)), headers,
                        HttpStatus.OK);
            default:
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    public static ResponseEntity<StreamingResponseBody> createSuccessResponseFromFile(HttpServletResponse response,
            CatalogPluginResponseType type, File file) {
        HttpHeaders headers = new HttpHeaders();
        switch (type) {
            case FILE_IMG_PNG:
                headers.setContentType(MediaType.IMAGE_PNG);
                response.setContentType(MediaType.IMAGE_PNG_VALUE);
                headers.set(HttpHeaders.CONTENT_DISPOSITION, file.getName());
                break;
            case FILE_DOWNLOAD:
                headers.set(HttpHeaders.CONTENT_DISPOSITION, file.getName());
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                break;
            case FILE_IMG_JPG:
            default:
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(toStreamingResponseBody(file), headers, HttpStatus.OK);
    }

    private static StreamingResponseBody toStreamingResponseBody(String value) {
        return outputStream -> outputStream.write(value.getBytes());
    }

    private static StreamingResponseBody toStreamingResponseBody(File file) {
        return outputStream -> {
            Path path = file.toPath();
            Files.copy(path, outputStream);
            outputStream.flush();
        };
    }

}
