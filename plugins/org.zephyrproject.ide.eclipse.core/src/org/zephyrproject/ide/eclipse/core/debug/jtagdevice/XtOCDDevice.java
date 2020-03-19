/*
 * Copyright (c) 2008, 2012 QNX Software Systems and others.
 * Copyright (c) 2020 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

/*
 * Originally from org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.GenericDevice
 */

package org.zephyrproject.ide.eclipse.core.debug.jtagdevice;

import java.util.Collection;

import org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.DefaultGDBJtagDeviceImpl;

public class XtOCDDevice extends DefaultGDBJtagDeviceImpl {

	@Override
	public void doDelay(int delay, Collection<String> commands) {
	}

	@Override
	public void doHalt(Collection<String> commands) {
	}

	@Override
	public void doReset(Collection<String> commands) {
	}

	@Override
	public String getDefaultIpAddress() {
		return "localhost"; //$NON-NLS-1$
	}

	@Override
	public String getDefaultPortNumber() {
		return "20000"; //$NON-NLS-1$
	}

}
