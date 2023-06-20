/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.service.glacier;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.storage.dao.IGlacierArchiveRepository;
import fr.cnes.regards.modules.storage.domain.database.GlacierArchive;
import io.jsonwebtoken.lang.Assert;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service to handle actions on {@link fr.cnes.regards.modules.storage.domain.database.GlacierArchive}s
 *
 * @author Thibaud Michaudel
 **/
@Service
@MultitenantTransactional
public class GlacierArchiveService {

    IGlacierArchiveRepository glacierArchiveRepository;

    public GlacierArchiveService(IGlacierArchiveRepository glacierArchiveRepository) {
        this.glacierArchiveRepository = glacierArchiveRepository;
    }

    public GlacierArchive saveGlacierArchive(String storage, String url, String checksum, Long archiveSize) {
        Assert.notNull(url);
        Assert.notNull(checksum);
        Assert.notNull(archiveSize);

        Optional<GlacierArchive> optionalArchive = glacierArchiveRepository.findOneByStorageAndUrl(storage, url);
        if (optionalArchive.isPresent()) {
            GlacierArchive archive = optionalArchive.get();
            archive.setChecksum(checksum);
            archive.setArchiveSize(archiveSize);
            return glacierArchiveRepository.save(archive);
        } else {
            return glacierArchiveRepository.save(new GlacierArchive(storage, url, checksum, archiveSize));
        }
    }

    public void deleteGlacierArchive(String storage, String url) {
        Optional<GlacierArchive> optionalArchive = glacierArchiveRepository.findOneByStorageAndUrl(storage, url);
        optionalArchive.ifPresent(glacierArchive -> glacierArchiveRepository.delete(glacierArchive));
    }
}
