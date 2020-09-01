/*
 * Copyright (c) 2019-2020 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.internal.launch.java11;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.zephyrproject.ide.eclipse.core.build.ZephyrApplicationBuildConfiguration;
import org.zephyrproject.ide.eclipse.core.internal.ZephyrHelpers;
import org.zephyrproject.ide.eclipse.core.launch.IZephyrLaunchHelper;

public final class ZephyrLaunchHelpers implements IZephyrLaunchHelper {

	public ZephyrLaunchHelpers() {
	}

	private static String findCommand(String command) {
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			if (!command.toString().endsWith(".exe") //$NON-NLS-1$
					&& !command.toString().endsWith(".bat")) { //$NON-NLS-1$
				command = command.trim() + ".exe"; //$NON-NLS-1$
			}
		}

		/* Look for it in the path environment var */
		String path = System.getenv("PATH"); //$NON-NLS-1$
		for (String entry : path.split(File.pathSeparator)) {
			Path entryPath = Paths.get(entry);
			Path cmdPath = entryPath.resolve(command);
			if (Files.isExecutable(cmdPath)) {
				return cmdPath.toString();
			}
		}

		return null;
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

		/*
		 * Path to West may not have been cached by CMake.
		 * So this try to find West here.
		 */
		if ((westPath == null) || (westPath.trim().isEmpty())) {
			westPath = findCommand("west"); //$NON-NLS-1$
		}

		if ((westPath == null) || (westPath.trim().isEmpty())) {
			throw new CoreException(ZephyrHelpers.errorStatus(
					"Project is not correctly configured.", //$NON-NLS-1$
					new RuntimeException("Cannot get path for West."))); //$NON-NLS-1$
		}

		ArrayList<String> cmds = new ArrayList<String>();
		cmds.add(westPath);
		cmds.add(action);
		if (args != null) {
			cmds.addAll(ZephyrHelpers.parseWestArguments(args));
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
