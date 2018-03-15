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
package fr.cnes.regards.framework.microservice.rest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import fr.cnes.regards.framework.gson.GsonBuilderFactory;
import fr.cnes.regards.framework.gson.adapters.ClassAdapter;
import fr.cnes.regards.framework.gson.strategy.SerializationExclusionStrategy;
import fr.cnes.regards.framework.microservice.manager.MicroserviceConfiguration;
import fr.cnes.regards.framework.module.manager.ConfigIgnore;
import fr.cnes.regards.framework.module.manager.IModuleConfigurationManager;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItemAdapter;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;

/**
 * Microservice configuration controller
 *
 * @author Marc Sordi
 */
@RestController
@RequestMapping(MicroserviceConfigurationController.TYPE_MAPPING)
public class MicroserviceConfigurationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicroserviceConfigurationController.class);

    public static final String TYPE_MAPPING = "/microservice/configuration";

    /**
     * Prefix for imported/exported filename
     */
    private static final String CONFIGURATION_FILE_PREFIX = "config-";

    /**
     * Suffix for imported/exported filename
     */
    private static final String CONFIGURATION_FILE_EXTENSION = ".json";

    @Value("${spring.application.name}")
    private String microserviceName;

    @Autowired
    private GsonBuilderFactory gsonBuilderFactory;

    private Gson configGson;

    private Gson configItemGson;

    @Autowired(required = false)
    private List<IModuleConfigurationManager> managers;

    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Export microservice configuration")
    public void exportConfiguration(HttpServletRequest request, HttpServletResponse response) throws ModuleException {

        String exportedFilename = CONFIGURATION_FILE_PREFIX + microserviceName + CONFIGURATION_FILE_EXTENSION;

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportedFilename + "\"");

        // Prepare data
        MicroserviceConfiguration microConfig = new MicroserviceConfiguration();
        microConfig.setMicroservice(microserviceName);
        if ((managers != null) && !managers.isEmpty()) {
            for (IModuleConfigurationManager manager : managers) {
                microConfig.addModule(manager.exportConfiguration());
            }
        }

        // Stream data
        try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"))) {
            writer.setIndent("  ");
            getConfigGson().toJson(microConfig, MicroserviceConfiguration.class, writer);
            writer.flush();
        } catch (IOException e) {
            String message = String.format("Error exporting configuration for microservice %s", microserviceName);
            LOGGER.error(message, e);
            throw new ModuleException(e);
        }
    }

    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Import microservice configuration")
    public ResponseEntity<Void> importConfiguration(@RequestParam("file") MultipartFile file) throws ModuleException {

        try (JsonReader reader = new JsonReader(new InputStreamReader(file.getInputStream(), "UTF-8"))) {
            MicroserviceConfiguration microConfig = getConfigGson().fromJson(reader, MicroserviceConfiguration.class);
            // Propagate configuration to modules
            if ((managers != null) && !managers.isEmpty()) {
                for (ModuleConfiguration module : microConfig.getModules()) {
                    for (IModuleConfigurationManager manager : managers) {
                        if (manager.isApplicable(module)) {
                            manager.importConfiguration(module);
                        }
                    }
                }
                return ResponseEntity.status(HttpStatus.CREATED).build();
            } else {
                LOGGER.warn("Configuration cannot be imported because no module configuration manager is found!");
                return ResponseEntity.unprocessableEntity().build();
            }

        } catch (IOException e) {
            String message = String.format("Error importing configuration for microservice %s", microserviceName);
            LOGGER.error(message, e);
            throw new ModuleException(e);
        }
    }

    private Gson getConfigGson() {
        if (configGson == null) {
            // Create GSON for generic module configuration item adapter without itself! (avoid stackOverflow)
            GsonBuilder customBuilder = gsonBuilderFactory.newBuilder();
            customBuilder.addSerializationExclusionStrategy(new SerializationExclusionStrategy<>(ConfigIgnore.class));
            customBuilder.registerTypeHierarchyAdapter(Class.class, new ClassAdapter());
            configItemGson = customBuilder.create();

            // Create GSON with specific adapter to dynamically analyze parameterized type
            customBuilder = gsonBuilderFactory.newBuilder();
            customBuilder.addSerializationExclusionStrategy(new SerializationExclusionStrategy<>(ConfigIgnore.class));
            customBuilder.registerTypeHierarchyAdapter(Class.class, new ClassAdapter());
            customBuilder.registerTypeHierarchyAdapter(ModuleConfigurationItem.class,
                                                       new ModuleConfigurationItemAdapter(configItemGson));
            configGson = customBuilder.create();
        }
        return configGson;
    }
}
