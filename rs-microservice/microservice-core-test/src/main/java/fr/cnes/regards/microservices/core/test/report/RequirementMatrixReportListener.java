/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.test.report;

import java.nio.file.Paths;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.microservices.core.test.report.annotation.Purpose;
import fr.cnes.regards.microservices.core.test.report.annotation.Requirement;
import fr.cnes.regards.microservices.core.test.report.annotation.Requirements;
import fr.cnes.regards.microservices.core.test.report.xml.XmlHelper;
import fr.cnes.regards.microservices.core.test.report.xml.XmlRequirement;
import fr.cnes.regards.microservices.core.test.report.xml.XmlRequirements;
import fr.cnes.regards.microservices.core.test.report.xml.XmlTest;

/**
 * JUnit listener to help writing requirement matrix report
 *
 * @author msordi
 *
 */
public class RequirementMatrixReportListener extends RunListener {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RequirementMatrixReportListener.class);

    /**
     * Report prefix
     */
    private static final String REPORT_PREFIX = "RQMT-";

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
    private final XmlRequirements xmlRequirements_;

    /**
     * Report filename
     */
    private String filename_;

    /**
     * Constructor
     */
    public RequirementMatrixReportListener() {
        xmlRequirements_ = new XmlRequirements();
    }

    @Override
    public void testFinished(Description pDescription) throws Exception {

        // Build filename if not
        // ! workaround because testRunStarted cannot be capture
        if (filename_ == null) {
            filename_ = REPORT_PREFIX + pDescription.getTestClass().getCanonicalName();
        }

        // Get common report purpose
        Purpose purpose = pDescription.getAnnotation(Purpose.class);

        Requirements requirements = pDescription.getAnnotation(Requirements.class);
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
     *
     * Init a new report element if requirement exists
     *
     * @param pDescription
     *            Test description
     * @param pRequirement
     *            Requirement
     * @param pPurpose
     *            Test purpose
     */
    private void handleRequirementTest(Description pDescription, Requirement pRequirement, Purpose pPurpose) {
        if (pRequirement == null) {
            // Nothing to do if requirement not set
            return;
        }

        XmlRequirement xmlReq = new XmlRequirement();
        xmlReq.setRequirement(pRequirement.value());

        XmlTest xmlTest = new XmlTest();
        xmlTest.setTestClass(pDescription.getTestClass().getCanonicalName());
        xmlTest.setTestMethodName(pDescription.getMethodName());
        if (pPurpose != null) {
            xmlTest.setPurpose(pPurpose.value());
        }
        xmlReq.addTest(xmlTest);

        xmlRequirements_.addRequirement(xmlReq);
    }

    @Override
    public void testRunFinished(Result pResult) throws Exception {
        LOG.debug("" + xmlRequirements_.getRequirements().size());
        XmlHelper.write(Paths.get(MVN_OUTPUT_DIRECTORY, REPORT_DIR), filename_, XmlRequirements.class,
                        xmlRequirements_);
    }
}
