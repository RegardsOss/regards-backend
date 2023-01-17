/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.gson.adapters.sample9;

import com.google.gson.Gson;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Sébastien Binda
 **/
public class ConcurrentTest {

    public AtomicInteger successCounter = new AtomicInteger();

    public class RegisterSubtypeThread extends Thread {

        private DataAdapterFactory factory;

        private String tenant;

        private Class attributeClass;

        public RegisterSubtypeThread(DataAdapterFactory factory, String tenant, Class attributeClass) {
            this.tenant = tenant;
            this.factory = factory;
            this.attributeClass = attributeClass;
        }

        @Override
        public void run() {
            factory.registerSubtype(tenant, attributeClass);
        }
    }

    public class TestThread extends Thread {

        private final AtomicInteger successCounter;

        private final int index;

        private DataAdapterFactory factory;

        private String tenant;

        private Gson gson;

        private int size;

        public TestThread(int size,
                          int index,
                          Gson gson,
                          DataAdapterFactory factory,
                          String tenant,
                          AtomicInteger successCounter) {
            this.index = index;
            this.gson = gson;
            this.tenant = tenant;
            this.factory = factory;
            this.size = size;
            this.successCounter = successCounter;
        }

        @Override
        public void run() {
            for (int i = 0; i < 10; i++) {
                factory.doMapping(gson, tenant);
                checkMapsSize(factory, tenant, size);
            }
            successCounter.incrementAndGet();
        }
    }

    @Test
    public void test() throws InterruptedException, NoSuchFieldException {
        Gson gson = new Gson();
        IRuntimeTenantResolver mock = Mockito.mock(IRuntimeTenantResolver.class);
        DataAdapterFactory factory = new DataAdapterFactory(mock);
        List<Class> subTypesToRegister = List.of(DataForConcurrentTest.Data1.class,
                                                 DataForConcurrentTest.Data2.class,
                                                 DataForConcurrentTest.Data3.class,
                                                 DataForConcurrentTest.Data4.class,
                                                 DataForConcurrentTest.Data5.class,
                                                 DataForConcurrentTest.Data6.class,
                                                 DataForConcurrentTest.Data7.class,
                                                 DataForConcurrentTest.Data8.class,
                                                 DataForConcurrentTest.Data9.class,
                                                 DataForConcurrentTest.Data10.class);

        String tenant = "tenant1";
        subTypesToRegister.forEach(st -> new RegisterSubtypeThread(factory, tenant, st).start());
        Thread.sleep(500);

        int testCount = 100;
        for (int i = 0; i < testCount; i++) {
            Thread t = new TestThread(subTypesToRegister.size(), i, gson, factory, tenant, successCounter);
            t.start();
        }
        Thread.sleep(2000);
        Assertions.assertEquals(testCount, successCounter.get(), "test count and success counter should be equals");
    }

    private static void checkMapsSize(DataAdapterFactory factory, String tenant, int expectedSize) {
        Assertions.assertEquals(expectedSize, factory.getSubtypeToDelegateMap().get(tenant).entrySet().size());

        Assertions.assertEquals(expectedSize, factory.getDiscToDelegateMap().get(tenant).entrySet().size());

        Assertions.assertEquals(expectedSize, factory.getDiscToSubtypeMap().get(tenant).entrySet().size());

        Assertions.assertEquals(expectedSize, factory.getSubtypeToDiscMap().get(tenant).entrySet().size());
    }
}
