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
import org.zephyrproject.ide.eclipse.core.build.makefiles.ZephyrApplicationMakefilesBuildConfiguration;
import org.zephyrproject.ide.eclipse.core.build.ninja.ZephyrApplicationNinjaBuildConfiguration;
import org.zephyrproject.ide.eclipse.core.build.toolchain.ZephyrGCCToolChain;

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
			ICBuildConfiguration cBuildCfg = null;

			/* Figure out the toolchain */
			IToolChain toolChain = this.getToolChain(config);

			if (toolChain == null) {
				return null;
			}

			ZephyrGCCToolChain zToolChain =
					(ZephyrGCCToolChain) toolChain;

			zToolChain.initCMakeVarsFromProjectPerfStore(config.getProject());

			/* Create new build configuration */
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
	 * @throws CoreException
	 */
	private IToolChain getToolChain(IBuildConfiguration config)
			throws CoreException {
		String configName = config.getName();
		StringBuilder tcId =
				new StringBuilder(ZephyrGCCToolChain.TOOLCHAIN_ID);

		String[] elem = configName.split("#", 2); //$NON-NLS-1$
		if (elem.length == 2) {
			tcId.append("#"); //$NON-NLS-1$
			tcId.append(elem[1]);
		}

		String tcIdStr = tcId.toString();

		IToolChain toolChain = toolChainManager
				.getToolChain(ZephyrGCCToolChain.TYPE_ID, tcIdStr);

		if (toolChain != null) {
			return toolChain;
		}

		toolChain = new ZephyrGCCToolChain(tcIdStr);
		toolChainManager.addToolChain(toolChain);

		return toolChain;
	}

}
