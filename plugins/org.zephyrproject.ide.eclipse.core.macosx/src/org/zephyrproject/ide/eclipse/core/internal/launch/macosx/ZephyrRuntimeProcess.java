/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.internal.launch.macosx;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
		Process process = getSystemProcess();

		if (!isTerminated()
				&& (process.getClass().getName().equals("java.lang.UNIXProcess")
						|| process.getClass().getName()
								.equals("java.lang.ProcessImpl"))) {
			/* For Linux and macOS */
			try {
				/* Grab the PID from root of process tree */
				Field fPid = process.getClass().getDeclaredField("pid");
				fPid.setAccessible(true);
				int pid = fPid.getInt(process);
				fPid.setAccessible(false);

				/* Find all leaf nodes in the process tree */
				HashSet<Integer> leafPids = new HashSet<Integer>();
				findAllLeafProcesses(pid, leafPids);

				/* Terminate the leaf node processes */
				for (Integer p : leafPids) {
					killPid(p);
				}
			} catch (Exception e) {
			}
		}

		super.terminate();
	}

	private static void findAllLeafProcesses(Integer pid,
			HashSet<Integer> pids) {
		String[] command = {
			"pgrep", //$NON-NLS-1$
			"-P", //$NON-NLS-1$ ,
			pid.toString()
		};

		ProcessBuilder pb = new ProcessBuilder(command);
		Process process;
		try {
			process = pb.start();
			int exitval = process.waitFor();
			boolean noChild = false;

			if (exitval == 0) {
				/* pgrep succeeded, parse for children PIDs */
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(process.getInputStream()));
				String line;
				List<String> children = new ArrayList<>();
				while ((line = reader.readLine()) != null) {
					children.add(line);
				}
				reader.close();

				if (children.isEmpty()) {
					/* If nothing is returned, assume no children */
					noChild = true;
				} else {
					/* Go through each PIDs and find their children */
					for (String child : children) {
						try {
							Integer cPid = Integer.valueOf(child);
							findAllLeafProcesses(cPid, pids);
						} catch (NumberFormatException e) {
						}
					}
				}
			}

			if ((exitval != 0) || noChild) {
				/* No more children, so this is a leaf node */
				pids.add(pid);
			}
		} catch (Exception e) {
		}
	}

	private static boolean killPid(Integer pid) {
		try {
			String command = String.format("kill -s TERM %d", pid);
			Process process = Runtime.getRuntime().exec(command);

			int exitval = process.waitFor();

			if (exitval == 0) {
				return true;
			}
		} catch (Exception e) {
		}

		return false;
	}

}
