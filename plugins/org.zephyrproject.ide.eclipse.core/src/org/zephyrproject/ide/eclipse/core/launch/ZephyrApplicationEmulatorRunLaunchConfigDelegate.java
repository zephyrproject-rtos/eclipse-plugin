/*
 * Copyright (c) 2019-2020 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.launch;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.launchbar.core.target.ILaunchTarget;
import org.eclipse.launchbar.core.target.launch.ITargetedLaunch;
import org.zephyrproject.ide.eclipse.core.ZephyrStrings;
import org.zephyrproject.ide.eclipse.core.build.ZephyrApplicationBuildConfiguration;
import org.zephyrproject.ide.eclipse.core.build.toolchain.ZephyrGCCToolChain;
import org.zephyrproject.ide.eclipse.core.build.CMakeConstants;
import org.zephyrproject.ide.eclipse.core.internal.ZephyrHelpers;

public class ZephyrApplicationEmulatorRunLaunchConfigDelegate
		extends ZephyrAbstractRunLaunchConfigDelegate {

	private static final String CMD_RUN = "run"; //$NON-NLS-1$

	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		if (ILaunchManager.RUN_MODE.equals(mode)) {
			run(configuration, mode, launch, monitor);
		} else {
			throw new CoreException(ZephyrHelpers.errorStatus(
					"Project is not correctly configured.", //$NON-NLS-1$
					new RuntimeException(
							String.format("Unknown mode: %s", mode)))); // $NON-NLS-1$
		}
	}

	private void run(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		IProject project = getProject(configuration);
		ILaunchTarget target = ((ITargetedLaunch) launch).getLaunchTarget();

		ZephyrApplicationBuildConfiguration appBuildCfg =
				getBuildConfiguration(project, mode, target, monitor);
		ZephyrGCCToolChain toolChain =
				(ZephyrGCCToolChain) appBuildCfg.getToolChain();

		String cmakeGenerator = ZephyrHelpers.getCMakeGenerator(project);
		String makeProgram = toolChain.getMakeProgram();
		if (makeProgram == null) {
			throw new CoreException(ZephyrHelpers.errorStatus(
					"Project is not correctly configured.", //$NON-NLS-1$
					new RuntimeException("Cannot get CMake generator used."))); //$NON-NLS-1$
		}

		/* Figure out whether to do run, flash or custom command */
		String commandSelection = configuration.getAttribute(
				ZephyrLaunchConstants.ATTR_EMULATOR_RUN_CMD_SEL,
				ZephyrStrings.EMPTY_STRING);
		String cmdToRun = null;
		if (commandSelection
				.equals(ZephyrLaunchConstants.EMULATOR_RUN_CMD_SEL_BUILDSYS)) {
			cmdToRun = CMD_RUN;

			if (cmakeGenerator
					.equals(CMakeConstants.CMAKE_GENERATOR_MAKEFILE)) {
				doMakefile(project, appBuildCfg, launch, makeProgram, cmdToRun);
			} else if (cmakeGenerator
					.equals(CMakeConstants.CMAKE_GENERATOR_NINJA)) {
				doNinja(project, appBuildCfg, launch, makeProgram, cmdToRun);
			} else {
				throw new CoreException(ZephyrHelpers.errorStatus(
						"Project is not correctly configured.", //$NON-NLS-1$
						new RuntimeException("Unknown CMake Generator."))); //$NON-NLS-1$
			}
		} else if (commandSelection
				.equals(ZephyrLaunchConstants.EMULATOR_RUN_CMD_SEL_WEST)) {
			String args = configuration.getAttribute(
					ZephyrLaunchConstants.ATTR_RUN_CMD_WEST_ARGS,
					ZephyrStrings.EMPTY_STRING);

			args = "-t run " + args; //$NON-NLS-1$
			args = args.trim();

			runWest(project, appBuildCfg, launch, "build", args); //$NON-NLS-1$
		} else {
			throw new CoreException(ZephyrHelpers.errorStatus(
					"Project is not correctly configured.", //$NON-NLS-1$
					new RuntimeException("Unknown Command to Run"))); //$NON-NLS-1$
		}

	}

}
