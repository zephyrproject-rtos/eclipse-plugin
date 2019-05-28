package org.zephyrproject.ide.eclipse.core.build.makefiles;

import org.eclipse.cdt.core.build.IToolChainProvider;
import org.zephyrproject.ide.eclipse.core.build.ZephyrApplicationToolChain;

public class ZephyrApplicationMakefilesToolChain
		extends ZephyrApplicationToolChain {

	public static final String TOOLCHAIN_ID = "zephyr.toolchain.makefiles"; //$NON-NLS-1$

	public static final String TOOLCHAIN_PKG = "makefiles"; //$NON-NLS-1$

	public ZephyrApplicationMakefilesToolChain(IToolChainProvider provider) {
		super(provider, TOOLCHAIN_ID);
		super.setProperty(ATTR_PACKAGE, TOOLCHAIN_PKG);
	}

}
