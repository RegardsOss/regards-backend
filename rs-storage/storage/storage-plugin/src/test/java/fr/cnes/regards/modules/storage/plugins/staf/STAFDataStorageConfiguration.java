/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugins.staf;

import java.util.List;
import java.util.Map;

import org.mockito.Mockito;
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
        List<Integer> sessions = Lists.newArrayList();
        sessions.add(0);
        Mockito.when(stafManagerMock.getReservations()).thenReturn(sessions);
        Mockito.when(stafManagerMock.getNewSession()).thenReturn(stafSessionMock);
        return stafManagerMock;
    }

}
