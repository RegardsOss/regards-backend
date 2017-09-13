/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugins.staf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.Lists;
import com.netflix.governator.annotations.binding.Primary;

import fr.cnes.regards.framework.staf.STAFConfiguration;
import fr.cnes.regards.framework.staf.STAFException;
import fr.cnes.regards.framework.staf.STAFManager;
import fr.cnes.regards.framework.staf.STAFSession;

@Configuration
public class STAFDataStorageConfiguration {

    @Bean
    @Primary
    public STAFManager getStafManager() throws STAFException {
        STAFConfiguration configuration = new STAFConfiguration();
        configuration.setMinFileSize(5000L);
        configuration.setMaxFileSize(15000L);

        // TODO : Use of two limits max and threshold
        configuration.setTarSizeThreshold(2000L);
        configuration.setMaxTarSize(5000L);
        configuration.setMaxTarArchivingHours(5L);

        configuration.setAttemptsBeforeFail(5);
        configuration.setBiggerFileGenClass("CS1");
        configuration.setBiggerFileGFClass("CS2");
        configuration.setMaxSessionsArchivingMode(10);
        configuration.setMaxSessionsRestitutionMode(10);
        configuration.setMaxSessionStreamsArchivingMode(10);
        configuration.setMaxSessionStreamsRestitutionMode(10);
        configuration.setMaxStreamFilesArchivingMode(10);
        configuration.setMaxStreamFilesRestitutionMode(10);

        STAFSession stafSessionMock = Mockito.mock(STAFSession.class);
        STAFManager stafManagerMock = Mockito.mock(STAFManager.class);

        Mockito.when(stafManagerMock.getNewArchiveAccessService(Mockito.any())).thenCallRealMethod();
        Mockito.when(stafManagerMock.getConfiguration()).thenReturn(configuration);
        Mockito.when(stafManagerMock.getReservation(Mockito.any())).thenReturn(0);
        // Simulate archive is ok for all files.
        Mockito.when(stafSessionMock.staffilArchive(Mockito.any(), Mockito.any(), Mockito.anyBoolean()))
                .thenAnswer(pInvocation -> {
                    Map<String, String> files = pInvocation.getArgumentAt(0, Map.class);
                    return Lists.newArrayList(files.keySet());
                });
        // Simulate STAF Files restitution
        Mockito.doAnswer(invocation -> mockRestoration(invocation)).when(stafSessionMock)
                .staffilRetrieveBuffered(Mockito.any());
        List<Integer> sessions = Lists.newArrayList();
        sessions.add(0);
        Mockito.when(stafManagerMock.getReservations()).thenReturn(sessions);
        Mockito.when(stafManagerMock.getNewSession()).thenReturn(stafSessionMock);
        return stafManagerMock;
    }

    /**
     * Mock to simulate STAF files restoration.
     * @param invocation
     * @return
     * @throws STAFException
     */
    public static Void mockRestoration(InvocationOnMock invocation) throws STAFException {
        Object[] args = invocation.getArguments();
        @SuppressWarnings("unchecked")
        Map<String, String> files = (Map<String, String>) args[0];
        for (Entry<String, String> file : files.entrySet()) {
            try {
                String fileName = Paths.get(file.getValue()).getFileName().toString();
                if (fileName.contains("error")) {
                    throw new STAFException("STAF Error simulation");
                }
                Path mockedFilePath = Paths.get("src/test/resources/staf/mock", fileName);
                // If file exists in mock copy it into destination
                if (mockedFilePath.toFile().exists()) {
                    Files.copy(mockedFilePath, Paths.get(file.getValue()));
                } else {
                    Files.createFile(Paths.get(file.getValue()));
                }
            } catch (IOException e) {
                throw new STAFException(e);
            }
        }
        return null;
    }

}
