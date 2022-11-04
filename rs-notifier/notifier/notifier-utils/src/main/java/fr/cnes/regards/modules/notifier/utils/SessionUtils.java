/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

package fr.cnes.regards.modules.notifier.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utils used to compute session information
 *
 * @author Thibaud Michaudel
 **/
public class SessionUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionUtils.class);

    /**
     * Pattern used to create the session name in case there was an error
     */
    public static final String SESSION_NAME_PATTERN_ERROR = "{sessionNamePatternError}-#day";

    /**
     * Path to sessionOwner property in metadata
     */
    public static final String SESSION_OWNER_METADATA_PATH = "sessionOwner";

    /**
     * Path to session property in metadata
     */
    public static final String SESSION_METADATA_PATH = "session";

    /**
     * Session owner token to use when using name from pattern
     */
    public static final String DEFAULT_SESSION_OWNER_TOKEN = "REGARDS-";

    private static final Configuration JSON_PATH_CONFIGURATION = Configuration.builder()
                                                                              .jsonProvider(new GsonJsonProvider())
                                                                              .options(Option.SUPPRESS_EXCEPTIONS)
                                                                              .build();

    private static final String WS_SESSION_PATTERN = "^\\{(.+)}-(#day)(.*)$";

    private static final String WS_SESSION_NAME_PATTERN_NAME = "sessionNamePattern";

    private SessionUtils() {
    }

    public static SessionNameAndOwner computeSessionNameAndOwner(NotificationRequest notificationRequest,
                                                                 String sessionNamePattern,
                                                                 String tenantName) {
        JsonObject feature = notificationRequest.getPayload();
        String sessionOwnerName;
        String sessionName;

        // if the session pattern is null fill sessionOwnerName and session with the request metadata
        // else use the pattern
        if (sessionNamePattern == null) {
            DocumentContext metadataContext = JsonPath.using(JSON_PATH_CONFIGURATION)
                                                      .parse(notificationRequest.getMetadata());
            sessionOwnerName = ((JsonPrimitive) metadataContext.read(SESSION_OWNER_METADATA_PATH)).getAsString();
            sessionName = ((JsonPrimitive) metadataContext.read(SESSION_METADATA_PATH)).getAsString();
        } else {
            sessionOwnerName = DEFAULT_SESSION_OWNER_TOKEN + tenantName;
            sessionName = getSessionNameFromPattern(feature, sessionNamePattern);
        }

        return new SessionNameAndOwner(sessionName, sessionOwnerName);
    }

    private static String getSessionNameFromPattern(JsonObject feature, String sessionNamePattern) {
        JsonElement featureType = null;
        String currentDate = OffsetDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        // check if the feature type can be retrieved from the WS_SESSION_PATTERN
        Pattern pattern = Pattern.compile(WS_SESSION_PATTERN);
        Matcher matcher = pattern.matcher(sessionNamePattern);
        if (matcher.find()) {
            // access the feature type from json path
            String jsonPathToAccessFeatureType = matcher.group(1);
            featureType = JsonPath.using(JSON_PATH_CONFIGURATION).parse(feature).read(jsonPathToAccessFeatureType);
        }

        // if the property was not found, replace the session with the default session name
        if (featureType == null || !featureType.isJsonPrimitive()) {
            String defaultSessionName = SESSION_NAME_PATTERN_ERROR.replaceAll(WS_SESSION_PATTERN,
                                                                              String.format("$1-%s", currentDate));
            LOGGER.warn("""
                            The pattern configured in {} "{}" has an invalid pattern.
                            Check if : 
                            - The RegExp is valid and follow the pattern {<jsonPathToAccessProductType>}-#day(.*)
                            - The JsonPath to access the feature type is valid.
                            The session will be named by default: "{}".
                            """, WS_SESSION_NAME_PATTERN_NAME, sessionNamePattern, defaultSessionName);
            return defaultSessionName;
        } else {
            return sessionNamePattern.replaceAll(WS_SESSION_PATTERN,
                                                 String.format("%s-%s$3", featureType.getAsString(), currentDate));
        }
    }
}
