/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.test.report.xml;

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
    private String purpose;

    /**
     * Test class
     */
    private String testClass;

    /**
     * Test method
     */
    private String testMethodName;

    public String getPurpose() {
        return purpose;
    }

    @XmlElement(name = "purpose")
    public void setPurpose(String pPurpose) {
        purpose = pPurpose;
    }

    public String getTestClass() {
        return testClass;
    }

    @XmlElement(name = "class")
    public void setTestClass(String pTestClass) {
        testClass = pTestClass;
    }

    public String getTestMethodName() {
        return testMethodName;
    }

    @XmlElement(name = "name")
    public void setTestMethodName(String pTestMethodName) {
        testMethodName = pTestMethodName;
    }
}
