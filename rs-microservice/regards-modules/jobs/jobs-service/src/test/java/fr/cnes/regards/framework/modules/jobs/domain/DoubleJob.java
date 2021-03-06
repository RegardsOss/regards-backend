package fr.cnes.regards.framework.modules.jobs.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xbrochard
 */
public class DoubleJob extends AbstractNoParamJob<Map<String, Double>> {

    @Override
    public void run() {
        Map<String, Double> map = new HashMap<>();
        map.put("toto", 1.0);
        map.put("tutu", 2.0);
        map.put("titi", 3.0);
        super.setResult(map);
    }
}
