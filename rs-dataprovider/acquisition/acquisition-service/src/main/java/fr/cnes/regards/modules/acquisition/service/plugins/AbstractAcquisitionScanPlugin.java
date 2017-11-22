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

package fr.cnes.regards.modules.acquisition.service.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.acquisition.builder.FileAcquisitionInformationsBuilder;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileStatus;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;

/**
 * 
 * @author Christophe Mertz
 *
 */
public abstract class AbstractAcquisitionScanPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAcquisitionScanPlugin.class);

    /**
     * Process the checksum of the {@link File} inside the {@link AcquisitionFile}.
     *  
     * @param acqFile the {@link AcquisitionFile}
     * @param algorithm the algorithm to used, {@link MessageDigest} for the possible values
     */
    protected void calcCheckSum(AcquisitionFile acqFile, String algorithm) {
        if (algorithm == null) {
            acqFile.setChecksum(null);
            acqFile.setChecksumAlgorithm(null);
            return;
        }
        try (FileInputStream fis = new FileInputStream(acqFile.getFile())) {
            acqFile.setChecksum(ChecksumUtils.computeHexChecksum(fis, algorithm));
            acqFile.setChecksumAlgorithm(algorithm);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }

    }

    /**
     * Transforme le pattern indique dans la fourniture en pattern Java.<br>
     * Les correspondances implementees sont presentees dans le tableau suivant.<br>
     * Il est primordial de ne pas modifier l'ordre de prise en compte.<br>
     * <table border=1 cellpadding=2>
     * <tr>
     * <th>Ordre de prise en compte</th>
     * <th>Original</th>
     * <th>Adapte</th>
     * </tr>
     * <tr>
     * <td>1</td>
     * <td>.</td>
     * <td>\.</td>
     * </tr>
     * <tr>
     * <td>2</td>
     * <td>*</td>
     * <td>.*</td>
     * </tr>
     * </table>
     * 
     * @param originalPattern
     * @return
     */
    protected String getAdaptedPattern(String originalPattern) {

        String adaptedPattern = originalPattern;
        // "." => "\."
        adaptedPattern = replacePattern("\\.", "\\\\.", adaptedPattern);
        // "*" => ".*"
        adaptedPattern = replacePattern("\\*", "\\.\\*", adaptedPattern);
        return adaptedPattern;
    }

    /**
     * Replace a pattern by a replacement value in a {@link String}
     * 
     * @param patternToReplace the {@link String} to replace
     * @param replacement
     * @param target
     * @return a new {@link String} where the pattern is replaced by a replacement value
     */
    protected String replacePattern(String patternToReplace, String replacement, String target) {
        Pattern pattern = Pattern.compile(patternToReplace);
        Matcher matcher = pattern.matcher(target);
        return matcher.replaceAll(replacement);
    }

    /**
     * {@link List} the files of a directory that match a {@link RegexFilenameFilter} and that the last modification date is after a {@link OffsetDateTime} 
     *
     * @param dirFile the directory where to get the files 
     * @param filter the {@link RegexFilenameFilter} to apply
     * @param lastAcqDate the {@link OffsetDateTime} to apply to filer the file
     * @return List<File> the {@link List} of {@link File} that match the {@link RegexFilenameFilter} and the {@link OffsetDateTime}
     */
    protected List<File> filteredFileList(File dirFile, RegexFilenameFilter filter, OffsetDateTime lastAcqDate) {
        // Look for files with match the pattern
        File[] nameFileArray = dirFile.listFiles(filter);

        List<File> sortedFileList = new ArrayList<>(nameFileArray.length);
        for (File element : nameFileArray) {

            if (lastAcqDate == null
                    || OffsetDateTime.ofInstant(Instant.ofEpochMilli(element.lastModified()), ZoneId.of("UTC"))
                            .isAfter(lastAcqDate.atZoneSameInstant(ZoneId.of("UTC")).toOffsetDateTime())) {
                sortedFileList.add(element);
            } else {
                LOGGER.info("File <{}> is too old", element.getName());
            }

        }

        return sortedFileList;
    }

    /**
     * Create an {@link AcquisitionFile} and process the checksum of the {@link File}
     * @param baseFile the {@link File} for which to create an {@link AcquisitionFile}
     * @param metaFile the {@link MetaFile}
     * @param algorithm the algorithm to used for the checksum, {@link MessageDigest} for the possible values
     * @return
     */
    protected AcquisitionFile initAcquisitionFile(File baseFile, MetaFile metaFile, String algorithm) {
        AcquisitionFile acqFile = new AcquisitionFile();
        acqFile.setMetaFile(metaFile);
        acqFile.setStatus(AcquisitionFileStatus.IN_PROGRESS);
        acqFile.setFileName(baseFile.getName());
        acqFile.setSize(baseFile.length());
        acqFile.setAcquisitionInformations(FileAcquisitionInformationsBuilder.build(baseFile.getParent().toString())
                .get());
        calcCheckSum(acqFile, algorithm);

        return acqFile;
    }

}
