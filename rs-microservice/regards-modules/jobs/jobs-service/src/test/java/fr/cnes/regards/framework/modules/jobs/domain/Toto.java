package fr.cnes.regards.framework.modules.jobs.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * @author oroussel
 */
public class Toto {

    private int i = 0;

    private List<Titi> list = new ArrayList<>();

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }

    public List<Titi> getList() {
        return list;
    }

    public void setList(List<Titi> list) {
        this.list = list;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Toto toto = (Toto) o;

        return i == toto.i;
    }

    @Override
    public int hashCode() {
        return i;
    }
}
