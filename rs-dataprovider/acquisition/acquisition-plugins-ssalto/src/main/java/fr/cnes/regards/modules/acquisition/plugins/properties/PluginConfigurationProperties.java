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
package fr.cnes.regards.modules.acquisition.plugins.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

import fr.cnes.regards.modules.acquisition.finder.AbstractAttributeFinder;

/**
 * This class is used to load the DataSet plugin configuration and <br>
 * to pass the parameters of the ProductMetaPlugin.<br>
 * 
 * @author Christophe Mertz
 *
 */
public class PluginConfigurationProperties {

    /**
     * The separator's value used in the ORL file properties
     */
    private static final String SEPARATOR = ";";

    protected String cycleFilePath;

    protected String arcFilePath;

    protected String orfFilePathPattern;

    /**
     * filePattern du nom du fichier
     */
    protected String fileNamePattern;

    /**
     * liste des finder
     */
    private SortedMap<Integer, AbstractAttributeFinder> finderList;

    /**
     * Default constructor
     */
    public PluginConfigurationProperties() {
        super();
    }

    public String getFileNamePattern() {
        return fileNamePattern;
    }

    public String getCycleFilePath() {
        return cycleFilePath;
    }

    public void setCycleFilePath(String cycleFilePath) {
        this.cycleFilePath = cycleFilePath;
    }

    public String getArcFilePath() {
        return arcFilePath;
    }

    public void setArcFilePath(String arcFilePath) {
        this.arcFilePath = arcFilePath;
    }

    public String[] getOrfFilePathPattern() {
        String[] orfFilePath = new String[0];

        orfFilePath = orfFilePathPattern.split(SEPARATOR);

        return orfFilePath;
    }

    public void setOrfFilePathPattern(String orfFilePathPattern) {
        this.orfFilePathPattern = orfFilePathPattern;
    }

    public void setFileNamePattern(String filePattern) {
        fileNamePattern = filePattern;
    }

    /**
     * ajoute un finder standard
     * 
     * @param finder
     */
    public void addFileFinder(AbstractAttributeFinder finder) {
        if (finderList == null) {
            finderList = new TreeMap<>();
        }
        finderList.put(Integer.valueOf(finder.getOrder()), finder);
    }

    public Collection<AbstractAttributeFinder> getFinderList() {
        if (finderList == null) {
            return new ArrayList<>();
        } else {
            return finderList.values();
        }
    }

}
