package fr.cnes.regards.modules.processing.entities;

import lombok.Value;

import java.util.Arrays;
import java.util.List;

@Value

public class Steps {

    List<Step> values;

    public static Steps of(Step... steps) {
        return new Steps(Arrays.asList(steps));
    }
}
