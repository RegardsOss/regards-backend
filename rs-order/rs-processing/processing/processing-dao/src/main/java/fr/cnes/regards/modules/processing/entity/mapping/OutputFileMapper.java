package fr.cnes.regards.modules.processing.entity.mapping;

import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.entity.OutputFileEntity;
import org.springframework.stereotype.Component;

@Component
public class OutputFileMapper implements DomainEntityMapper.OutputFile {

    @Override public OutputFileEntity toEntity(POutputFile domain) {
        return new OutputFileEntity(
                domain.getId(),
                domain.getExecId(),
                domain.getUrl(),
                domain.getName(),
                domain.getChecksum().getValue(),
                domain.getChecksum().getMethod(),
                domain.getSize(),
                domain.getCreated(),
                domain.isDownloaded(),
                domain.isDeleted(),
                domain.isPersisted()
        );
    }

    @Override public POutputFile toDomain(OutputFileEntity entity) {
        return new POutputFile(
                entity.getId(),
                entity.getExecId(),
                entity.getName(),
                new POutputFile.Digest(entity.getChecksumMethod(), entity.getChecksumValue()),
                entity.getUrl(),
                entity.getSizeInBytes(),
                entity.getCreated(),
                entity.isDownloaded(),
                entity.isDeleted(),
                entity.isPersisted()
        );
    }

}
