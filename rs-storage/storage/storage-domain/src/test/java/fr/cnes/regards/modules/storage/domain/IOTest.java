/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { IOTestConfiguration.class })
public class IOTest {

    @Autowired
    private Gson gson;

    private Map<String, Object> fakeAip;

    @Before
    public void init() throws MalformedURLException {
        fakeAip = new HashMap<>();
        fakeAip.put("sipId", "JA1_GDR_2PcP243_249");
        fakeAip.put("ipId", "URN:AIP:COLLECTION:PROJECT:67db4ba0-0ba5-40f7-ac6d-fe2fdbf42664:V1");
        fakeAip.put("type", "DATA");
        // Tags
        List<String> tags = new ArrayList<>();
        tags.add("NASA");
        tags.add("regards:facilities:HST");
        tags.add("regards:instruments:ACS");
        tags.add("regards:proposals:857");
        tags.add("regards:filters:H");
        tags.add("regards:detectors:WFC");
        fakeAip.put("tags", tags);
        List<InformationObject> ioList = new ArrayList<>();
        // InformationObject 1
        // ContentInformation 1
        ContentInformation contentInfo1 = new ContentInformation();
        // DataObject 1
        DataObject do1 = new DataObject();
        do1.setType(FileType.valueOf("RAWDATA"));
        do1.setUrl(new URL("file:///tmp/example.fits"));
        contentInfo1.setDataObject(do1);
        // RepresentationInformation 1
        RepresentationInformation ri1 = new RepresentationInformation();
        // Syntax 1
        Syntax syntax1 = new Syntax();
        syntax1.setName("FITS(FlexibleImageTransport)");
        syntax1.setDescription("http://www.iana.org/assignments/media-types/application/fits");
        syntax1.setMimeType("application/fits");
        ri1.setSyntax(syntax1);
        // Semantic 1
        Semantic sem1 = new Semantic();
        sem1.setDescription("|Bytes|Format|Units|Label|Explanations|\n|------|-----------|-----|---------|----------------------------------------|\n|1-9|I9|---|Corot|CoRoTnumber|\n|11|I1|h|RAh|Rightascension(J2000)|\n|13-14|I2|min|RAm|Rightascension(J2000)|\n|16-20|F5.2|s|RAs|Rightascension(J2000)|\n|22|A1|---|DE-|Declinationsign(J2000)|\n|23|I1|deg|DEd|Declination(J2000)|\n|25-26|I2|arcmin|DEm|Declination(J2000)|\n|28-32|F5.2|arcsec|DEs|Declination(J2000)|");
        ri1.setSemantic(sem1);
        contentInfo1.setRepresentationInformation(ri1);
        // END OF contentInformation 1
        // PDI 1
        PreservationDescriptionInformation pdi1 = new PreservationDescriptionInformation();
        Map<String, Object> contextInfo1 = new LinkedTreeMap<>();
        contextInfo1.put("name", "HST_ACS_23:05:02+05:12:34.fits");
        contextInfo1.put("target", "M31");
        // characterisation
        Map<String, Object> characterisation = new LinkedTreeMap<>();
        // spatialAxis
        Map<String, Object> spatialAxis = new LinkedTreeMap<>();
        // Coverage
        Map<String, Object> coverage = new LinkedTreeMap<>();
        // Geometry
        Map<String, Object> geometry = new LinkedTreeMap<>();
        geometry.put("type", "Polygon");
        // coordinates
        List<List<List<Double>>> coordinates = new ArrayList<>();
        List<List<Double>> points = new ArrayList<>();
        List<Double> point1 = new ArrayList<>();
        point1.add(-10.0);
        point1.add(-10.0);
        points.add(point1);
        List<Double> point2 = new ArrayList<>();
        point2.add(10.0);
        point2.add(-10.0);
        points.add(point2);
        List<Double> point3 = new ArrayList<>();
        point3.add(10.0);
        point3.add(10.0);
        points.add(point3);
        List<Double> point4 = new ArrayList<>();
        point4.add(-10.0);
        point4.add(10.0);
        points.add(point4);
        coordinates.add(points);
        geometry.put("coordinates", coordinates);
        coverage.put("geometry", geometry);
        // crs
        Map<String, Object> crs = new LinkedTreeMap<>();
        crs.put("type", "name");
        Map<String, Object> propertiesCRS = new LinkedTreeMap<>();
        propertiesCRS.put("name", "urn:iau:def:crs:IAU:ICRS");
        crs.put("properties", propertiesCRS);
        coverage.put("crs", crs);
        spatialAxis.put("coverage", coverage);
        List<Double> resolution = new ArrayList<>();
        resolution.add(0.00001);
        resolution.add(0.00001);
        spatialAxis.put("resolution", resolution);
        List<Integer> sampling = new ArrayList<>();
        sampling.add(200);
        sampling.add(300);
        spatialAxis.put("sampling", sampling);
        List<Double> accuracy = new ArrayList<>();
        accuracy.add(0.1);
        accuracy.add(0.1);
        spatialAxis.put("accuracy", accuracy);
        characterisation.put("spatialAxis", spatialAxis);
        // END OF spatialAxis
        // timeAxis
        Map<String, Object> timeAxis = new LinkedTreeMap<>();
        Map<String, Object> coverageTA = new LinkedTreeMap<>();
        List<List<OffsetDateTime>> value = new ArrayList<>();
        List<OffsetDateTime> value1 = new ArrayList<>();
        value1.add(OffsetDateTime.parse("2014-01-01T10:23:00Z"));
        value1.add(OffsetDateTime.parse("2014-01-01T10:25:00Z"));
        value.add(value1);
        List<OffsetDateTime> value2 = new ArrayList<>();
        value2.add(OffsetDateTime.parse("2014-01-01T11:23:00Z"));
        value2.add(OffsetDateTime.parse("2014-01-01T11:25:00Z"));
        value.add(value2);
        coverageTA.put("value", value);
        Map<String, Object> trs = new LinkedTreeMap<>();
        trs.put("type", "name");
        Map<String, Object> propertiesTRS = new LinkedTreeMap<>();
        propertiesTRS.put("name", "urn:iau:def:trs:IAU:GregoryCalendar");
        trs.put("properties", propertiesTRS);
        coverageTA.put("trs", trs);
        timeAxis.put("coverage", coverageTA);
        timeAxis.put("accuracy", 0.000001);
        characterisation.put("timeAxis", timeAxis);
        // END OF timeAxis
        // frequencyAxis
        Map<String, Object> frequencyAxis = new LinkedTreeMap<>();
        Map<String, Object> coverageFA = new LinkedTreeMap<>();
        List<List<Integer>> valueFA = new ArrayList<>();
        List<Integer> valueFA1 = new ArrayList<>();
        valueFA1.add(400);
        valueFA1.add(600);
        valueFA.add(valueFA1);
        List<Integer> valueFA2 = new ArrayList<>();
        valueFA2.add(650);
        valueFA2.add(800);
        valueFA.add(valueFA2);
        coverageFA.put("value", valueFA);
        Map<String, Object> frs = new LinkedTreeMap<>();
        frs.put("type", "name");
        Map<String, Object> propertiesFRS = new LinkedTreeMap<>();
        propertiesFRS.put("name", "urn:iau:def:frs:IAU:Wavelength");
        frs.put("properties", propertiesFRS);
        coverageFA.put("frs", frs);
        frequencyAxis.put("coverage", coverageFA);
        characterisation.put("frequencyAxis", frequencyAxis);
        // END OF frequencyAxis
        // observableAxis
        List<Map<String, Object>> observableAxis = new ArrayList<>();
        Map<String, Object> observableAxis1 = new LinkedTreeMap<>();
        observableAxis1.put("name", "electronCount");
        observableAxis1.put("coverage", "....");
        observableAxis1.put("resolution", "....");
        observableAxis1.put("sampling", "....");
        observableAxis1.put("accuracy", "....");
        observableAxis.add(observableAxis1);
        characterisation.put("observableAxis", observableAxis);
        characterisation.put("options", new LinkedTreeMap<String, Object>());
        contextInfo1.put("characterisation", characterisation);
        // END OF characterisation
        pdi1.setContextInformation(contextInfo1);
        // END OF ContextInformation 1
        // ProvenanceInformation 1
        ProvenanceInformation pi1 = new ProvenanceInformation();
        Map<String, Object> additionnal = new LinkedTreeMap<>();
        additionnal.put("instrument", "ACS");
        additionnal.put("detector", "WFC");
        additionnal.put("filter", "H");
        additionnal.put("proposal", 857);
        pi1.setAdditional(additionnal);
        pi1.setFacility("HST");
        List<Event> history = new ArrayList<>();
        history.add(new Event("acquisitionoftheobservation", OffsetDateTime.parse("2014-01-01T23:10:05Z")));
        history.add(new Event("astrometrycalibration", OffsetDateTime.parse("2014-01-02T23:10:05Z")));
        history.add(new Event("receivedinformationfromtheproducertothearchive",
                OffsetDateTime.parse("2014-02-01T23:10:05Z")));
        history.add(new Event("AIPiscreated", OffsetDateTime.parse("2014-02-01T23:30:05Z")));
        history.add(new Event("AIPisstored", OffsetDateTime.parse("2014-02-02T23:10:05Z")));
        history.add(new Event("AIPisarchived", OffsetDateTime.parse("2014-02-03T23:10:05Z")));
        history.add(new Event("HSTMissionislinkedtoAIP", OffsetDateTime.parse("2014-02-08T23:10:05Z")));
        history.add(new Event("HSTProposalislinkedtoAIP", OffsetDateTime.parse("2014-02-08T23:10:05Z")));
        history.add(new Event("Informationaddedaboutfilter", OffsetDateTime.parse("2014-02-18T23:10:05Z")));
        pi1.setHistory(history);
        pdi1.setProvenanceInformation(pi1);
        // END OF ProvenanceInformation 1
        // fixityInformation 1
        FixityInformation fi1 = new FixityInformation();
        fi1.setChecksum("d6aa97d33d459ea3670056e737c99a3d");
        fi1.setAlgorithm("md5");
        fi1.setFileSize(1024.0);
        pdi1.setFixityInformation(fi1);
        // END OF ficityInformation 1
        // accessRightInformation 1
        AccessRightInformation ari1 = new AccessRightInformation();
        ari1.setDataRights("secure");
        ari1.setPublicReleaseDate(OffsetDateTime.parse("2017-01-01T01:00:00Z"));
        ari1.setPublisherDID("<regards_id>");
        ari1.setPublisherID("<sip_id>");
        pdi1.setAccesRightInformation(ari1);
        // END OF accessRightsInformation 1
        // END OF PreservationDescriptionInformation 1
        InformationObject io1 = new InformationObject(contentInfo1, pdi1);
        ioList.add(io1);
        // END OF informationObject 1
        // InformationObject 2
        // ContentInformation 2
        ContentInformation contentInfo2 = new ContentInformation();
        // DataObject 2
        DataObject do2 = new DataObject();
        do2.setType(FileType.valueOf("QUICKLOOK"));
        do2.setUrl(new URL("file:///tmp/example.png"));
        contentInfo2.setDataObject(do2);
        // RepresentationInformation 2
        RepresentationInformation ri2 = new RepresentationInformation();
        // Syntax 2
        Syntax syntax2 = new Syntax();
        syntax2.setName("PNG");
        syntax2.setDescription("http://www.iana.org/assignments/media-types/image/png");
        syntax2.setMimeType("image/png");
        ri2.setSyntax(syntax2);
        contentInfo2.setRepresentationInformation(ri2);
        // END OF contentInformation 2
        // PreservationDescriptionInformation 2
        PreservationDescriptionInformation pdi2 = new PreservationDescriptionInformation();
        // ProvenanceInformation 2
        ProvenanceInformation pi2 = new ProvenanceInformation();
        pi2.setFacility("CNESarchive");
        List<Event> history2 = new ArrayList<>();
        history2.add(new Event("thequicklookiscreated", OffsetDateTime.parse("2014-02-01T23:30:05Z")));
        history2.add(new Event("thequicklookisstored", OffsetDateTime.parse("2014-01-02T23:10:05Z")));
        pi2.setHistory(history2);
        pdi2.setProvenanceInformation(pi2);
        // END OF ProvenanceInformation 2
        // fixityInformation 2
        FixityInformation fi2 = new FixityInformation();
        fi2.setChecksum("d6bbbbd33d459ea3670056e737c99a3d");
        fi2.setAlgorithm("md5");
        pdi2.setFixityInformation(fi2);
        // END OF fixityInformation 2
        // accessRightInformation 2
        AccessRightInformation ari2 = new AccessRightInformation();
        ari2.setDataRights("public");
        ari2.setPublicReleaseDate(OffsetDateTime.parse("2014-01-02T23:10:05Z"));
        ari2.setPublisherDID("<regards_id>");
        ari2.setPublisherID("<regards_id>");
        pdi2.setAccesRightInformation(ari2);
        // END OF accessRightsInformation 2
        // END OF PreservationDescriptionInformation 2
        InformationObject io2 = new InformationObject(contentInfo2, pdi2);
        ioList.add(io2);
        fakeAip.put("informationObjects", ioList);
    }

    @Test
    public void testParsingAndSerialiaze() throws IOException {
        FileReader fr = new FileReader("src/test/resources/aip_sample.json");
        BufferedReader br = new BufferedReader(fr);
        AIP aip = gson.fromJson(br, AIP.class);
        br.close();
        fr.close();

        JsonObject fakeAipTree = (JsonObject) gson.toJsonTree(fakeAip);
        JsonObject aipTree = (JsonObject) gson.toJsonTree(aip);

        Assert.assertTrue(aipTree.equals(fakeAipTree));

    }

}
