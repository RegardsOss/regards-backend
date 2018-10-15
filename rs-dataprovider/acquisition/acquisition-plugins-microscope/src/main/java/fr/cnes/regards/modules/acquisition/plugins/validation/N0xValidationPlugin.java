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
package fr.cnes.regards.modules.acquisition.plugins.validation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import com.google.common.base.Strings;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.acquisition.exception.MetadataException;
import fr.cnes.regards.modules.acquisition.plugins.IValidationPlugin;
import fr.cnes.regards.modules.acquisition.plugins.MicroHelper;
import fr.cnes.regards.modules.acquisition.plugins.Microscope;

/**
 * Microscope N0x products validation.<br/>
 * File to validate is found under directory with same name as given metadata file (without _metadata.xml at the end)
 * and has tgz extension. A file with same name with _MD5.txt at the end (in place of .tgz) contains MD5 informations.
 * @author Olivier Rousselot
 */
@Plugin(id = "N0xValidationPlugin", version = "1.0.0-SNAPSHOT", description =
        "Read given metadata XML file path and validate tgz file under associated directory with MD5 value contained "
                + "into '_MD5.txt'", author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0",
        owner = "CSSI", url = "https://github.com/RegardsOss")
public class N0xValidationPlugin implements IValidationPlugin {

    @Override
    public boolean validate(Path metadataFilePath) throws ModuleException {
        try {
            File dir = findProductDirectory(metadataFilePath);

            // Search for ".tgz" data file...
            File dataFile = MicroHelper.findFileWithExtension(dir, Microscope.TGZ_EXT);
            // ...and associated "_MD5.txt" file
            File checksumFile = new File(dataFile.getParentFile(), dataFile.getName()
                    .replace(Microscope.TGZ_EXT, Microscope.CHECKSUM_FILE_SUFFIX));
            if (!checksumFile.exists()) {
                throw new FileNotFoundException(
                        String.format("MD5 file '%s' does not exist", checksumFile.getAbsolutePath()));
            } else if (!checksumFile.canRead()) {
                throw new IOException(String.format("Missing read access to '%s'", checksumFile.getAbsolutePath()));
            }
            // Search for provided checksum value
            String providedChecksum = null;
            for (String line : Files.readAllLines(checksumFile.toPath())) {
                if (line.startsWith(Microscope.CHECKSUM_KEY_COMMENT)) {
                    providedChecksum = line.substring(
                            line.indexOf(Microscope.CHECKSUM_KEY_COMMENT) + Microscope.CHECKSUM_KEY_COMMENT.length())
                            .trim();
                }
            }
            if (Strings.isNullOrEmpty(providedChecksum)) {
                throw new IOException(String.format(
                        "Cannot find a line starting with '%s' and containing checksum value into '%s' file",
                        Microscope.CHECKSUM_KEY_COMMENT, checksumFile.getAbsolutePath()));
            }
            // Compute data file checksum
            String computedChecksum = ChecksumUtils
                    .computeHexChecksum(new FileInputStream(dataFile), Microscope.CHECKSUM_ALGO);
            return providedChecksum.equals(computedChecksum);
        } catch (IOException e) {
            throw new MetadataException(
                    String.format("Error while attempting to read metadata file '%s'", metadataFilePath.toString()), e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieve product directory from metadata "_metadata.xml" path.
     * For N0b_CMSM and N0b_CMSM, product directory is near metadata file with same name until "_metadata.xml"
     */
    protected File findProductDirectory(Path metadataFilePath) throws IOException {
        // Determine product directory
        return MicroHelper.findDirStartingWithSameName(metadataFilePath);
    }
}
