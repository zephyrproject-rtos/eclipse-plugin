/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.ui.preferences;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.zephyrproject.ide.eclipse.core.ZephyrPlugin;
import org.zephyrproject.ide.eclipse.core.preferences.ZephyrPreferenceConstants;

/**
 * Preference page for toolchain defaults.
 */
public class ZephyrPreferenceToolchainPage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	public ZephyrPreferenceToolchainPage() {
		super(GRID);
		setPreferenceStore(ZephyrPlugin.getDefault().getPreferenceStore());
		setDescription("Toolchain Defaults for Zephyr");
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common
	 * GUI blocks needed to manipulate various types of preferences. Each field
	 * editor knows how to save and restore itself.
	 */
	@Override
	public void createFieldEditors() {
		/* Official Zephyr SDK Install Directory */
		DirectoryFieldEditor zephyrSDK = new DirectoryFieldEditor(
				ZephyrPreferenceConstants.P_ZEPHYR_SDK_INSTALL_DIR,
				"Default Zephyr &SDK Install Directory:",
				getFieldEditorParent());
		zephyrSDK.setEmptyStringAllowed(true);
		addField(zephyrSDK);

		/* GNU ARM EMB Toolchain Path */
		DirectoryFieldEditor gnuArmEmbPath = new DirectoryFieldEditor(
				ZephyrPreferenceConstants.P_GNUARMEMB_TOOLCHAIN_PATH,
				"Default GNU &ARM EMB Toolchain Path:", getFieldEditorParent());
		gnuArmEmbPath.setEmptyStringAllowed(true);
		addField(gnuArmEmbPath);

		/* Crosstool-NG Toolchains Path */
		DirectoryFieldEditor xtoolsPath = new DirectoryFieldEditor(
				ZephyrPreferenceConstants.P_XTOOLS_TOOLCHAIN_PATH,
				"Default Crosstool-&NG Toolchains Path:",
				getFieldEditorParent());
		xtoolsPath.setEmptyStringAllowed(true);
		addField(xtoolsPath);

		/* Cross compile prefix */
		StringFieldEditor crossCompilePrefix = new StringFieldEditor(
				ZephyrPreferenceConstants.P_CROSS_COMPILE_PREFIX,
				"Default &Cross Compile Prefix:", getFieldEditorParent());
		crossCompilePrefix.setEmptyStringAllowed(true);
		addField(crossCompilePrefix);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	@Override
	public void init(IWorkbench workbench) {
	}

}
