/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.zephyrproject.ide.eclipse.core;

import java.util.Map;

import org.eclipse.cdt.core.build.CBuilder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class ZephyrApplicationBuilder extends CBuilder {

	public static final String BUILDER_ID =
			ZephyrPlugin.PLUGIN_ID + ".zephyrApplicationBuilder"; //$NON-NLS-1$

	@Override
	protected IProject[] build(int kind, Map<String, String> args,
			IProgressMonitor monitor) throws CoreException {
		return super.build(kind, args, monitor);
	}

	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		super.clean(monitor);
	}

}
