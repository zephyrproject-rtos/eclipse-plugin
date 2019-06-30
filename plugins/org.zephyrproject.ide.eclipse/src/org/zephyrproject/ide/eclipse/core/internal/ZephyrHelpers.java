/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.internal;

import java.io.File;
import java.io.IOException;
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
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.launchbar.core.target.ILaunchTarget;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.zephyrproject.ide.eclipse.core.ZephyrPlugin;
import org.zephyrproject.ide.eclipse.core.ZephyrStrings;
import org.zephyrproject.ide.eclipse.core.build.CMakeConstants;
import org.zephyrproject.ide.eclipse.core.build.ZephyrApplicationBuildConfiguration;
import org.zephyrproject.ide.eclipse.core.build.ZephyrApplicationBuildConfigurationProvider;
import org.zephyrproject.ide.eclipse.core.build.ZephyrToolChainConstants;
import org.zephyrproject.ide.eclipse.core.build.ZephyrToolChainConstants.CrossCompileToolChain;
import org.zephyrproject.ide.eclipse.core.build.ZephyrToolChainConstants.CrosstoolsToolChain;
import org.zephyrproject.ide.eclipse.core.build.ZephyrToolChainConstants.CustomToolChain;
import org.zephyrproject.ide.eclipse.core.build.ZephyrToolChainConstants.GnuArmEmbToolChain;
import org.zephyrproject.ide.eclipse.core.build.ZephyrToolChainConstants.IssmToolChain;
import org.zephyrproject.ide.eclipse.core.build.ZephyrToolChainConstants.ZephyrSdkToolChain;
import org.zephyrproject.ide.eclipse.core.internal.launch.unix.ZephyrUnixLaunchHelpers;
import org.zephyrproject.ide.eclipse.core.internal.launch.windows.ZephyrWindowsLaunchHelpers;
import org.zephyrproject.ide.eclipse.core.preferences.ZephyrProjectPreferences;
import org.zephyrproject.ide.eclipse.core.preferences.ZephyrProjectPreferences.ZephyrBase;

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

		public static Map<String, String> getBuildEnvironmentMap(
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

		public static String[] getBuildEnvironmentArray(
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

		public static Process doMakefile(IProject project,
				ZephyrApplicationBuildConfiguration appBuildCfg, ILaunch launch,
				String makeProgram, String mode) throws CoreException {
			try {
				if (Platform.getOS().equals(Platform.OS_LINUX)
						|| Platform.getOS().equals(Platform.OS_MACOSX)) {
					return ZephyrUnixLaunchHelpers.doMakefile(project,
							appBuildCfg, launch, makeProgram, mode);
				} else if (Platform.getOS().equals(Platform.OS_WIN32)) {
					return ZephyrWindowsLaunchHelpers.doMakefile(project,
							appBuildCfg, launch, makeProgram, mode);
				} else {
					return null;
				}
			} catch (IOException e) {
				throw new CoreException(ZephyrHelpers
						.errorStatus("Error running Makefile command.", e)); //$NON-NLS-1$
			}
		}

		public static Process doNinja(IProject project,
				ZephyrApplicationBuildConfiguration appBuildCfg, ILaunch launch,
				String makeProgram, String mode) throws CoreException {
			try {
				if (Platform.getOS().equals(Platform.OS_LINUX)
						|| Platform.getOS().equals(Platform.OS_MACOSX)) {
					return ZephyrUnixLaunchHelpers.doNinja(project, appBuildCfg,
							launch, makeProgram, mode);
				} else if (Platform.getOS().equals(Platform.OS_WIN32)) {
					return ZephyrWindowsLaunchHelpers.doNinja(project,
							appBuildCfg, launch, makeProgram, mode);
				} else {
					return null;
				}
			} catch (IOException e) {
				throw new CoreException(ZephyrHelpers
						.errorStatus("Error running Ninja command.", e)); //$NON-NLS-1$
			}
		}

		public static Process doCustomCommand(IProject project,
				ZephyrApplicationBuildConfiguration appBuildCfg, ILaunch launch,
				ILaunchConfiguration configuration, String attrCustomCmd)
				throws CoreException {
			try {
				if (Platform.getOS().equals(Platform.OS_LINUX)
						|| Platform.getOS().equals(Platform.OS_MACOSX)) {
					return ZephyrUnixLaunchHelpers.doCustomCommand(project,
							appBuildCfg, launch, configuration, attrCustomCmd);
				} else if (Platform.getOS().equals(Platform.OS_WIN32)) {
					return ZephyrWindowsLaunchHelpers.doCustomCommand(project,
							appBuildCfg, launch, configuration, attrCustomCmd);
				} else {
					return null;
				}
			} catch (IOException e) {
				throw new CoreException(ZephyrHelpers
						.errorStatus("Error running custom command.", e)); //$NON-NLS-1$
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
				pStore.getString(ZephyrBase.ZEPHYR_BASE_LOCATION);

		env.put(ZephyrBase.ZEPHYR_BASE, zephyrBase);

		/* Set toolchain environment variables */
		String variant = pStore
				.getString(ZephyrToolChainConstants.ZEPHYR_TOOLCHAIN_VARIANT);
		env.put(ZephyrToolChainConstants.ZEPHYR_TOOLCHAIN_VARIANT, variant);

		String tcVarName;
		if (variant.equals(ZephyrSdkToolChain.VARIANT)) {
			tcVarName = ZephyrSdkToolChain.ENV;
		} else if (variant.equals(CrosstoolsToolChain.VARIANT)) {
			tcVarName = CrosstoolsToolChain.ENV;
		} else if (variant.equals(GnuArmEmbToolChain.VARIANT)) {
			tcVarName = GnuArmEmbToolChain.ENV;
		} else if (variant.equals(IssmToolChain.VARIANT)) {
			tcVarName = IssmToolChain.ENV;
		} else if (variant.equals(CrossCompileToolChain.VARIANT)) {
			tcVarName = CrossCompileToolChain.ENV;
		} else {
			tcVarName = CustomToolChain.ENV;
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
		return CMakeConstants.CMAKE_GENERATOR_NINJA;
	}

	public static String getCMakeGenerator(ScopedPreferenceStore pStore) {
		return pStore.getString(CMakeConstants.CMAKE_GENERATOR);
	}

	public static String getCMakeGenerator(IProject project) {
		return getCMakeGenerator(getProjectPreferenceStore(project));
	}

	public static String getBoardName(ScopedPreferenceStore pStore) {
		return pStore.getString(ZephyrProjectPreferences.BOARD);
	}

	public static String getBoardName(IProject project) {
		return getBoardName(getProjectPreferenceStore(project));
	}

	/**
	 * Get the name-value pair of project preference.
	 *
	 * @param pStore Project ScopedPreferenceStore
	 * @param name Name of the preference
	 * @return Value of the named preference, or null if name-value pair does
	 *         not exist
	 */
	public static String getProjectPreference(ScopedPreferenceStore pStore,
			String name) {
		if (!pStore.contains(name)) {
			return null;
		}

		return pStore.getString(name);
	}

	/**
	 * Get the name-value pair of project preference.
	 *
	 * @param project Project
	 * @param name Name of the preference
	 * @return Value of the named preference, or null if name-value pair does
	 *         not exist
	 */
	public static String getProjectPreference(IProject project, String key) {
		return getProjectPreference(getProjectPreferenceStore(project), key);
	}
}
