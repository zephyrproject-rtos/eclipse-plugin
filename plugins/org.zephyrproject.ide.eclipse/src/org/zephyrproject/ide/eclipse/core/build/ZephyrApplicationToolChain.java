/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.zephyrproject.ide.eclipse.core.build;

import org.eclipse.cdt.build.gcc.core.GCCToolChain;
import org.eclipse.cdt.core.build.IToolChainProvider;
import org.eclipse.core.resources.IBuildConfiguration;

/**
 * Wrapper of GCCToolChain to build Zephyr Applications.
 */
public class ZephyrApplicationToolChain extends GCCToolChain {

	public static final String TOOLCHAIN_ID = "zephyr.default"; //$NON-NLS-1$
	public static final String TOOLCHAIN_OS = "zephyr"; //$NON-NLS-1$

	public ZephyrApplicationToolChain(IToolChainProvider provider) {
		super(provider, TOOLCHAIN_ID, "");
		super.setProperty(ATTR_OS, TOOLCHAIN_OS);
	}

	ZephyrApplicationToolChain(IToolChainProvider provider,
			IBuildConfiguration config) {
		super(provider, TOOLCHAIN_ID, "");
		super.setProperty(ATTR_OS, TOOLCHAIN_OS);
	}

	public ZephyrApplicationToolChain(IToolChainProvider provider, String id,
			String version) {
		super(provider, id, version);
		super.setProperty(ATTR_OS, TOOLCHAIN_OS);
	}

}
