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
package fr.cnes.regards.modules.acquisition.service.plugins;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.modules.acquisition.plugins.IScanPlugin;

/**
 * Scan directories and return detected files according to last modification date filter and a regular expression
 * pattern.
 *
 * @author Marc Sordi
 *
 */
@Plugin(id = "RegexDiskScanning", version = "1.0.0-SNAPSHOT",
        description = "Scan directories to detect files filtering with a regular expression pattern",
        author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class RegexDiskScanning implements IScanPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegexDiskScanning.class);

    public static final String FIELD_DIRS = "directories";

    public static final String FIELD_REGEX = "pattern";

    @PluginParameter(name = FIELD_DIRS, label = "List of directories to scan")
    private List<String> directories;

    @PluginParameter(name = FIELD_REGEX, label = "Regular expression", defaultValue = ".*", optional = true)
    private String regex;

    private DirectoryStream.Filter<Path> filter;

    @Override
    public List<Path> scan(Optional<OffsetDateTime> lastModificationDate) throws ModuleException {

        // Init filter
        filter = file -> Pattern.compile(regex).matcher(file.getFileName().toString()).matches();

        List<Path> scannedFiles = new ArrayList<>();

        for (String dir : directories) {
            Path dirPath = Paths.get(dir);
            if (Files.isDirectory(dirPath)) {
                scannedFiles.addAll(scanDirectory(dirPath, lastModificationDate));
            } else {
                LOGGER.error("Invalid directory path : {}", dirPath.toString());
            }
        }
        return scannedFiles;
    }

    private List<Path> scanDirectory(Path dirPath, Optional<OffsetDateTime> lastModificationDate) {
        long startTime = System.currentTimeMillis();
        List<Path> scannedFiles = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, filter)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    if (lastModificationDate.isPresent()) {
                        OffsetDateTime lmd = OffsetDateTime.ofInstant(Files.getLastModifiedTime(entry).toInstant(),
                                                                      ZoneOffset.UTC);
                        if (lmd.isAfter(lastModificationDate.get())) {
                            scannedFiles.add(entry);
                        }
                    } else {
                        scannedFiles.add(entry);
                    }
                }
            }
        } catch (IOException x) {
            throw new PluginUtilsRuntimeException("Scanning failure", x);
        }

        LOGGER.info("{} new file(s) scanned inside the directory {} in {} milliseconds", scannedFiles.size(), dirPath,
                    System.currentTimeMillis() - startTime);
        return scannedFiles;
    }

    // FIXME test before regex adapting
    // /**
    // * Converts a a pattern to a Java pattern.<br>
    // * The table below shows the conversions that are applied.<br>
    // * The order of this 2 conversions is important, it should be not modified.<br>
    // * <table border=1 cellpadding=2>
    // * <tr>
    // * <th>Order</th>
    // * <th>Original pattern</th>
    // * <th>Java pattern</th>
    // * </tr>
    // * <tr>
    // * <td>1</td>
    // * <td>.</td>
    // * <td>\.</td>
    // * </tr>
    // * <tr>
    // * <td>2</td>
    // * <td>*</td>
    // * <td>.*</td>
    // * </tr>
    // * </table>
    // *
    // * @param originalPattern a pattern to converts to a Java pattern
    // * @return the Java pattern
    // */
    // protected String getAdaptedPattern(String originalPattern) {
    //
    // String adaptedPattern = originalPattern;
    // // "." => "\."
    // adaptedPattern = replacePattern("\\.", "\\\\.", adaptedPattern);
    // // "*" => ".*"
    // adaptedPattern = replacePattern("\\*", "\\.\\*", adaptedPattern);
    // return adaptedPattern;
    // }
    //
    // /**
    // * Replace a pattern by a replacement value in a {@link String}
    // *
    // * @param patternToReplace the {@link String} to replace
    // * @param replacement the replacement value
    // * @param target the {@link String} in that apply the replacement
    // * @return a new {@link String} where a the pattern is replaced by a replacement value
    // */
    // protected String replacePattern(String patternToReplace, String replacement, String target) {
    // Pattern pattern = Pattern.compile(patternToReplace);
    // Matcher matcher = pattern.matcher(target);
    // return matcher.replaceAll(replacement);
    // }

}
