/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.build;

public final class CMakeConstants {

	public static final String CMAKE_GENERATOR = "CMAKE_GENERATOR"; //$NON-NLS-1$
	public static final String CMAKE_GENERATOR_DESC = "CMake Generator"; //$NON-NLS-1$
	public static final String CMAKE_GENERATOR_MAKEFILE = "Unix Makefiles"; //$NON-NLS-1$
	public static final String CMAKE_GENERATOR_NINJA = "Ninja"; //$NON-NLS-1$

	public static final String[] CMAKE_GENERATOR_LIST = {
		CMAKE_GENERATOR_NINJA,
		CMAKE_GENERATOR_MAKEFILE
	};

}
