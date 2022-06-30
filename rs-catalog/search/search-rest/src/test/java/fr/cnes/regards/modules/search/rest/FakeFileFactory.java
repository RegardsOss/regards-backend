/*
 * Copyright 2017-20XX CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.rest;

import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.indexer.domain.DataFile;

import java.util.Arrays;
import java.util.List;

public class FakeFileFactory {

    public String invalidFile() {
        return "checksumKo";
    }

    public List<DataFile> allDataFiles() {
        return Arrays.asList(quicklook_sd(),
                             quicklook_md(),
                             quicklook_hd(),
                             thumbnail(),
                             document(),
                             rawdata(),
                             description(),
                             aip(),
                             other());
    }

    public List<String> validFiles() {
        return Arrays.asList(validFile(),
                             quicklook_sd().getChecksum(),
                             quicklook_md().getChecksum(),
                             quicklook_hd().getChecksum(),
                             thumbnail().getChecksum(),
                             document().getChecksum(),
                             description().getChecksum());
    }

    public String validFile() {
        return "checksumOk";
    }

    public DataFile quicklook_sd() {
        DataFile file = new DataFile();
        file.setChecksum("quicklook_sd");
        file.setDataType(DataType.QUICKLOOK_SD);
        return file;
    }

    public DataFile quicklook_md() {
        DataFile file = new DataFile();
        file.setChecksum("quicklook_md");
        file.setDataType(DataType.QUICKLOOK_MD);
        return file;
    }

    public DataFile quicklook_hd() {
        DataFile file = new DataFile();
        file.setChecksum("quicklook_hd");
        file.setDataType(DataType.QUICKLOOK_HD);
        return file;
    }

    public DataFile thumbnail() {
        DataFile file = new DataFile();
        file.setChecksum("thumbnail");
        file.setDataType(DataType.THUMBNAIL);
        return file;
    }

    public DataFile document() {
        DataFile file = new DataFile();
        file.setChecksum("thumbnail");
        file.setDataType(DataType.DOCUMENT);
        return file;
    }

    public DataFile rawdata() {
        DataFile file = new DataFile();
        file.setChecksum("rawdata");
        file.setDataType(DataType.RAWDATA);
        return file;
    }

    public DataFile description() {
        DataFile file = new DataFile();
        file.setChecksum("description");
        file.setDataType(DataType.DESCRIPTION);
        return file;
    }

    public DataFile aip() {
        DataFile file = new DataFile();
        file.setChecksum("aip_file");
        file.setDataType(DataType.AIP);
        return file;
    }

    public DataFile other() {
        DataFile file = new DataFile();
        file.setChecksum("other_file");
        file.setDataType(DataType.OTHER);
        return file;
    }
}
