/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.utils.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * Utils to handle files.
 * @author Sébastien Binda
 */
public class CommonFileUtils {

    private CommonFileUtils() {
    }

    /**
     * Return the first non existing file name into the pDirectory {@link Path} given and related to the given {@link String} pOrigineFileName.
     * If a file exists in the pDirectory with the pOrigineFileName as name, so this method return a file name as :<br/>
     *  [pOrigineFileName without extension]_[i].[pOrigineFileName extension] where i is an integer.
     * @param pDirectory {@link Path} Directory to scan for existings files.
     * @param pOrigineFileName {@link String} Original file name wanted.
     * @return {@link String} First available file name.
     * @throws IOException Error reading the {@link Path} pDirectory
     */
    public static String getAvailableFileName(Path pDirectory, String pOrigineFileName) throws IOException {

        String availableFileName = pOrigineFileName;

        int cpt = 1;
        // Get all existing file names
        Set<String> fileNames = Sets.newHashSet();
        Files.walk(pDirectory, 1).forEach(f -> fileNames.add(f.getFileName().toString()));
        while (fileNames.contains(availableFileName)) {
            int index = availableFileName.indexOf('.');
            if (index > 0) {
                availableFileName = String.format("%s_%d.%s", availableFileName.substring(0, index), cpt,
                                                  availableFileName.substring(index + 1));
            }
            cpt++;
        }

        return availableFileName;

    }

}
