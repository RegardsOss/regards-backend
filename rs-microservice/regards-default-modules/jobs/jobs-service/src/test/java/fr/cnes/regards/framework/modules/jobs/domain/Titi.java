package fr.cnes.regards.framework.modules.jobs.domain;

/**
 * @author oroussel
 */
public class Titi {
    private int j = 0;

    public int getJ() {
        return j;
    }

    public void setJ(int j) {
        this.j = j;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Titi titi = (Titi) o;

        return j == titi.j;
    }

    @Override
    public int hashCode() {
        return j;
    }
}
