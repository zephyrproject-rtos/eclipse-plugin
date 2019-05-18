/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.ui.preferences.internal;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.zephyrproject.ide.eclipse.core.ZephyrConstants;
import org.zephyrproject.ide.eclipse.core.internal.ZephyrHelpers;

/**
 * Directory field editor for specifying Zephyr Base as preference.
 *
 * This extends {@code DirectoryFieldEditor} so it can check for a valid
 * Zephyr Base directory.
 */
public class ZephyrBaseLocationDirectoryFieldEditor
		extends DirectoryFieldEditor {

	public ZephyrBaseLocationDirectoryFieldEditor(String name, String labelText,
			Composite parent) {
		super(name, labelText, parent);
		setErrorMessage("Not a valid " + ZephyrConstants.ZEPHYR_BASE_DESC_DIR);
	}

	/**
	 * Check if the specified directory is a valid Zephyr Base directory.
	 *
	 * Note that this allows an empty string in the field editor so that
	 * there is simply no default directory to be used for project creation.
	 *
	 * @return True if the directory is valid, false otherwise.
	 *
	 * @see org.eclipse.jface.preference.DirectoryFieldEditor#doCheckState()
	 */
	@Override
	protected boolean doCheckState() {
		if (!super.doCheckState()) {
			return false;
		}

		String path = getStringValue();
		if (path == null) {
			return false;
		}
		if (!path.isEmpty() && !ZephyrHelpers.checkValidZephyrBase(path)) {
			/* Empty string is allowed */
			return false;
		}

		return true;
	}

}
