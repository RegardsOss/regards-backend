package fr.cnes.regards.modules.dam.service.entities;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

/**
 * Document LS (Local Storage)
 * Aims at providing a local storage for Document files before rs-archival-storage
 * saves it permently
 *
 * @author Léo Mieulet
 */
public interface ILocalStorageService {

    /**
     * Save several files on disk
     *
     * @param entity          the entity which we are editing
     * @param dataType        data type
     * @param attachments     the files to attach to the document
     * @param fileUriTemplate the file's uri template in order to generate each file's uri
     * @return the set of files
     */
    Collection<DataFile> attachFiles(AbstractEntity<?> entity,
                                     DataType dataType,
                                     MultipartFile[] attachments,
                                     String fileUriTemplate) throws ModuleException;

    Collection<DataFile> attachLocalFiles(AbstractEntity<?> entity,
                                          DataType dataType,
                                          List<DataFile> localFiles,
                                          String fileUriTemplate) throws ModuleException;

    /**
     * @param entity   the entity which we are editing
     * @param dataFile the file we're removing
     */
    void removeFile(AbstractEntity<?> entity, DataFile dataFile) throws ModuleException;

    /**
     * @param entity   {@link AbstractEntity}
     * @param dataFile {@link DataFile}
     * @return true when the database knows this entity
     */
    boolean isFileLocallyStored(AbstractEntity<?> entity, DataFile dataFile);

    /**
     * Write the file in the output stream.<br/>
     * {@link OutputStream} has to be flush after this method completes.
     *
     * @param output {@link OutputStream}
     */
    void getFileContent(String checksum, OutputStream output) throws ModuleException;
}
