/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.build;

import org.eclipse.cdt.core.build.IToolChainProvider;
import org.zephyrproject.ide.eclipse.core.ZephyrPlugin;

/**
 * Toolchain provider for Zephyr Application.
 *
 * This creates IToolChain objects necessary for building Zephyr Applications.
 */
public class ZephyrApplicationToolChainProvider implements IToolChainProvider {

	public static final String ID = ZephyrPlugin.PLUGIN_ID
			+ ".build.zephyrApplicationToolChainProvider"; //$NON-NLS-1$

	@Override
	public String getId() {
		return ID;
	}

}
