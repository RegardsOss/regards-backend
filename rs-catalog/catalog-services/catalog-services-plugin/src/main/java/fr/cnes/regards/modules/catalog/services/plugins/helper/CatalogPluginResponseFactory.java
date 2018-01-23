/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.catalog.services.plugins.helper;

import java.io.File;
import java.io.InputStream;
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
import com.google.common.io.ByteStreams;
import com.google.gson.GsonBuilder;

/**
 * Factory to handle CatalogPlugin services streaming response.
 * @author Sébastien Binda
 */
public class CatalogPluginResponseFactory {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogPluginResponseFactory.class);

    /**
     * CONTENT_DISPOSITION for streaming response body.
     */
    private static final String INLINE_FILE_CONTENT_DISPOSITION_FORMAT = "inline; filename=sampleServiceResults.%s";

    /**
     * Catalog plugin service response body type allowed
     * @author Sébastien Binda
     */
    public enum CatalogPluginResponseType {
        XML,
        JSON,
        FILE_IMG_PNG,
        FILE_IMG_JPG,
        FILE_DOWNLOAD;
    }

    /**
     * Create a success response with the given {@link StreamingResponseBody}
     * @param response {@link HttpServletResponse} spring http response
     * @param responseContent {@link StreamingResponseBody}  response body
     * @param fileName {@link String} content body file name
     * @return {@link ResponseEntity}
     */
    public static ResponseEntity<StreamingResponseBody> createStreamSuccessResponse(HttpServletResponse response,
            StreamingResponseBody responseContent, String fileName) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, fileName);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        return new ResponseEntity<>(responseContent, headers, HttpStatus.OK);
    }

    /**
     * Create a success response by serializing the given object responseContent
     * @param response {@link HttpServletResponse} spring http response
     * @param type {@link CatalogPluginResponseType} type of response body
     * @param responseContent {@link Object} to serialize.
     * @return {@link ResponseEntity}
     */
    public static ResponseEntity<StreamingResponseBody> createSuccessResponse(HttpServletResponse response,
            CatalogPluginResponseType type, Object responseContent) {
        switch (type) {
            case XML:
                return createXmlSuccessResponse(response, responseContent);
            case JSON:
                return createJsonSuccessResponse(response, responseContent);
            default:
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Create a success response by streaming the given file
     * @param response {@link HttpServletResponse} spring http response
     * @param type {@link CatalogPluginResponseType} type of response body
     * @param file {@link File} file to stream into the response body
     * @return {@link ResponseEntity}
     */
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
                headers.setContentType(MediaType.IMAGE_JPEG);
                response.setContentType(MediaType.IMAGE_JPEG_VALUE);
                headers.set(HttpHeaders.CONTENT_DISPOSITION, file.getName());
                break;
            default:
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(toStreamingResponseBody(file), headers, HttpStatus.OK);
    }

    /**
     * Create a success response by streaming the given file
     * @param response {@link HttpServletResponse} spring http response
     * @param type {@link CatalogPluginResponseType} type of response body
     * @param file {@link File} file to stream into the response body
     * @return {@link ResponseEntity}
     */
    public static ResponseEntity<StreamingResponseBody> createSuccessResponseFromInputStream(
            HttpServletResponse response, CatalogPluginResponseType type, InputStream is, String fileName) {
        HttpHeaders headers = new HttpHeaders();
        switch (type) {
            case FILE_IMG_PNG:
                headers.setContentType(MediaType.IMAGE_PNG);
                response.setContentType(MediaType.IMAGE_PNG_VALUE);
                headers.set(HttpHeaders.CONTENT_DISPOSITION, fileName);
                break;
            case FILE_DOWNLOAD:
                headers.set(HttpHeaders.CONTENT_DISPOSITION, fileName);
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                break;
            case FILE_IMG_JPG:
                headers.setContentType(MediaType.IMAGE_JPEG);
                response.setContentType(MediaType.IMAGE_JPEG_VALUE);
                headers.set(HttpHeaders.CONTENT_DISPOSITION, fileName);
                break;
            default:
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(toStreamingResponseBody(is), headers, HttpStatus.OK);
    }

    /**
     * Create a  {@link ResponseEntity<StreamingResponseBody>} by serializing into XML format the given object.
     * @param response {@link HttpServletResponse} spring http response
     * @param responseContent {@link Object} to serialize.
     * @return {@link ResponseEntity}
     */
    public static ResponseEntity<StreamingResponseBody> createXmlSuccessResponse(HttpServletResponse response,
            Object responseContent) {
        HttpHeaders headers = new HttpHeaders();
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
    }

    /**
     * Create a  {@link ResponseEntity<StreamingResponseBody>} by serializing into JSON format the given object.
     * @param response {@link HttpServletResponse} spring http response
     * @param responseContent {@link Object} to serialize.
     * @return {@link ResponseEntity}
     */
    public static ResponseEntity<StreamingResponseBody> createJsonSuccessResponse(HttpServletResponse response,
            Object responseContent) {
        GsonBuilder builder = new GsonBuilder();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, getFileName("json"));
        headers.setContentType(MediaType.APPLICATION_JSON);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        return new ResponseEntity<>(toStreamingResponseBody(builder.create().toJson(responseContent)), headers,
                HttpStatus.OK);
    }

    /**
     * Create a {@link StreamingResponseBody} containing the given String
     * @param value {@link String} to stream as response body
     * @return {@link StreamingResponseBody}
     */
    public static StreamingResponseBody toStreamingResponseBody(String value) {
        return outputStream -> outputStream.write(value.getBytes());
    }

    /**
     * Create a {@link StreamingResponseBody} containing the given file
     * @param value {@link File} to stream as response body
     * @return {@link StreamingResponseBody}
     */
    public static StreamingResponseBody toStreamingResponseBody(File file) {
        return outputStream -> {
            Path path = file.toPath();
            Files.copy(path, outputStream);
            outputStream.flush();
        };
    }

    public static StreamingResponseBody toStreamingResponseBody(InputStream file) {
        return outputStream -> {
            ByteStreams.copy(file, outputStream);
            outputStream.flush();
        };
    }

    /**
     * Default response body attachement file name with the given extension
     * @param extension
     * @return {@link String} file name for attachments.
     */
    private static String getFileName(String extension) {
        return String.format(INLINE_FILE_CONTENT_DISPOSITION_FORMAT, extension);
    }

}
