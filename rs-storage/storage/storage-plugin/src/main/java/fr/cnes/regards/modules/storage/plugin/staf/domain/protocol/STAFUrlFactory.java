/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.staf.domain.protocol;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.google.common.collect.Maps;

import fr.cnes.regards.modules.storage.plugin.staf.domain.AbstractPhysicalFile;
import fr.cnes.regards.modules.storage.plugin.staf.domain.PhysicalCutPartFile;
import fr.cnes.regards.modules.storage.plugin.staf.domain.PhysicalNormalFile;
import fr.cnes.regards.modules.storage.plugin.staf.domain.PhysicalTARFile;
import fr.cnes.regards.modules.storage.plugin.staf.domain.exception.STAFException;

/**
 * STAF URL Factory to create STAF URLs of stored files.
 *
 * Exemples :
 * <ul>
 * <li>Simple file : staf:/ARCHIVE/node/foo.txt</li>
 * <li>File stored in TAR file : staf:/ARCHIVE/node/foo.tar?filename=bar.txt</li>
 * <li>File stored as multiple cut parts : staf:/ARCHIVE/node/foo.txt?parts=12</li>
 * </ul>
 *
 * @author Sébastien Binda
 *
 */
public class STAFUrlFactory {

    /**
     * Protocole name
     */
    public static final String STAF_URL_PROTOCOLE = "staf";

    /**
     * Regexp to decode STAF URL.<br/>
     * <ul>
     * <li> group 1 : STAF Archive name</li>
     * <li> group 2 : STAF File path. Containing STAF Node and STAF File name</li>
     * <li> group 3 (optional) : parameters as
     * filename : for files in TAR.
     * parts : for cuted file this parameter is the number of parts
     * </li>.
     * </ul>
     */
    public static final String STAF_URL_REGEXP = "^staf://(.*)/([^?]*)?{0,1}(.*)$";

    /**
     * filename parameter of the url to indicate the name of the file associated to the URL into the given TAR.
     * Exemple : staf:/ARCHIVE/node/foo.tar?filename=bar.txt
     */
    public static final String TAR_FILENAME_PARAMETER = "filename";

    /**
     * parts parameter of the url to indicate the number of cute files stored into STAF and composing the full file stored.
     * Exemple for a file cuted in 12 parts into STAF : staf:/ARCHIVE/node/foo.txt?parts=12
     */
    public static final String CUT_PARTS_PARAMETER = "parts";

    /**
     * String format to generate a STAF URL for a given file path.
     */
    private static final String STANDARD_URL_STRING_FORMAT = STAF_URL_PROTOCOLE + ":/%s";

    /**
     * PRivate constructor. Only static methods.
     */
    private STAFUrlFactory() {
    }

    /**
     * Create a mapping between the Path of the RAW files stored and the STAF URLs.
     *
     * @param pSTAFStoredFile {@link AbstractPhysicalFile} stored.
     * @return {@link Map}<@link Path},{@link URL}>
     * @throws STAFUrlException Error during URL mapping.
     */
    public static Map<Path, URL> getSTAFFullURLs(AbstractPhysicalFile pSTAFStoredFile) throws STAFUrlException {

        Map<Path, URL> urls = Maps.newHashMap();
        Optional<Path> firstRawFile = pSTAFStoredFile.getRawAssociatedFiles().stream().findFirst();
        if (firstRawFile.isPresent()) {
            try {
                // Add file access
                switch (pSTAFStoredFile.getArchiveMode()) {
                    case CUT_PART:
                        urls.put(firstRawFile.get(), getCutPartFileSTAFUrl((PhysicalCutPartFile) pSTAFStoredFile));
                        break;
                    case TAR:
                        urls.putAll(getTARFilesSTAFUrl((PhysicalTARFile) pSTAFStoredFile));
                        break;
                    case NORMAL:
                    default:
                        urls.put(firstRawFile.get(), getNormalFileSTAFUrl((PhysicalNormalFile) pSTAFStoredFile));
                        break;
                }
            } catch (STAFException e) {
                throw new STAFUrlException(e.getMessage(), e);
            }
        }
        return urls;
    }

    /**
     * Create the STAF Protocol URL for the given {@link PhysicalCutPartFile}
     * @param pCurPartFile {@link PhysicalCutPartFile} file to retrieive STAF Protocol URL.
     * @return {@link URL}
     * @throws STAFUrlException Error during URL construction.
     */
    public static URL getCutPartFileSTAFUrl(PhysicalCutPartFile pCutPartFile) throws STAFUrlException {
        try {
            // Construct standard staf:/<ARCHIVE>/<NODE> path
            String urlInitialPath = String
                    .format(STANDARD_URL_STRING_FORMAT,
                            Paths.get(pCutPartFile.getStafArchiveName(), pCutPartFile.getStafNode()));
            String fileNamePath = pCutPartFile.getIncludingCutFile().getSTAFFilePath().toString();
            return new URL(String.format("%s/%s?%s=%d", urlInitialPath, fileNamePath, CUT_PARTS_PARAMETER,
                                         pCutPartFile.getIncludingCutFile().getCutedFileParts().size()));
        } catch (MalformedURLException e) {
            throw new STAFUrlException(e.getMessage(), e);
        }
    }

    /**
     * Create a mapping between raw files stored and STAF URL into the given TAR.
     *
     * @param pTarFile {@link PhysicalTARFile} TAR File stored.
     * @return {@link Map}<@link Path},{@link URL}>
     * @throws STAFUrlException Error creating STAF URLs
     */
    public static Map<Path, URL> getTARFilesSTAFUrl(PhysicalTARFile pTarFile) throws STAFUrlException {
        try {
            Map<Path, URL> urls = Maps.newHashMap();
            // Construct standard staf:/<ARCHIVE>/<NODE> path
            String urlInitialPath = String.format(STANDARD_URL_STRING_FORMAT,
                                                  Paths.get(pTarFile.getStafArchiveName(), pTarFile.getStafNode()));
            for (Entry<Path, Path> fileInTar : pTarFile.getFilesInTar().entrySet()) {
                Path localFilePath = fileInTar.getValue();
                Path stafFilePath = pTarFile.getSTAFFilePath();
                if ((stafFilePath != null) && (localFilePath != null)) {
                    URL url = new URL(
                            String.format("%s/%s?%s=%s", urlInitialPath, stafFilePath.getFileName().toString(),
                                          TAR_FILENAME_PARAMETER, localFilePath.getFileName().toString()));
                    urls.put(fileInTar.getValue(), url);
                }
            }
            return urls;
        } catch (MalformedURLException | STAFException e) {
            throw new STAFUrlException(e.getMessage(), e);
        }
    }

    /**
     * Create the STAF Protocol URL for the given {@link Path} file from the {@link PhysicalTARFile} TAR file.<br/>
     * <b>Exemple</b> : staf:/ARCHIVE/node/foo.tar?filename=bar.txt
     *
     * @param pRawfile {@link Path} The file to get STAF URL for.
     * @param pTarFile {@link PhysicalTARFile} file to retrieive STAF Protocol URL.
     * @return {@link URL}
     * @throws STAFUrlException Error during URL construction.
     */
    public static Optional<URL> getTARFileSTAFUrl(PhysicalTARFile pTarFile, Path pRawFile) throws STAFUrlException {
        try {
            Optional<URL> url = Optional.empty();
            // Construct standard staf:/<ARCHIVE>/<NODE> path
            String urlInitialPath = String.format("%s:/%s", STAF_URL_PROTOCOLE,
                                                  Paths.get(pTarFile.getStafArchiveName(), pTarFile.getStafNode()));
            for (Entry<Path, Path> fileInTar : pTarFile.getFilesInTar().entrySet()) {
                if (pRawFile == fileInTar.getKey()) {
                    url = Optional.of(new URL(String
                            .format("%s/%s?%s=%s", urlInitialPath, pTarFile.getSTAFFilePath().getFileName().toString(),
                                    TAR_FILENAME_PARAMETER, fileInTar.getValue().getFileName().toString())));
                }
            }
            return url;
        } catch (MalformedURLException | STAFException e) {
            throw new STAFUrlException(e.getMessage(), e);
        }
    }

    /**
     * Create the STAF Protocol URL for the given {@link PhysicalNormalFile}
     * @param pFile {@link PhysicalNormalFile} file to retrieive STAF Protocol URL.
     * @return {@link URL}
     * @throws STAFUrlException Error during URL construction.
     */
    public static URL getNormalFileSTAFUrl(PhysicalNormalFile pFile) throws STAFUrlException {
        try {
            // Construct standard staf:/<ARCHIVE>/<NODE> path
            String urlInitialPath = String.format("%s:/%s", STAF_URL_PROTOCOLE,
                                                  Paths.get(pFile.getStafArchiveName(), pFile.getStafNode()));
            String fileNamePath = pFile.getSTAFFilePath().getFileName().toString();
            return new URL(String.format("%s/%s", urlInitialPath, fileNamePath));
        } catch (MalformedURLException e) {
            throw new STAFUrlException(e.getMessage(), e);
        }
    }

}
