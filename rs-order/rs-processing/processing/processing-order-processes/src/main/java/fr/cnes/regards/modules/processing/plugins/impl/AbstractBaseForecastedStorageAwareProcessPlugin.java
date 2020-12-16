/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.plugins.impl;

import static fr.cnes.regards.modules.processing.domain.engine.ExecutionEvent.event;
import static fr.cnes.regards.modules.processing.utils.ReactorErrorTransformers.addInContext;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PInputFile;
import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.domain.engine.IExecutable;
import fr.cnes.regards.modules.processing.domain.exception.ProcessingExecutionException;
import fr.cnes.regards.modules.processing.exceptions.ProcessingExceptionType;
import fr.cnes.regards.modules.processing.order.OrderInputFileMetadata;
import fr.cnes.regards.modules.processing.order.OrderInputFileMetadataMapper;
import fr.cnes.regards.modules.processing.storage.ExecutionLocalWorkdir;
import fr.cnes.regards.modules.processing.storage.IExecutionLocalWorkdirService;
import fr.cnes.regards.modules.processing.storage.ISharedStorageService;
import fr.cnes.regards.modules.search.client.ILegacySearchEngineJsonClient;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineMappings;
import io.vavr.collection.Seq;
import reactor.core.publisher.Mono;

/**
 * This class is a base abstract class for process plugins which interact with the
 * storage to store input/output files.
 *
 * @author gandrieu
 */
public abstract class AbstractBaseForecastedStorageAwareProcessPlugin extends AbstractBaseForecastedProcessPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBaseForecastedStorageAwareProcessPlugin.class);

    private static final String METADATA_DIR_PATH = "metadata";

    private static final String METADATA_FILE_EXT = ".json";

    @Autowired
    protected IExecutionLocalWorkdirService workdirService;

    @Autowired
    protected ISharedStorageService storageService;

    @Autowired
    private ILegacySearchEngineJsonClient searchClient;

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private Gson gson;

    @PluginParameter(label = "Retrieve features metadata", description = "Define if metadata are provided to process",
            name = "addMetadata", defaultValue = "false")
    protected boolean addMetadata = false;

    public IExecutable prepareWorkdir() {
        return context -> {
            PExecution exec = context.getExec();
            Seq<PInputFile> inputFiles = exec.getInputFiles();
            //@formatter:off
            return workdirService
                    .makeWorkdir(exec)
                    .flatMap(wd -> workdirService.writeInputFilesToWorkdirInput(wd, inputFiles))
                    .flatMap(wd -> {
                        if (addMetadata) {
                            return addMetadataInWorkdir(wd, inputFiles, context.getBatch().getTenant());
                        } else {
                            return Mono.just(wd);
                        }
                    })
                    .map(wd -> context.withParam(ExecutionLocalWorkdir.class, wd))
                    .subscriberContext(addInContext(PExecution.class, exec))
                    .switchIfEmpty(Mono.error(new WorkdirPreparationException(exec, "Unknown error")));
          //@formatter:on
        };
    }

    /**
     * Add metadata file for each features. Metadata are retrieved from catalog service with feature id (urn)
     *
     * @param inputFiles {@link PInputFile}s
     * @return
     */
    private Mono<ExecutionLocalWorkdir> addMetadataInWorkdir(ExecutionLocalWorkdir wd, Seq<PInputFile> inputFiles,
            String tenant) {

        return Mono.fromCallable(() -> {
            runtimeTenantResolver.forceTenant(tenant);
            FeignSecurityManager.asSystem();
            OrderInputFileMetadataMapper mapper = new OrderInputFileMetadataMapper();
            //@formatter:off
            inputFiles
                .map(PInputFile::getMetadata)
                .flatMap(mapper::fromMap)
                .map(OrderInputFileMetadata::getFeatureId)
                .distinct().forEach(urn -> {
             //@formatter:on
                        try {
                            ResponseEntity<JsonObject> response = searchClient
                                    .getDataobject(urn, SearchEngineMappings.getJsonHeaders());
                            if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
                                JsonObject feature = response.getBody();
                                String featureName = urn.toString();
                                JsonElement featureContent = feature.get("content");
                                if ((featureContent != null) && featureContent.isJsonObject()) {
                                    JsonElement el = featureContent.getAsJsonObject().get("providerId");
                                    if ((el != null) && (el.getAsString() != null)) {
                                        featureName = el.getAsString();
                                    }
                                    Path mfPath = Paths.get(wd.inputFolder().toString(), urn.toString(),
                                                            METADATA_DIR_PATH,
                                                            featureName.replaceAll(" ", "_") + METADATA_FILE_EXT);
                                    Files.createDirectories(mfPath.getParent());
                                    try (FileWriter writer = new FileWriter(Files.createFile(mfPath).toFile())) {
                                        gson.toJson(featureContent, writer);
                                    }

                                }

                            } else {
                                LOGGER.warn("Unable to retrieve entity {} catalog return code={}", urn,
                                            response.getStatusCodeValue());
                            }
                        } catch (HttpServerErrorException | HttpClientErrorException | IOException e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                    });
            return wd;
        }).doOnTerminate(() -> {
            FeignSecurityManager.reset();
            runtimeTenantResolver.clearTenant();
        });

    }

    public IExecutable storeOutputFiles() {
        return context -> {
            PExecution exec = context.getExec();
            return context.getParam(ExecutionLocalWorkdir.class).flatMap(wd -> storageService.storeResult(context, wd))
                    .flatMap(out -> context.sendEvent(event(PStep.success(""), out)))
                    .subscriberContext(addInContext(PExecution.class, exec));
        };
    }

    public IExecutable cleanWorkdir() {
        return context -> context.getParam(ExecutionLocalWorkdir.class).flatMap(workdirService::cleanupWorkdir)
                .map(wd -> context).onErrorResume(t -> {
                    LOGGER.error("execId={} Failed to cleanup execution workdir: {} - {}", context.getExec().getId(),
                                 t.getClass(), t.getMessage());
                    return Mono.just(context);
                }).switchIfEmpty(Mono.just(context));
    }

    public IExecutionLocalWorkdirService getWorkdirService() {
        return workdirService;
    }

    public void setWorkdirService(IExecutionLocalWorkdirService workdirService) {
        this.workdirService = workdirService;
    }

    public ISharedStorageService getStorageService() {
        return storageService;
    }

    public void setStorageService(ISharedStorageService storageService) {
        this.storageService = storageService;
    }

    public static class WorkdirPreparationException extends ProcessingExecutionException {

        public WorkdirPreparationException(PExecution exec, String message) {
            super(ProcessingExceptionType.WORKDIR_PREPARATION_ERROR, exec, message);
        }
    }
}
