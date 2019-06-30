/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.preferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.zephyrproject.ide.eclipse.core.internal.ZephyrHelpers;

public final class ZephyrProjectPreferences {

	public static final String BUILD_DIR = "BUILD_DIR"; //$NON-NLS-1$

	public static final String BUILD_DIR_DEFAULT = "build"; //$NON-NLS-1$

	public static final String SOURCE_DIR = "SOURCE_DIR"; //$NON-NLS-1$

	public static final String SOURCE_DIR_DEFAULT = "src"; //$NON-NLS-1$

	/**
	 * Get the project relative path of build directory.
	 *
	 * @param pStore Project ScopedPreferenceStore
	 * @return Build directory
	 */
	public static String getBuildDirectory(ScopedPreferenceStore pStore) {
		String buildDir = ZephyrHelpers.getProjectPreference(pStore,
				ZephyrProjectPreferences.BUILD_DIR);

		if (buildDir == null) {
			buildDir = ZephyrProjectPreferences.BUILD_DIR_DEFAULT;
		}

		return buildDir;
	}

	/**
	 * Get the project relative path of build directory.
	 *
	 * @param project Project
	 * @return Build directory
	 */
	public static String getBuildDirectory(IProject project) {
		return getBuildDirectory(
				ZephyrHelpers.getProjectPreferenceStore(project));
	}

	/**
	 * Get the project relative path of source directory.
	 *
	 * @param pStore Project ScopedPreferenceStore
	 * @return Source directory
	 */
	public static String getSourceDirectory(ScopedPreferenceStore pStore) {
		String buildDir = ZephyrHelpers.getProjectPreference(pStore,
				ZephyrProjectPreferences.BUILD_DIR);

		if (buildDir == null) {
			buildDir = ZephyrProjectPreferences.BUILD_DIR_DEFAULT;
		}

		return buildDir;
	}

	/**
	 * Get the project relative path of source directory.
	 *
	 * @param project Project
	 * @return Source directory
	 */
	public static String getSourceDirectory(IProject project) {
		return getBuildDirectory(
				ZephyrHelpers.getProjectPreferenceStore(project));
	}

}
