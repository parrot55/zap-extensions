package org.zaproxy.zap.extension.tlsdebug.util;

import java.io.IOException;
import java.io.OutputStream;

import org.zaproxy.zap.extension.tlsdebug.TlsDebugPanel;

public class PanelOutputStream extends OutputStream {
	private static final int CR = 13;
	private static final int LF = 10;
	private static final int BUF_MAX_SIZE = 256;

	private TlsDebugPanel tlsDebugPanel;
	private int[] buffer = new int[BUF_MAX_SIZE + 2];
	private boolean isPreviousCR = false;
	private int charIndex = 0;

	public PanelOutputStream(TlsDebugPanel tlsDebugPanel) {
		this.tlsDebugPanel = tlsDebugPanel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.OutputStream#write(int)
	 */
	@Override
	public void write(int b) throws IOException {
		String strg = null;
		if (isPreviousCR || (charIndex >= BUF_MAX_SIZE) || (b == LF)) {
			if (b == LF) {
				buffer[charIndex++] = b;
			} else if (charIndex >= BUF_MAX_SIZE) {
				buffer[charIndex++] = CR;
				buffer[charIndex++] = LF;
			}
			strg = new String(buffer, 0, charIndex);
			isPreviousCR = false;
			charIndex = 0;
			tlsDebugPanel.writeConsole(strg);
		} else {
			buffer[charIndex++] = b;
			isPreviousCR = (b == CR);
		}
	}
}
