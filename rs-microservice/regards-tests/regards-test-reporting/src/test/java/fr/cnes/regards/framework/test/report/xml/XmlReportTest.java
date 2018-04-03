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
package fr.cnes.regards.framework.test.report.xml;

import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.test.report.exception.ReportException;
import fr.cnes.regards.framework.test.report.xls.XlsxHelper;

/**
 * Test XML report marshalling/unmarshalling
 *
 * @author msordi
 *
 */
public class XmlReportTest {

    private static final Logger LOG = LoggerFactory.getLogger(XmlReportTest.class);

    private static XmlRequirements reqs_;

    @BeforeClass
    public static void setupRequirements() {
        reqs_ = new XmlRequirements();

        XmlRequirement req1 = new XmlRequirement();
        req1.setRequirement("TOTO_REF");

        XmlTest test1 = new XmlTest();
        test1.setPurpose("comment");
        test1.setTestClass("testclass");
        test1.setTestMethodName("testname");
        req1.addTest(test1);
        reqs_.addRequirement(req1);

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

        reqs_.addRequirement(req2);
    }

    @Test
    public void testXML() throws UnsupportedEncodingException {

        String filename = "requirements.xml";

        try {
            // Write data
            XmlHelper.write(Paths.get("target"), filename, XmlRequirements.class, reqs_);

            // Read data
            XmlRequirements refReq = XmlHelper.read(Paths.get("target"), filename, XmlRequirements.class);

            Assert.assertNotNull(refReq.getRequirements());
            Assert.assertEquals(refReq.getRequirements().size(), 2);

            for (XmlRequirement reqToTest : refReq.getRequirements()) {

                for (XmlRequirement req : reqs_.getRequirements()) {
                    compareRequirement(req, reqToTest);
                }
            }
        } catch (ReportException e) {
            Assert.fail();
        }
    }

    private void compareRequirement(XmlRequirement pReq1, XmlRequirement pReq2) {
        Assert.assertNotNull(pReq1);
        Assert.assertNotNull(pReq2);
        Assert.assertNotNull(pReq1.getRequirement());
        Assert.assertNotNull(pReq2.getRequirement());
        if (pReq1.getRequirement().equals(pReq2.getRequirement())) {
            if ((pReq1.getTests().size() == 1) && (pReq2.getTests().size() == 1)) {
                compareTest(pReq1.getTests().get(0), pReq2.getTests().get(0));
            } else {
                LOG.debug("Only compare number of test in case of multiple tests");
                Assert.assertEquals(pReq1.getTests().size(), pReq2.getTests().size());
            }
        }
    }

    private void compareTest(XmlTest pTest1, XmlTest pTest2) {
        Assert.assertNotNull(pTest1);
        Assert.assertNotNull(pTest2);
        Assert.assertEquals(pTest1.getPurpose(), pTest2.getPurpose());
        Assert.assertEquals(pTest1.getTestClass(), pTest2.getTestClass());
        Assert.assertEquals(pTest1.getTestMethodName(), pTest2.getTestMethodName());
        LOG.debug("Single test \"" + pTest1.getTestMethodName() + "\" compared");
    }

    @Test
    public void testXlsx() {
        String filename = "requirements.xlsx";
        try {
            XlsxHelper.write(Paths.get("target", filename), reqs_, "dam");
        } catch (ReportException e) {
            Assert.fail();
        }
    }
}
