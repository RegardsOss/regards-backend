/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.plugins.ssalto;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Christophe Mertz
 */

public class PluginTestDef {

    private boolean isMultipleFileTest;

    private String dataSetName;

    private String fileDirectory;

    private List<String> fileNameList;

    private String productName;

    public PluginTestDef(String newDataSetName, String newFileDirectory) {
        super();
        dataSetName = newDataSetName;
        fileDirectory = newFileDirectory;
        fileNameList = new ArrayList<String>();
        isMultipleFileTest = false;
    }

    public PluginTestDef(String newDataSetName, String newFileDirectory, String newFileName) {
        super();
        dataSetName = newDataSetName;
        fileDirectory = newFileDirectory;
        fileNameList = new ArrayList<String>();
        fileNameList.add(newFileName);
        productName = newFileName;
        isMultipleFileTest = false;
    }

    public PluginTestDef(String newDataSetName, String newFileDirectory, List<String> newFileNameList, String newProductName) {
        super();
        dataSetName = newDataSetName;
        fileDirectory = newFileDirectory;
        fileNameList = newFileNameList;
        productName = newProductName;
        isMultipleFileTest = true;
    }

    protected String getDataSetName() {
        return dataSetName;
    }

    protected String getFileDirectory() {
        return fileDirectory;
    }

    protected List<String> getFileNameList() {
        return fileNameList;
    }

    protected String getProductName() {
        return productName;
    }

    protected boolean isMultipleFileTest() {
        return isMultipleFileTest;
    }

    protected void setProductName(String newProductName) {
        productName = newProductName;
    }
}
