package fr.cs.cmz.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
public class DownloadZipController {
	/*
	 * download OK ZIP OK
	 * 
	 */

	// File[] sourceFiles = { new File("/home/christophe/linux/settings.xml"),
	// new File("/home/christophe/linux/dashboard.json.txt"),
	// new File("/home/christophe/linux/commande system.ksh") };
	File[] sourceFiles = { new File("/home/christophe/linux/jdk-8u65-linux-x64.tar.gz"),
			new File("/home/christophe/linux/netbeans-8.1-javaee-linux.sh"),
			new File("/home/christophe/linux/jdk-7u67-linux-x64.tar.gz"),
			new File("/home/christophe/linux/kibana-4.4.0-linux-x64.tar.gz") };

	private final String PREFIX = "download zip";

	@RequestMapping(value = "/zip", produces = "application/zip")
	@ResponseBody
	public StreamingResponseBody zip(HttpServletResponse response) {
		System.out.println(PREFIX + " start");

		response.addHeader("Content-disposition", "attachment;filename=myfilename.zip");
		response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);

		return os -> {
			ZipOutputStream zipOutputStream = new ZipOutputStream(os);

			for (int i = 0; i < sourceFiles.length; i++) {
				System.out.println("Adding " + sourceFiles[i].getName());

				zipOutputStream.putNextEntry(new ZipEntry(sourceFiles[i].getName()));
				InputStream in = new FileInputStream(sourceFiles[i]);
				System.out.println("in progress . ");
				IOUtils.copy(in, zipOutputStream);

				// byte[] filedata = new byte[1048576];
				// while (-1 != in.read(filedata)) {
				// zipOutputStream.write(filedata);
				// zipOutputStream.flush();
				// System.out.print(".");
				// }

				zipOutputStream.closeEntry();
				in.close();

				System.out.println("Done " + sourceFiles[i].getName());
			}
			zipOutputStream.flush();
			zipOutputStream.finish();

			IOUtils.closeQuietly(zipOutputStream);

			this.finish();
		};
	}

	private void finish() {
		System.out.println("Finish ");
	}
}
