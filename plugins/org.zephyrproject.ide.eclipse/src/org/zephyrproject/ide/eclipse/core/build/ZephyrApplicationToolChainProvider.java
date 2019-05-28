/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.build;

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
			+ ".core.build.zephyrApplicationToolChainProvider"; //$NON-NLS-1$

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void init(IToolChainManager manager) throws CoreException {
		manager.addToolChain(new ZephyrApplicationMakefilesToolChain(this));
		manager.addToolChain(new ZephyrApplicationNinjaToolChain(this));
	}

}
