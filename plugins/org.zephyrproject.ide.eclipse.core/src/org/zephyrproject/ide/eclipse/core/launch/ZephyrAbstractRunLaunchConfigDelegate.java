/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.launch;

import org.eclipse.cdt.core.build.ICBuildConfiguration;
import org.eclipse.cdt.debug.core.launch.CoreBuildLaunchConfigDelegate;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.launchbar.core.target.ILaunchTarget;
import org.zephyrproject.ide.eclipse.core.build.ZephyrApplicationBuildConfiguration;
import org.zephyrproject.ide.eclipse.core.internal.ZephyrHelpers;

public abstract class ZephyrAbstractRunLaunchConfigDelegate
		extends CoreBuildLaunchConfigDelegate {

	@Override
	protected ZephyrApplicationBuildConfiguration getBuildConfiguration(
			IProject project, String mode, ILaunchTarget target,
			IProgressMonitor monitor) throws CoreException {
		return ZephyrHelpers.Launch.getBuildConfiguration(project, mode, target,
				monitor);
	}

	@Override
	protected ICBuildConfiguration getBuildConfiguration(
			ILaunchConfiguration configuration, String mode,
			ILaunchTarget target, IProgressMonitor monitor)
			throws CoreException {
		return this.getBuildConfiguration(getProject(configuration), mode,
				target, monitor);
	}

	protected void doMakefile(IProject project,
			ZephyrApplicationBuildConfiguration appBuildCfg, ILaunch launch,
			String makeProgram, String mode) throws CoreException {
		ZephyrHelpers.Launch.doMakefile(project, appBuildCfg, launch,
				makeProgram, mode);
	}

	protected void doNinja(IProject project,
			ZephyrApplicationBuildConfiguration appBuildCfg, ILaunch launch,
			String makeProgram, String mode) throws CoreException {
		ZephyrHelpers.Launch.doNinja(project, appBuildCfg, launch, makeProgram,
				mode);
	}

}
