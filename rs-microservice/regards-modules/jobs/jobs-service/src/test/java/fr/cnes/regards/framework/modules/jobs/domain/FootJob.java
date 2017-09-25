package fr.cnes.regards.framework.modules.jobs.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xbrochard
 */
public class FootJob extends AbstractNoParamJob<Toto> {

    @Override
    public void run() {
        List<Titi> list = new ArrayList<>();
        Toto toto = new Toto();
        toto.setI(15);
        Titi titi = new Titi();
        titi.setJ(150);
        list.add(titi);
        toto.setList(list);
        super.setResult(toto);
    }
}
