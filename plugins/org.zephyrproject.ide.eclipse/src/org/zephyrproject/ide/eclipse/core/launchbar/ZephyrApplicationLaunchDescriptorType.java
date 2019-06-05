/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.launchbar;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.launchbar.core.ILaunchDescriptor;
import org.eclipse.launchbar.core.ILaunchDescriptorType;
import org.eclipse.launchbar.core.ProjectLaunchDescriptor;
import org.zephyrproject.ide.eclipse.core.ZephyrApplicationNature;

public class ZephyrApplicationLaunchDescriptorType
		implements ILaunchDescriptorType {

	@Override
	public ILaunchDescriptor getDescriptor(Object launchObject)
			throws CoreException {
		if (launchObject instanceof IProject
				&& ZephyrApplicationNature.hasNature((IProject) launchObject)) {
			return new ProjectLaunchDescriptor(this, (IProject) launchObject);
		}

		return null;
	}

}
