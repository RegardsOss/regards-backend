package fr.cs.regards.tools.elastic;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class IAU2000Importer {
	static final RestTemplate restTemplate = new RestTemplate();
	public static void main(String[] args) throws IOException {
		File iauFile = new File("iau2000");
		File epsgFile = new File("epsg");
		FileWriter iauWriter = new FileWriter(iauFile);
		FileWriter epsgWriter = new FileWriter(epsgFile);
		for(int i = 0; i< 100; i++){
			int refNumber = 49900+i;
			String url = String.format("http://spatialreference.org/ref/iau2000/%d/proj4/", refNumber);
			try{
				ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
				String params = resp.getBody();
				if (params == null) continue;
				String crsLine = String.format("<%d> %s <>\n", refNumber, params);
				iauWriter.write(crsLine);
				crsLine = String.format("<%d> %s <>\n", refNumber+900000, params);
				epsgWriter.write(crsLine);
				
				
			}catch(HttpClientErrorException e){
				continue;
			}
			
		}	
		iauWriter.close();
		epsgWriter.close();
	}
}
