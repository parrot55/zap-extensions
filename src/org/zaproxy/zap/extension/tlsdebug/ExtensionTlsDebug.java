/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2012 The ZAP development team
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.zaproxy.zap.extension.tlsdebug;

import java.awt.Container;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.log4j.Logger;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.control.Control.Mode;
import org.parosproxy.paros.extension.CommandLineArgument;
import org.parosproxy.paros.extension.ExtensionAdaptor;
import org.parosproxy.paros.extension.ExtensionHook;
import org.parosproxy.paros.extension.report.ReportLastScan;
import org.parosproxy.paros.model.Session;
import org.zaproxy.zap.extension.ext.ExtensionExtension;
import org.zaproxy.zap.extension.help.ExtensionHelp;
import org.zaproxy.zap.extension.tlsdebug.util.PanelOutputStream;

public class ExtensionTlsDebug extends ExtensionAdaptor {

	public static final String NAME = "ExtensionTlsDebug";
	protected static final String SCRIPT_CONSOLE_HOME_PAGE = Constant.ZAP_HOMEPAGE;
	private static final Logger LOGGER = Logger.getLogger(ExtensionTlsDebug.class);

	private TlsDebugPanel tlsDebugPanel = null;
	private PrintStream outputConsoleStream;
	private PrintStream original_out = null;
	private PrintStream original_err = null;

	public ExtensionTlsDebug() {
		super();
		initialize();
	}

	/**
	 * @param name
	 */
	public ExtensionTlsDebug(String name) {
		super(name);
	}

	/**
	 * This method initializes this
	 */
	private void initialize() {
		this.setName(NAME);
	}

	@Override
	public void hook(ExtensionHook extensionHook) {
		super.hook(extensionHook);

		if (getView() != null) {
			extensionHook.getHookView().addWorkPanel(getTlsDebugPanel());

			ExtensionHelp.enableHelpKey(getTlsDebugPanel(), "tlsdebug");
		}
	}

	@Override
	public boolean canUnload() {
		return true;
	}

	private TlsDebugPanel getTlsDebugPanel() {
		if (tlsDebugPanel == null) {
			tlsDebugPanel = new TlsDebugPanel(this);
			tlsDebugPanel.setName(Constant.messages.getString("tlsdebug.panel.title"));
			;
		}
		return tlsDebugPanel;
	}

	@Override
	public String getAuthor() {
		return Constant.ZAP_TEAM;
	}

	@Override
	public String getDescription() {
		return Constant.messages.getString("tlsdebug.desc");
	}

	@Override
	public URL getURL() {
		try {
			return new URL(Constant.ZAP_HOMEPAGE);
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public void launchDebug(URL url) throws IOException {
		HttpsCallerLauncher pl = new HttpsCallerLauncher(this);
		pl.startProcess(url, this.getTlsDebugPanel().getDebugProperty());
	}

	private PrintStream getOutputConsoleStream() {
		if (outputConsoleStream == null) {
			PanelOutputStream pos = new PanelOutputStream(this.tlsDebugPanel);
			outputConsoleStream = new PrintStream(pos);
		}
		return outputConsoleStream;
	}

	public void notifyResponse(String line) {
		this.tlsDebugPanel.writeConsole(line);
	}

	public void beginCheck() {
		this.original_out = System.out;
		this.original_err = System.err;

		System.setOut(getOutputConsoleStream());
		System.setErr(getOutputConsoleStream());
		System.out.println("Begin TLS debugging...");
	}

	public void endCheck() {
		System.setOut(original_out);
		System.setErr(original_err);
		getOutputConsoleStream().flush();
	}

	public void showOnStart(boolean showOnStart) {
		if (!showOnStart) {
			// Remove the tab right away
			Container parent = this.getTlsDebugPanel().getParent();
			parent.remove(this.getTlsDebugPanel());
		}

		// Save in configs
		ExtensionExtension extExt = (ExtensionExtension) Control.getSingleton().getExtensionLoader()
				.getExtension(ExtensionExtension.NAME);
		if (extExt != null) {
			extExt.enableExtension(NAME, showOnStart);
		}
	}
}
