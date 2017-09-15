package fr.cnes.regards.modules.storage.service;

import java.time.OffsetDateTime;
import java.util.Set;

import fr.cnes.regards.modules.storage.domain.database.CoupleAvailableError;
import fr.cnes.regards.modules.storage.domain.database.DataFile;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface ICachedFileService {

    CoupleAvailableError restore(Set<DataFile> nearlineFiles, OffsetDateTime expirationDate);

}
