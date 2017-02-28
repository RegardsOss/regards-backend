package fr.cnes.regards.framework.test;
import org.junit.Assert;
import org.junit.Test;

import fr.cnes.regards.framework.test.util.Beans;

public class BeansTest {

    @Test
    public void testEqualsObjectObject() {
        Item item1 = new Item(1, "Item 1", new Tutu(Math.PI));
        Item item2 = new Item(2, "Item 1", new Tutu(Math.PI));
        Item item3 = new Item(1, "Item 2", new Tutu(Math.PI));
        Item item4 = new Item(1, "Item 1", new Tutu(Math.E));
        Item item1bis = new Item(1, "Item 1", new Tutu(Math.PI));

        Assert.assertTrue(Beans.equals(item1, item1));
        Assert.assertTrue(Beans.equals(item1, item1bis));
        Assert.assertFalse(Beans.equals(item1, item2));
        Assert.assertFalse(Beans.equals(item1, item3));
        Assert.assertFalse(Beans.equals(item1, item4));
    }

    // CHECKSTYLE:OFF
    public static class RootItem {

        private int toto;

        public RootItem(int toto) {
            this.toto = toto;
        }

        public int getToto() {
            return toto;
        }

        @Override
        public boolean equals(Object pObj) {
            return true;
        }
    }

    public static class Tutu {

        private Double value;

        public Tutu(Double value) {
            this.value = value;
        }

        public Double getValue() {
            return value;
        }

        @Override
        public boolean equals(Object pObj) {
            return true;
        }
    }

    public static class Item extends RootItem {

        private String titi;

        private Tutu tutu;

        public Item(int toto, String titi, Tutu tutu) {
            super(toto);
            this.titi = titi;
            this.tutu = tutu;
        }

        public String getTiti() {
            return titi;
        }

        public Tutu getTutu() {
            return tutu;
        }

        @Override
        public boolean equals(Object pObj) {
            return true;
        }
    }
    // CHECKSTYLE:ON
}
