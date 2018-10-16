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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.acquisition.plugins.IValidationPlugin;
import fr.cnes.regards.modules.acquisition.plugins.Microscope;

/**
 * Microscope CMSM scientific product validation.<br/>
 * File to validate is ZIP file. A ".md5sum" exists near it with MD5 checksum value.
 * @author Olivier Rousselot
 */
@Plugin(id = "CmsmScientificValidationPlugin", version = "1.0.0-SNAPSHOT",
        description = "Read given metadata XML file and validate determined file of which name is under "
                + "'nomFichierDonnee' tag with MD5 value under md5Check tag", author = "REGARDS Team",
        contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class CmsmScientificValidationPlugin implements IValidationPlugin {

    @Override
    public boolean validate(Path filePath) throws ModuleException {
        File md5SumFile = new File(filePath.getParent().toFile(),
                                   filePath.getFileName().toString().replace(".zip", ".md5sum"));
        try {
            if (!Files.isRegularFile(md5SumFile.toPath())) {
                throw new IOException(String.format("'%s' is not a regular file", md5SumFile.getAbsolutePath()));
            } else if (!md5SumFile.canRead()) {
                throw new IOException(String.format("Missing read access to '%s'", md5SumFile.getAbsolutePath()));
            }
            String md5Value = null;
            for (String line : Files.readAllLines(md5SumFile.toPath())) {
                String trimmedLine = line.trim();
                if (trimmedLine.length() != 0) {
                    md5Value = line.trim();
                    break;
                }
            }
            if (md5Value == null) {
                throw new IOException(
                        String.format("'%s' file doesn't contain MD5 checksum value", md5SumFile.getAbsolutePath()));
            }
            return ChecksumUtils.computeHexChecksum(new FileInputStream(filePath.toFile()), Microscope.CHECKSUM_ALGO)
                    .equals(md5Value);
        } catch (IOException e) {
            throw new ModuleException(
                    String.format("Error while attempting to read metadadata file '%s'", md5SumFile.toString()), e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
