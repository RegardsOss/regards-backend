package fr.cnes.regards.framework.utils.file;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class CommonFileUtilsTests {

    @Test
    public void testGetAvailableFileNameWithExtension() throws IOException {
        // create new file in target for walk
        Path dirWithFile = Paths.get("target", "CommonFileUtilsTests", "testGetAvailableFileNameWithExtension");
        testGetAvailableFileName(dirWithFile, "toto.titi", "toto_1.titi");
    }

    @Test
    public void testGetAvailableFileNameWithExtensionAndDotInMiddle() throws IOException {
        // create new file in target for walk
        Path dirWithFile = Paths.get("target",
                                     "CommonFileUtilsTests",
                                     "testGetAvailableFileNameWithExtensionAndDotInMiddle");
        testGetAvailableFileName(dirWithFile, "to.to.titi", "to.to_1.titi");
    }

    @Test
    public void testGetAvailableFileNameWithExtensionStartsWithDot() throws IOException {
        // create new file in target for walk
        Path dirWithFile = Paths.get("target",
                                     "CommonFileUtilsTests",
                                     "testGetAvailableFileNameWithExtensionStartsWithDot");
        testGetAvailableFileName(dirWithFile, ".toto.titi", ".toto_1.titi");
    }

    @Test
    public void testGetAvailableFileNameWithoutExtension() throws IOException {
        // create new file in target for walk
        Path dirWithFile = Paths.get("target", "CommonFileUtilsTests", "testGetAvailableFileNameWithoutExtension");
        testGetAvailableFileName(dirWithFile, "toto", "toto_1");
    }

    @Test
    public void testGetAvailableFileNameWithoutExtensionStartsWithDot() throws IOException {
        // create new file in target for walk
        Path dirWithFile = Paths.get("target",
                                     "CommonFileUtilsTests",
                                     "testGetAvailableFileNameWithoutExtensionStartsWithDot");
        testGetAvailableFileName(dirWithFile, ".toto", ".toto_1");
    }

    @Test
    public void testGetAvailableFileNameEndsWithDot() throws IOException {
        // create new file in target for walk
        Path dirWithFile = Paths.get("target", "CommonFileUtilsTests", "testGetAvailableFileNameEndsWithDot");
        testGetAvailableFileName(dirWithFile, "toto.", "toto_1.");
    }

    private void testGetAvailableFileName(Path dirWithFile, String fileName, String expectedFileName)
        throws IOException {
        Path alreadyExistingFile = dirWithFile.resolve(fileName);
        Files.createDirectories(dirWithFile);
        Files.createFile(alreadyExistingFile);
        String availableFileName = CommonFileUtils.getAvailableFileName(dirWithFile, fileName);
        Assert.assertEquals(fileName
                            + " already exist so next available file name for "
                            + fileName
                            + " should be "
                            + expectedFileName, expectedFileName, availableFileName);
        //clean dir for next test run
        Files.walkFileTree(dirWithFile, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

}
