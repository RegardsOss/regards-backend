/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @author msordi
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
