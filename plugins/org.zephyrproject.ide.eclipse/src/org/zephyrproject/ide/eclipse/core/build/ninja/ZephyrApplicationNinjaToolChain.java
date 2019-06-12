package org.zephyrproject.ide.eclipse.core.build.ninja;

import org.eclipse.cdt.core.build.IToolChainProvider;
import org.zephyrproject.ide.eclipse.core.build.ZephyrApplicationToolChain;

public class ZephyrApplicationNinjaToolChain
		extends ZephyrApplicationToolChain {

	public static final String TOOLCHAIN_ID = "zephyr.toolchain.ninja"; //$NON-NLS-1$

	public ZephyrApplicationNinjaToolChain(IToolChainProvider provider,
			String id) {
		super(provider, id);
	}

}
