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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christophe Mertz
 *
 */
public class AcquisitionScanPluginHelper {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionScanPluginHelper.class);

    /**
     * Converts a a pattern to a Java pattern.<br>
     * The table below shows the conversions that are applied.<br>
     * The order of this 2 conversions is important, it should be not modified.<br>
     * <table border=1 cellpadding=2>
     * <tr>
     * <th>Order</th>
     * <th>Original pattern</th>
     * <th>Java pattern</th>
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
     * @param originalPattern a pattern to converts to a Java pattern
     * @return the Java pattern
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
     * @param replacement the replacement value
     * @param target the {@link String} in that apply the replacement
     * @return a new {@link String} where a the pattern is replaced by a replacement value
     */
    protected String replacePattern(String patternToReplace, String replacement, String target) {
        Pattern pattern = Pattern.compile(patternToReplace);
        Matcher matcher = pattern.matcher(target);
        return matcher.replaceAll(replacement);
    }

    /**
     * {@link List} the files of a directory that match a {@link RegexFilenameFilter} and that the last modification
     * date is after a {@link OffsetDateTime}
     *
     * @param dirFile the directory where to get the files
     * @param filter the {@link RegexFilenameFilter} to apply
     * @param lastAcqDate the {@link OffsetDateTime} to apply to filer the file
     * @return List<File> the {@link List} of {@link File} that match the {@link RegexFilenameFilter} and the
     *         {@link OffsetDateTime}
     */
    protected List<File> filteredFileList(File dirFile, RegexFilenameFilter filter, OffsetDateTime lastAcqDate) {
        // Look for files with match the pattern
        File[] nameFileArray = dirFile.listFiles(filter);

        List<File> sortedFileList = new ArrayList<>(nameFileArray.length);
        for (File element : nameFileArray) {

            if ((lastAcqDate == null)
                    || OffsetDateTime.ofInstant(Instant.ofEpochMilli(element.lastModified()), ZoneId.of("UTC"))
                            .isAfter(lastAcqDate.atZoneSameInstant(ZoneId.of("UTC")).toOffsetDateTime())) {
                sortedFileList.add(element);
            } else {
                LOGGER.info("File <{}> is too old", element.getName());
            }

        }

        return sortedFileList;
    }
}
