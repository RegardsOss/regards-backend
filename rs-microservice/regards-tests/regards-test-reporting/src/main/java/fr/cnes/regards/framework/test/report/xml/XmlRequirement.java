/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.test.report.xml;

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
    private String requirement;

    /**
     * List of associated tests
     */
    private List<XmlTest> tests;

    /**
     * @return the requirement
     */
    public String getRequirement() {
        return requirement;
    }

    /**
     * @param pRequirement
     *            the requirement to set
     */
    @XmlAttribute(name = "ref")
    public void setRequirement(String pRequirement) {
        requirement = pRequirement;
    }

    /**
     * @return the tests
     */
    public List<XmlTest> getTests() {
        return tests;
    }

    /**
     * @param pTests
     *            the tests to set
     */
    @XmlElement(name = "test")
    public void setTests(List<XmlTest> pTests) {
        tests = pTests;
    }

    /**
     * Add a test
     *
     * @param pTest
     *            test to add
     */
    public void addTest(XmlTest pTest) {
        if (tests == null) {
            tests = new ArrayList<>();
        }
        tests.add(pTest);
    }

    /**
     * Add all tests
     *
     * @param pTests
     *            tests to add
     */
    public void addAllTests(List<XmlTest> pTests) {
        if (tests == null) {
            tests = new ArrayList<>();
        }
        tests.addAll(pTests);
    }
}
