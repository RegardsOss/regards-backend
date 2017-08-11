package fr.cnes.regards.modules.order.test;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ObjectArrays;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.modules.entities.domain.DataType;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.entities.urn.OAISIdentifier;
import fr.cnes.regards.modules.entities.urn.UniformResourceName;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSubSummary;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.indexer.domain.summary.FilesSummary;
import fr.cnes.regards.modules.models.domain.EntityType;
import fr.cnes.regards.modules.search.client.ICatalogClient;

/**
 * Mock of ICatalogClient to be used by ServiceConfiguration
 * @author oroussel
 */
public class CatalogClientMock implements ICatalogClient {

    public static final UniformResourceName DS1_IP_ID = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATASET,
                                                                                "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DS2_IP_ID = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATASET,
                                                                                "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DS3_IP_ID = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATASET,
                                                                                "ORDER", UUID.randomUUID(), 1);

    private static final Dataset ds1 = new Dataset();

    private static final Dataset ds2 = new Dataset();

    private static final Dataset ds3 = new Dataset();

    private static final Map<UniformResourceName, Dataset> DS_MAP = new ImmutableMap.Builder<UniformResourceName, Dataset>()
            .put(DS1_IP_ID, ds1).put(DS2_IP_ID, ds2).put(DS3_IP_ID, ds3).build();

    static {
        ds1.setIpId(DS1_IP_ID);
        ds1.setLabel("DS1");

        ds2.setIpId(DS2_IP_ID);
        ds2.setLabel("DS2");

        ds3.setIpId(DS3_IP_ID);
        ds3.setLabel("DS3");
    }

    private static OffsetDateTime[] dates = new OffsetDateTime[0];

    private static final Pattern PATTERN = Pattern.compile(".*creationDate:\\[\\* TO ([^\\]]+)\\].*");

    public static void addLandmark(OffsetDateTime date) {
        dates = ObjectArrays.concat(dates, date);
    }

    @Override
    public ResponseEntity<Resource<Dataset>> getDataset(UniformResourceName urn) {
        return new ResponseEntity<>(new Resource<>(DS_MAP.get(urn)), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DocFilesSummary> computeDatasetsSummary(Map<String, String> allParams, String datasetIpId,
            String... fileTypes) {
        // Thanks to landmark dates and requestion creation date range, determine call index oif this method
        int insertionIdx = retrieveLandmarkIdx(allParams);
        DocFilesSummary summary = null;
        switch (insertionIdx) {
            case 0:
                if (datasetIpId.equals(DS1_IP_ID.toString())) {
                    // First call : empty opensearch request (only creationDate range), datasetIpId = DS1_IP_ID and all files
                    if (fileTypes.length == 4) {
                        summary = createSummaryForDs1AllFilesFirstCall();
                    } else if ((fileTypes.length == 1) && (fileTypes[0] == DataType.RAWDATA.toString())) {
                        // Second call when computeDatasetSummary is asked for updating dataset only with RAW_DATA
                        summary = createSummaryForDs1Raw();
                    } else if ((fileTypes.length == 3) && (fileTypes[0] != DataType.RAWDATA.toString())
                            && (fileTypes[1] != DataType.RAWDATA.toString())
                            && (fileTypes[2] != DataType.RAWDATA.toString())) {
                        // Third call when changing file types to QUICKLOOK
                        summary = createSummaryForDs1QuicklooksFiles();
                    } else {
                        throw new RuntimeException(
                                "Someone completely shit out this test ! Investigate and kick his ass !");
                    }
                } else {
                    throw new RuntimeException(
                            "Someone completely shit out this test ! Investigate and kick his ass !");
                }
                break;
            case 4:
                // When adding a selection on DS2 and DS3 (All files)
                if ((datasetIpId == null) && (fileTypes.length == 4)) {
                    summary = createSummaryForDs2Ds3AllFilesFirstCall();
                } else if ((datasetIpId.equals(DS2_IP_ID.toString())) && (fileTypes.length == 1)
                        && (fileTypes[0] == DataType.RAWDATA.toString())) {
                    // same selection adding process, recomputing DS2 selection with default type (ie RAWADATA)
                    summary = createSummaryForDs2Raw();
                } else if ((datasetIpId.equals(DS3_IP_ID.toString())) && (fileTypes.length == 1)
                        && (fileTypes[0] == DataType.RAWDATA.toString())) {
                    // same selection adding process, recomputing DS3 selection with default type (ie RAWADATA)
                    summary = createSummaryForDs3Raw();
                } else {
                    throw new RuntimeException(
                            "Someone completely shit out this test ! Investigate and kick his ass !");
                }
                break;
            case 7:
                // Adding a selection on everything
                if ((datasetIpId == null) && (fileTypes.length == 4)) {
                    summary = createSummaryForAllDsAllFilesFirstCall();
                } else if ((datasetIpId.equals(DS1_IP_ID.toString())) && (fileTypes.length == 3)
                        && (fileTypes[0] != DataType.RAWDATA.toString())
                        && (fileTypes[1] != DataType.RAWDATA.toString())
                        && (fileTypes[2] != DataType.RAWDATA.toString())) {
                    summary = createSummaryForDs1QuicklooksFiles();
                } else if ((datasetIpId.equals(DS2_IP_ID.toString())) && (fileTypes.length == 1)
                        && (fileTypes[0] == DataType.RAWDATA.toString())) {
                    // same selection adding process, recomputing DS2 selection with default type (ie RAWADATA)
                    summary = createSummaryForDs2Raw();
                } else if ((datasetIpId.equals(DS3_IP_ID.toString())) && (fileTypes.length == 1)
                        && (fileTypes[0] == DataType.RAWDATA.toString())) {
                    // same selection adding process, recomputing DS3 selection with default type (ie RAWADATA)
                    summary = createSummaryForDs3Raw();
                } else {
                    throw new RuntimeException(
                            "Someone completely shit out this test ! Investigate and kick his ass !");
                }
                break;
        }

        addLandmark(OffsetDateTime.now());
        return new ResponseEntity<>(summary, HttpStatus.OK);
    }

    /**
     * DS1 => 2 documents, 2 RAWDATA files (+ 6 QUICKLOOKS 2 x 3 of each size), 1 Mb each RAW file
     * 500 b QUICKLOOK SD, 1 kb MD, 500 kb HD (1 501 500 b)
     */
    private static DocFilesSummary createSummaryForDs1AllFilesFirstCall() {
        DocFilesSummary summary = new DocFilesSummary(2, 8, 3_003_000);
        DocFilesSubSummary dsSummary = new DocFilesSubSummary(2, 8, 3_003_000);
        FilesSummary rawSummary = new FilesSummary(2, 2_000_000);
        dsSummary.getFileTypesSummaryMap().put(DataType.RAWDATA.toString(), rawSummary);
        FilesSummary qlHdSummary = new FilesSummary(2, 1_000_000);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_HD.toString(), qlHdSummary);
        FilesSummary qlMdSummary = new FilesSummary(2, 2_000);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_MD.toString(), qlMdSummary);
        FilesSummary qlSdSummary = new FilesSummary(2, 1_000);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_SD.toString(), qlSdSummary);

        summary.getSubSummariesMap().put(DS1_IP_ID.toString(), dsSummary);
        return summary;
    }

    /**
     * DS1 => 2 documents, 2 RAWDATA files, 1 Mb each RAW file
     */
    private static DocFilesSummary createSummaryForDs1Raw() {
        DocFilesSummary summary = new DocFilesSummary(2, 2, 2_000_000);
        DocFilesSubSummary dsSummary = new DocFilesSubSummary(2, 2, 2_000_000);
        FilesSummary rawSummary = new FilesSummary(2, 2_000_000);
        dsSummary.getFileTypesSummaryMap().put(DataType.RAWDATA.toString(), rawSummary);

        summary.getSubSummariesMap().put(DS1_IP_ID.toString(), dsSummary);
        return summary;

    }

    /**
     * DS1 => 2 documents, 6 QUICKLOOKS 2 x 3 of each size
     * 500 b QUICKLOOK SD, 1 kb MD, 500 kb HD (1 501 500 b)
     */
    private static DocFilesSummary createSummaryForDs1QuicklooksFiles() {
        DocFilesSummary summary = new DocFilesSummary(2, 6, 1_003_000);
        DocFilesSubSummary dsSummary = new DocFilesSubSummary(2, 6, 1_003_000);
        FilesSummary qlHdSummary = new FilesSummary(2, 1_000_000);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_HD.toString(), qlHdSummary);
        FilesSummary qlMdSummary = new FilesSummary(2, 2_000);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_MD.toString(), qlMdSummary);
        FilesSummary qlSdSummary = new FilesSummary(2, 1_000);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_SD.toString(), qlSdSummary);

        summary.getSubSummariesMap().put(DS1_IP_ID.toString(), dsSummary);
        return summary;
    }

    /**
     * 2 docs for DS2, 1 for DS3
     * 1 raw by doc, 3 quicklooks
     * 1Mb, 10 kb, 100 b, 1 b
     */
    private static DocFilesSummary createSummaryForDs2Ds3AllFilesFirstCall() {
        DocFilesSummary summary = new DocFilesSummary(3, 12, 3_030_303);
        DocFilesSubSummary ds2Summary = new DocFilesSubSummary(2, 8, 2_020_202);
        Map<String, FilesSummary> fileTypesSummaryDs2Map = ds2Summary.getFileTypesSummaryMap();
        fileTypesSummaryDs2Map.put(DataType.RAWDATA.toString(), new FilesSummary(2, 2_000_000));
        fileTypesSummaryDs2Map.put(DataType.QUICKLOOK_HD.toString(), new FilesSummary(2, 20_000));
        fileTypesSummaryDs2Map.put(DataType.QUICKLOOK_MD.toString(), new FilesSummary(2, 200));
        fileTypesSummaryDs2Map.put(DataType.QUICKLOOK_SD.toString(), new FilesSummary(2, 20));

        DocFilesSubSummary ds3Summary = new DocFilesSubSummary(1, 4, 1_010_101);
        Map<String, FilesSummary> fileTypesSummaryDs3Map = ds3Summary.getFileTypesSummaryMap();
        fileTypesSummaryDs3Map.put(DataType.RAWDATA.toString(), new FilesSummary(1, 1_000_000));
        fileTypesSummaryDs3Map.put(DataType.QUICKLOOK_HD.toString(), new FilesSummary(1, 10_000));
        fileTypesSummaryDs3Map.put(DataType.QUICKLOOK_MD.toString(), new FilesSummary(1, 100));
        fileTypesSummaryDs3Map.put(DataType.QUICKLOOK_SD.toString(), new FilesSummary(1, 10));

        summary.getSubSummariesMap().put(DS2_IP_ID.toString(), ds2Summary);
        summary.getSubSummariesMap().put(DS3_IP_ID.toString(), ds3Summary);
        return summary;
    }

    /**
     * 2 docs for DS2
     * 1 raw by doc
     * 1Mb
     */
    private static DocFilesSummary createSummaryForDs2Raw() {
        DocFilesSummary summary = new DocFilesSummary(2, 2, 2_000_000);
        DocFilesSubSummary ds2Summary = new DocFilesSubSummary(2, 2, 2_000_000);
        Map<String, FilesSummary> fileTypesSummaryDs2Map = ds2Summary.getFileTypesSummaryMap();
        fileTypesSummaryDs2Map.put(DataType.RAWDATA.toString(), new FilesSummary(2, 2_000_000));

        summary.getSubSummariesMap().put(DS2_IP_ID.toString(), ds2Summary);
        return summary;
    }


    /**
     * 1 doc for DS3
     * 1 raw by doc
     * 1Mb
     */
    private static DocFilesSummary createSummaryForDs3Raw() {
        DocFilesSummary summary = new DocFilesSummary(1, 1, 1_000_000);
        DocFilesSubSummary ds3Summary = new DocFilesSubSummary(1, 1, 1_000_000);
        Map<String, FilesSummary> fileTypesSummaryDs3Map = ds3Summary.getFileTypesSummaryMap();
        fileTypesSummaryDs3Map.put(DataType.RAWDATA.toString(), new FilesSummary(1, 1_000_000));

        summary.getSubSummariesMap().put(DS3_IP_ID.toString(), ds3Summary);
        return summary;
    }

    private static DocFilesSummary createSummaryForAllDsAllFilesFirstCall() {
        DocFilesSummary summary = new DocFilesSummary(5, 20, 5_023_202);

        DocFilesSubSummary dsSummary = new DocFilesSubSummary(2, 8, 3_003_00);
        FilesSummary rawSummary = new FilesSummary(2, 2_000_000);
        dsSummary.getFileTypesSummaryMap().put(DataType.RAWDATA.toString(), rawSummary);
        FilesSummary qlHdSummary = new FilesSummary(2, 1_000_000);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_HD.toString(), qlHdSummary);
        FilesSummary qlMdSummary = new FilesSummary(2, 2_000);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_MD.toString(), qlMdSummary);
        FilesSummary qlSdSummary = new FilesSummary(2, 1_000);
        dsSummary.getFileTypesSummaryMap().put(DataType.QUICKLOOK_SD.toString(), qlSdSummary);

        summary.getSubSummariesMap().put(DS1_IP_ID.toString(), dsSummary);

        DocFilesSubSummary ds2Summary = new DocFilesSubSummary(2, 8, 2_020_202);
        Map<String, FilesSummary> fileTypesSummaryDs2Map = ds2Summary.getFileTypesSummaryMap();
        fileTypesSummaryDs2Map.put(DataType.RAWDATA.toString(), new FilesSummary(2, 2_000_000));
        fileTypesSummaryDs2Map.put(DataType.QUICKLOOK_HD.toString(), new FilesSummary(2, 20_000));
        fileTypesSummaryDs2Map.put(DataType.QUICKLOOK_MD.toString(), new FilesSummary(2, 200));
        fileTypesSummaryDs2Map.put(DataType.QUICKLOOK_SD.toString(), new FilesSummary(2, 20));

        DocFilesSubSummary ds3Summary = new DocFilesSubSummary(1, 4, 1_010_101);
        Map<String, FilesSummary> fileTypesSummaryDs3Map = ds3Summary.getFileTypesSummaryMap();
        fileTypesSummaryDs3Map.put(DataType.RAWDATA.toString(), new FilesSummary(1, 1_000_000));
        fileTypesSummaryDs3Map.put(DataType.QUICKLOOK_HD.toString(), new FilesSummary(1, 10_000));
        fileTypesSummaryDs3Map.put(DataType.QUICKLOOK_MD.toString(), new FilesSummary(1, 100));
        fileTypesSummaryDs3Map.put(DataType.QUICKLOOK_SD.toString(), new FilesSummary(1, 10));

        summary.getSubSummariesMap().put(DS2_IP_ID.toString(), ds2Summary);
        summary.getSubSummariesMap().put(DS3_IP_ID.toString(), ds3Summary);
        return summary;
    }

    private int retrieveLandmarkIdx(Map<String, String> allParams) {
        String opensearcReq = allParams.get("q");
        Matcher matcher = PATTERN.matcher(opensearcReq);
        if (!matcher.matches()) {
            throw new RuntimeException("Opensearch request doesn't contain creation:[* TO <date>]");
        }
        String reqDateStr = matcher.group(1);
        OffsetDateTime reqDate = OffsetDateTimeAdapter.parse(reqDateStr);
        return -Arrays.binarySearch(dates, reqDate) - 1;
    }
}
