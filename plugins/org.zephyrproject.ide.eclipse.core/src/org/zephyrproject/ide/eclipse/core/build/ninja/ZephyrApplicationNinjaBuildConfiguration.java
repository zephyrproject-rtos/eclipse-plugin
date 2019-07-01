/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.build.ninja;

import org.eclipse.cdt.core.build.IToolChain;
import org.eclipse.core.resources.IBuildConfiguration;
import org.zephyrproject.ide.eclipse.core.build.ZephyrApplicationBuildConfiguration;
import org.zephyrproject.ide.eclipse.core.build.ZephyrApplicationBuildConfigurationProvider;

public class ZephyrApplicationNinjaBuildConfiguration
		extends ZephyrApplicationBuildConfiguration {

	public static final String CONFIG_NAME =
			ZephyrApplicationBuildConfigurationProvider.ID
					+ "/zephyr.app.build.config.ninja"; //$NON-NLS-1$

	public ZephyrApplicationNinjaBuildConfiguration(IBuildConfiguration config,
			String name, IToolChain toolChain) {
		super(config, name, toolChain);
	}
}
