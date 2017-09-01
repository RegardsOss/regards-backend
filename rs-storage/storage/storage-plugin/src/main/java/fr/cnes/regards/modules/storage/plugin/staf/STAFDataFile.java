package fr.cnes.regards.modules.storage.plugin.staf;

import org.springframework.util.MimeType;

import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.DataObject;
import fr.cnes.regards.modules.storage.domain.database.DataFile;

public class STAFDataFile extends DataFile {

    public STAFDataFile(DataObject pFile, String pAlgorithm, String pChecksum, Double pFileSize, MimeType pMimeType,
            AIP pAip) {
        super(pFile, pAlgorithm, pChecksum, pFileSize, pMimeType, pAip);
    }

}
