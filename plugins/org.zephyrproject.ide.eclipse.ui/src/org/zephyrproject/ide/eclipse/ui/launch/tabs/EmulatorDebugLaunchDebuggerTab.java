/*
 * Copyright (c) 2020 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.ui.launch.tabs;

import org.zephyrproject.ide.eclipse.ui.internal.debug.JTagDeviceDesc;
import org.zephyrproject.ide.eclipse.ui.internal.launch.tabs.CommonDebugLaunchDebuggerTab;

public class EmulatorDebugLaunchDebuggerTab
		extends CommonDebugLaunchDebuggerTab {

	public EmulatorDebugLaunchDebuggerTab() {
		super();

		/*
		 * Some QEMU boards do not have runner 'qemu' set. So set QEMU as
		 * default here.
		 */
		super.defaultJtagDevice =
				"org.zephyrproject.ide.eclipse.core.debug.jtagdevice.qemuDevice"; //$NON-NLS-1$
		super.defaultHost = JTagDeviceDesc.IP_ADDR_LOCALHOST;
		super.defaultPort = 1234;
	}

}
