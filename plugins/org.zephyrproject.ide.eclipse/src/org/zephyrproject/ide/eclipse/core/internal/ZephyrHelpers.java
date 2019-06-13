/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.internal;

import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_CROSS_COMPILE;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_CROSS_COMPILE_ENV;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_CUSTOM_ENV;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB_ENV;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_ISSM;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_ISSM_ENV;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS_ENV;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR_ENV;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.build.ICBuildConfiguration;
import org.eclipse.cdt.core.build.ICBuildConfigurationManager;
import org.eclipse.cdt.core.build.ICBuildConfigurationProvider;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.launchbar.core.target.ILaunchTarget;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.zephyrproject.ide.eclipse.core.ZephyrConstants;
import org.zephyrproject.ide.eclipse.core.ZephyrPlugin;
import org.zephyrproject.ide.eclipse.core.ZephyrStrings;
import org.zephyrproject.ide.eclipse.core.build.ZephyrApplicationBuildConfiguration;
import org.zephyrproject.ide.eclipse.core.build.ZephyrApplicationBuildConfigurationProvider;

/**
 * Helper class
 *
 * Contains helper functions.
 */
public final class ZephyrHelpers {

	public static class Build {

		private static void removeCBuildConfigManagerNoConfig(
				ICBuildConfigurationManager configManager,
				IBuildConfiguration config) {
			/*
			 * There are times when CBuildConfigurationManager cannot get
			 * the Zephyr build configurations at start-up (possibly due to
			 * start-up initialization latencies of plugins), and the project
			 * build configuration is forever blacklisted. This affects all
			 * the build, run and debug operations relying on the manager
			 * returning a working CBuildConfiguration. There is currently
			 * no methods to remove a blacklisted configuration. So here
			 * we are to workaround this by forcing access to the blacklist
			 * and manipulate it ourselves.
			 */

			try {
				Field fNoConfigs =
						configManager.getClass().getDeclaredField("noConfigs");
				Field fConfigs =
						configManager.getClass().getDeclaredField("configs");

				boolean oldAccessNoCfg = fNoConfigs.isAccessible();
				boolean oldAccessCfg = fConfigs.isAccessible();
				fNoConfigs.setAccessible(true);
				fConfigs.setAccessible(true);

				try {
					Object objNoCfg = fNoConfigs.get(configManager);
					Object objCfg = fConfigs.get(configManager);
					if (objNoCfg instanceof Set<?>) {
						@SuppressWarnings("unchecked")
						Set<IBuildConfiguration> ibcs =
								(Set<IBuildConfiguration>) objNoCfg;
						synchronized (objCfg) {
							ibcs.remove(config);
						}
					}
				} catch (Exception e) {
				} finally {
					fNoConfigs.setAccessible(oldAccessNoCfg);
					fConfigs.setAccessible(oldAccessCfg);
				}
			} catch (Exception e) {
			}
		}

		public static ICBuildConfiguration fixBuildConfig(
				IBuildConfiguration config) throws CoreException {
			ICBuildConfigurationManager configManager =
					CCorePlugin.getService(ICBuildConfigurationManager.class);

			if (configManager == null) {
				throw new CoreException(ZephyrHelpers.errorStatus(
						"Cannot get build configuration manager!", null));
			}

			/*
			 * Remove from blacklist and ask CBuildConfigurationManager to
			 * retry.
			 */
			removeCBuildConfigManagerNoConfig(configManager, config);

			ICBuildConfiguration buildCfg =
					configManager.getBuildConfiguration(config);

			if ((buildCfg != null)
					&& (buildCfg instanceof ZephyrApplicationBuildConfiguration)) {
				return buildCfg;
			}

			/* Need to do it manually now... */
			removeCBuildConfigManagerNoConfig(configManager, config);

			ICBuildConfigurationProvider provider = configManager.getProvider(
					ZephyrApplicationBuildConfigurationProvider.ID);

			if (provider == null) {
				throw new CoreException(ZephyrHelpers.errorStatus(
						"Cannot get build configuration provider!", null));
			}

			buildCfg = provider.getCBuildConfiguration(config, null);

			if ((buildCfg == null)
					|| !(buildCfg instanceof ZephyrApplicationBuildConfiguration)) {
				throw new CoreException(ZephyrHelpers.errorStatus(
						"Unable to retrieve build configuration!", null));
			}

			configManager.addBuildConfiguration(config, buildCfg);

			return buildCfg;
		}

	}

	public static class Launch {

		public static ZephyrApplicationBuildConfiguration getBuildConfiguration(
				IProject project) throws CoreException {
			ICBuildConfiguration appBuildCfg = project.getActiveBuildConfig()
					.getAdapter(ICBuildConfiguration.class);

			if ((appBuildCfg == null)
					|| !(appBuildCfg instanceof ZephyrApplicationBuildConfiguration)) {
				throw new CoreException(ZephyrHelpers.errorStatus(
						"Build not configured properly.", //$NON-NLS-1$
						new RuntimeException(
								"Build configuration is not valid."))); //$NON-NLS-1$
			}

			return (ZephyrApplicationBuildConfiguration) appBuildCfg;
		}

		public static ZephyrApplicationBuildConfiguration getBuildConfiguration(
				IProject project, String mode, ILaunchTarget target,
				IProgressMonitor monitor) throws CoreException {
			return getBuildConfiguration(project);
		}

		private static Map<String, String> getBuildEnvironmentMap(
				ZephyrApplicationBuildConfiguration appBuildCfg) {
			/* Fake a ProcessBuilder for its environment */
			ProcessBuilder pb = new ProcessBuilder(ZephyrStrings.EMPTY_STRING);

			Map<String, String> envMap = pb.environment();

			/* Get build environment from CDT */
			IEnvironmentVariable[] vars = CCorePlugin.getDefault()
					.getBuildEnvironmentManager()
					.getVariables(appBuildCfg.getBuildConfiguration(), true);
			for (IEnvironmentVariable e : vars) {
				envMap.put(e.getName(), e.getValue());
			}

			/* Append Zephyr build environment */
			appBuildCfg.setBuildEnvironment(envMap);

			/* Turn into "name=val" pairs */
			List<String> envp = new ArrayList<>();
			for (Map.Entry<String, String> entry : envMap.entrySet()) {
				envp.add(String.format("%s=%s", entry.getKey(),
						entry.getValue()));
			}

			return envMap;
		}

		private static String[] getBuildEnvironmentArray(
				ZephyrApplicationBuildConfiguration appBuildCfg) {
			Map<String, String> envMap = getBuildEnvironmentMap(appBuildCfg);

			/* Turn into "name=val" pairs */
			List<String> envp = new ArrayList<>();
			for (Map.Entry<String, String> entry : envMap.entrySet()) {
				envp.add(String.format("%s=%s", entry.getKey(),
						entry.getValue()));
			}

			return envp.toArray(new String[0]);
		}

		public static void doMakefile(IProject project,
				ZephyrApplicationBuildConfiguration appBuildCfg, ILaunch launch,
				String makeProgram, String mode) throws CoreException {
			try {
				String[] command = {
					makeProgram,
					mode
				};

				ProcessBuilder builder = new ProcessBuilder(command)
						.directory(appBuildCfg.getBuildDirectory().toFile());
				builder.environment()
						.putAll(getBuildEnvironmentMap(appBuildCfg));
				Process process = builder.start();
				IProcess iproc = DebugPlugin.newProcess(launch, process,
						ZephyrHelpers.getBoardName(project));
				launch.addProcess(iproc);
			} catch (IOException e) {
				throw new CoreException(ZephyrHelpers
						.errorStatus("Error running application.", e)); //$NON-NLS-1$
			}
		}

		public static void doNinja(IProject project,
				ZephyrApplicationBuildConfiguration appBuildCfg, ILaunch launch,
				String makeProgram, String mode) throws CoreException {
			/*
			 * TODO: Rework Ninja execution when using Java SE > 8.
			 *
			 * When Ninja gets SIGTERM, it won't spawn new processes but will
			 * simply wait for already spawned processes to finish. So if one of
			 * those spawned processes continues execution, Ninja won't
			 * terminate. This issue is problematic with QEMU (and possibly
			 * other emulators) as they will keep on running forever in
			 * background. This is because the Process class sends SIGTERM to
			 * Ninja (e.g. click on Terminate button in Eclipse's console) but
			 * Ninja simply decides to spin waiting. This also applies to
			 * flashing as the user will not be able to stop the flashing
			 * process.
			 *
			 * This gets more complicated as Eclipse 4.6.x (which this plugins
			 * is currently targeting) can only recognize Java SE 8 (the only
			 * one available below Java SE 11 at time of writing this).
			 * Searching online seems to indicate that Java SE 11 is only
			 * supported on Eclipse 4.9+. As Java SE 8 does not support getting
			 * children of a Process object, and thus unable to terminate Ninja
			 * spawned processes correctly.
			 *
			 * So in the meantime, dry run Ninja to extract the necessary
			 * commands and run those commands instead.
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
				builder.environment()
						.putAll(getBuildEnvironmentMap(appBuildCfg));
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
				 * or
				 * [1/1] <command> && <command>
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
				String cmdLine = null;
				if (line.startsWith("[0/1]")) { //$NON-NLS-1$
					cmdLine = line.substring("[0/1]".length() + 1).trim(); //$NON-NLS-1$
				} else if (line.startsWith("[1/1]")) { //$NON-NLS-1$
					cmdLine = line.substring("[1/1]".length() + 1).trim(); //$NON-NLS-1$
				} else { // $NON-NLS-1$
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
						getBuildEnvironmentArray(appBuildCfg),
						appBuildCfg.getBuildDirectory().toFile());
				IProcess iproc = DebugPlugin.newProcess(launch, process,
						ZephyrHelpers.getBoardName(project));
				launch.addProcess(iproc);
			} catch (IOException | InterruptedException e) {
				throw new CoreException(ZephyrHelpers
						.errorStatus("Error running application.", e)); //$NON-NLS-1$
			}
		}

		public static void doCustomCommand(IProject project,
				ZephyrApplicationBuildConfiguration appBuildCfg, ILaunch launch,
				ILaunchConfiguration configuration, String attrCustomCmd)
				throws CoreException {
			try {
				String cmdLine = configuration.getAttribute(attrCustomCmd,
						ZephyrStrings.EMPTY_STRING);

				if (cmdLine.trim().isEmpty()) {
					/* Nothing to run */
					return;
				}

				Process process = Runtime.getRuntime().exec(cmdLine,
						getBuildEnvironmentArray(appBuildCfg),
						appBuildCfg.getBuildDirectory().toFile());
				IProcess iproc = DebugPlugin.newProcess(launch, process,
						ZephyrHelpers.getBoardName(project));
				launch.addProcess(iproc);
			} catch (IOException e) {
				throw new CoreException(ZephyrHelpers
						.errorStatus("Error running application.", e)); //$NON-NLS-1$
			}
		}

	}

	/**
	 * Check if {@code path} is a valid directory.
	 *
	 * @param path Path to be verified.
	 * @return True if a valid directory, false otherwise.
	 * @throws SecurityException
	 */
	public static boolean checkValidDirectory(String path)
			throws SecurityException {
		if (path == null) {
			return false;
		}

		File f = new File(path);

		if (f.isDirectory()) {
			return true;
		}

		return false;
	}

	/**
	 * Check if {@code path} contains a valid Zephyr Base directory.
	 *
	 * @param path Path to Zephyr Base.
	 * @return True if a valid Zephyr Base, false otherwise.
	 * @throws SecurityException
	 */
	public static boolean checkValidZephyrBase(String path)
			throws SecurityException {
		if (path == null) {
			return false;
		}

		File versionFile = new File(path, "VERSION"); //$NON-NLS-1$

		if (versionFile.isFile()) {
			return true;
		}

		return false;
	}

	/**
	 * Check if {@code path} is a valid Zephyr SDK installation directory.
	 *
	 * @param path Path for the Zephyr SDK installation directory.
	 * @return True if a valid Zephyr SDK installation directory, false
	 *         otherwise.
	 * @throws SecurityException
	 */
	public static boolean checkValidZephyrSdkInstallDir(String path)
			throws SecurityException {
		if (path == null) {
			return false;
		}

		File f = new File(path, "sdk_version"); //$NON-NLS-1$

		if (f.isFile()) {
			return true;
		}

		return false;
	}

	/**
	 * Check if {@code path} is a valid Crosstool-NG toolchain directory.
	 *
	 * @param path Path for the Crosstool-NG toolchain directory.
	 * @return True if a valid Crosstool-NG toolchain directory, false
	 *         otherwise.
	 * @throws SecurityException
	 */
	public static boolean checkValidXToolsDirectory(String path)
			throws SecurityException {
		if (path == null) {
			return false;
		}

		/* Make sure it is a directory first */
		if (!checkValidDirectory(path)) {
			return false;
		}

		/* Check for toolchain names, e.g. i586-zephyr-elf */
		File[] dirs = (new File(path)).listFiles();
		for (File d : dirs) {
			if (!d.isDirectory()) {
				continue;
			}

			/* Since it's "arch-os-abi", we just check the "os" part */
			String name = d.getName();
			if (name.contains("-zephyr-")) { //$NON-NLS-1$
				return true;
			}
		}

		return false;
	}

	/**
	 * Create an error {@code IStatus} message.
	 *
	 * @param message
	 * @param cause
	 * @return
	 */
	public static IStatus errorStatus(String message, Throwable cause) {
		return new Status(IStatus.ERROR, ZephyrPlugin.getId(), message, cause);
	}

	/**
	 * Setup environment variables for build command execution.
	 *
	 * @param project The Project.
	 * @param env The environment to be manipulated.
	 */
	public static void setupBuildCommandEnvironment(IProject project,
			Map<String, String> env) {
		ScopedPreferenceStore pStore =
				ZephyrHelpers.getProjectPreferenceStore(project);
		setupBuildCommandEnvironment(pStore, env);
	}

	/**
	 * Setup environment variables for build command execution.
	 *
	 * @param pStore Project preference store.
	 * @param env The environment to be manipulated.
	 */
	public static void setupBuildCommandEnvironment(
			ScopedPreferenceStore pStore, Map<String, String> env) {

		/* Set ZEPHYR_BASE */
		String zephyrBase =
				pStore.getString(ZephyrConstants.ZEPHYR_BASE_LOCATION);

		env.put(ZephyrConstants.ZEPHYR_BASE, zephyrBase);

		/* Set toolchain environment variables */
		String variant = pStore.getString(ZEPHYR_TOOLCHAIN_VARIANT);
		env.put(ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT, variant);

		String tcVarName;
		if (variant.equals(ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR)) {
			tcVarName = ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR_ENV;
		} else if (variant.equals(ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS)) {
			tcVarName = ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS_ENV;
		} else if (variant.equals(ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB)) {
			tcVarName = ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB_ENV;
		} else if (variant.equals(ZEPHYR_TOOLCHAIN_VARIANT_ISSM)) {
			tcVarName = ZEPHYR_TOOLCHAIN_VARIANT_ISSM_ENV;
		} else if (variant.equals(ZEPHYR_TOOLCHAIN_VARIANT_CROSS_COMPILE)) {
			tcVarName = ZEPHYR_TOOLCHAIN_VARIANT_CROSS_COMPILE_ENV;
		} else {
			tcVarName = ZEPHYR_TOOLCHAIN_VARIANT_CUSTOM_ENV;
		}

		String tcVarValue = pStore.getString(tcVarName);
		env.put(tcVarName, tcVarValue);
	}

	public static ScopedPreferenceStore getProjectPreferenceStore(
			IProject project) {
		return new ScopedPreferenceStore(new ProjectScope(project),
				ZephyrPlugin.PLUGIN_ID);
	}

	public static String getDefaultCMakeGenerator() {
		return ZephyrConstants.CMAKE_GENERATOR_NINJA;
	}

	public static String getCMakeGenerator(ScopedPreferenceStore pStore) {
		return pStore.getString(ZephyrConstants.CMAKE_GENERATOR);
	}

	public static String getCMakeGenerator(IProject project) {
		return getCMakeGenerator(getProjectPreferenceStore(project));
	}

	public static String getBoardName(ScopedPreferenceStore pStore) {
		return pStore.getString(ZephyrConstants.ZEPHYR_BOARD);
	}

	public static String getBoardName(IProject project) {
		return getBoardName(getProjectPreferenceStore(project));
	}

}
