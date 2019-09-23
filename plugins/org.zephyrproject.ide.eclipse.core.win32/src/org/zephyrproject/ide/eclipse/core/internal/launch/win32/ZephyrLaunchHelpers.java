/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.internal.launch.win32;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.zephyrproject.ide.eclipse.core.build.ZephyrApplicationBuildConfiguration;
import org.zephyrproject.ide.eclipse.core.internal.ZephyrHelpers;
import org.zephyrproject.ide.eclipse.core.launch.IZephyrLaunchHelper;

public final class ZephyrLaunchHelpers implements IZephyrLaunchHelper {

	private static final String BAT_FILE_PREFIX = ".zephyr-eclipse-"; //$NON-NLS-1$
	private static final String BAT_FILE_SUFFIX = ".bat"; //$NON-NLS-1$

	public ZephyrLaunchHelpers() {
	}

	private static Path createBatchFile(
			ZephyrApplicationBuildConfiguration appBuildCfg, String cmd)
			throws CoreException, IOException {
		Path buildDir = appBuildCfg.getBuildDirectory();

		Path tmpFile = Files.createTempFile(buildDir, BAT_FILE_PREFIX,
				BAT_FILE_SUFFIX);
		tmpFile.toFile().deleteOnExit();

		try (BufferedWriter writer =
				new BufferedWriter(new FileWriter(tmpFile.toFile()))) {

			writer.write("@echo off"); //$NON-NLS-1$
			writer.newLine();

			writer.write(String.format("CALL %s", cmd)); //$NON-NLS-1$
			writer.newLine();

			writer.write("pause"); //$NON-NLS-1$
			writer.newLine();

			writer.write("exit"); //$NON-NLS-1$
			writer.newLine();

			writer.flush();
		}

		return tmpFile;
	}

	private static Process runCmd(IProject project,
			ZephyrApplicationBuildConfiguration appBuildCfg, ILaunch launch,
			String cmd) throws CoreException, IOException {
		Path batFile = createBatchFile(appBuildCfg, cmd);

		cmd = String.format("cmd.exe /c start \"%s\" /WAIT %s",
				ZephyrRuntimeProcess.WINDOWTITLE,
				batFile.toAbsolutePath().toString());

		Process process = Runtime.getRuntime().exec(cmd,
				ZephyrHelpers.Launch.getBuildEnvironmentArray(appBuildCfg),
				appBuildCfg.getBuildDirectory().toFile());
		DebugPlugin.newProcess(launch, process,
				ZephyrHelpers.getBoardName(project));

		return process;
	}

	public Process doMakefile(IProject project,
			ZephyrApplicationBuildConfiguration appBuildCfg, ILaunch launch,
			String makeProgram, String mode) throws CoreException, IOException {
		String cmd = String.format("%s %s", makeProgram, mode);
		return runCmd(project, appBuildCfg, launch, cmd);
	}

	public Process doNinja(IProject project,
			ZephyrApplicationBuildConfiguration appBuildCfg, ILaunch launch,
			String makeProgram, String mode) throws CoreException, IOException {
		String cmd = String.format("%s %s", makeProgram, mode);
		return runCmd(project, appBuildCfg, launch, cmd);
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

		if (args == null) {
			String cmd = String.format("%s %s", westPath, action);
			return runCmd(project, appBuildCfg, launch, cmd);
		} else {
			String cmd = String.format("%s %s %s", westPath, action, args);
			return runCmd(project, appBuildCfg, launch, cmd);
		}
	}
}
