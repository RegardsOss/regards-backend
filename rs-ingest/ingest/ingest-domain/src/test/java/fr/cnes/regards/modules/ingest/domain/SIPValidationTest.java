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
package fr.cnes.regards.modules.ingest.domain;

import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.HashMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.MimeType;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.oais.InformationPackageProperties;
import fr.cnes.regards.framework.oais.builder.PDIBuilder;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;
import fr.cnes.regards.modules.ingest.domain.builder.SIPCollectionBuilder;

/**
 *
 * {@link SIP} and {@link SIPCollection} validation tests
 *
 * @author Marc Sordi
 *
 */
@RunWith(SpringRunner.class)
@EnableAutoConfiguration(exclude = { JpaRepositoriesAutoConfiguration.class })
public class SIPValidationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SIPValidationTest.class);

    private static final String SIP_ID = "sipId";

    @Autowired
    private Validator validator;

    private Errors errors;

    @Before
    public void before() {
        errors = new MapBindingResult(new HashMap<>(), "sip");
    }

    @After
    public void logErrors() {
        errors.getAllErrors().forEach(error -> {
            LOGGER.debug("Validation ERROR ----------------> " + error.toString());
        });
    }

    /**
     * Check that SIP has to be passed either by reference or by regards. See {@link SIP} type constraints.
     */
    @Test
    @Requirement("REGARDS_DSL_ING_PRO_130")
    @Purpose("SIP validation")
    public void emptySIP() {

        SIP sip = new SIP();
        sip.setIpType(EntityType.DATA);

        validator.validate(sip, errors);
        if (!errors.hasErrors()) {
            Assert.fail("An empty SIP should be invalid");
        }
        Assert.assertTrue(errors.getErrorCount() == 2);
    }

    /**
     * Check validation on SIP passed by reference. See {@link SIPReference} for constraint list.
     */
    @Test
    @Requirement("REGARDS_DSL_ING_PRO_140")
    @Purpose("SIP validation")
    public void invalidSIPReference() {

        SIP sip = new SIP();
        sip.setId(SIP_ID);
        sip.setIpType(EntityType.DATA);
        SIPReference ref = new SIPReference();
        sip.setRef(ref);

        validator.validate(sip, errors);
        if (!errors.hasErrors()) {
            Assert.fail("An empty SIP reference should be invalid");
        }
        Assert.assertTrue(errors.getErrorCount() == 4);
    }

    /**
     * Check that builder properly build a SIP reference. See {@link #invalidSIPReference()} for required properties.
     */
    @Test
    @Requirement("REGARDS_DSL_ING_PRO_140")
    @Purpose("SIP validation")
    public void validSIPReference() {

        SIPBuilder builder = new SIPBuilder(SIP_ID);
        SIP sip = builder.buildReference(Paths.get("sip.xml"), "abpfbfp222");
        validator.validate(sip, errors);
        if (errors.hasErrors()) {
            Assert.fail("Builder should properly build SIP reference");
        }
    }

    /**
     * Check validation on SIP passed by value. See {@link InformationPackageProperties} for constraint list.
     */
    @Test
    @Requirement("REGARDS_DSL_ING_PRO_140")
    @Purpose("SIP validation")
    public void invalidSIPValue() {

        SIP sip = new SIP();
        sip.setId(SIP_ID);
        sip.setIpType(EntityType.DATA);
        sip.setProperties(new InformationPackageProperties());

        validator.validate(sip, errors);
        if (!errors.hasErrors()) {
            Assert.fail("An empty SIP value should be invalid");
        }
        // Missing at least one content information
        Assert.assertTrue(errors.getErrorCount() == 1);
    }

    /**
     * Check that builder properly build a SIP value.
     */
    @Test
    @Requirement("REGARDS_DSL_ING_PRO_140")
    @Purpose("SIP validation")
    public void validSIPValue() {

        SIPBuilder sipBuilder = new SIPBuilder(SIP_ID);

        // Geometry
        sipBuilder.setGeometry(IGeometry.point(IGeometry.position(10.0, 10.0)));
        // Content information
        // Content information - data object
        sipBuilder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, Paths.get("sip.fits"),
                                                                "abff1dffdfdf2sdsfsd");
        // Content information - data object representation information
        sipBuilder.getContentInformationBuilder().setSyntaxAndSemantic("FITS",
                                                                       "http://www.iana.org/assignments/media-types/application/fits",
                                                                       MimeType.valueOf("application/fits"), "semanticDescription");
        // Effectively add content information to the current SIP
        sipBuilder.addContentInformation();

        // PDI
        PDIBuilder pdiBuilder = sipBuilder.getPDIBuilder();
        // PDI - context information
        pdiBuilder.addTags("CNES", "TOULOUSE", "FRANCE");
        pdiBuilder.addContextInformation("CNES", "http://www.cnes.fr");
        // PDI - reference information
        pdiBuilder.addReferenceInformation("doi", "dfdg://Dsfd.;");
        // PDI - provenance information
        pdiBuilder.setFacility("facility");
        pdiBuilder.setInstrument("instrument");
        pdiBuilder.setFilter("filter");
        pdiBuilder.setDetector("detector");
        pdiBuilder.setProposal("proposal");
        pdiBuilder.addAdditionalProvenanceInformation("from", "ender");
        // PDI - provenance information events
        pdiBuilder.addProvenanceInformationEvent("SIP initialization");
        pdiBuilder.addProvenanceInformationEvent("SIP_CREATION", "SIP creation", OffsetDateTime.now());
        // PDI - fixity
        pdiBuilder.addFixityInformation("fixity", "see content info. for checksum algorithm");
        // PDI - access right information
        pdiBuilder.setAccessRightInformation("MIT", "public", OffsetDateTime.now());

        SIP sip = sipBuilder.build();

        validator.validate(sip, errors);
        if (errors.hasErrors()) {
            Assert.fail("Builder should properly build SIP value");
        }
    }

    /**
     * Check that {@link IngestMetadata} is properly validated. Empty collection is accepted!
     */
    @Test
    public void validateSIPCollection() {

        SIPCollectionBuilder builder = new SIPCollectionBuilder("processingChain");

        validator.validate(builder.build(), errors);
        if (errors.hasErrors()) {
            Assert.fail("Builder should properly build SIP collection ");
        }
    }
}
