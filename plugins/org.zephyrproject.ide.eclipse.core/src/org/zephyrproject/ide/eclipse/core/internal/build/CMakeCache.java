/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.internal.build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;

/**
 * Store Variables in CMakeCache.txt
 */
public class CMakeCache {

	public static final String CMAKE_CXX_COMPILER = "CMAKE_CXX_COMPILER"; //$NON-NLS-1$

	public static final String CMAKE_C_COMPILER = "CMAKE_C_COMPILER"; //$NON-NLS-1$

	public static final String CMAKE_MAKE_PROGRAM = "CMAKE_MAKE_PROGRAM"; //$NON-NLS-1$

	public static final String CMAKE_GDB = "CMAKE_GDB"; //$NON-NLS-1$

	public static final String WEST = "WEST"; //$NON-NLS-1$

	public static final String ZEPHYR_BOARD_DEBUG_RUNNER =
			"ZEPHYR_BOARD_DEBUG_RUNNER"; //$NON-NLS-1$

	private IProject project;

	private Map<String, String> variables;

	public CMakeCache(IProject project) throws IOException {
		this.project = project;
		this.variables = new HashMap<>();
	}

	/**
	 * @return The project associated with this cache
	 */
	public IProject getProject() {
		return project;
	}

	/**
	 * @param name Name of CMake cached variable
	 * @return The raw variable value or {@code null} if variable does not exist
	 */
	public String getRawValue(String name) {
		return variables.get(name);
	}

	private String getTypedValue(String type, String name) {
		String str = variables.get(name);
		if (str == null) {
			return null;
		} else if (!str.startsWith(type)) {
			return null;
		} else {
			return str.substring(type.length());
		}
	}

	/**
	 * @param name Name of cached variable with type STRING
	 * @return The STRING value or {@code null} if variable does not exist or
	 *         not of type STRING
	 */
	public String getString(String name) {
		return getTypedValue("STRING=", name); //$NON-NLS-1$
	}

	/**
	 * @param name Name of cached variable with type FILEPATH
	 * @return The FILEPATH value or {@code null} if variable does not exist or
	 *         not of type FILEPATH
	 */
	public String getFilePath(String name) {
		return getTypedValue("FILEPATH=", name); //$NON-NLS-1$
	}

	/**
	 * @return Path to C compiler as discovered by CMake
	 */
	public String getCCompiler() {
		return getFilePath(CMAKE_C_COMPILER);
	}

	/**
	 * @return Path to C++ compiler as discovered by CMake
	 */
	public String getCXXCompiler() {
		return getFilePath(CMAKE_CXX_COMPILER);
	}

	/**
	 * @return Path to Make program as discovered by CMake
	 */
	public String getMakeProgram() {
		return getFilePath(CMAKE_MAKE_PROGRAM);
	}

	/**
	 * @return Path to GDB as discovered by CMake
	 */
	public String getGdb() {
		return getFilePath(CMAKE_GDB);
	}

	/**
	 * @return Path to West as discovered by CMake
	 */
	public String getWest() {
		return getFilePath(WEST);
	}

	/**
	 * @return Debug Runner
	 */
	public String getDebugRunner() {
		return getString(ZEPHYR_BOARD_DEBUG_RUNNER);
	}

	/**
	 * @param f File to CMakeCache.txt
	 * @return true if there is any variables read, false otherwise
	 * @throws IOException
	 */
	public boolean parseFile(File f) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(f));

		/* Clear the mappings before reading new ones */
		variables.clear();

		String line;
		while ((line = reader.readLine()) != null) {
			String l = line.trim();
			if (l.startsWith("#") || l.startsWith("/") || l.isEmpty()) { //$NON-NLS-1$ //$NON-NLS-2$
				continue;
			}

			String[] pair = l.split(":", 2);
			if (pair.length == 2) {
				variables.put(pair[0], pair[1]);
			}
		}

		reader.close();

		return !variables.isEmpty();
	}
}
