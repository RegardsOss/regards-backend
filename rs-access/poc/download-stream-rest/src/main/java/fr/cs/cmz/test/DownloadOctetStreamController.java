package fr.cs.cmz.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DownloadOctetStreamController {

	private final File bigFile = new File("/home/christophe/vivaldi-stable_1.0.435.42-1_amd64.deb");

	private final String PREFIX = "download octet";

	@RequestMapping(value = "/octet")
	public void octet(HttpServletResponse response) {
		System.out.println(PREFIX + " start");
		FileInputStream fin;
		try {
			fin = new FileInputStream(bigFile);

			response.addHeader("Content-disposition", "attachment;filename=myfilename.deb");
			response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);

			// Copy the stream to the response's output stream.

			IOUtils.copy(fin, response.getOutputStream());
			response.flushBuffer();
		} catch (IOException e) {
			System.out.println(PREFIX + " error");
		}
		System.out.println(PREFIX + " stop");
	};
}
