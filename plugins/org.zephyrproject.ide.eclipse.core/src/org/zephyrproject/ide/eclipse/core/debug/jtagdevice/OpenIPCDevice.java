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

import org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.DefaultGDBJtagConnectionImpl;

public class OpenIPCDevice extends DefaultGDBJtagConnectionImpl {

	@Override
	public void doDelay(int delay, Collection<String> commands) {
		addCmd(commands, "monitor sleep " + String.valueOf(delay * 1000)); //$NON-NLS-1$
	}

	@Override
	public void doResetAndHalt(Collection<String> commands) {
		addCmd(commands, "monitor reset halt"); //$NON-NLS-1$
	}

}