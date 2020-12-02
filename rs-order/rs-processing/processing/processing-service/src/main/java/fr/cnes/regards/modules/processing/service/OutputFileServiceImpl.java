/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.service;

import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.domain.repository.IPOutputFilesRepository;
import fr.cnes.regards.modules.processing.domain.service.IOutputFileService;
import fr.cnes.regards.modules.processing.storage.ISharedStorageService;
import io.vavr.collection.List;
import reactor.core.publisher.Flux;

/**
 * TODO : Class description
 *
 * @author Guillaume Andrieu
 *
 */
@Service
public class OutputFileServiceImpl implements IOutputFileService {

    private final IPOutputFilesRepository outFileRepo;

    private final ISharedStorageService storageService;

    @Autowired
    public OutputFileServiceImpl(IPOutputFilesRepository outFileRepo, ISharedStorageService storageService) {
        this.outFileRepo = outFileRepo;
        this.storageService = storageService;
    }

    @Override
    public Flux<POutputFile> markDownloaded(List<URL> urls) {
        return outFileRepo.save(outFileRepo.findByUrlIn(urls).map(POutputFile::markDownloaded));
    }

    @Scheduled(fixedRate = 60L * 60L * 1000L // Every hour TODO make configurable? // TODO add jitter?
    )
    @Override
    public void scheduledDeleteDownloadedFiles() {
        outFileRepo.save(outFileRepo.findByDownloadedIsTrueAndDeletedIsFalse().flatMap(storageService::delete))
                .subscribe();
    }
}
