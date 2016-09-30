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

        XmlTest test1 = new XmlTest();
        test1.setPurpose("comment");
        test1.setTestClass("testclass");
        test1.setTestMethodName("testname");
        req1.addTest(test1);
        reqs.addRequirement(req1);

        XmlRequirement req2 = new XmlRequirement();
        req2.setRequirement("foo_REF");

        XmlTest test2 = new XmlTest();
        test2.setPurpose("comment 2");
        test2.setTestClass("testclass.2");
        test2.setTestMethodName("testname2");
        req2.addTest(test2);

        XmlTest test3 = new XmlTest();
        test3.setPurpose("comment 3");
        test3.setTestClass("testclass.3");
        test3.setTestMethodName("testname3");
        req2.addTest(test3);

        reqs.addRequirement(req2);

        // File result = new File("requirements.xml");
        String filename = "requirements.xml";

        try {
            // Write data
            XmlHelper.write(Paths.get("target"), filename, XmlRequirements.class, reqs);

            // Read data
            XmlRequirements refReq = XmlHelper.read(Paths.get("target"), filename, XmlRequirements.class);

            Assert.assertNotNull(refReq.getRequirements());
            Assert.assertEquals(refReq.getRequirements().size(), 2);

            for (XmlRequirement reqToTest : refReq.getRequirements()) {

                Assert.assertNotNull(reqToTest.getTests());
                Assert.assertNotEquals(reqToTest.getTests().size(), 0);
                XmlTest test = reqToTest.getTests().get(0);

                if (reqToTest.getRequirement().equals(req1.getRequirement())) {
                    Assert.assertEquals(test.getPurpose(), test1.getPurpose());
                    Assert.assertEquals(test.getTestClass(), test1.getTestClass());
                    Assert.assertEquals(test.getTestMethodName(), test1.getTestMethodName());

                }
                if (reqToTest.getRequirement().equals(req2.getRequirement())) {
                    Assert.assertEquals(test.getPurpose(), test2.getPurpose());
                    Assert.assertEquals(test.getTestClass(), test2.getTestClass());
                    Assert.assertEquals(test.getTestMethodName(), test2.getTestMethodName());

                }
            }
        }
        catch (XMLHelperException e) {
            LOG.error("Cannot marshal report", e);
            Assert.fail();
        }
    }
}
