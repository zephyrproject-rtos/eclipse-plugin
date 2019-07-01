/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

public class ZephyrApplicationNature implements IProjectNature {

	/**
	 * ID of this project nature
	 */
	public static final String NATURE_ID =
			ZephyrPlugin.PLUGIN_ID + ".zephyrApplicationNature"; //$NON-NLS-1$

	private IProject project;

	@Override
	public void configure() throws CoreException {
	}

	@Override
	public void deconfigure() throws CoreException {
	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public void setProject(IProject project) {
		this.project = project;
	}

	public static boolean hasNature(IProject project) throws CoreException {
		IProjectDescription projDesc = project.getDescription();
		for (String id : projDesc.getNatureIds()) {
			if (id.equals(NATURE_ID)) {
				return true;
			}
		}
		return false;
	}

}
