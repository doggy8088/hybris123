package com.hybris.hybris123.runtime.helper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersionHelper {
	
	private static final Logger LOG = LoggerFactory.getLogger(VersionHelper.class);
	
	private VersionHelper() {}
	
	/**
	 * old format: 6.X.0.0, new format: YY.MM or YYMM
	 * @return enum matching the format Vdddd
	 */
	public static Version getVersion() {
		String buildNumberPath = System.getenv("HYBRIS_HOME_DIR");
		buildNumberPath += "/hybris/bin/platform/build.number";

		try (Stream<String> stream = Files.lines(Paths.get(buildNumberPath))) {
			String versionString = stream.filter(s -> s.contains("version=")).findFirst().orElseThrow(IOException::new);
			versionString = versionString.split("=")[1];
			versionString = "V" + versionString.replaceAll("[^0-9]", "").substring(0, 4);
			LOG.info("Commerce123 version is: {}", versionString);
			return Version.valueOf(versionString);
		} 
		catch (EnumConstantNotPresentException exc) {
			String wrongConstant = exc.constantName();
			if (wrongConstant.startsWith("V6")) {
				// given version is probably not from a release ZIP
				String likelyValue = wrongConstant.substring(0, 3) + "00";
				try {
					return Version.valueOf(likelyValue);
				}
				catch (EnumConstantNotPresentException ex) {
					return Version.UNDEFINED;
				}
			}
		}
		catch (IOException e) {
			LOG.error("Version not found. Please make sure hybris123 is placed in the SAP Commerce directory.");
			LOG.error("If hybris123 is placed correctly,"
					+ " check whether [HYBRIS_HOME_DIR]/hybris/bin/platform/build.number contains a version number.");
		}
		return Version.UNDEFINED;
	}
	
}
