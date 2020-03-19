/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.build.toolchain;

import org.zephyrproject.ide.eclipse.core.ZephyrStrings;

/**
 * Toolchain related constants.
 */
public final class ZephyrToolChainConstants {

	public static final String ZEPHYR_TOOLCHAIN_VARIANT =
			"ZEPHYR_TOOLCHAIN_VARIANT"; //$NON-NLS-1$

	public static class ZephyrSdkToolChain {

		public static final String VARIANT = "zephyr"; //$NON-NLS-1$

		public static final String DESCRIPTION = "Zephyr SDK";

		public static final String DIRECTORY_DESCRIPTION =
				String.join(ZephyrStrings.ONE_EMPTY_SPACE, DESCRIPTION, // $NON-NLS-1$
						"Install", ZephyrStrings.PATH); //$NON-NLS-1$

		public static final String ENV = "ZEPHYR_SDK_INSTALL_DIR"; //$NON-NLS-1$

	}

	public static class CrosstoolsToolChain {

		public static final String VARIANT = "xtools"; //$NON-NLS-1$

		public static final String DESCRIPTION = "Crosstool-NG";

		public static final String DIRECTORY_DESCRIPTION =
				String.join(ZephyrStrings.ONE_EMPTY_SPACE, DESCRIPTION,
						ZephyrStrings.TOOLCHAIN, ZephyrStrings.PATH);

		public static final String ENV = "XTOOLS_TOOLCHAIN_PATH"; //$NON-NLS-1$

	}

	public static class GnuArmEmbToolChain {

		public static final String VARIANT = "gnuarmemb"; //$NON-NLS-1$

		public static final String DESCRIPTION = "GNU ARM Embedded Toolchain";

		public static final String DIRECTORY_DESCRIPTION = String.join(
				ZephyrStrings.ONE_EMPTY_SPACE, DESCRIPTION, ZephyrStrings.PATH);

		public static final String ENV = "GNUARMEMB_TOOLCHAIN_PATH"; //$NON-NLS-1$

	}

	public static class CrossCompileToolChain {

		public static final String VARIANT = "cross-compiler"; //$NON-NLS-1$

		public static final String DESCRIPTION = "Cross Compile";

		public static final String PREFIX_DESCRIPTION = String
				.join(ZephyrStrings.ONE_EMPTY_SPACE, DESCRIPTION, "Prefix");

		public static final String ENV = "CROSS_COMPILE"; //$NON-NLS-1$

	}

	public static class CustomToolChain {

		public static final String VARIANT = "Custom CMake Toolchain"; //$NON-NLS-1$

		public static final String DESCRIPTION = "Custom CMake Toolchain";

		public static final String DIRECTORY_DESCRIPTION = String.join(
				ZephyrStrings.ONE_EMPTY_SPACE, DESCRIPTION, ZephyrStrings.PATH);

		public static final String ENV = "TOOLCHAIN_ROOT"; //$NON-NLS-1$

	}

	public static final String[] ZEPHYR_TOOLCHAIN_DESC_LIST = {
		ZephyrSdkToolChain.DESCRIPTION,
		CrosstoolsToolChain.DESCRIPTION,
		GnuArmEmbToolChain.DESCRIPTION,
		CrossCompileToolChain.DESCRIPTION,
		CustomToolChain.DESCRIPTION
	};

}
