/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.internal.launch.java11;

import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.RuntimeProcess;

public class ZephyrRuntimeProcess extends RuntimeProcess {

	public ZephyrRuntimeProcess(ILaunch launch, Process process, String name,
			Map<String, String> attributes) {
		super(launch, process, name, attributes);
	}

	@Override
	public void terminate() throws DebugException {
		if (!isTerminated()) {
			Process process = getSystemProcess();

			try {
				/* Stop all descendants */
				Stream<ProcessHandle> handles = process.descendants();
				handles.forEach(ProcessHandle::destroy);
			} catch (Exception e) {
			}
		}

		super.terminate();
	}

}
