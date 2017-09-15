package fr.cnes.regards.modules.storage.client;

import java.io.InputStream;

import org.springframework.stereotype.Component;

/**
 * @author oroussel
 */
@Component
public interface IAipClient {
    InputStream downloadFile(String aipId, String checksum);
}
