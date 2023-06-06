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
package fr.cnes.regards.modules.order.service.processing;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.order.test.SearchClientMock;
import fr.cnes.regards.modules.search.domain.plugin.legacy.FacettedPagedModel;
import fr.cnes.regards.modules.search.dto.ComplexSearchRequest;
import fr.cnes.regards.modules.search.dto.SearchRequest;
import org.junit.Assert;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mock of ISearchClient to be used by ServiceConfiguration
 *
 * @author oroussel
 * @author Sébastien Binda
 */
public class ProcessingSearchClientMock extends SearchClientMock {

    @Override
    public ResponseEntity<DocFilesSummary> computeDatasetsSummary(ComplexSearchRequest complexSearchRequest) {
        List<SearchRequest> requests = complexSearchRequest.getRequests();
        Assert.assertFalse("Cannot handle empty complex search", requests.isEmpty());
        SearchRequest request = requests.stream().findFirst().get();
        String query = request.getSearchParameters().get("q").stream().findFirst().orElse(null);
        String datasetUrn = request.getDatasetUrn();

        if (datasetUrn == null) {
            if (QUERY_DS2_DS3.equals(query)) {
                return new ResponseEntity<>(createSummaryForDs2Ds3AllFilesFirstCall(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(createSummaryForAllDsAllFiles(), HttpStatus.OK);
            }
        } else if (datasetUrn.equals(DS1_IP_ID.toString())) {
            return new ResponseEntity<>(createSummaryForDs1AllFiles(), HttpStatus.OK);
        } else if (datasetUrn.equals(DS2_IP_ID.toString())) {
            return new ResponseEntity<>(createSummaryForDs2AllFiles(), HttpStatus.OK);
        } else if (datasetUrn.equals(DS3_IP_ID.toString())) {
            return new ResponseEntity<>(createSummaryForDs3AllFiles(), HttpStatus.OK);
        } else {
            throw new RuntimeException("Someone completely shit out this test ! Investigate and kick his ass !");
        }
    }

    @Override
    public ResponseEntity<FacettedPagedModel<EntityModel<EntityFeature>>> searchDataObjects(ComplexSearchRequest complexSearchRequest) {
        if (complexSearchRequest.getPage() == 0) {
            try {
                List<EntityModel<EntityFeature>> list = new ArrayList<>();
                registerFilesIn("src/test/resources/files", list);
                registerFilesIn("src/test/resources/processing/files", list);
                return ResponseEntity.ok(new FacettedPagedModel<>(Sets.newHashSet(),
                                                                  list,
                                                                  new PagedModel.PageMetadata(list.size(),
                                                                                              0,
                                                                                              list.size())));
            } catch (URISyntaxException e) {
                throw new RsRuntimeException(e);
            }
        }
        return ResponseEntity.ok(new FacettedPagedModel<>(Sets.newHashSet(),
                                                          Collections.emptyList(),
                                                          new PagedModel.PageMetadata(0, 0, 0)));
    }

}
