/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.internal.launch.win32;

import java.util.Map;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.RuntimeProcess;

public class ZephyrRuntimeProcess extends RuntimeProcess {

	public static final String WINDOWTITLE = "zephyr-eclipse"; //$NON-NLS-1$

	public ZephyrRuntimeProcess(ILaunch launch, Process process, String name,
			Map<String, String> attributes) {
		super(launch, process, name, attributes);
	}

	@Override
	public void terminate() throws DebugException {
		Process process = getSystemProcess();

		if (!isTerminated() && (process.getClass().getName()
				.equals("java.lang.Win32Process")
				|| process.getClass().getName()
						.equals("java.lang.ProcessImpl"))) {
			String killCmd = String.format(
					"taskkill /T /F /FI \"WINDOWTITLE eq %s*\"", WINDOWTITLE);

			try {
				Process killp = Runtime.getRuntime().exec(killCmd);
				int exitval = killp.waitFor();
				if (exitval != 0) {

				}
			} catch (Exception e) {

			}
		}

		super.terminate();
	}
}
