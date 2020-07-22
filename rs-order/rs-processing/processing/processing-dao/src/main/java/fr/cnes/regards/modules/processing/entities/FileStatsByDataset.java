package fr.cnes.regards.modules.processing.entities;

import fr.cnes.regards.modules.processing.domain.size.FileSetStatistics;
import lombok.*;

import java.util.Map;

@Data @With
@AllArgsConstructor @NoArgsConstructor
@Builder(toBuilder = true)

public class FileStatsByDataset {

    private Map<String, FileSetStatistics> map;

}
