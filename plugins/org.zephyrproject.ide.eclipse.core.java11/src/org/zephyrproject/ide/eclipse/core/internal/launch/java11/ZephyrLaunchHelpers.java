/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.internal.launch.java11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
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

	public Process runWest(IProject project,
			ZephyrApplicationBuildConfiguration appBuildCfg, ILaunch launch,
			String action, String args) throws CoreException, IOException {
		String westPath = ZephyrHelpers.getWestPath(project);

		if ((westPath == null) || (westPath.trim().isEmpty())) {
			throw new CoreException(ZephyrHelpers.errorStatus(
					"Project is not correctly configured.", //$NON-NLS-1$
					new RuntimeException("Cannot get path for West."))); //$NON-NLS-1$
		}

		ArrayList<String> cmds = new ArrayList<String>();
		cmds.add(westPath);
		cmds.add(action);
		if (args != null) {
			cmds.addAll(ZephyrHelpers.getArguments(args));
		}

		String[] command = cmds.toArray(new String[0]);

		ProcessBuilder builder = new ProcessBuilder(command)
				.directory(appBuildCfg.getBuildDirectory().toFile());
		builder.environment().putAll(
				ZephyrHelpers.Launch.getBuildEnvironmentMap(appBuildCfg));
		Process process = builder.start();
		DebugPlugin.newProcess(launch, process,
				ZephyrHelpers.getBoardName(project));

		return process;
	}

}
