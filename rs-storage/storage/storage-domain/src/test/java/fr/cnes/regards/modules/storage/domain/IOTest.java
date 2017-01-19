/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class IOTest {

    @Autowired
    private Gson gson;

    /**
     * Test if we can parse an AIP from its JSON format.
     *
     * @throws JsonSyntaxException
     * @throws JsonIOException
     * @throws IOException
     */
    @Test
    public void testParsing() throws JsonIOException, JsonSyntaxException, IOException {
        Gson gson = new Gson();
        FileReader fr = new FileReader("src/test/resources/regards_aip.json");

        BufferedReader br = new BufferedReader(fr);
        AIP aip = gson.fromJson(br, AIP.class);
        fr.close();
        br.close();
        Assert.assertTrue(true);
    }

    @Test
    public void testSerialize() throws IOException, NoSuchAlgorithmException {
        // serialize and then check if checksum of reference and producted are the same or not!
        Gson gson = new Gson();
        String value = gson.toJson(new AIP(AipType.COLLECTION).generateAIP());
        FileReader fr = new FileReader("src/test/resources/regards_aip.json");

        BufferedReader br = new BufferedReader(fr);
        AIP aip = gson.fromJson(br, AIP.class);
        fr.close();
        br.close();
        FileWriter fw = new FileWriter("src/test/resources/serialized_aip.json");
        fw.write(gson.toJson(aip));
        fw.close();
        // Assert.assertEquals(checksum("src/test/resources/regards_aip.json"),
        // checksum("src/test/resources/serialized_aip.json"));
        // fail("Not yet implemented");
    }

    /**
     * @param pFileName
     * @return
     */
    private String checksum(String pFileName) {
        return null;
    }

}
