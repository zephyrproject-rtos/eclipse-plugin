/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.launchbar;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.launchbar.core.ILaunchDescriptor;
import org.eclipse.launchbar.core.ProjectPerTargetLaunchConfigProvider;
import org.eclipse.launchbar.core.target.ILaunchTarget;
import org.eclipse.launchbar.core.target.ILaunchTargetManager;
import org.zephyrproject.ide.eclipse.core.ZephyrConstants.Launch;

public class ZephyrApplicationLaunchConfigurationProvider
		extends ProjectPerTargetLaunchConfigProvider {

	@Override
	protected ILaunchConfiguration createLaunchConfiguration(
			ILaunchDescriptor descriptor, ILaunchTarget target)
			throws CoreException {
		ILaunchManager launchManager =
				DebugPlugin.getDefault().getLaunchManager();
		String name = launchManager
				.generateLaunchConfigurationName(descriptor.getName());
		ILaunchConfigurationType type =
				getLaunchConfigurationType(descriptor, target);
		ILaunchConfigurationWorkingCopy workingCopy =
				type.newInstance(null, name);

		populateLaunchConfiguration(descriptor, target, workingCopy);

		return workingCopy.doSave();
	}

	@Override
	public ILaunchConfigurationType getLaunchConfigurationType(
			ILaunchDescriptor descriptor, ILaunchTarget target)
			throws CoreException {
		return DebugPlugin.getDefault().getLaunchManager()
				.getLaunchConfigurationType(target.getTypeId());
	}

	@Override
	public boolean supports(ILaunchDescriptor descriptor, ILaunchTarget target)
			throws CoreException {
		if (Launch.LAUNCH_TARGET_EMULATOR_RUN_TYPE_ID
				.equals(target.getTypeId())) {
			return true;
		} else if (Launch.LAUNCH_TARGET_HARDWARE_RUN_TYPE_ID
				.equals(target.getTypeId())) {
			return true;
		}

		return false;
	}

	@Override
	protected void populateLaunchConfiguration(ILaunchDescriptor descriptor,
			ILaunchTarget target, ILaunchConfigurationWorkingCopy workingCopy)
			throws CoreException {
		super.populateLaunchConfiguration(descriptor, target, workingCopy);

		/* Set the project and the connection */
		IProject project = descriptor.getAdapter(IProject.class);
		workingCopy.setMappedResources(new IResource[] {
			project
		});

		if (target.getTypeId()
				.equals(Launch.LAUNCH_TARGET_EMULATOR_RUN_TYPE_ID)) {
			workingCopy.setAttribute(Launch.ATTR_EMULATOR_RUN_CMD_SEL,
					Launch.EMULATOR_RUN_CMD_SEL_DFLT);
		} else if (target.getTypeId()
				.equals(Launch.LAUNCH_TARGET_HARDWARE_RUN_TYPE_ID)) {
			workingCopy.setAttribute(Launch.ATTR_FLASH_CMD_SEL,
					Launch.FLASH_CMD_SEL_DFLT);
		}
	}

	@Override
	protected ILaunchTarget getLaunchTarget(ILaunchConfiguration configuration)
			throws CoreException {
		@SuppressWarnings("restriction")
		ILaunchTargetManager manager =
				org.eclipse.launchbar.core.internal.Activator
						.getLaunchTargetManager();

		ILaunchTarget target = manager.getDefaultLaunchTarget(configuration);
		if (target != null) {
			return target;
		}

		String type = configuration.getType().getIdentifier();

		if (type.equals(Launch.LAUNCH_TARGET_EMULATOR_RUN_TYPE_ID)) {
			target = manager.getLaunchTarget(
					Launch.LAUNCH_TARGET_EMULATOR_RUN_TYPE_ID,
					Launch.LAUNCH_TARGET_EMULATOR_RUN_NAME);
		} else if (type.equals(Launch.LAUNCH_TARGET_HARDWARE_RUN_TYPE_ID)) {
			target = manager.getLaunchTarget(
					Launch.LAUNCH_TARGET_HARDWARE_RUN_TYPE_ID,
					Launch.LAUNCH_TARGET_HARDWARE_RUN_NAME);
		}

		return target;
	}

}
