package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.domain.repository.IPOutputFilesRepository;
import fr.cnes.regards.modules.processing.domain.service.IOutputFileService;
import fr.cnes.regards.modules.processing.storage.ISharedStorageService;
import io.vavr.collection.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.net.URL;
import java.util.UUID;

@Service
public class OutputFileServiceImpl implements IOutputFileService {

    private final IPOutputFilesRepository outFileRepo;
    private final ISharedStorageService storageService;

    @Autowired
    public OutputFileServiceImpl(IPOutputFilesRepository outFileRepo, ISharedStorageService storageService) {
        this.outFileRepo = outFileRepo;
        this.storageService = storageService;
    }

    @Override public Flux<POutputFile> markDownloaded(List<URL> urls) {
        return outFileRepo.save(outFileRepo.findByUrlIn(urls).map(POutputFile::markDownloaded));
    }

    @Scheduled(
        fixedRate = 60L * 60L * 1000L // Every hour TODO make configurable? // TODO add jitter?
    )
    @Override public void scheduledDeleteDownloadedFiles() {
        outFileRepo.save(outFileRepo.findByDownloadedIsTrueAndDeletedIsFalse()
            .flatMap(storageService::delete))
            .subscribe();
    }
}
