/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.zephyrproject.ide.eclipse.core.ZephyrPlugin;
import org.zephyrproject.ide.eclipse.core.ZephyrStrings;

/**
 * Class used to initialize default preference values.
 */
public class ZephyrPreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#
	 * initializeDefaultPreferences()
	 */
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = ZephyrPlugin.getDefault().getPreferenceStore();

		store.setDefault(ZephyrPreferenceConstants.P_ZEPHYR_BASE,
				ZephyrStrings.EMPTY_STRING);

		store.setDefault(ZephyrPreferenceConstants.P_ZEPHYR_SDK_INSTALL_DIR,
				ZephyrStrings.EMPTY_STRING);
		store.setDefault(ZephyrPreferenceConstants.P_GNUARMEMB_TOOLCHAIN_PATH,
				ZephyrStrings.EMPTY_STRING);
		store.setDefault(ZephyrPreferenceConstants.P_XTOOLS_TOOLCHAIN_PATH,
				ZephyrStrings.EMPTY_STRING);
		store.setDefault(ZephyrPreferenceConstants.P_CROSS_COMPILE_PREFIX,
				ZephyrStrings.EMPTY_STRING);
	}

}
