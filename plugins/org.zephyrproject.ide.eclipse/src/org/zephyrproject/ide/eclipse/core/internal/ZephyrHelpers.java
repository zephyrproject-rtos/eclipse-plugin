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
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.zephyrproject.ide.eclipse.core.ZephyrConstants;
import org.zephyrproject.ide.eclipse.core.ZephyrPlugin;

/**
 * Helper class
 *
 * Contains helper functions.
 */
public final class ZephyrHelpers {

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

}
