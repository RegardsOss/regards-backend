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
package fr.cnes.regards.modules.order.test;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;
import fr.cnes.regards.modules.dam.domain.entities.feature.DatasetFeature;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSubSummary;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.indexer.domain.summary.FilesSummary;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.search.client.IComplexSearchClient;
import fr.cnes.regards.modules.search.domain.plugin.legacy.FacettedPagedModel;
import fr.cnes.regards.modules.search.dto.ComplexSearchRequest;
import fr.cnes.regards.modules.search.dto.SearchRequest;
import org.junit.Assert;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Mock of ISearchClient to be used by ServiceConfiguration
 *
 * @author oroussel
 * @author Sébastien Binda
 */
public class SearchClientMock implements IComplexSearchClient {

    public static final UniformResourceName DS1_IP_ID = UniformResourceName.build(OAISIdentifier.AIP,
                                                                                  EntityType.DATASET,
                                                                                  "ORDER",
                                                                                  UUID.randomUUID(),
                                                                                  1);

    public static final UniformResourceName DS2_IP_ID = UniformResourceName.build(OAISIdentifier.AIP,
                                                                                  EntityType.DATASET,
                                                                                  "ORDER",
                                                                                  UUID.randomUUID(),
                                                                                  1);

    public static final UniformResourceName DS3_IP_ID = UniformResourceName.build(OAISIdentifier.AIP,
                                                                                  EntityType.DATASET,
                                                                                  "ORDER",
                                                                                  UUID.randomUUID(),
                                                                                  1);

    protected static Dataset ds1;

    protected static Dataset ds2;

    protected static Dataset ds3;

    static {

        Model dsModel = new Model();
        dsModel.setName("datasetModel");
        dsModel.setType(EntityType.DATASET);
        dsModel.setId(1L);

        ds1 = new Dataset(dsModel, "tenant", "DS1", "DS1");
        ds1.setIpId(DS1_IP_ID);

        ds2 = new Dataset(dsModel, "tenant", "DS2", "DS2");
        ds2.setIpId(DS2_IP_ID);

        ds3 = new Dataset(dsModel, "tenant", "DS3", "DS3");
        ds3.setIpId(DS3_IP_ID);
    }

    public static final String QUERY_DS2_DS3 = "tags:(" + DS2_IP_ID + " OR " + DS3_IP_ID + ")";

    public static final Map<UniformResourceName, DatasetFeature> DS_MAP = new ImmutableMap.Builder<UniformResourceName, DatasetFeature>().put(
        DS1_IP_ID,
        ds1.getFeature()).put(DS2_IP_ID, ds2.getFeature()).put(DS3_IP_ID, ds3.getFeature()).build();

    /**
     * DS1 => 2 documents, 2 RAWDATA files (+ 6 QUICKLOOKS 2 x 3 of each size), 1 Mb each RAW file
     * 500 b QUICKLOOK SD, 1 kb MD, 500 kb HD (1 501 500 b)
     */
    protected static DocFilesSummary createSummaryForDs1AllFiles() {
        DocFilesSummary summary = new DocFilesSummary();
        summary.addDocumentsCount(2);

        DocFilesSubSummary dsSummary = new DocFilesSubSummary();
        dsSummary.addDocumentsCount(2);

        long rawCount = 2L;
        long rawSize = 2_000_000L;
        FilesSummary rawSummary = new FilesSummary();
        rawSummary.addFilesCount(rawCount);
        rawSummary.addFilesSize(rawSize);
        dsSummary.getFileTypesSummaryMap().put(DataType.RAWDATA + "_ref", new FilesSummary());
        dsSummary.getFileTypesSummaryMap().put(DataType.RAWDATA + "_!ref", rawSummary);
        dsSummary.getFileTypesSummaryMap().put(DataType.RAWDATA.toString(), rawSummary);
        dsSummary.addFilesCount(rawCount);
        dsSummary.addFilesSize(rawSize);
        summary.addFilesCount(rawCount);
        summary.addFilesSize(rawSize);

        long qlHdCount = 2L;
        long qlHdSize = 1_000_000L;
        FilesSummary qlHdSummary = new FilesSummary();
        qlHdSummary.addFilesCount(qlHdCount);
        qlHdSummary.addFilesSize(qlHdSize);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_HD + "_ref", new FilesSummary());
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_HD + "_!ref", qlHdSummary);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_HD.toString(), qlHdSummary);
        dsSummary.addFilesCount(qlHdCount);
        dsSummary.addFilesSize(qlHdSize);
        summary.addFilesCount(qlHdCount);
        summary.addFilesSize(qlHdSize);

        long qlMdCount = 2L;
        long qlMdSize = 2_000L;
        FilesSummary qlMdSummary = new FilesSummary();
        qlMdSummary.addFilesCount(qlMdCount);
        qlMdSummary.addFilesSize(qlMdSize);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_MD + "_ref", new FilesSummary());
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_MD + "_!ref", qlMdSummary);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_MD.toString(), qlMdSummary);
        dsSummary.addFilesCount(qlMdCount);
        dsSummary.addFilesSize(qlMdSize);
        summary.addFilesCount(qlMdCount);
        summary.addFilesSize(qlMdSize);

        long qlSdCount = 2L;
        long qlSdSize = 1_000L;
        FilesSummary qlSdSummary = new FilesSummary();
        qlSdSummary.addFilesCount(qlSdCount);
        qlSdSummary.addFilesSize(qlSdSize);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_SD + "_ref", new FilesSummary());
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_SD + "_!ref", qlSdSummary);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_SD.toString(), qlSdSummary);
        dsSummary.addFilesCount(qlSdCount);
        dsSummary.addFilesSize(qlSdSize);
        summary.addFilesCount(qlSdCount);
        summary.addFilesSize(qlSdSize);

        summary.getSubSummariesMap().put(DS1_IP_ID.toString(), dsSummary);
        return summary;
    }

    /**
     * 2 docs for DS2, 1 for DS3
     * 1 raw by doc, 3 quicklooks
     * 1Mb, 10 kb, 100 b, 1 b
     */
    protected static DocFilesSummary createSummaryForDs2Ds3AllFilesFirstCall() {
        DocFilesSummary summary = new DocFilesSummary();
        summary.addDocumentsCount(3);

        DocFilesSubSummary ds2Summary = new DocFilesSubSummary();
        ds2Summary.addDocumentsCount(2);
        Map<String, FilesSummary> fileTypesSummaryDs2Map = ds2Summary.getFileTypesSummaryMap();

        long rawCount2 = 2L;
        long rawSize2 = 2_000_000L;
        FilesSummary rawSummary2 = new FilesSummary();
        rawSummary2.addFilesCount(rawCount2);
        rawSummary2.addFilesSize(rawSize2);
        fileTypesSummaryDs2Map.put(DataType.RAWDATA + "_ref", new FilesSummary());
        fileTypesSummaryDs2Map.put(DataType.RAWDATA + "_!ref", rawSummary2);
        fileTypesSummaryDs2Map.put(DataType.RAWDATA.toString(), rawSummary2);
        ds2Summary.addFilesCount(rawCount2);
        ds2Summary.addFilesSize(rawSize2);
        summary.addFilesCount(rawCount2);
        summary.addFilesSize(rawSize2);

        long qlHdCount2 = 2L;
        long qlHdSize2 = 20_000L;
        FilesSummary qlHdSummary2 = new FilesSummary();
        qlHdSummary2.addFilesCount(qlHdCount2);
        qlHdSummary2.addFilesSize(qlHdSize2);
        fileTypesSummaryDs2Map.put(DataType.QUICKLOOK_HD + "_ref", new FilesSummary());
        fileTypesSummaryDs2Map.put(DataType.QUICKLOOK_HD + "_!ref", qlHdSummary2);
        fileTypesSummaryDs2Map.put(DataType.QUICKLOOK_HD.toString(), qlHdSummary2);
        ds2Summary.addFilesCount(qlHdCount2);
        ds2Summary.addFilesSize(qlHdSize2);
        summary.addFilesCount(qlHdCount2);
        summary.addFilesSize(qlHdSize2);

        long qlMdCount2 = 2L;
        long qlMdSize2 = 200L;
        FilesSummary qlMdSummary2 = new FilesSummary();
        qlMdSummary2.addFilesCount(qlMdCount2);
        qlMdSummary2.addFilesSize(qlMdSize2);
        fileTypesSummaryDs2Map.put(DataType.QUICKLOOK_MD + "_ref", new FilesSummary());
        fileTypesSummaryDs2Map.put(DataType.QUICKLOOK_MD + "_!ref", qlMdSummary2);
        fileTypesSummaryDs2Map.put(DataType.QUICKLOOK_MD.toString(), qlMdSummary2);
        ds2Summary.addFilesCount(qlMdCount2);
        ds2Summary.addFilesSize(qlMdSize2);
        summary.addFilesCount(qlMdCount2);
        summary.addFilesSize(qlMdSize2);

        long qlSdCount2 = 2L;
        long qlSdSize2 = 2L;
        FilesSummary qlSdSummary2 = new FilesSummary();
        qlSdSummary2.addFilesCount(qlSdCount2);
        qlSdSummary2.addFilesSize(qlSdSize2);
        fileTypesSummaryDs2Map.put(DataType.QUICKLOOK_SD + "_ref", new FilesSummary());
        fileTypesSummaryDs2Map.put(DataType.QUICKLOOK_SD + "_!ref", qlSdSummary2);
        fileTypesSummaryDs2Map.put(DataType.QUICKLOOK_SD.toString(), qlSdSummary2);
        ds2Summary.addFilesCount(qlSdCount2);
        ds2Summary.addFilesSize(qlSdSize2);
        summary.addFilesCount(qlSdCount2);
        summary.addFilesSize(qlSdSize2);

        summary.getSubSummariesMap().put(DS2_IP_ID.toString(), ds2Summary);

        DocFilesSubSummary ds3Summary = new DocFilesSubSummary();
        ds3Summary.addDocumentsCount(1);
        Map<String, FilesSummary> fileTypesSummaryDs3Map = ds3Summary.getFileTypesSummaryMap();

        long rawCount3 = 1L;
        long rawSize3 = 1_000_000L;
        FilesSummary rawSummary3 = new FilesSummary();
        rawSummary3.addFilesCount(rawCount3);
        rawSummary3.addFilesSize(rawSize3);
        fileTypesSummaryDs3Map.put(DataType.RAWDATA + "_ref", new FilesSummary());
        fileTypesSummaryDs3Map.put(DataType.RAWDATA + "_!ref", rawSummary3);
        fileTypesSummaryDs3Map.put(DataType.RAWDATA.toString(), rawSummary3);
        ds3Summary.addFilesCount(rawCount3);
        ds3Summary.addFilesSize(rawSize3);
        summary.addFilesCount(rawCount3);
        summary.addFilesSize(rawSize3);

        long qlHdCount3 = 1L;
        long qlHdSize3 = 10_000L;
        FilesSummary qlHdSummary3 = new FilesSummary();
        qlHdSummary3.addFilesCount(qlHdCount3);
        qlHdSummary3.addFilesSize(qlHdSize3);
        fileTypesSummaryDs3Map.put(DataType.QUICKLOOK_HD + "_ref", new FilesSummary());
        fileTypesSummaryDs3Map.put(DataType.QUICKLOOK_HD + "_!ref", qlHdSummary3);
        fileTypesSummaryDs3Map.put(DataType.QUICKLOOK_HD.toString(), qlHdSummary3);
        ds3Summary.addFilesCount(qlHdCount3);
        ds3Summary.addFilesSize(qlHdSize3);
        summary.addFilesCount(qlHdCount3);
        summary.addFilesSize(qlHdSize3);

        long qlMdCount3 = 1L;
        long qlMdSize3 = 100L;
        FilesSummary qlMdSummary3 = new FilesSummary();
        qlMdSummary3.addFilesCount(qlMdCount3);
        qlMdSummary3.addFilesSize(qlMdSize3);
        fileTypesSummaryDs3Map.put(DataType.QUICKLOOK_MD + "_ref", new FilesSummary());
        fileTypesSummaryDs3Map.put(DataType.QUICKLOOK_MD + "_!ref", qlMdSummary3);
        fileTypesSummaryDs3Map.put(DataType.QUICKLOOK_MD.toString(), qlMdSummary3);
        ds3Summary.addFilesCount(qlMdCount3);
        ds3Summary.addFilesSize(qlMdSize3);
        summary.addFilesCount(qlMdCount3);
        summary.addFilesSize(qlMdSize3);

        long qlSdCount3 = 1L;
        long qlSdSize3 = 1L;
        FilesSummary qlSdSummary3 = new FilesSummary();
        qlSdSummary3.addFilesCount(qlSdCount3);
        qlSdSummary3.addFilesSize(qlSdSize3);
        fileTypesSummaryDs3Map.put(DataType.QUICKLOOK_SD + "_ref", new FilesSummary());
        fileTypesSummaryDs3Map.put(DataType.QUICKLOOK_SD + "_!ref", qlSdSummary3);
        fileTypesSummaryDs3Map.put(DataType.QUICKLOOK_SD.toString(), qlSdSummary3);
        ds3Summary.addFilesCount(qlSdCount3);
        ds3Summary.addFilesSize(qlSdSize3);
        summary.addFilesCount(qlSdCount3);
        summary.addFilesSize(qlSdSize3);

        summary.getSubSummariesMap().put(DS3_IP_ID.toString(), ds3Summary);
        return summary;
    }

    /**
     * 2 docs for DS2
     * 1 raw by doc
     * 1Mb
     */
    protected static DocFilesSummary createSummaryForDs2AllFiles() {
        DocFilesSummary summary = new DocFilesSummary();
        summary.addDocumentsCount(2);

        DocFilesSubSummary dsSummary = new DocFilesSubSummary();
        dsSummary.addDocumentsCount(2);

        long rawCount = 2L;
        long rawSize = 2_000_000L;
        FilesSummary rawSummary = new FilesSummary();
        rawSummary.addFilesCount(rawCount);
        rawSummary.addFilesSize(rawSize);
        dsSummary.getFileTypesSummaryMap().put(DataType.RAWDATA + "_ref", new FilesSummary());
        dsSummary.getFileTypesSummaryMap().put(DataType.RAWDATA + "_!ref", rawSummary);
        dsSummary.getFileTypesSummaryMap().put(DataType.RAWDATA.toString(), rawSummary);
        dsSummary.addFilesCount(rawCount);
        dsSummary.addFilesSize(rawSize);
        summary.addFilesCount(rawCount);
        summary.addFilesSize(rawSize);

        long qlHdCount = 2L;
        long qlHdSize = 20_000L;
        FilesSummary qlHdSummary = new FilesSummary();
        qlHdSummary.addFilesCount(qlHdCount);
        qlHdSummary.addFilesSize(qlHdSize);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_HD + "_ref", new FilesSummary());
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_HD + "_!ref", qlHdSummary);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_HD.toString(), qlHdSummary);
        dsSummary.addFilesCount(qlHdCount);
        dsSummary.addFilesSize(qlHdSize);
        summary.addFilesCount(qlHdCount);
        summary.addFilesSize(qlHdSize);

        long qlMdCount = 2L;
        long qlMdSize = 200L;
        FilesSummary qlMdSummary = new FilesSummary();
        qlMdSummary.addFilesCount(qlMdCount);
        qlMdSummary.addFilesSize(qlMdSize);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_MD + "_ref", new FilesSummary());
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_MD + "_!ref", qlMdSummary);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_MD.toString(), qlMdSummary);
        dsSummary.addFilesCount(qlMdCount);
        dsSummary.addFilesSize(qlMdSize);
        summary.addFilesCount(qlMdCount);
        summary.addFilesSize(qlMdSize);

        long qlSdCount = 2L;
        long qlSdSize = 2L;
        FilesSummary qlSdSummary = new FilesSummary();
        qlSdSummary.addFilesCount(qlSdCount);
        qlSdSummary.addFilesSize(qlSdSize);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_SD + "_ref", new FilesSummary());
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_SD + "_!ref", qlSdSummary);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_SD.toString(), qlSdSummary);
        dsSummary.addFilesCount(qlSdCount);
        dsSummary.addFilesSize(qlSdSize);
        summary.addFilesCount(qlSdCount);
        summary.addFilesSize(qlSdSize);

        summary.getSubSummariesMap().put(DS2_IP_ID.toString(), dsSummary);
        return summary;
    }

    /**
     * 1 doc for DS3
     * 1 raw by doc
     * 1Mb
     */
    protected static DocFilesSummary createSummaryForDs3AllFiles() {
        DocFilesSummary summary = new DocFilesSummary();
        summary.addDocumentsCount(1);

        DocFilesSubSummary dsSummary = new DocFilesSubSummary();
        dsSummary.addDocumentsCount(1);

        long rawCount = 1L;
        long rawSize = 1_000_000L;
        FilesSummary rawSummary = new FilesSummary();
        rawSummary.addFilesCount(rawCount);
        rawSummary.addFilesSize(rawSize);
        dsSummary.getFileTypesSummaryMap().put(DataType.RAWDATA + "_ref", new FilesSummary());
        dsSummary.getFileTypesSummaryMap().put(DataType.RAWDATA + "_!ref", rawSummary);
        dsSummary.getFileTypesSummaryMap().put(DataType.RAWDATA.toString(), rawSummary);
        dsSummary.addFilesCount(rawCount);
        dsSummary.addFilesSize(rawSize);
        summary.addFilesCount(rawCount);
        summary.addFilesSize(rawSize);

        long qlHdCount = 1L;
        long qlHdSize = 10_000L;
        FilesSummary qlHdSummary = new FilesSummary();
        qlHdSummary.addFilesCount(qlHdCount);
        qlHdSummary.addFilesSize(qlHdSize);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_HD + "_ref", new FilesSummary());
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_HD + "_!ref", qlHdSummary);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_HD.toString(), qlHdSummary);
        dsSummary.addFilesCount(qlHdCount);
        dsSummary.addFilesSize(qlHdSize);
        summary.addFilesCount(qlHdCount);
        summary.addFilesSize(qlHdSize);

        long qlMdCount = 1L;
        long qlMdSize = 100L;
        FilesSummary qlMdSummary = new FilesSummary();
        qlMdSummary.addFilesCount(qlMdCount);
        qlMdSummary.addFilesSize(qlMdSize);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_MD + "_ref", new FilesSummary());
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_MD + "_!ref", qlMdSummary);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_MD.toString(), qlMdSummary);
        dsSummary.addFilesCount(qlMdCount);
        dsSummary.addFilesSize(qlMdSize);
        summary.addFilesCount(qlMdCount);
        summary.addFilesSize(qlMdSize);

        long qlSdCount = 1L;
        long qlSdSize = 1L;
        FilesSummary qlSdSummary = new FilesSummary();
        qlSdSummary.addFilesCount(qlSdCount);
        qlSdSummary.addFilesSize(qlSdSize);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_SD + "_ref", new FilesSummary());
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_SD + "_!ref", qlSdSummary);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_SD.toString(), qlSdSummary);
        dsSummary.addFilesCount(qlSdCount);
        dsSummary.addFilesSize(qlSdSize);
        summary.addFilesCount(qlSdCount);
        summary.addFilesSize(qlSdSize);

        summary.getSubSummariesMap().put(DS3_IP_ID.toString(), dsSummary);
        return summary;
    }

    protected static DocFilesSummary createSummaryForAllDsAllFiles() {
        DocFilesSummary summary = new DocFilesSummary();
        summary.addDocumentsCount(5);

        DocFilesSubSummary dsSummary = new DocFilesSubSummary();
        dsSummary.addDocumentsCount(2);

        long rawCount = 2L;
        long rawSize = 2_000_000L;
        FilesSummary rawSummary = new FilesSummary();
        rawSummary.addFilesCount(rawCount);
        rawSummary.addFilesSize(rawSize);
        dsSummary.getFileTypesSummaryMap().put(DataType.RAWDATA + "_ref", new FilesSummary());
        dsSummary.getFileTypesSummaryMap().put(DataType.RAWDATA + "_!ref", rawSummary);
        dsSummary.getFileTypesSummaryMap().put(DataType.RAWDATA.toString(), rawSummary);
        dsSummary.addFilesCount(rawCount);
        dsSummary.addFilesSize(rawSize);
        summary.addFilesCount(rawCount);
        summary.addFilesSize(rawSize);

        long qlHdCount = 2L;
        long qlHdSize = 1_000_000L;
        FilesSummary qlHdSummary = new FilesSummary();
        qlHdSummary.addFilesCount(qlHdCount);
        qlHdSummary.addFilesSize(qlHdSize);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_HD + "_ref", new FilesSummary());
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_HD + "_!ref", qlHdSummary);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_HD.toString(), qlHdSummary);
        dsSummary.addFilesCount(qlHdCount);
        dsSummary.addFilesSize(qlHdSize);
        summary.addFilesCount(qlHdCount);
        summary.addFilesSize(qlHdSize);

        long qlMdCount = 2L;
        long qlMdSize = 2_000L;
        FilesSummary qlMdSummary = new FilesSummary();
        qlMdSummary.addFilesCount(qlMdCount);
        qlMdSummary.addFilesSize(qlMdSize);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_MD + "_ref", new FilesSummary());
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_MD + "_!ref", qlMdSummary);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_MD.toString(), qlMdSummary);
        dsSummary.addFilesCount(qlMdCount);
        dsSummary.addFilesSize(qlMdSize);
        summary.addFilesCount(qlMdCount);
        summary.addFilesSize(qlMdSize);

        long qlSdCount = 2L;
        long qlSdSize = 1_000L;
        FilesSummary qlSdSummary = new FilesSummary();
        qlSdSummary.addFilesCount(qlSdCount);
        qlSdSummary.addFilesSize(qlSdSize);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_SD + "_ref", new FilesSummary());
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_SD + "_!ref", qlSdSummary);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_SD.toString(), qlSdSummary);
        dsSummary.addFilesCount(qlSdCount);
        dsSummary.addFilesSize(qlSdSize);
        summary.addFilesCount(qlSdCount);
        summary.addFilesSize(qlSdSize);

        summary.getSubSummariesMap().put(DS1_IP_ID.toString(), dsSummary);

        DocFilesSubSummary ds2Summary = new DocFilesSubSummary();
        ds2Summary.addDocumentsCount(2);

        long rawCount2 = 2L;
        long rawSize2 = 2_000_000L;
        FilesSummary rawSummary2 = new FilesSummary();
        rawSummary2.addFilesCount(rawCount2);
        rawSummary2.addFilesSize(rawSize2);
        ds2Summary.getFileTypesSummaryMap().put(DataType.RAWDATA + "_ref", new FilesSummary());
        ds2Summary.getFileTypesSummaryMap().put(DataType.RAWDATA + "_!ref", rawSummary2);
        ds2Summary.getFileTypesSummaryMap().put(DataType.RAWDATA.toString(), rawSummary2);
        ds2Summary.addFilesCount(rawCount2);
        ds2Summary.addFilesSize(rawSize2);
        summary.addFilesCount(rawCount2);
        summary.addFilesSize(rawSize2);

        long qlHdCount2 = 2L;
        long qlHdSize2 = 20_000L;
        FilesSummary qlHdSummary2 = new FilesSummary();
        qlHdSummary2.addFilesCount(qlHdCount2);
        qlHdSummary2.addFilesSize(qlHdSize2);
        ds2Summary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_HD + "_ref", new FilesSummary());
        ds2Summary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_HD + "_!ref", qlHdSummary2);
        ds2Summary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_HD.toString(), qlHdSummary2);
        ds2Summary.addFilesCount(qlHdCount2);
        ds2Summary.addFilesSize(qlHdSize2);
        summary.addFilesCount(qlHdCount2);
        summary.addFilesSize(qlHdSize2);

        long qlMdCount2 = 2L;
        long qlMdSize2 = 200L;
        FilesSummary qlMdSummary2 = new FilesSummary();
        qlMdSummary2.addFilesCount(qlMdCount2);
        qlMdSummary2.addFilesSize(qlMdSize2);
        ds2Summary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_MD + "_ref", new FilesSummary());
        ds2Summary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_MD + "_!ref", qlMdSummary2);
        ds2Summary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_MD.toString(), qlMdSummary2);
        ds2Summary.addFilesCount(qlMdCount2);
        ds2Summary.addFilesSize(qlMdSize2);
        summary.addFilesCount(qlMdCount2);
        summary.addFilesSize(qlMdSize2);

        long qlSdCount2 = 2L;
        long qlSdSize2 = 2L;
        FilesSummary qlSdSummary2 = new FilesSummary();
        qlSdSummary2.addFilesCount(qlSdCount2);
        qlSdSummary2.addFilesSize(qlSdSize2);
        ds2Summary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_SD + "_ref", new FilesSummary());
        ds2Summary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_SD + "_!ref", qlSdSummary2);
        ds2Summary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_SD.toString(), qlSdSummary2);
        ds2Summary.addFilesCount(qlSdCount2);
        ds2Summary.addFilesSize(qlSdSize2);
        summary.addFilesCount(qlSdCount2);
        summary.addFilesSize(qlSdSize2);

        summary.getSubSummariesMap().put(DS2_IP_ID.toString(), ds2Summary);

        DocFilesSubSummary ds3Summary = new DocFilesSubSummary();
        ds3Summary.addDocumentsCount(1);

        long rawCount3 = 1L;
        long rawSize3 = 1_000_000L;
        FilesSummary rawSummary3 = new FilesSummary();
        rawSummary3.addFilesCount(rawCount3);
        rawSummary3.addFilesSize(rawSize3);
        ds3Summary.getFileTypesSummaryMap().put(DataType.RAWDATA + "_ref", new FilesSummary());
        ds3Summary.getFileTypesSummaryMap().put(DataType.RAWDATA + "_!ref", rawSummary3);
        ds3Summary.getFileTypesSummaryMap().put(DataType.RAWDATA.toString(), rawSummary3);
        ds3Summary.addFilesCount(rawCount3);
        ds3Summary.addFilesSize(rawSize3);
        summary.addFilesCount(rawCount3);
        summary.addFilesSize(rawSize3);

        long qlHdCount3 = 1L;
        long qlHdSize3 = 10_000L;
        FilesSummary qlHdSummary3 = new FilesSummary();
        qlHdSummary3.addFilesCount(qlHdCount3);
        qlHdSummary3.addFilesSize(qlHdSize3);
        ds3Summary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_HD + "_ref", new FilesSummary());
        ds3Summary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_HD + "_!ref", qlHdSummary3);
        ds3Summary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_HD.toString(), qlHdSummary3);
        ds3Summary.addFilesCount(qlHdCount3);
        ds3Summary.addFilesSize(qlHdSize3);
        summary.addFilesCount(qlHdCount3);
        summary.addFilesSize(qlHdSize3);

        long qlMdCount3 = 1L;
        long qlMdSize3 = 100L;
        FilesSummary qlMdSummary3 = new FilesSummary();
        qlMdSummary3.addFilesCount(qlMdCount3);
        qlMdSummary3.addFilesSize(qlMdSize3);
        ds3Summary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_MD + "_ref", new FilesSummary());
        ds3Summary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_MD + "_!ref", qlMdSummary3);
        ds3Summary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_MD.toString(), qlMdSummary3);
        ds3Summary.addFilesCount(qlMdCount3);
        ds3Summary.addFilesSize(qlMdSize3);
        summary.addFilesCount(qlMdCount3);
        summary.addFilesSize(qlMdSize3);

        long qlSdCount3 = 1L;
        long qlSdSize3 = 1L;
        FilesSummary qlSdSummary3 = new FilesSummary();
        qlSdSummary3.addFilesCount(qlSdCount3);
        qlSdSummary3.addFilesSize(qlSdSize3);
        ds3Summary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_SD + "_ref", new FilesSummary());
        ds3Summary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_SD + "_!ref", qlSdSummary3);
        ds3Summary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_SD.toString(), qlSdSummary3);
        ds3Summary.addFilesCount(qlSdCount3);
        ds3Summary.addFilesSize(qlSdSize3);
        summary.addFilesCount(qlSdCount3);
        summary.addFilesSize(qlSdSize3);

        summary.getSubSummariesMap().put(DS3_IP_ID.toString(), ds3Summary);
        return summary;
    }

    protected static DataType getDataType(String filename) {
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

    protected void registerFilesIn(String path, List<EntityModel<EntityFeature>> list) throws URISyntaxException {
        File testDir = new File(path);
        for (File dir : Objects.requireNonNull(testDir.listFiles())) {
            EntityFeature feature = new DataObjectFeature(UniformResourceName.fromString(dir.getName()),
                                                          dir.getName(),
                                                          dir.getName());
            Multimap<DataType, DataFile> fileMultimap = ArrayListMultimap.create();
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                DataFile dataFile = new DataFile();
                dataFile.setOnline(false);
                dataFile.setUri(new URI("file:///test/" + file.getName()).toString());
                dataFile.setFilename(file.getName());
                dataFile.setFilesize(file.length());
                dataFile.setReference(false);
                dataFile.setChecksum(file.getName());
                dataFile.setDigestAlgorithm("MD5");
                dataFile.setMimeType(file.getName().endsWith("txt") ?
                                         MediaType.TEXT_PLAIN :
                                         MediaType.APPLICATION_OCTET_STREAM);
                dataFile.setDataType(getDataType(file.getName()));
                fileMultimap.put(getDataType(file.getName()), dataFile);
            }
            feature.setFiles(fileMultimap);
            list.add(EntityModel.of(feature));
        }
    }
}
