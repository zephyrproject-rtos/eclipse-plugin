/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.zephyrproject.ide.eclipse.core;

import java.util.Map;

import org.eclipse.cdt.core.build.CBuilder;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class ZephyrApplicationBuilder extends CBuilder {

	public static final String BUILDER_ID =
			ZephyrPlugin.PLUGIN_ID + ".zephyrApplicationBuilder"; //$NON-NLS-1$

	/**
	 * Adjust build kind if needed.
	 *
	 * This looks at the resource delta to determine whether a new kind value
	 * is needed to be passed to the builder. For example, if CMakeLists.txt
	 * has changed, a {@code FULL_BUILD} is needed to regenerate cmake files.
	 *
	 * @param oldKind Current kind value.
	 * @param delta Resource delta.
	 * @return New kind value to be passed to the builder.
	 */
	private int adjustKind(int oldKind, IResourceDelta delta) {
		int newKind = oldKind;
		IResourceDelta[] children = delta.getAffectedChildren(
				IResourceDelta.CHANGED | IResourceDelta.CONTENT);

		for (IResourceDelta oneDelta : children) {
			IResource res = oneDelta.getResource();
			if (res instanceof IFile) {
				IFile file = (IFile) res;

				/* If CMakeLists.txt has changed, force a full build */
				if (file.getName().equals("CMakeLists.txt")) { //$NON-NLS-1$
					newKind = FULL_BUILD;
				}

				/* If prj.conf has changed, force a full build */
				if (file.getName().equals("prj.conf")) { //$NON-NLS-1$
					newKind = FULL_BUILD;
				}
			}
		}

		return newKind;
	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args,
			IProgressMonitor monitor) throws CoreException {
		if (kind == FULL_BUILD) {
			/* Pass along for FULL_BUILD */
			return super.build(kind, args, monitor);
		} else {
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				return super.build(FULL_BUILD, args, monitor);
			}

			return super.build(adjustKind(kind, delta), args, monitor);
		}
	}

	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		super.clean(monitor);
	}

}
