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
import org.zephyrproject.ide.eclipse.core.build.ZephyrApplicationBuildConfiguration;
import org.zephyrproject.ide.eclipse.core.build.toolchain.ZephyrGCCToolChain;
import org.zephyrproject.ide.eclipse.core.build.toolchain.ZephyrGenericToolChain;
import org.zephyrproject.ide.eclipse.core.build.toolchain.ZephyrToolChainConstants.CrossCompileToolChain;
import org.zephyrproject.ide.eclipse.core.build.toolchain.ZephyrToolChainConstants.CrosstoolsToolChain;
import org.zephyrproject.ide.eclipse.core.build.toolchain.ZephyrToolChainConstants.GnuArmEmbToolChain;
import org.zephyrproject.ide.eclipse.core.build.toolchain.ZephyrToolChainConstants.ZephyrSdkToolChain;
import org.zephyrproject.ide.eclipse.core.internal.ZephyrHelpers;

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
	public synchronized ICBuildConfiguration getCBuildConfiguration(
			IBuildConfiguration config, String name) throws CoreException {

		/*
		 * Returns a default C build configuration if asked to provide one.
		 * If this happens, the project configuration is probably broken
		 * so we need a minimally working build configuration. This allows
		 * the project to be built without erroring out all the time.
		 */
		if (config.getName().equals(IBuildConfiguration.DEFAULT_CONFIG_NAME)) {
			ICBuildConfiguration cBuildCfg = null;
			IToolChain toolChain = toolChainManager.getToolChain(
					ZephyrGenericToolChain.TYPE_ID,
					ZephyrGenericToolChain.TOOLCHAIN_ID);

			if (toolChain == null) {
				return null;
			}

			/* Create new build configuration */
			if (config.getName().startsWith(
					ZephyrApplicationBuildConfiguration.CONFIG_NAME)) {
				cBuildCfg = new ZephyrApplicationBuildConfiguration(config,
						name, toolChain);
			} else {
				return null;
			}

			return cBuildCfg;
		}

		try {
			ICBuildConfiguration cBuildCfg = null;

			/* Figure out the toolchain */
			IToolChain toolChain = this.getToolChain(config);

			if (toolChain == null) {
				return null;
			}

			ZephyrGCCToolChain zToolChain = (ZephyrGCCToolChain) toolChain;

			zToolChain.initCMakeVarsFromProjectPerfStore(config.getProject());

			/* Create new build configuration */
			if (config.getName().startsWith(
					ZephyrApplicationBuildConfiguration.CONFIG_NAME)) {
				cBuildCfg = new ZephyrApplicationBuildConfiguration(config,
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
		String tcVariant =
				ZephyrHelpers.getToolChainVariant(config.getProject());

		StringBuilder tcId = new StringBuilder();
		if (tcVariant == null) {
			tcId.append(ZephyrGenericToolChain.TOOLCHAIN_ID);
		} else if (tcVariant.equals(ZephyrSdkToolChain.VARIANT)) {
			tcId.append(ZephyrGCCToolChain.TOOLCHAIN_ID);
		} else if (tcVariant.equals(CrosstoolsToolChain.VARIANT)) {
			tcId.append(ZephyrGCCToolChain.TOOLCHAIN_ID);
		} else if (tcVariant.equals(GnuArmEmbToolChain.VARIANT)) {
			tcId.append(ZephyrGCCToolChain.TOOLCHAIN_ID);
		} else if (tcVariant.equals(CrossCompileToolChain.VARIANT)) {
			tcId.append(ZephyrGenericToolChain.TOOLCHAIN_ID);
		} else {
			tcId.append(ZephyrGenericToolChain.TOOLCHAIN_ID);
		}

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
