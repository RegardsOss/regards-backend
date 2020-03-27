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
package fr.cnes.regards.framework.test.report.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Requirement information
 * @author msordi
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
     * @param pRequirement the requirement to set
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
     * @param pTests the tests to set
     */
    @XmlElement(name = "test")
    public void setTests(List<XmlTest> pTests) {
        tests = pTests;
    }

    /**
     * Add a test
     * @param pTest test to add
     */
    public void addTest(XmlTest pTest) {
        if (tests == null) {
            tests = new ArrayList<>();
        }
        tests.add(pTest);
    }

    /**
     * Add all tests
     * @param pTests tests to add
     */
    public void addAllTests(List<XmlTest> pTests) {
        if (tests == null) {
            tests = new ArrayList<>();
        }
        tests.addAll(pTests);
    }
}
