/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.zephyrproject.ide.eclipse.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IPathEntry;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tools.templates.freemarker.FMProjectGenerator;
import org.eclipse.tools.templates.freemarker.SourceRoot;
import org.osgi.framework.Bundle;

/**
 * New project generator for Zephyr Application.
 *
 * This generates the project files using templates, and sets up the project.
 */
public class ZephyrApplicationNewProjectGenerator extends FMProjectGenerator {

	public ZephyrApplicationNewProjectGenerator(String manifestPath) {
		super(manifestPath);
	}

	/**
	 * Setup the project with natures and build commands.
	 *
	 * @param desc IProjectDescription for the project.
	 *
	 * @see org.eclipse.tools.templates.freemarker.FMProjectGenerator#
	 *      initProjectDescription(org.eclipse.core.resources.IProjectDescription)
	 */
	@Override
	protected void initProjectDescription(IProjectDescription desc)
			throws CoreException {

		/*
		 * Setup Nature for project.
		 *
		 * The C/C++ natures are also needed for support of indexes/code
		 * styling/etc.
		 */
		desc.setNatureIds(new String[] {
			ZephyrApplicationNature.NATURE_ID,
			CProjectNature.C_NATURE_ID,
			CCProjectNature.CC_NATURE_ID
		});

		/*
		 * Setup command with Zephyr Application builder. For now, turn off
		 * "auto building when resource changes".
		 */
		ICommand cmd = desc.newCommand();
		cmd.setBuilderName(ZephyrApplicationBuilder.BUILDER_ID);
		cmd.setBuilding(IncrementalProjectBuilder.AUTO_BUILD, false);
		desc.setBuildSpec(new ICommand[] {
			cmd
		});
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.tools.templates.freemarker.FMGenerator#getSourceBundle()
	 */
	@Override
	protected Bundle getSourceBundle() {
		return ZephyrPlugin.getDefault().getBundle();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.tools.templates.freemarker.FMProjectGenerator#populateModel(
	 * java.util.Map)
	 */
	@Override
	protected void populateModel(Map<String, Object> model) {
		super.populateModel(model);

		/* Set the source directory parameter to be replaced in template */
		model.put("srcDir", ZephyrConstants.DEFAULT_SRC_DIR); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.tools.templates.freemarker.FMProjectGenerator#generate(java.
	 * util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void generate(Map<String, Object> model, IProgressMonitor monitor)
			throws CoreException {
		super.generate(model, monitor);

		IProject project = getProject();
		ICProject cproject =
				CCorePlugin.getDefault().getCoreModel().create(project);
		List<IPathEntry> entries = new ArrayList<>(
				Arrays.asList(CoreModel.getRawPathEntries(cproject)));

		/* Mark the source directories indicated in the template */
		List<SourceRoot> srcRoots = getManifest().getSrcRoots();
		for (SourceRoot srcRoot : srcRoots) {
			IFolder sourceFolder = project.getFolder(srcRoot.getDir());
			entries.add(CoreModel.newSourceEntry(sourceFolder.getFullPath()));
		}

		/* Tell CDT source and build paths */
		cproject.setRawPathEntries(
				entries.toArray(new IPathEntry[entries.size()]), monitor);
	}

}
