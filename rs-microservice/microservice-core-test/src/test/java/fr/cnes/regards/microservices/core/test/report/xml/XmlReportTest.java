/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.test.report.xml;

import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.microservices.core.test.report.exception.XMLHelperException;

/**
 * Test XML report marshalling/unmarshalling
 *
 * @author msordi
 *
 */
public class XmlReportTest {

    private static final Logger LOG = LoggerFactory.getLogger(XmlReportTest.class);

    @Test
    public void testXML() {

        XmlRequirements reqs = new XmlRequirements();

        XmlRequirement req1 = new XmlRequirement();
        req1.setRequirement("TOTO_REF");
        req1.setPurpose("comment");
        req1.setTestClass("testclass");
        req1.setTestMethodName("testname");
        reqs.addRequirement(req1);

        XmlRequirement req2 = new XmlRequirement();
        req2.setRequirement("foo_REF");
        req2.setPurpose("comment 2");
        req2.setTestClass("testclass.2");
        req2.setTestMethodName("testname2");
        reqs.addRequirement(req2);

        // File result = new File("requirements.xml");
        String filename = "requirements.xml";

        try {
            // Write data
            XmlHelper.write(Paths.get("."), filename, XmlRequirements.class, reqs);

            // Read data
            XmlRequirements refReq = XmlHelper.read(Paths.get("."), filename, XmlRequirements.class);

            Assert.assertNotNull(refReq.getRequirements());
            Assert.assertEquals(refReq.getRequirements().size(), 2);

            for (XmlRequirement reqToTest : refReq.getRequirements()) {
                if (reqToTest.getRequirement().equals(req1.getRequirement())) {
                    Assert.assertEquals(reqToTest.getPurpose(), req1.getPurpose());
                    Assert.assertEquals(reqToTest.getTestClass(), req1.getTestClass());
                    Assert.assertEquals(reqToTest.getTestMethodName(), req1.getTestMethodName());

                }
                if (reqToTest.getRequirement().equals(req2.getRequirement())) {
                    Assert.assertEquals(reqToTest.getPurpose(), req2.getPurpose());
                    Assert.assertEquals(reqToTest.getTestClass(), req2.getTestClass());
                    Assert.assertEquals(reqToTest.getTestMethodName(), req2.getTestMethodName());

                }
            }
        }
        catch (XMLHelperException e) {
            LOG.error("Cannot marshal report", e);
            Assert.fail();
        }
    }
}
