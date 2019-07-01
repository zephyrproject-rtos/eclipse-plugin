/*
 *
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.build.makefiles;

import org.eclipse.cdt.core.build.IToolChain;
import org.eclipse.core.resources.IBuildConfiguration;
import org.zephyrproject.ide.eclipse.core.build.ZephyrApplicationBuildConfiguration;
import org.zephyrproject.ide.eclipse.core.build.ZephyrApplicationBuildConfigurationProvider;

public class ZephyrApplicationMakefilesBuildConfiguration
		extends ZephyrApplicationBuildConfiguration {

	public static final String CONFIG_NAME =
			ZephyrApplicationBuildConfigurationProvider.ID
					+ "/zephyr.app.build.config.makefiles"; //$NON-NLS-1$

	public ZephyrApplicationMakefilesBuildConfiguration(
			IBuildConfiguration config, String name, IToolChain toolChain) {
		super(config, name, toolChain);
	}

}
