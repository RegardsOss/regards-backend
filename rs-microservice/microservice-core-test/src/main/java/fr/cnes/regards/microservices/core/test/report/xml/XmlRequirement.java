/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.test.report.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Requirement information
 *
 * @author msordi
 *
 */
@XmlRootElement(name = "requirement")
public class XmlRequirement {

    /**
     * Requirement reference
     */
    private String requirement_;

    /**
     * List of associated tests
     */
    private List<XmlTest> tests_;

    /**
     * @return the requirement
     */
    public String getRequirement() {
        return requirement_;
    }

    /**
     * @param pRequirement
     *            the requirement to set
     */
    @XmlAttribute(name = "ref")
    public void setRequirement(String pRequirement) {
        requirement_ = pRequirement;
    }

    /**
     * @return the tests
     */
    public List<XmlTest> getTests() {
        return tests_;
    }

    /**
     * @param pTests
     *            the tests to set
     */
    @XmlElement(name = "test")
    public void setTests(List<XmlTest> pTests) {
        tests_ = pTests;
    }

    /**
     * Add a test
     *
     * @param pTest
     *            test to add
     */
    public void addTest(XmlTest pTest) {
        if (tests_ == null) {
            tests_ = new ArrayList<>();
        }
        tests_.add(pTest);
    }

    /**
     * Add all tests
     *
     * @param pTests
     *            tests to add
     */
    public void addAllTests(List<XmlTest> pTests) {
        if (tests_ == null) {
            tests_ = new ArrayList<>();
        }
        tests_.addAll(pTests);
    }
}
