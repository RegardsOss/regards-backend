package fr.cnes.regards.framework.staf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;

import org.mockito.invocation.InvocationOnMock;

import fr.cnes.regards.framework.staf.exception.STAFException;

public class STAFMock {

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
