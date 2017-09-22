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
package fr.cnes.regards.modules.order.test;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.search.client.ICatalogClient;
import fr.cnes.regards.modules.storage.client.IAipClient;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.database.AvailabilityResponse;
import fr.cnes.regards.modules.storage.domain.event.DataFileEvent;
import fr.cnes.regards.modules.storage.domain.event.DataFileEventState;

/**
 * @author oroussel
 */
@Configuration
@ComponentScan(basePackages = { "fr.cnes.regards.modules.order.service", "fr.cnes.regards.framework.modules.jobs" })
@EnableAutoConfiguration
@PropertySource(value = { "classpath:test.properties", "classpath:test_${user.name}.properties" },
        ignoreResourceNotFound = true)
public class Service2Configuration {
    @Bean
    public ICatalogClient catalogClient() {
        return new ICatalogClient() {

            @Override
            public ResponseEntity<Resource<Dataset>> getDataset(UniformResourceName urn) {
                return null;
            }

            @Override
            public ResponseEntity<DocFilesSummary> computeDatasetsSummary(Map<String, String> allParams,
                    String datasetIpId, String... fileTypes) {
                return null;
            }

            // This method is called for datasetSelection (the only one)
            @Override
            public ResponseEntity<PagedResources<Resource<DataObject>>> searchDataobjects(
                    Map<String, String> allParams, Pageable pageable) {
                if (pageable.getPageNumber() == 0) {
                    try {
                        List<Resource<DataObject>> list = new ArrayList<>();
                        File testDir = new File("src/test/resources/files");
                        for (File dir : testDir.listFiles()) {
                            DataObject object = new DataObject();
                            object.setIpId(UniformResourceName.fromString(dir.getName()));
                            Multimap<DataType, DataFile> fileMultimap = ArrayListMultimap.create();
                            for (File file : dir.listFiles()) {
                                DataFile dataFile = new DataFile();
                                dataFile.setOnline(false);
                                dataFile.setUri(new URI("file:///test/" + file.getName()));
                                dataFile.setName(file.getName());
                                dataFile.setSize(file.length());
                                dataFile.setChecksum(file.getName());
                                dataFile.setDigestAlgorithm("MD5");
                                dataFile.setMimeType(file.getName().endsWith("txt") ?
                                                             MediaType.TEXT_PLAIN :
                                                             MediaType.APPLICATION_OCTET_STREAM);
                                fileMultimap.put(getDataType(file.getName()), dataFile);
                            }
                            object.setFiles(fileMultimap);
                            list.add(new Resource<>(object));
                        }

                        return ResponseEntity.ok(new PagedResources<>(list,
                                                                      new PagedResources.PageMetadata(list.size(),
                                                                                                      0,
                                                                                                      list.size())));
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
                return ResponseEntity.ok(new PagedResources<Resource<DataObject>>(Collections.emptyList(),
                                                                                  new PagedResources.PageMetadata(0,
                                                                                                                  0,
                                                                                                                  0)));
            }
        };

    }

    private static DataType getDataType(String filename) {
        if (filename.endsWith("ql_hd.txt")) {
            return DataType.QUICKLOOK_HD;
        } else if (filename.endsWith("ql_md.txt")) {
            return DataType.QUICKLOOK_MD;
        } else if (filename.endsWith("ql_sd.txt")) {
            return DataType.QUICKLOOK_SD;
        } else {
            return DataType.RAWDATA;
        }
    }

    @Bean
    public IAipClient aipClient() {
        return new IAipClient() {

            @Autowired
            private IPublisher publisher;

            @Override
            public InputStream downloadFile(String aipId, String checksum) {
                return getClass().getResourceAsStream("/files/" + checksum);
            }

            @Override
            public HttpEntity<PagedResources<Resource<AIP>>> retrieveAIPs(AIPState pState, OffsetDateTime pFrom,
                    OffsetDateTime pTo, int pPage, int pSize) {
                return null;
            }

            @Override
            public HttpEntity<Set<UUID>> createAIP(Set<AIP> aips) {
                return null;
            }

            @Override
            public HttpEntity<List<fr.cnes.regards.framework.oais.DataObject>> retrieveAIPFiles(
                    UniformResourceName pIpId) {
                return null;
            }

            @Override
            public HttpEntity<List<String>> retrieveAIPVersionHistory(UniformResourceName pIpId, int pPage,
                    int pSize) {
                return null;
            }

            @Override
            public HttpEntity<AvailabilityResponse> makeFilesAvailable(AvailabilityRequest availabilityRequest) {
                for (String checksum : availabilityRequest.getChecksums()) {
                    publisher.publish(new DataFileEvent(DataFileEventState.AVAILABLE, checksum));
                }
                return new HttpEntity<>(new AvailabilityResponse(Collections.emptySet(), Collections.emptySet(),
                                                                 Collections.emptySet()));
            }
        };
    }
}
