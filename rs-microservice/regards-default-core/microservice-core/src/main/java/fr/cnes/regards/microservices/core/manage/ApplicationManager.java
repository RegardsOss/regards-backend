/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.manage;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import org.springframework.context.ApplicationContext;

/**
 * @author svissier
 *
 */
public class ApplicationManager {

    private final ApplicationContext applicationContext;

    public ApplicationManager(ApplicationContext pApplicationContext) {
        applicationContext = pApplicationContext;
    }

    public void immediateShutdown() throws IOException {
        final String pid = getPid();
        final String command = "kill -9 " + pid;
        Runtime.getRuntime().exec(command);
    }

    /**
     * method taken from {@link org.springframework.boot.ApplicationPid}
     *
     * @author Phillip Webb
     *
     *         Copyright 2012-2015 the original author or authors.
     *
     *         Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
     *         compliance with the License. You may obtain a copy of the License at
     *
     *         http://www.apache.org/licenses/LICENSE-2.0
     *
     *         Unless required by applicable law or agreed to in writing, software distributed under the License is
     *         distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     *         See the License for the specific language governing permissions and limitations under the License.
     *
     *
     */
    private String getPid() {
        try {
            String jvmName = ManagementFactory.getRuntimeMXBean().getName();
            return jvmName.split("@")[0];
        } catch (Throwable ex) { // NOSONAR : foreign code
            return null;
        }
    }

}
