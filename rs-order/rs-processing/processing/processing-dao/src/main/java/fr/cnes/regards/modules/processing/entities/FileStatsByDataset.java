package fr.cnes.regards.modules.processing.entities;

import fr.cnes.regards.modules.processing.domain.size.FileSetStatistics;
import lombok.*;

import java.util.Map;

@Value
public class FileStatsByDataset {

    Map<String, FileSetStatistics> map;

}
