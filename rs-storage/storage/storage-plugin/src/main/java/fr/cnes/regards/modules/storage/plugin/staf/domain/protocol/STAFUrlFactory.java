package fr.cnes.regards.modules.storage.plugin.staf.domain.protocol;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

import fr.cnes.regards.modules.storage.plugin.staf.domain.AbstractPhysicalFile;
import fr.cnes.regards.modules.storage.plugin.staf.domain.PhysicalCutPartFile;
import fr.cnes.regards.modules.storage.plugin.staf.domain.PhysicalTARFile;
import fr.cnes.regards.modules.storage.plugin.staf.domain.STAFException;

public class STAFUrlFactory {

    public static final String STAF_URL_PROTOCOLE = "staf";

    public static final String STAF_URL_REGEXP = "^staf://(.*)/([^?]*)?{0,1}(.*)$";

    private STAFUrlFactory() {

    }

    public static Map<Path, URL> getSTAFFullURLs(AbstractPhysicalFile pSTAFStoredFile)
            throws MalformedURLException, STAFException {

        Map<Path, URL> urls = Maps.newHashMap();
        // Construct standard staf:/<ARCHIVE>/<NODE> path
        String urlInitialPath = String
                .format("%s:/%s", STAF_URL_PROTOCOLE,
                        Paths.get(pSTAFStoredFile.getStafArchiveName(), pSTAFStoredFile.getStafNode()));
        String fileNamePath = pSTAFStoredFile.getSTAFFilePath().getFileName().toString();
        Set<Path> rawFiles = pSTAFStoredFile.getRawAssociatedFiles();

        if (!rawFiles.isEmpty()) {
            // Add file access
            switch (pSTAFStoredFile.getArchiveMode()) {
                case CUT_PART:
                    PhysicalCutPartFile cutPartFile = (PhysicalCutPartFile) pSTAFStoredFile;
                    fileNamePath = cutPartFile.getIncludingCutFile().getSTAFFilePath().toString();
                    urls.put(rawFiles.stream().findFirst().get(),
                             new URL(String.format("%s/%s?parts=%d", urlInitialPath, fileNamePath,
                                                   cutPartFile.getIncludingCutFile().getCutedFileParts().size())));
                    break;
                case TAR:
                    PhysicalTARFile tarFile = (PhysicalTARFile) pSTAFStoredFile;
                    //  Find associated raw file to the given file in TAR by name
                    for (Path fileInTar : tarFile.getFilesInTar().keySet()) {
                        Path rawFilePath = tarFile.getFilesInTar().get(fileInTar);
                        if (rawFilePath != null) {
                            urls.put(rawFilePath,
                                     new URL(String.format("%s/%s?filename=%s", urlInitialPath,
                                                           pSTAFStoredFile.getSTAFFilePath().getFileName().toString(),
                                                           fileInTar.getFileName().toString())));
                        }

                    }
                    break;
                case NORMAL:
                default:
                    urls.put(rawFiles.stream().findFirst().get(),
                             new URL(String.format("%s/%s", urlInitialPath, fileNamePath)));
                    break;
            }
        }
        return urls;

    }

}
