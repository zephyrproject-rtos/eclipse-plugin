/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.build;

import java.util.Collection;

import org.eclipse.cdt.core.build.IToolChain;
import org.eclipse.cdt.core.build.IToolChainManager;
import org.eclipse.cdt.core.build.IToolChainProvider;
import org.eclipse.core.runtime.CoreException;
import org.zephyrproject.ide.eclipse.core.ZephyrPlugin;
import org.zephyrproject.ide.eclipse.core.build.makefiles.ZephyrApplicationMakefilesToolChain;
import org.zephyrproject.ide.eclipse.core.build.ninja.ZephyrApplicationNinjaToolChain;

/**
 * Toolchain provider for Zephyr Application.
 *
 * This creates IToolChain objects necessary for building Zephyr Applications.
 */
public class ZephyrApplicationToolChainProvider implements IToolChainProvider {

	public static final String ID = ZephyrPlugin.PLUGIN_ID
			+ ".build.zephyrApplicationToolChainProvider"; //$NON-NLS-1$

	private IToolChainManager toolChainManager =
			ZephyrPlugin.getService(IToolChainManager.class);

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public IToolChain getToolChain(String id, String version)
			throws CoreException {
		Collection<IToolChain> zToolChains = toolChainManager
				.getToolChains(ZephyrApplicationToolChainProvider.ID);

		if (zToolChains != null) {
			for (IToolChain tc : zToolChains) {
				if (id.equals(tc.getId())) {
					return tc;
				}
			}
		}

		IToolChain toolChain = null;
		if (id.startsWith(ZephyrApplicationMakefilesToolChain.TOOLCHAIN_ID)) {
			toolChain = new ZephyrApplicationMakefilesToolChain(this, id);
		} else if (id
				.startsWith(ZephyrApplicationNinjaToolChain.TOOLCHAIN_ID)) {
			toolChain = new ZephyrApplicationNinjaToolChain(this, id);
		}

		if (toolChain != null) {
			toolChainManager.addToolChain(toolChain);
		}

		return toolChain;
	}

}
