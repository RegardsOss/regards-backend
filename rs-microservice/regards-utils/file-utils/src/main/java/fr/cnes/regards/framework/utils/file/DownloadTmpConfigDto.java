package fr.cnes.regards.framework.utils.file;

import java.nio.file.Path;

/**
 * Configuration to know if the file to download should be copied locally temporarily before being transferred to a
 * specified location.
 *
 * @param forceTmpFile     If the file should be downloaded locally regardless of the maxContentLength.
 * @param maxContentLength Maximum content length of file to decide if the file can be downloaded directly or must be saved first to a local temporary file.
 *                         If the file size is lower than this limit, download will use the original source while downloading.
 *                         If the file size is bigger than this limit, a local temporary file will be created and the resulting InputStream will be from this temporary file.
 * @param tmpFilePath      The temporary location where the file will be created and read. Warning: this path must be
 *                         unique, you can use the file checksum for exemple.
 * @param deleteTmpFile    if the tmp file should be deleted once the downloading of the file is completed. If false,
 *                         the caller should handle the file deleting.
 * @author Iliana Ghazali
 **/
public record DownloadTmpConfigDto(boolean forceTmpFile,
                                   long maxContentLength,
                                   Path tmpFilePath,
                                   boolean deleteTmpFile) {

}
