/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.internal.launch.java11;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.zephyrproject.ide.eclipse.core.ZephyrStrings;
import org.zephyrproject.ide.eclipse.core.build.ZephyrApplicationBuildConfiguration;
import org.zephyrproject.ide.eclipse.core.internal.ZephyrHelpers;
import org.zephyrproject.ide.eclipse.core.launch.IZephyrLaunchHelper;

public final class ZephyrLaunchHelpers implements IZephyrLaunchHelper {

	public ZephyrLaunchHelpers() {
	}

	public Process doMakefile(IProject project,
			ZephyrApplicationBuildConfiguration appBuildCfg, ILaunch launch,
			String makeProgram, String mode) throws CoreException, IOException {
		String[] command = {
			makeProgram,
			mode
		};

		ProcessBuilder builder = new ProcessBuilder(command)
				.directory(appBuildCfg.getBuildDirectory().toFile());
		builder.environment().putAll(
				ZephyrHelpers.Launch.getBuildEnvironmentMap(appBuildCfg));
		Process process = builder.start();
		DebugPlugin.newProcess(launch, process,
				ZephyrHelpers.getBoardName(project));

		return process;
	}

	public Process doNinja(IProject project,
			ZephyrApplicationBuildConfiguration appBuildCfg, ILaunch launch,
			String makeProgram, String mode) throws CoreException, IOException {
		String[] command = {
			makeProgram,
			mode
		};

		ProcessBuilder builder = new ProcessBuilder(command)
				.directory(appBuildCfg.getBuildDirectory().toFile());
		builder.environment().putAll(
				ZephyrHelpers.Launch.getBuildEnvironmentMap(appBuildCfg));
		Process process = builder.start();
		DebugPlugin.newProcess(launch, process,
				ZephyrHelpers.getBoardName(project));

		return process;
	}

	public Process doCustomCommand(IProject project,
			ZephyrApplicationBuildConfiguration appBuildCfg, ILaunch launch,
			ILaunchConfiguration configuration, String attrCustomCmd)
			throws CoreException, IOException {
		String cmdLine = configuration.getAttribute(attrCustomCmd,
				ZephyrStrings.EMPTY_STRING);

		if (cmdLine.trim().isEmpty()) {
			/* Nothing to run */
			return null;
		}

		Process process = Runtime.getRuntime().exec(cmdLine,
				ZephyrHelpers.Launch.getBuildEnvironmentArray(appBuildCfg),
				appBuildCfg.getBuildDirectory().toFile());
		DebugPlugin.newProcess(launch, process,
				ZephyrHelpers.getBoardName(project));

		return process;
	}

}
