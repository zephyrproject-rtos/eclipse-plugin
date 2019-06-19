/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.ui.launch.tabs;

import org.eclipse.cdt.debug.gdbjtag.core.IGDBJtagConstants;
import org.eclipse.cdt.debug.gdbjtag.ui.GDBJtagStartupTab;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

public class CommonDebugLaunchStartupTab extends GDBJtagStartupTab {

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		super.setDefaults(configuration);

		configuration.setAttribute(IGDBJtagConstants.ATTR_LOAD_IMAGE, false);
	}

}
