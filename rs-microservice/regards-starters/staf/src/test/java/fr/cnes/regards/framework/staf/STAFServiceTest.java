package fr.cnes.regards.framework.staf;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.staf.domain.ArchiveAccessModeEnum;
import fr.cnes.regards.framework.staf.domain.STAFArchive;
import fr.cnes.regards.framework.staf.domain.STAFConfiguration;
import fr.cnes.regards.framework.staf.exception.STAFException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

public class STAFServiceTest {

    private STAFConfiguration configuration;

    private STAFSession stafSessionMock;

    private STAFSessionManager stafManagerMock;

    /**
     * Initialize STAF Configuration and STAF Manager before each test
     * @throws STAFException
     * @throws IOException
     */
    @Before
    public void init() throws STAFException, IOException {

        configuration = new STAFConfiguration();
        configuration.setMinFileSize(5000L);
        configuration.setMaxFileSize(1000L);

        configuration.setTarSizeThreshold(4500L);
        configuration.setMaxTarSize(5000L);
        configuration.setMaxTarArchivingHours(50L);

        configuration.setAttemptsBeforeFail(5);
        configuration.setBiggerFileGenClass("CS2");
        configuration.setBiggerFileGFClass("CS3");
        configuration.setLittleFileClass("CS1");
        configuration.setMaxSessionsArchivingMode(10);
        configuration.setMaxSessionsRestitutionMode(10);
        configuration.setMaxSessionStreamsArchivingMode(10);
        configuration.setMaxSessionStreamsRestitutionMode(10);
        configuration.setMaxStreamFilesArchivingMode(10);
        configuration.setMaxStreamFilesRestitutionMode(10);
        configuration.setMaxNumberOfFilesPerNode(5000);

        stafSessionMock = Mockito.mock(STAFSession.class);
        stafManagerMock = Mockito.mock(STAFSessionManager.class);

        Mockito.when(stafManagerMock.getConfiguration()).thenReturn(configuration);
        Mockito.when(stafManagerMock.getReservation(Mockito.any())).thenReturn(0);
        List<Integer> sessions = Lists.newArrayList();
        sessions.add(0);
        Mockito.when(stafManagerMock.getReservations()).thenReturn(sessions);
        Mockito.when(stafManagerMock.getNewSession()).thenReturn(stafSessionMock);
    }

    @Purpose("Test STAF standard API to connect a STAF archive")
    @Requirement("REGARDS_DSL_STAF_ARC_020")
    @Test
    public void testArchiveConnection() throws STAFException {
        STAFArchive archive = new STAFArchive();
        archive.setArchiveName("STAF_ARCHIVE_TEST");
        archive.setPassword("testPassword");
        STAFService stafService = new STAFService(stafManagerMock, archive);
        stafService.connectArchiveSystem(ArchiveAccessModeEnum.ARCHIVE_MODE);
        stafService.disconnectArchiveSystem(ArchiveAccessModeEnum.ARCHIVE_MODE);
    }

}
