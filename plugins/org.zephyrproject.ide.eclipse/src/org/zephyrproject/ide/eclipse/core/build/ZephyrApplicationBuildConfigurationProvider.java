/*
 * Copyright (c) 2016 QNX Software Systems and others.
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.build;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.build.ICBuildConfiguration;
import org.eclipse.cdt.core.build.ICBuildConfigurationProvider;
import org.eclipse.cdt.core.build.IToolChain;
import org.eclipse.cdt.core.build.IToolChainManager;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.runtime.CoreException;
import org.zephyrproject.ide.eclipse.core.ZephyrPlugin;

/**
 * Build configuration provider for Zephyr Application
 *
 * Originally from
 * org.eclipse.cdt.cmake.core.internal.CMakeBuildConfigurationProvider.
 */
public class ZephyrApplicationBuildConfigurationProvider
		implements ICBuildConfigurationProvider {

	public static final String ID = ZephyrPlugin.PLUGIN_ID
			+ ".core.build.zephyrApplicationBuildConfigProvider"; //$NON-NLS-1$

	private IToolChainManager toolChainManager =
			ZephyrPlugin.getService(IToolChainManager.class);

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.cdt.core.build.ICBuildConfigurationProvider#getId()
	 */
	@Override
	public String getId() {
		return ID;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.cdt.core.build.ICBuildConfigurationProvider#
	 * getCBuildConfiguration(org.eclipse.core.resources.IBuildConfiguration,
	 * java.lang.String)
	 */
	@Override
	public ICBuildConfiguration getCBuildConfiguration(
			IBuildConfiguration config, String name) throws CoreException {
		try {
			if (config.getName()
					.equals(IBuildConfiguration.DEFAULT_CONFIG_NAME)) {
				Map<String, String> properties = new HashMap<>();
				properties.put(IToolChain.ATTR_OS, "zephyr");
				for (IToolChain toolChain : toolChainManager
						.getToolChainsMatching(properties)) {
					return new ZephyrApplicationBuildConfiguration(config, name,
							toolChain);
				}
				return null;
			} else {
				return new ZephyrApplicationBuildConfiguration(config, name);
			}
		} catch (CoreException e) {
			// Failed to create the build config. Return null so it gets
			// recreated.
			e.printStackTrace();
			return null;
		}
	}

}
