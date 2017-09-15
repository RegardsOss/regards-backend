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
package fr.cnes.regards.framework.oais;

import java.net.MalformedURLException;
import java.net.URL;

import javax.validation.constraints.NotNull;

import fr.cnes.regards.framework.oais.urn.DataType;

/**
 *
 * OAIS data object
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 *
 */
public class DataObject {

    @NotNull
    private DataType dataType;

    @NotNull
    private URL url;

    private String filename;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    // FIXME to remove
    @Deprecated
    public DataObject generate() throws MalformedURLException {
        dataType = DataType.OTHER;
        url = new URL("ftp://bla");
        return this;
    }

    @Override
    public boolean equals(Object pOther) {
        return (pOther instanceof DataObject) && dataType.equals(((DataObject) pOther).dataType)
                && url.toString().equals(((DataObject) pOther).url.toString());
    }

    @Override
    public int hashCode() {
        return url.toString().hashCode();
    }
}
