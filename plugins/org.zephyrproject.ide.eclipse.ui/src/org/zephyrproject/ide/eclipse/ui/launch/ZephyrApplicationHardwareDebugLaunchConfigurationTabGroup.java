/*
 * Copyright (c) 2019-2020 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.ui.launch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.zephyrproject.ide.eclipse.ui.launch.tabs.CommonDebugLaunchStartupTab;
import org.zephyrproject.ide.eclipse.ui.launch.tabs.HardwareDebugLaunchDebuggerTab;
import org.zephyrproject.ide.eclipse.ui.launch.tabs.HardwareDebugLaunchMainTab;

public class ZephyrApplicationHardwareDebugLaunchConfigurationTabGroup
		extends AbstractLaunchConfigurationTabGroup {

	public ZephyrApplicationHardwareDebugLaunchConfigurationTabGroup() {
		super();
	}

	@Override
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		List<ILaunchConfigurationTab> tabs = new ArrayList<>();

		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			tabs.add(new HardwareDebugLaunchMainTab());
			tabs.add(new HardwareDebugLaunchDebuggerTab());
			tabs.add(new CommonDebugLaunchStartupTab());
			tabs.add(new SourceLookupTab());
			tabs.add(new CommonTab());
		}

		setTabs(tabs.toArray(new ILaunchConfigurationTab[0]));
	}

}
