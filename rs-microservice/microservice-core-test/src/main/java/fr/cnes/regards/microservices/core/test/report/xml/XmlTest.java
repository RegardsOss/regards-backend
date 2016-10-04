/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.test.report.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @author msordi
 *
 */
@XmlType(propOrder = { "purpose", "testMethodName", "testClass" })
@XmlRootElement(name = "test")
public class XmlTest {

    /**
     * Test purpose
     */
    private String purpose_;

    /**
     * Test class
     */
    private String testClass_;

    /**
     * Test method
     */
    private String testMethodName_;

    /**
     * @return the Purpose
     */
    public String getPurpose() {
        return purpose_;
    }

    /**
     * @param pPurpose
     *            the Purpose to set
     */
    @XmlElement(name = "purpose")
    public void setPurpose(String pPurpose) {
        purpose_ = pPurpose;
    }

    /**
     * @return the testClass
     */
    public String getTestClass() {
        return testClass_;
    }

    /**
     * @param pTestClass
     *            the testClass to set
     */
    @XmlElement(name = "class")
    public void setTestClass(String pTestClass) {
        testClass_ = pTestClass;
    }

    /**
     * @return the testName
     */
    public String getTestMethodName() {
        return testMethodName_;
    }

    /**
     * @param pTestName
     *            the testName to set
     */
    @XmlElement(name = "name")
    public void setTestMethodName(String pTestMethodName) {
        testMethodName_ = pTestMethodName;
    }
}
