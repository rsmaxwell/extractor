package com.rsmaxwell.extractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rsmaxwell.diaryjson.Fragment;
import com.rsmaxwell.diaryjson.OutputDocument;
import com.rsmaxwell.extractor.parser.MyDocument;

public enum Extractor {

	INSTANCE;

	public int year;
	public int month;
	public int day;
	public String order;
	public String reference;

	public void unzip(String archive, String destDirName) throws IOException {
		File destDir = new File(destDirName);
		byte[] buffer = new byte[1024];
		ZipInputStream zis = new ZipInputStream(new FileInputStream(archive));
		ZipEntry zipEntry = zis.getNextEntry();
		while (zipEntry != null) {

			if ("word/document.xml".contentEquals(zipEntry.getName())) {

				String filename = destDirName + "/" + zipEntry.getName();
				File file = new File(filename);
				File parentFolder = new File(file.getParent());
				parentFolder.mkdirs();

				File newFile = newFile(destDir, zipEntry);
				FileOutputStream fos = new FileOutputStream(newFile);
				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				fos.close();
			}

			zipEntry = zis.getNextEntry();
		}
		zis.closeEntry();
		zis.close();
	}

	private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
		File destFile = new File(destinationDir, zipEntry.getName());

		String destDirPath = destinationDir.getCanonicalPath();
		String destFilePath = destFile.getCanonicalPath();

		if (!destFilePath.startsWith(destDirPath + File.separator)) {
			throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
		}

		return destFile;
	}

	public void toJson(String workingDirName, File dependancyDir, File fragmentDir, int year) throws Exception {

		String inputFilename = workingDirName + "/word/document.xml";

		this.year = year;

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(inputFilename);

		doc.getDocumentElement().normalize();
		Element root = doc.getDocumentElement();

		MyDocument document = MyDocument.create(root, 0);
		OutputDocument outputDocument = document.toOutput();

		ObjectMapper objectMapper = new ObjectMapper();
		for (Fragment fragment : outputDocument.fragments) {

			String filename = String.format("%04d-%02d-%02d-%s-%s", fragment.year, fragment.month, fragment.day,
					fragment.order, fragment.reference) + ".json";

			if (fragment.html == null) {
				throw new Exception("null line found in fragment: " + filename);
			}

			File outputFile = new File(fragmentDir, filename);
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, fragment);

			File pageFile = new File(dependancyDir, fragment.reference);
			touch(pageFile);
		}
	}

	public static void touch(File file) throws IOException {
		long timestamp = System.currentTimeMillis();
		touch(file, timestamp);
	}

	public static void touch(File file, long timestamp) throws IOException {
		if (!file.exists()) {
			new FileOutputStream(file).close();
		}

		file.setLastModified(timestamp);
	}
}
