package fr.cnes.regards.modules.processing.entity;

import lombok.Value;

import java.util.Arrays;
import java.util.List;

@Value

public class Steps {

    List<StepEntity> values;

    public static Steps of(StepEntity... steps) {
        return new Steps(Arrays.asList(steps));
    }
}
