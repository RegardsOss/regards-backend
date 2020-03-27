/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.test.report;

import java.nio.file.Paths;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.test.report.annotation.Requirements;
import fr.cnes.regards.framework.test.report.xml.XmlHelper;
import fr.cnes.regards.framework.test.report.xml.XmlRequirement;
import fr.cnes.regards.framework.test.report.xml.XmlRequirements;
import fr.cnes.regards.framework.test.report.xml.XmlTest;

/**
 * JUnit listener to help writing requirement matrix report
 * @author msordi
 */
public class RequirementMatrixReportListener extends RunListener {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RequirementMatrixReportListener.class);

    /**
     * Report prefix
     */
    public static final String REPORT_PREFIX = "RQMT-";

    /**
     * Maven build directory
     */
    private static final String MVN_OUTPUT_DIRECTORY = "target";

    /**
     * Requirement report directory
     */
    private static final String REPORT_DIR = "requirement-reports";

    /**
     * Requirement list
     */
    private final XmlRequirements xmlRequirements;

    /**
     * Report filename
     */
    private String filename;

    /**
     * Constructor
     */
    public RequirementMatrixReportListener() {
        xmlRequirements = new XmlRequirements();
    }

    @Override
    public void testFinished(Description pDescription) {

        // Build filename if not
        // ! workaround because testRunStarted cannot be capture
        if (filename == null) {
            filename = REPORT_PREFIX + pDescription.getTestClass().getCanonicalName();
        }

        // Get common report purpose
        final Purpose purpose = pDescription.getAnnotation(Purpose.class);

        final Requirements requirements = pDescription.getAnnotation(Requirements.class);
        if (requirements != null) {
            for (Requirement req : requirements.value()) {
                handleRequirementTest(pDescription, req, purpose);
            }
        } else {
            // Try to get single requirement
            handleRequirementTest(pDescription, pDescription.getAnnotation(Requirement.class), purpose);
        }
    }

    /**
     * Init a new report element if requirement exists
     * @param pDescription Test description
     * @param pRequirement Requirement
     * @param pPurpose Test purpose
     */
    private void handleRequirementTest(Description pDescription, Requirement pRequirement, Purpose pPurpose) {
        if (pRequirement == null) {
            // Nothing to do if requirement not set
            return;
        }

        final XmlRequirement xmlReq = new XmlRequirement();
        xmlReq.setRequirement(pRequirement.value());

        final XmlTest xmlTest = new XmlTest();
        xmlTest.setTestClass(pDescription.getTestClass().getCanonicalName());
        xmlTest.setTestMethodName(pDescription.getMethodName());
        if (pPurpose != null) {
            xmlTest.setPurpose(pPurpose.value());
        }
        xmlReq.addTest(xmlTest);

        xmlRequirements.addRequirement(xmlReq);
    }

    @Override
    public void testRunFinished(Result pResult) throws Exception {
        if (xmlRequirements.getRequirements() != null) {
            LOG.debug(Integer.toString(xmlRequirements.getRequirements().size()));
            XmlHelper.write(Paths.get(MVN_OUTPUT_DIRECTORY, REPORT_DIR), filename, XmlRequirements.class,
                            xmlRequirements);
        }
    }
}
