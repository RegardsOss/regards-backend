package fr.cnes.regards.modules.processing.entity;

import fr.cnes.regards.modules.processing.domain.PInputFile;
import lombok.Value;

import java.util.List;

@Value
public class FileParameters {

    List<PInputFile> values;

}
