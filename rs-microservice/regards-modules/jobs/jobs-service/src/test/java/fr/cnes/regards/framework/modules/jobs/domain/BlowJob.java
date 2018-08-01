package fr.cnes.regards.framework.modules.jobs.domain;

import java.util.Random;

/**
 * @author xbrochard
 */
public class BlowJob extends AbstractNoParamJob<Float> {

    @Override
    public void run() {
        Random random = new Random();
        super.setResult(random.nextFloat());
    }
}
