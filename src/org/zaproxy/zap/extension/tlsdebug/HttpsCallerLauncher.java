package org.zaproxy.zap.extension.tlsdebug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.parosproxy.paros.Constant;

public class HttpsCallerLauncher {

	private ExtensionTlsDebug extension;

	public HttpsCallerLauncher(ExtensionTlsDebug extension) {
		this.extension = extension;
	}

	public void startProcess(URL url, String debugStatus) throws IOException {

		ProcessBuilder pb = new ProcessBuilder(getArgumentsList(url, debugStatus));
		Process p = pb.start();

		Thread tout = new Thread(new StreamController(p.getInputStream()));
		Thread terr = new Thread(new StreamController(p.getErrorStream()));
		tout.start();
		terr.start();
		try {
			tout.join();
			terr.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private List<String> getArgumentsList(URL url, String debugStatus) throws IOException {
		List<String> argumentsList = new ArrayList<String>();
		argumentsList.add(System.getProperty("java.home") + "/bin/java");
		argumentsList.add("-classpath");
		argumentsList.add(getClasspath());
		argumentsList.add("-Djavax.net.debug=" + debugStatus);
		// argumentsList.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=1044");
		argumentsList.add(HttpsCallerProcess.class.getName());
		argumentsList.add(url.toString());
		return argumentsList;
	}

	private String getClasspath() throws IOException {
		StringBuffer classpath = new StringBuffer(System.getProperty("java.class.path"));
		// /lang for message bundles
		classpath.append(";").append(Constant.getZapInstall()).append("lang");
		// path to TLS Debug extension
		classpath.append(";").append(getPluginPath());
		return classpath.toString();
	}

	private String getPluginPath() throws IOException {
		String current = Paths.get(".").toAbsolutePath().toString();
		File pluginFolder = new File(current, Constant.FOLDER_PLUGIN);
		if (!pluginFolder.exists()) {
			String msg = String.format("Did not find plugin folder %s", pluginFolder);
			throw new FileNotFoundException(msg);
		}

		// validate
		String[] files = pluginFolder.list(new TlsPluginFilenameFilter());
		// validate plugin file
		if ((files == null) || (files.length == 0)) {
			String msg = String.format("Did not find tlsdebug*.zap in %s", Constant.FOLDER_PLUGIN);
			throw new FileNotFoundException(msg);
		}
		if (files.length > 1) {
			String msg = String.format("Found too many tlsdebug*.zap in %s", Constant.FOLDER_PLUGIN);
			throw new IllegalStateException(msg);
		}
		File fullPath = new File(pluginFolder,files[0]);
		return fullPath.getCanonicalPath();
	}

	private class StreamController implements Runnable {

		private BufferedReader reader;

		public StreamController(InputStream in) {
			InputStreamReader inr = new InputStreamReader(in);
			reader = new BufferedReader(inr);
		}

		@Override
		public void run() {
			try {
				String line = null;
				while ((line = reader.readLine()) != null) {
					extension.notifyResponse(line + "\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private class TlsPluginFilenameFilter implements FilenameFilter {

		@Override
		public boolean accept(File dir, String name) {
			return Pattern.matches("tlsdebug.*\\.zap", name);
		}

	}
}