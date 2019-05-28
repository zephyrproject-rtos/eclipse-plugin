package org.zephyrproject.ide.eclipse.core.build.ninja;

import org.eclipse.cdt.core.build.IToolChainProvider;
import org.zephyrproject.ide.eclipse.core.build.ZephyrApplicationToolChain;

public class ZephyrApplicationNinjaToolChain
		extends ZephyrApplicationToolChain {

	public static final String TOOLCHAIN_ID = "zephyr.toolchain.ninja"; //$NON-NLS-1$

	public static final String TOOLCHAIN_PKG = "ninja"; //$NON-NLS-1$

	public ZephyrApplicationNinjaToolChain(IToolChainProvider provider) {
		super(provider, TOOLCHAIN_ID);
		super.setProperty(ATTR_PACKAGE, TOOLCHAIN_PKG);
	}

}
