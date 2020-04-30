/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.ui.wizards;

import org.zephyrproject.ide.eclipse.ui.wizards.internal.ZephyrApplicationAbstractProjectWizard;

public class ZephyrApplicationNewProjectWizard
		extends ZephyrApplicationAbstractProjectWizard {

	public ZephyrApplicationNewProjectWizard() {
		super("templates/ZephyrApplication/template.xml"); //$NON-NLS-1$
	}

}
