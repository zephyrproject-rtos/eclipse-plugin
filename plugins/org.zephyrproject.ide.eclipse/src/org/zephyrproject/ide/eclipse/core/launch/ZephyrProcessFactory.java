/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.launch;

import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.IProcessFactory;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.zephyrproject.ide.eclipse.core.internal.launch.unix.ZephyrUnixRuntimeProcess;
import org.zephyrproject.ide.eclipse.core.internal.launch.windows.ZephyrWindowsRuntimeProcess;

public class ZephyrProcessFactory implements IProcessFactory {

	public static final String ID =
			"org.zephyrproject.ide.eclipse.processFactory"; //$NON-NLS-1$

	@Override
	public IProcess newProcess(ILaunch launch, Process process, String label,
			Map<String, String> attributes) {
		if (Platform.getOS().equals(Platform.OS_LINUX)
				|| Platform.getOS().equals(Platform.OS_MACOSX)) {
			return new ZephyrUnixRuntimeProcess(launch, process, label,
					attributes);
		} else if (Platform.getOS().equals(Platform.OS_WIN32)) {
			return new ZephyrWindowsRuntimeProcess(launch, process, label,
					attributes);
		} else {
			return new RuntimeProcess(launch, process, label, attributes);
		}
	}

}
