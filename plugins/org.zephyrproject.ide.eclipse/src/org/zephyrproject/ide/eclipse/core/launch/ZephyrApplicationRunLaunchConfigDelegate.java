/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.launch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.build.ICBuildConfiguration;
import org.eclipse.cdt.debug.core.launch.CoreBuildLaunchConfigDelegate;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.launchbar.core.target.ILaunchTarget;
import org.eclipse.launchbar.core.target.launch.ITargetedLaunch;
import org.zephyrproject.ide.eclipse.core.ZephyrConstants;
import org.zephyrproject.ide.eclipse.core.ZephyrStrings;
import org.zephyrproject.ide.eclipse.core.build.ZephyrApplicationBuildConfiguration;
import org.zephyrproject.ide.eclipse.core.build.ZephyrApplicationToolChain;
import org.zephyrproject.ide.eclipse.core.internal.ZephyrHelpers;

public class ZephyrApplicationRunLaunchConfigDelegate
		extends CoreBuildLaunchConfigDelegate {

	public static final String CMD_RUN = "run"; //$NON-NLS-1$

	public static final String CMD_FLASH = "flash"; //$NON-NLS-1$

	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		IProject project = getProject(configuration);
		ILaunchTarget target = ((ITargetedLaunch) launch).getLaunchTarget();

		ZephyrApplicationBuildConfiguration appBuildCfg =
				getBuildConfiguration(project, mode, target, monitor);
		ZephyrApplicationToolChain toolChain =
				(ZephyrApplicationToolChain) appBuildCfg.getToolChain();

		String cmakeGenerator = ZephyrHelpers.getCMakeGenerator(project);
		String makeProgram = toolChain.getMakeProgram();
		if (makeProgram == null) {
			throw new CoreException(ZephyrHelpers.errorStatus(
					"Project is not correctly configured.", //$NON-NLS-1$
					new RuntimeException("Cannot get CMake generator used."))); //$NON-NLS-1$
		}

		/* Figure out whether to do run, flash or custom command */
		String commandSelection = configuration.getAttribute(
				ZephyrConstants.Launch.ATTR_COMMAND_SELECTION,
				ZephyrStrings.EMPTY_STRING);
		String cmdToRun = null;
		if (commandSelection
				.equals(ZephyrConstants.Launch.COMMAND_SELECTION_CUSTOM)) {
			/* Need to run custom command */
			doCustomCommand(project, appBuildCfg, launch, configuration);
			return;
		} else if (commandSelection
				.equals(ZephyrConstants.Launch.COMMAND_SELECTION_FLASHTGT)) {
			cmdToRun = CMD_FLASH;
		} else if (commandSelection
				.equals(ZephyrConstants.Launch.COMMAND_SELECTION_EMULATOR)) {
			cmdToRun = CMD_RUN;
		}

		if (cmdToRun == null) {
			throw new CoreException(ZephyrHelpers.errorStatus(
					"Project is not correctly configured.", //$NON-NLS-1$
					new RuntimeException("Unknown Command to Run"))); //$NON-NLS-1$
		}

		if (cmakeGenerator.equals(ZephyrConstants.CMAKE_GENERATOR_MAKEFILE)) {
			doMakefile(project, appBuildCfg, launch, makeProgram, cmdToRun);
		} else if (cmakeGenerator
				.equals(ZephyrConstants.CMAKE_GENERATOR_NINJA)) {
			doNinja(project, appBuildCfg, launch, makeProgram, cmdToRun);
		} else {
			throw new CoreException(ZephyrHelpers.errorStatus(
					"Project is not correctly configured.", //$NON-NLS-1$
					new RuntimeException("Unknown CMake Generator."))); //$NON-NLS-1$
		}
	}

	@Override
	protected ZephyrApplicationBuildConfiguration getBuildConfiguration(
			IProject project, String mode, ILaunchTarget target,
			IProgressMonitor monitor) throws CoreException {
		ICBuildConfiguration appBuildCfg = project.getActiveBuildConfig()
				.getAdapter(ICBuildConfiguration.class);

		if ((appBuildCfg == null)
				|| !(appBuildCfg instanceof ZephyrApplicationBuildConfiguration)) {
			throw new CoreException(ZephyrHelpers.errorStatus(
					"Build not configured properly.", //$NON-NLS-1$
					new RuntimeException("Build configuration is not valid."))); //$NON-NLS-1$
		}

		return (ZephyrApplicationBuildConfiguration) appBuildCfg;
	}

	private String[] getEnvironmentArray(
			ZephyrApplicationBuildConfiguration appBuildCfg) {
		Map<String, String> env = new HashMap<>(System.getenv());
		appBuildCfg.setBuildEnvironment(env);

		List<String> envp = new ArrayList<>();
		for (Map.Entry<String, String> entry : env.entrySet()) {
			envp.add(String.format("%s=%s", entry.getKey(), entry.getValue()));
		}

		return envp.toArray(new String[0]);
	}

	private void doMakefile(IProject project,
			ZephyrApplicationBuildConfiguration appBuildCfg, ILaunch launch,
			String makeProgram, String mode) throws CoreException {
		try {
			String[] command = {
				makeProgram,
				mode
			};

			ProcessBuilder builder = new ProcessBuilder(command)
					.directory(appBuildCfg.getBuildDirectory().toFile());
			appBuildCfg.setBuildEnvironment(builder.environment());
			Process process = builder.start();
			IProcess iproc = DebugPlugin.newProcess(launch, process,
					ZephyrHelpers.getBoardName(project));
			launch.addProcess(iproc);
		} catch (IOException e) {
			throw new CoreException(
					ZephyrHelpers.errorStatus("Error running application.", e)); //$NON-NLS-1$
		}
	}

	private String[] doNinja(IProject project,
			ZephyrApplicationBuildConfiguration appBuildCfg, ILaunch launch,
			String makeProgram, String mode) throws CoreException {
		/*
		 * TODO: Rework Ninja execution when using Java SE > 8.
		 *
		 * When Ninja gets SIGTERM, it won't spawn new processes but will simply
		 * wait for already spawned processes to finish. So if one of those
		 * spawned processes continues execution, Ninja won't terminate. This
		 * issue is problematic with QEMU (and possibly other emulators) as they
		 * will keep on running forever in background. This is because the
		 * Process class sends SIGTERM to Ninja (e.g. click on Terminate button
		 * in Eclipse's console) but Ninja simply decides to spin waiting. This
		 * also applies to flashing as the user will not be able to stop the
		 * flashing process.
		 *
		 * This gets more complicated as Eclipse 4.6.x (which this plugins is
		 * currently targeting) can only recognize Java SE 8 (the only one
		 * available below Java SE 11 at time of writing this). Searching online
		 * seems to indicate that Java SE 11 is only supported on Eclipse 4.9+.
		 * As Java SE 8 does not support getting children of a Process object,
		 * and thus unable to terminate Ninja spawned processes correctly.
		 *
		 * So in the meantime, dry run Ninja to extract the necessary commands
		 * and run those commands instead.
		 */
		try {
			String[] command = {
				makeProgram,
				"-v", //$NON-NLS-1$
				"-n", //$NON-NLS-1$
				mode
			};

			/* First dry run Ninja to extract command to run */
			ProcessBuilder builder = new ProcessBuilder(command)
					.directory(appBuildCfg.getBuildDirectory().toFile());
			appBuildCfg.setBuildEnvironment(builder.environment());
			Process process = builder.start();
			process.waitFor();
			if (process.exitValue() != 0) {
				throw new CoreException(ZephyrHelpers.errorStatus(
						"Error running Ninja to extract command.", //$NON-NLS-1$
						new Exception(String.format("Ninja exit code %d", //$NON-NLS-1$
								process.exitValue()))));
			}

			/*
			 * Grab output from Ninja
			 *
			 * Currently only supports output of:
			 * [0/1] <command> && <command>
			 */
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(process.getInputStream()));
			String line;
			List<String> ninjaOutput = new ArrayList<>();
			while ((line = reader.readLine()) != null) {
				ninjaOutput.add(line);
			}
			reader.close();

			if (ninjaOutput.isEmpty()) {
				throw new CoreException(ZephyrHelpers.errorStatus(
						"Error running Ninja to extract command.", //$NON-NLS-1$
						new Exception("Ninja output is empty."))); //$NON-NLS-1$
			}
			if (ninjaOutput.size() > 1) {
				throw new CoreException(ZephyrHelpers.errorStatus(
						"Error running Ninja to extract command.", //$NON-NLS-1$
						new Exception("Ninja returns too many lines."))); //$NON-NLS-1$
			}

			line = ninjaOutput.get(0);
			if (!line.startsWith("[0/1]")) { //$NON-NLS-1$
				throw new CoreException(ZephyrHelpers.errorStatus(
						"Error running Ninja to extract command.", //$NON-NLS-1$
						new Exception(String.format(
								"Returned line does not start with '[0/1]': '%s'", //$NON-NLS-1$
								line))));
			}

			/*
			 * Command returned in Windows can be executed directly since
			 * it runs cmd.exe.
			 *
			 * Others return "cd <...> && <command to execute>" so we need
			 * to extract the second part.
			 */
			String cmdLine = line.substring("[0/1]".length() + 1).trim(); //$NON-NLS-1$
			if (!Platform.getOS().equals(Platform.OS_WIN32)) {
				String[] items = cmdLine.trim().split("&&");
				if (items.length != 2) {
					throw new CoreException(ZephyrHelpers.errorStatus(
							"Error running Ninja to extract command.", //$NON-NLS-1$
							new Exception(String.format(
									"Returned line does not have correct number of items: '%s'", //$NON-NLS-1$
									line))));
				}

				cmdLine = items[1].trim();
			}

			process = Runtime.getRuntime().exec(cmdLine,
					getEnvironmentArray(appBuildCfg));
			IProcess iproc = DebugPlugin.newProcess(launch, process,
					ZephyrHelpers.getBoardName(project));
			launch.addProcess(iproc);
		} catch (IOException | InterruptedException e) {
			throw new CoreException(
					ZephyrHelpers.errorStatus("Error running application.", e)); //$NON-NLS-1$
		}
		return null;
	}

	private void doCustomCommand(IProject project,
			ZephyrApplicationBuildConfiguration appBuildCfg, ILaunch launch,
			ILaunchConfiguration configuration) throws CoreException {
		try {
			String cmdLine = configuration.getAttribute(
					ZephyrConstants.Launch.ATTR_CUSTOM_COMMAND,
					ZephyrStrings.EMPTY_STRING);

			if (cmdLine.trim().isEmpty()) {
				/* Nothing to run */
				return;
			}

			Process process = Runtime.getRuntime().exec(cmdLine,
					getEnvironmentArray(appBuildCfg));
			IProcess iproc = DebugPlugin.newProcess(launch, process,
					ZephyrHelpers.getBoardName(project));
			launch.addProcess(iproc);
		} catch (IOException e) {
			throw new CoreException(
					ZephyrHelpers.errorStatus("Error running application.", e)); //$NON-NLS-1$
		}
	}

}
