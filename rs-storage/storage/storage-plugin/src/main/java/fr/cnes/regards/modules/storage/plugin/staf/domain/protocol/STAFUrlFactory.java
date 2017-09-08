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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;

import fr.cnes.regards.framework.staf.STAFArchiveModeEnum;
import fr.cnes.regards.framework.staf.STAFException;
import fr.cnes.regards.modules.storage.plugin.staf.domain.AbstractPhysicalFile;
import fr.cnes.regards.modules.storage.plugin.staf.domain.PhysicalCutPartFile;
import fr.cnes.regards.modules.storage.plugin.staf.domain.PhysicalNormalFile;
import fr.cnes.regards.modules.storage.plugin.staf.domain.PhysicalTARFile;

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
    public static final Pattern STAF_URL_REGEXP = Pattern.compile("^staf://([^/]*)(/[^?]*)?{0,1}(.*)$");

    /**
     * String format to generate a STAF URL for a given file path.
     */
    private static final String STANDARD_URL_STRING_FORMAT = STAF_URL_PROTOCOLE + "://%s";

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
            // Construct standard staf://<ARCHIVE>/<NODE> path
            String urlInitialPath = String
                    .format(STANDARD_URL_STRING_FORMAT,
                            Paths.get(pCutPartFile.getStafArchiveName(), pCutPartFile.getStafNode()));
            String fileNamePath = pCutPartFile.getIncludingCutFile().getSTAFFilePath().toString();
            return new URL(String.format("%s/%s?%s=%d", urlInitialPath, fileNamePath,
                                         STAFUrlParameter.CUT_PARTS_PARAMETER.getParameterName(),
                                         pCutPartFile.getIncludingCutFile().getCutedFileParts().size()));
        } catch (MalformedURLException | STAFException e) {
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
            // Construct standard staf://<ARCHIVE>/<NODE> path
            String urlInitialPath = String.format(STANDARD_URL_STRING_FORMAT,
                                                  Paths.get(pTarFile.getStafArchiveName(), pTarFile.getStafNode()));
            for (Entry<Path, Path> fileInTar : pTarFile.getFilesInTar().entrySet()) {
                Path localFilePath = fileInTar.getValue();
                Path stafFilePath = pTarFile.getSTAFFilePath();
                if ((stafFilePath != null) && (localFilePath != null)) {
                    URL url = new URL(
                            String.format("%s/%s?%s=%s", urlInitialPath, stafFilePath.getFileName().toString(),
                                          STAFUrlParameter.TAR_FILENAME_PARAMETER.getParameterName(),
                                          localFilePath.getFileName().toString()));
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
            // Construct standard staf://<ARCHIVE>/<NODE> path
            String urlInitialPath = String.format("%s:/%s", STAF_URL_PROTOCOLE,
                                                  Paths.get(pTarFile.getStafArchiveName(), pTarFile.getStafNode()));
            for (Entry<Path, Path> fileInTar : pTarFile.getFilesInTar().entrySet()) {
                if (pRawFile == fileInTar.getKey()) {
                    url = Optional.of(new URL(String.format("%s/%s?%s=%s", urlInitialPath,
                                                            pTarFile.getSTAFFilePath().getFileName().toString(),
                                                            STAFUrlParameter.TAR_FILENAME_PARAMETER.getParameterName(),
                                                            fileInTar.getValue().getFileName().toString())));
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
        } catch (MalformedURLException | STAFException e) {
            throw new STAFUrlException(e.getMessage(), e);
        }
    }

    /**
     * Return the complete STAF Node of the given {@link URL}
     * @param pUrl {@link URL}
     * @return {@link String}
     * @throws STAFUrlException Given {@link URL} is not a STAF URL.
     */
    public static String getSTAFNodeFromURL(URL pUrl) throws STAFUrlException {
        Matcher m = STAF_URL_REGEXP.matcher(pUrl.toString());
        if (m.matches()) {
            String filePath = m.group(2);
            int index = filePath.lastIndexOf('/');
            if (index >= 0) {
                return filePath.substring(0, index);
            } else {
                return filePath;
            }
        } else {
            throw new STAFUrlException(String.format("Invalid URL %s", pUrl.toString()));
        }
    }

    /**
     * Return the {@link STAFArchiveModeEnum} of the given {@link URL}
     * @param pUrl {@link URL}
     * @return {@link STAFArchiveModeEnum}
     * @throws STAFUrlException Given {@link URL} is not a STAF URL.
     */
    public static STAFArchiveModeEnum getSTAFArchiveModeFromURL(URL pUrl) throws STAFUrlException {
        Matcher m = STAF_URL_REGEXP.matcher(pUrl.toString());
        if (m.matches()) {
            if (pUrl.toString().contains(STAFUrlParameter.TAR_FILENAME_PARAMETER.getParameterName())) {
                return STAFArchiveModeEnum.TAR;
            }
            if (pUrl.toString().contains(STAFUrlParameter.CUT_PARTS_PARAMETER.getParameterName())) {
                return STAFArchiveModeEnum.CUT;
            }
            return STAFArchiveModeEnum.NORMAL;
        } else {
            throw new STAFUrlException(String.format("Invalid URL %s", pUrl.toString()));
        }
    }

    /**
     * Return the {@link STAFUrlParameter} values of the given {@link URL}
     * @param pUrl {@link URL}
     * @return {@link STAFUrlParameter}
     * @throws STAFUrlException Given {@link URL} is not a STAF URL.
     */
    public static Map<STAFUrlParameter, String> getSTAFURLParameters(URL pUrl) throws STAFUrlException {
        Map<STAFUrlParameter, String> parameters = Maps.newHashMap();
        Matcher m = STAF_URL_REGEXP.matcher(pUrl.toString());
        if (m.matches()) {
            String filePath = m.group(3);
            if (!filePath.isEmpty()) {
                int index = filePath.indexOf("=");
                if ((index >= 0) && filePath.contains(STAFUrlParameter.CUT_PARTS_PARAMETER.getParameterName())) {
                    parameters.put(STAFUrlParameter.CUT_PARTS_PARAMETER, filePath.substring(index + 1));
                } else if ((index >= 0)
                        && filePath.contains(STAFUrlParameter.TAR_FILENAME_PARAMETER.getParameterName())) {
                    parameters.put(STAFUrlParameter.TAR_FILENAME_PARAMETER, filePath.substring(index + 1));
                }
            }
            return parameters;
        } else {
            throw new STAFUrlException(String.format("Invalid URL %s", pUrl.toString()));
        }
    }

    /**
     * Return the STAF Archive name of the given {@link URL}
     * @param pUrl {@link URL}
     * @return {@link String}
     * @throws STAFUrlException Given {@link URL} is not a STAF URL.
     */
    public static String getSTAFArchiveFromURL(URL pUrl) throws STAFUrlException {
        Matcher m = STAF_URL_REGEXP.matcher(pUrl.toString());
        if (m.matches()) {
            return m.group(1);
        } else {
            throw new STAFUrlException(String.format("Invalid URL %s", pUrl.toString()));
        }
    }

    /**
     * Return the STAF File name of the given {@link URL}
     * @param pUrl {@link URL}
     * @return {@link String}
     * @throws STAFUrlException Given {@link URL} is not a STAF URL.
     */
    public static String getSTAFFileNameFromURL(URL pUrl) throws STAFUrlException {
        Matcher m = STAF_URL_REGEXP.matcher(pUrl.toString());
        if (m.matches()) {
            String filePath = m.group(2);
            int index = filePath.lastIndexOf('/');
            if (index >= 0) {
                return filePath.substring(index + 1);
            } else {
                return filePath;
            }
        } else {
            throw new STAFUrlException(String.format("Invalid URL %s", pUrl.toString()));
        }
    }

}
