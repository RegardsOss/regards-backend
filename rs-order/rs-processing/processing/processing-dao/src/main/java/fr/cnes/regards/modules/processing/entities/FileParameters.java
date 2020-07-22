package fr.cnes.regards.modules.processing.entities;

import fr.cnes.regards.modules.processing.domain.parameters.ExecutionFileParameterValue;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionStringParameterValue;
import io.vavr.control.Option;
import lombok.*;

import javax.persistence.Embeddable;
import java.net.URL;
import java.util.List;

@Data @With
@AllArgsConstructor @NoArgsConstructor
@Builder(toBuilder = true)

public class FileParameters {

    private List<ExecutionFileParameterValue> values;

}
