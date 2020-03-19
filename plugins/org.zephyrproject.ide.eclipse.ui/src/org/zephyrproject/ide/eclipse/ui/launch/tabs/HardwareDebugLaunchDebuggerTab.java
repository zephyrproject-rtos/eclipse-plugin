/*
 * Copyright (c) 2020 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.ui.launch.tabs;

import org.zephyrproject.ide.eclipse.ui.internal.debug.JTagDeviceDesc;
import org.zephyrproject.ide.eclipse.ui.internal.launch.tabs.CommonDebugLaunchDebuggerTab;

public class HardwareDebugLaunchDebuggerTab
		extends CommonDebugLaunchDebuggerTab {

	public HardwareDebugLaunchDebuggerTab() {
		super();

		/* Most boards use OpenOCD or pyOCD, so set it as default */
		super.defaultJtagDevice =
				"org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.OpenOCDSocket"; //$NON-NLS-1$
		super.defaultHost = JTagDeviceDesc.IP_ADDR_LOCALHOST;
		super.defaultPort = 3333;
	}

}
