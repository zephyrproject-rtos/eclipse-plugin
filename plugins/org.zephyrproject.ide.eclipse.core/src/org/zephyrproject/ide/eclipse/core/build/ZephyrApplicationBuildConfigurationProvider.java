/*
 * Copyright (c) 2016 QNX Software Systems and others.
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.build;

import org.eclipse.cdt.core.build.ICBuildConfiguration;
import org.eclipse.cdt.core.build.ICBuildConfigurationProvider;
import org.eclipse.cdt.core.build.IToolChain;
import org.eclipse.cdt.core.build.IToolChainManager;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.runtime.CoreException;
import org.zephyrproject.ide.eclipse.core.ZephyrPlugin;
import org.zephyrproject.ide.eclipse.core.ZephyrStrings;
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
			+ ".build.zephyrApplicationBuildConfigProvider"; //$NON-NLS-1$

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
		if (config.getName().equals(IBuildConfiguration.DEFAULT_CONFIG_NAME)) {
			/* Do not support default build configuration */
			return null;
		}

		try {
			IToolChain toolChain = toolChainManager
					.getProvider(ZephyrApplicationToolChainProvider.ID)
					.getToolChain(getBuildConfigToolChainId(config),
							ZephyrStrings.EMPTY_STRING);

			ICBuildConfiguration cBuildCfg = null;
			ZephyrApplicationToolChain zToolChain =
					(ZephyrApplicationToolChain) toolChain;

			zToolChain.initCMakeVarsFromProjectPerfStore(config.getProject());

			if (config.getName().startsWith(
					ZephyrApplicationMakefilesBuildConfiguration.CONFIG_NAME)) {
				cBuildCfg = new ZephyrApplicationMakefilesBuildConfiguration(
						config, config.getName(), toolChain);
			} else if (config.getName().startsWith(
					ZephyrApplicationNinjaBuildConfiguration.CONFIG_NAME)) {
				cBuildCfg = new ZephyrApplicationNinjaBuildConfiguration(config,
						config.getName(), toolChain);
			} else {
				return null;
			}

			return cBuildCfg;
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Get a toolchain ID from a build configuration name.
	 *
	 * @param config
	 * @return
	 */
	private static String getBuildConfigToolChainId(
			IBuildConfiguration config) {
		String configName = config.getName();
		StringBuilder tcId = new StringBuilder();

		if (configName.startsWith(
				ZephyrApplicationMakefilesBuildConfiguration.CONFIG_NAME)) {
			tcId.append(ZephyrApplicationMakefilesToolChain.TOOLCHAIN_ID);
		} else if (configName.startsWith(
				ZephyrApplicationNinjaBuildConfiguration.CONFIG_NAME)) {
			tcId.append(ZephyrApplicationNinjaToolChain.TOOLCHAIN_ID);
		}

		String[] elem = config.getName().split("#", 2); //$NON-NLS-1$
		if (elem.length == 2) {
			tcId.append("#"); //$NON-NLS-1$
			tcId.append(elem[1]);
		}

		return tcId.toString();
	}

}
