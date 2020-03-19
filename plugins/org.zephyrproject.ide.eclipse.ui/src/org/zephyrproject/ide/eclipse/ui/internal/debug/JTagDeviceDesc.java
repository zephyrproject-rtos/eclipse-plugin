/*
 * Copyright (c) 2020 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.ui.internal.debug;

public class JTagDeviceDesc {

	public static final String GENERIC_TCPIP_DEVICE =
			"org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.genericDevice"; //$NON-NLS-1$

	public static final String IP_ADDR_LOCALHOST = "localhost"; //$NON-NLS-1$

	public String id;
	public String jtag;
	public String host;
	public Integer port;
	public String connection;

	private JTagDeviceDesc(String id, String jtag) {
		this.id = id;
		this.jtag = jtag;
	}

	public JTagDeviceDesc(String id, String jtag, String host, Integer port) {
		this(id, jtag);
		this.host = host;
		this.port = port;
		this.connection = null;
	}

	public JTagDeviceDesc(String id, String jtag, String connection) {
		this(id, jtag);
		this.connection = connection;
	}
}
