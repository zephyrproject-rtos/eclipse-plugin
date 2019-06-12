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

import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.build.ICBuildConfiguration;
import org.eclipse.cdt.core.build.ICBuildConfigurationManager;
import org.eclipse.cdt.core.build.ICBuildConfigurationProvider;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.zephyrproject.ide.eclipse.core.ZephyrConstants;
import org.zephyrproject.ide.eclipse.core.ZephyrPlugin;
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
		ScopedPreferenceStore pStore = new ScopedPreferenceStore(
				new ProjectScope(project), ZephyrPlugin.PLUGIN_ID);
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

	public static String getDefaultCMakeGenerator() {
		return ZephyrConstants.CMAKE_GENERATOR_NINJA;
	}

	public static String getCMakeGenerator(ScopedPreferenceStore pStore) {
		return pStore.getString(ZephyrConstants.CMAKE_GENERATOR);
	}

	public static String getCMakeGenerator(IProject project) {
		ScopedPreferenceStore pStore = new ScopedPreferenceStore(
				new ProjectScope(project), ZephyrPlugin.PLUGIN_ID);
		return getCMakeGenerator(pStore);
	}

	public static String getBoardName(ScopedPreferenceStore pStore) {
		return pStore.getString(ZephyrConstants.ZEPHYR_BOARD);
	}

	public static String getBoardName(IProject project) {
		ScopedPreferenceStore pStore = new ScopedPreferenceStore(
				new ProjectScope(project), ZephyrPlugin.PLUGIN_ID);
		return getBoardName(pStore);
	}

}
