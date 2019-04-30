/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.zephyrproject.ide.eclipse.ui.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.zephyrproject.ide.eclipse.core.ZephyrPlugin;
import org.zephyrproject.ide.eclipse.core.ZephyrStrings;
import org.zephyrproject.ide.eclipse.ui.preferences.internal.ZephyrBaseLocationDirectoryFieldEditor;

/**
 * Preference page for Zephyr to define various defaults.
 */
public class ZephyrPreferenceTopPage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	public ZephyrPreferenceTopPage() {
		super(GRID);
		setPreferenceStore(ZephyrPlugin.getDefault().getPreferenceStore());
		setDescription(ZephyrStrings.EMPTY_STRING);
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common
	 * GUI blocks needed to manipulate various types of preferences. Each field
	 * editor knows how to save and restore itself.
	 */
	@Override
	public void createFieldEditors() {
		addField(new ZephyrBaseLocationDirectoryFieldEditor(
				ZephyrPreferenceConstants.P_ZEPHYR_BASE,
				"Default &Zephyr Base:", getFieldEditorParent()));
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
