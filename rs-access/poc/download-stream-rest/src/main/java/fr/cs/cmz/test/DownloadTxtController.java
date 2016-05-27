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
public class DownloadTxtController {

	private final String PREFIX = "download txt";

	private final File txtFile = new File("/home/christophe/coucou.txt");

	@RequestMapping(value = "/txt")
	public void txt(HttpServletResponse response) {
		System.out.println(PREFIX + " start");
		FileInputStream fin;
		try {
			fin = new FileInputStream(txtFile);

			response.addHeader("Content-disposition", "attachment;filename=myfilename.txt");
			response.setContentType(MediaType.TEXT_PLAIN_VALUE);

			// Copy the stream to the response's output stream.

			IOUtils.copy(fin, response.getOutputStream());
			response.flushBuffer();
		} catch (IOException e) {
			System.out.println(PREFIX + " error");
		}
		System.out.println(PREFIX + " stop");
	};
}
