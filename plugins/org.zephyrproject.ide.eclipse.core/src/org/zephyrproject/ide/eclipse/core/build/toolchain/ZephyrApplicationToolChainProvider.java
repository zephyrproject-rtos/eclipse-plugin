/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.build.toolchain;

import org.eclipse.cdt.core.build.IToolChain;
import org.eclipse.cdt.core.build.IToolChainManager;
import org.eclipse.cdt.core.build.IToolChainProvider;
import org.eclipse.core.runtime.CoreException;
import org.zephyrproject.ide.eclipse.core.ZephyrPlugin;

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
	public void init(IToolChainManager manager) throws CoreException {
		IToolChain tc = new ZephyrGenericToolChain();
		toolChainManager.addToolChain(tc);
	}

}
