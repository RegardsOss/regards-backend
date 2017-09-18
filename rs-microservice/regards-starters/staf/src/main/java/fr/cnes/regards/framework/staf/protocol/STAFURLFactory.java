/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf.protocol;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.staf.domain.AbstractPhysicalFile;
import fr.cnes.regards.framework.staf.domain.PhysicalCutFile;
import fr.cnes.regards.framework.staf.domain.PhysicalCutPartFile;
import fr.cnes.regards.framework.staf.domain.PhysicalNormalFile;
import fr.cnes.regards.framework.staf.domain.PhysicalTARFile;
import fr.cnes.regards.framework.staf.domain.STAFArchiveModeEnum;
import fr.cnes.regards.framework.staf.exception.STAFException;

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
public class STAFURLFactory {

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
    public static final Pattern STAF_URL_REGEXP = Pattern
            .compile("^" + STAF_URL_PROTOCOLE + "://([^/]*)(/[^?]*)?{0,1}(.*)$");

    /**
     * String format of the STAF url with parameters pattern.
     */
    public static final String STAF_PARAMETERIZED_URL_PATTERN = "%s/%s?%s=%s";

    /**
     * String format to generate a STAF URL for a given file path.
     */
    private static final String STANDARD_URL_STRING_FORMAT = STAF_URL_PROTOCOLE + "://%s";

    /**
     * PRivate constructor. Only static methods.
     */
    private STAFURLFactory() {
    }

    public static void initSTAFURLProtocol() {
        try {
            URL.setURLStreamHandlerFactory(new STAFURLStreamHandlerFactory());
        } catch (Error e) {
            // Factory already defined. Nothing to do.
        }
    }

    /**
     * Create a mapping between the Path of the RAW files stored and the STAF URLs.
     *
     * @param pSTAFStoredFile {@link AbstractPhysicalFile} stored.
     * @return {@link Map}<@link Path},{@link URL}>
     * @throws STAFURLException Error during URL mapping.
     */
    public static Map<Path, URL> getSTAFURLsPerRAWFileToArchive(AbstractPhysicalFile pSTAFStoredFile)
            throws STAFURLException {
        Map<Path, URL> urls = Maps.newHashMap();
        Optional<Path> firstRawFile = pSTAFStoredFile.getRawAssociatedFiles().stream().findFirst();
        if (firstRawFile.isPresent()) {
            try {
                // Add file access
                switch (pSTAFStoredFile.getArchiveMode()) {
                    case CUT_PART:
                        PhysicalCutPartFile partFile = (PhysicalCutPartFile) pSTAFStoredFile;
                        urls.put(firstRawFile.get(), getCutFileSTAFUrl(partFile.getIncludingCutFile()));
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
                throw new STAFURLException(e.getMessage(), e);
            }
        }
        return urls;
    }

    /**
     * Return all STAF URLs associated to given {@link AbstractPhysicalFile}
     * @param pPhysicalFile {@link AbstractPhysicalFile}
     * @return {@link Set}<{@link URL}> of each file in STAF associated to the given {@link AbstractPhysicalFile}
     * @throws STAFURLException Error during URL creation.
     */
    public static Set<URL> getSTAFURLs(AbstractPhysicalFile pPhysicalFile) throws STAFURLException {
        Set<URL> urls = Sets.newHashSet();
        // Add file access
        switch (pPhysicalFile.getArchiveMode()) {
            case CUT_PART:
                PhysicalCutPartFile partFile = (PhysicalCutPartFile) pPhysicalFile;
                urls.add(getCutFileSTAFUrl(partFile.getIncludingCutFile()));
                break;
            case TAR:
                urls.addAll(getTARFilesSTAFURLs((PhysicalTARFile) pPhysicalFile));
                break;
            case NORMAL:
                urls.add(getNormalFileSTAFUrl((PhysicalNormalFile) pPhysicalFile));
                break;
            case CUT:
                urls.add(getCutFileSTAFUrl((PhysicalCutFile) pPhysicalFile));
                break;
            default:
                // Nothing to do.
                break;
        }
        return urls;
    }

    /**
     * Create the STAF Protocol URL for the given {@link PhysicalCutPartFile}
     * @param pCurPartFile {@link PhysicalCutPartFile} file to retrieive STAF Protocol URL.
     * @return {@link URL}
     * @throws STAFURLException Error during URL construction.
     */
    public static URL getCutPartFileSTAFUrl(PhysicalCutPartFile pCutPartFile) throws STAFURLException {
        try {
            // Construct standard staf://<ARCHIVE>/<NODE> path
            String urlInitialPath = String
                    .format(STANDARD_URL_STRING_FORMAT,
                            Paths.get(pCutPartFile.getStafArchiveName(), pCutPartFile.getStafNode().toString()));
            String fileNamePath = pCutPartFile.getSTAFFilePath().getFileName().toString();
            return new URL(String.format("%s/%s", urlInitialPath, fileNamePath));
        } catch (MalformedURLException e) {
            throw new STAFURLException(e.getMessage(), e);
        }
    }

    /**
     * Create the STAF Protocol URL for the given {@link PhysicalCutFile}
     * @param pCutFile {@link PhysicalCutFile} file to retrieive STAF Protocol URL.
     * @return {@link URL}
     * @throws STAFURLException Error during URL construction.
     */
    public static URL getCutFileSTAFUrl(PhysicalCutFile pCutFile) throws STAFURLException {
        try {
            String urlInitialPath = String
                    .format(STANDARD_URL_STRING_FORMAT,
                            Paths.get(pCutFile.getStafArchiveName(), pCutFile.getStafNode().toString()));
            return new URL(String.format("%s/%s?%s=%d", urlInitialPath, pCutFile.getStafFileName(),
                                         STAFURLParameter.CUT_PARTS_PARAMETER.getParameterName(),
                                         pCutFile.getCutedFileParts().size()));
        } catch (MalformedURLException e) {
            throw new STAFURLException(e.getMessage(), e);
        }
    }

    /**
     * Create a mapping between raw files stored and STAF URL into the given TAR.
     *
     * @param pTarFile {@link PhysicalTARFile} TAR File stored.
     * @return {@link Map}<@link Path},{@link URL}>
     * @throws STAFURLException Error creating STAF URLs
     */
    public static Map<Path, URL> getTARFilesSTAFUrl(PhysicalTARFile pTarFile) throws STAFURLException {
        try {
            Map<Path, URL> urls = Maps.newHashMap();
            // Construct standard staf://<ARCHIVE>/<NODE> path
            String urlInitialPath = String
                    .format(STANDARD_URL_STRING_FORMAT,
                            Paths.get(pTarFile.getStafArchiveName(), pTarFile.getStafNode().toString()));
            for (Entry<Path, Path> fileInTar : pTarFile.getFilesInTar().entrySet()) {
                Path localFilePath = fileInTar.getValue();
                if (localFilePath != null) {
                    URL url = new URL(String.format("%s/%s?%s=%s", urlInitialPath, pTarFile.getStafFileName(),
                                                    STAFURLParameter.TAR_FILENAME_PARAMETER.getParameterName(),
                                                    localFilePath.getFileName().toString()));
                    urls.put(fileInTar.getValue(), url);
                }
            }
            return urls;
        } catch (MalformedURLException e) {
            throw new STAFURLException(e.getMessage(), e);
        }
    }

    /**
     * Create the STAF Protocol URL for the given {@link Path} file from the {@link PhysicalTARFile} TAR file.<br/>
     * <b>Exemple</b> : staf:/ARCHIVE/node/foo.tar?filename=bar.txt
     *
     * @param pRawfile {@link Path} The file to get STAF URL for.
     * @param pTarFile {@link PhysicalTARFile} file to retrieive STAF Protocol URL.
     * @return {@link URL}
     * @throws STAFURLException Error during URL construction.
     */
    public static Optional<URL> getTARFileSTAFUrl(PhysicalTARFile pTarFile, Path pRawFile) throws STAFURLException {
        try {
            Optional<URL> url = Optional.empty();
            // Construct standard staf://<ARCHIVE>/<NODE> path
            String urlInitialPath = String
                    .format(STANDARD_URL_STRING_FORMAT,
                            Paths.get(pTarFile.getStafArchiveName(), pTarFile.getStafNode().toString()));
            for (Entry<Path, Path> fileInTar : pTarFile.getFilesInTar().entrySet()) {
                if (pRawFile == fileInTar.getKey()) {
                    url = Optional.of(new URL(String.format(STAF_PARAMETERIZED_URL_PATTERN, urlInitialPath,
                                                            pTarFile.getSTAFFilePath().getFileName().toString(),
                                                            STAFURLParameter.TAR_FILENAME_PARAMETER.getParameterName(),
                                                            fileInTar.getValue().getFileName().toString())));
                }
            }
            return url;
        } catch (MalformedURLException e) {
            throw new STAFURLException(e.getMessage(), e);
        }
    }

    /**
     * Return all STAF URLs
     * @param pTarFile
     * @return
     * @throws STAFURLException
     */
    public static Set<URL> getTARFilesSTAFURLs(PhysicalTARFile pTarFile) throws STAFURLException {
        try {
            Set<URL> urls = Sets.newHashSet();
            // Construct standard staf://<ARCHIVE>/<NODE> path
            String urlInitialPath = String
                    .format(STANDARD_URL_STRING_FORMAT,
                            Paths.get(pTarFile.getStafArchiveName(), pTarFile.getStafNode().toString()));
            for (Entry<Path, Path> fileInTar : pTarFile.getFilesInTar().entrySet()) {
                urls.add(new URL(
                        String.format(STAF_PARAMETERIZED_URL_PATTERN, urlInitialPath, pTarFile.getStafFileName(),
                                      STAFURLParameter.TAR_FILENAME_PARAMETER.getParameterName(),
                                      fileInTar.getValue().getFileName().toString())));
            }
            return urls;
        } catch (MalformedURLException e) {
            throw new STAFURLException(e.getMessage(), e);
        }
    }

    /**
     * Create the STAF Protocol URL for the given {@link PhysicalNormalFile}
     * @param pFile {@link PhysicalNormalFile} file to retrieive STAF Protocol URL.
     * @return {@link URL}
     * @throws STAFURLException Error during URL construction.
     */
    public static URL getNormalFileSTAFUrl(PhysicalNormalFile pFile) throws STAFURLException {
        try {
            // Construct standard staf:/<ARCHIVE>/<NODE> path
            String urlInitialPath = String
                    .format(STANDARD_URL_STRING_FORMAT,
                            Paths.get(pFile.getStafArchiveName(), pFile.getStafNode().toString()));
            String fileNamePath = pFile.getSTAFFilePath().getFileName().toString();
            return new URL(String.format("%s/%s", urlInitialPath, fileNamePath));
        } catch (MalformedURLException e) {
            throw new STAFURLException(e.getMessage(), e);
        }
    }

    /**
     * Return the complete STAF Node of the given {@link URL}
     * @param pUrl {@link URL}
     * @return {@link Path}
     * @throws STAFURLException Given {@link URL} is not a STAF URL.
     */
    public static Path getSTAFNodeFromURL(URL pUrl) throws STAFURLException {
        Matcher m = STAF_URL_REGEXP.matcher(pUrl.toString());
        if (m.matches()) {
            String filePath = m.group(2);
            int index = filePath.lastIndexOf('/');
            if (index >= 0) {
                return Paths.get(filePath.substring(0, index));
            } else {
                return Paths.get(filePath);
            }
        } else {
            throw new STAFURLException(String.format("Invalid URL %s", pUrl.toString()));
        }
    }

    /**
     * Return the {@link STAFArchiveModeEnum} of the given {@link URL}
     * @param pUrl {@link URL}
     * @return {@link STAFArchiveModeEnum}
     * @throws STAFURLException Given {@link URL} is not a STAF URL.
     */
    public static STAFArchiveModeEnum getSTAFArchiveModeFromURL(URL pUrl) throws STAFURLException {
        Matcher m = STAF_URL_REGEXP.matcher(pUrl.toString());
        if (m.matches()) {
            if (pUrl.toString().contains(STAFURLParameter.TAR_FILENAME_PARAMETER.getParameterName())) {
                return STAFArchiveModeEnum.TAR;
            }
            if (pUrl.toString().contains(STAFURLParameter.CUT_PARTS_PARAMETER.getParameterName())) {
                return STAFArchiveModeEnum.CUT;
            }
            return STAFArchiveModeEnum.NORMAL;
        } else {
            throw new STAFURLException(String.format("Invalid URL %s", pUrl.toString()));
        }
    }

    /**
     * Return the {@link STAFURLParameter} values of the given {@link URL}
     * @param pUrl {@link URL}
     * @return {@link STAFURLParameter}
     * @throws STAFURLException Given {@link URL} is not a STAF URL.
     */
    public static Map<STAFURLParameter, String> getSTAFURLParameters(URL pUrl) throws STAFURLException {
        Map<STAFURLParameter, String> parameters = Maps.newHashMap();
        Matcher m = STAF_URL_REGEXP.matcher(pUrl.toString());
        if (m.matches()) {
            String filePath = m.group(3);
            if (!filePath.isEmpty()) {
                int index = filePath.indexOf('=');
                if ((index >= 0) && filePath.contains(STAFURLParameter.CUT_PARTS_PARAMETER.getParameterName())) {
                    parameters.put(STAFURLParameter.CUT_PARTS_PARAMETER, filePath.substring(index + 1));
                } else if ((index >= 0)
                        && filePath.contains(STAFURLParameter.TAR_FILENAME_PARAMETER.getParameterName())) {
                    parameters.put(STAFURLParameter.TAR_FILENAME_PARAMETER, filePath.substring(index + 1));
                }
            }
            return parameters;
        } else {
            throw new STAFURLException(String.format("Invalid URL %s", pUrl.toString()));
        }
    }

    /**
     * Return the STAF Archive name of the given {@link URL}
     * @param pUrl {@link URL}
     * @return {@link String}
     * @throws STAFURLException Given {@link URL} is not a STAF URL.
     */
    public static String getSTAFArchiveFromURL(URL pUrl) throws STAFURLException {
        Matcher m = STAF_URL_REGEXP.matcher(pUrl.toString());
        if (m.matches()) {
            return m.group(1);
        } else {
            throw new STAFURLException(String.format("Invalid URL %s", pUrl.toString()));
        }
    }

    /**
     * Return the STAF File name of the given {@link URL}
     * @param pUrl {@link URL}
     * @return {@link String}
     * @throws STAFURLException Given {@link URL} is not a STAF URL.
     */
    public static String getSTAFFileNameFromURL(URL pUrl) throws STAFURLException {
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
            throw new STAFURLException(String.format("Invalid URL %s", pUrl.toString()));
        }
    }

}
