/*
 * Copyright (c) 2016 QNX Software Systems and others.
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.build;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.build.ICBuildConfiguration;
import org.eclipse.cdt.core.build.ICBuildConfigurationManager;
import org.eclipse.cdt.core.build.ICBuildConfigurationProvider;
import org.eclipse.cdt.core.build.IToolChain;
import org.eclipse.cdt.core.build.IToolChainManager;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.runtime.CoreException;
import org.zephyrproject.ide.eclipse.core.ZephyrPlugin;
import org.zephyrproject.ide.eclipse.core.build.makefiles.ZephyrApplicationMakefilesBuildConfiguration;
import org.zephyrproject.ide.eclipse.core.build.makefiles.ZephyrApplicationMakefilesToolChain;
import org.zephyrproject.ide.eclipse.core.build.ninja.ZephyrApplicationNinjaBuildConfiguration;
import org.zephyrproject.ide.eclipse.core.build.ninja.ZephyrApplicationNinjaToolChain;

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
			Map<String, String> properties = new HashMap<>();
			properties.put(IToolChain.ATTR_OS,
					ZephyrApplicationToolChain.TOOLCHAIN_OS);

			if (config.getName().equals(
					ZephyrApplicationMakefilesBuildConfiguration.CONFIG_NAME)) {
				properties.put(IToolChain.ATTR_PACKAGE,
						ZephyrApplicationMakefilesToolChain.TOOLCHAIN_PKG);
			} else if (config.getName().equals(
					ZephyrApplicationNinjaBuildConfiguration.CONFIG_NAME)) {
				properties.put(IToolChain.ATTR_PACKAGE,
						ZephyrApplicationNinjaToolChain.TOOLCHAIN_PKG);
			} else {
				return null;
			}

			for (IToolChain toolChain : toolChainManager
					.getToolChainsMatching(properties)) {
				ICBuildConfiguration cBuildCfg = null;

				if (config.getName().equals(
						ZephyrApplicationMakefilesBuildConfiguration.CONFIG_NAME)) {
					cBuildCfg =
							new ZephyrApplicationMakefilesBuildConfiguration(
									config, config.getName(), toolChain);
				} else if (config.getName().equals(
						ZephyrApplicationNinjaBuildConfiguration.CONFIG_NAME)) {
					cBuildCfg = new ZephyrApplicationNinjaBuildConfiguration(
							config, config.getName(), toolChain);
				} else {
					return null;
				}

				ICBuildConfigurationManager configManager = CCorePlugin
						.getService(ICBuildConfigurationManager.class);
				configManager.addBuildConfiguration(config, cBuildCfg);

				return cBuildCfg;
			}

			return null;
		} catch (

		CoreException e) {
			// Failed to create the build config. Return null so it gets
			// recreated.
			e.printStackTrace();
			return null;
		}
	}

}
