/*
 * Copyright (c) 2019 Intel Corporation
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
import org.zephyrproject.ide.eclipse.ui.launch.tabs.CommonDebugLaunchDebuggerTab;
import org.zephyrproject.ide.eclipse.ui.launch.tabs.CommonDebugLaunchStartupTab;
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
			tabs.add(new CommonDebugLaunchDebuggerTab());
			tabs.add(new CommonDebugLaunchStartupTab());
			tabs.add(new SourceLookupTab());
			tabs.add(new CommonTab());
		}

		setTabs(tabs.toArray(new ILaunchConfigurationTab[0]));
	}

}
