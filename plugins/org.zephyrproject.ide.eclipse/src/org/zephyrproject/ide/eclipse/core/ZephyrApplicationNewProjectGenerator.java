/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.cdt.core.index.IndexerSetupParticipant;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IPathEntry;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tools.templates.freemarker.FMProjectGenerator;
import org.eclipse.tools.templates.freemarker.SourceRoot;
import org.osgi.framework.Bundle;
import org.zephyrproject.ide.eclipse.core.build.makefiles.ZephyrApplicationMakefilesBuildConfiguration;
import org.zephyrproject.ide.eclipse.core.build.ninja.ZephyrApplicationNinjaBuildConfiguration;

/**
 * New project generator for Zephyr Application.
 *
 * This generates the project files using templates, and sets up the project.
 */
public class ZephyrApplicationNewProjectGenerator extends FMProjectGenerator {

	private String cmakeGenerator;

	private IndexerSetupParticipant indexerSetupParticipant;

	private boolean projectCreationComplete;

	public ZephyrApplicationNewProjectGenerator(String manifestPath) {
		super(manifestPath);

		/* Default to use Ninja */
		this.cmakeGenerator = ZephyrConstants.CMAKE_GENERATOR_NINJA;

		this.projectCreationComplete = false;
		this.indexerSetupParticipant = new IndexerSetupParticipant() {
			@Override
			public boolean postponeIndexerSetup(ICProject project) {
				return !projectCreationComplete;
			}
		};
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
		this.setupProject(desc);
	}

	/**
	 * Sets up necessary bits for project.
	 *
	 * @param desc IProjectDescription associated with the project
	 */
	private void setupProject(IProjectDescription desc) {
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

		/* Setup build configuration */
		IProject project = getProject();
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		StringBuilder cfgName = new StringBuilder();
		if (cmakeGenerator.equals(ZephyrConstants.CMAKE_GENERATOR_MAKEFILE)) {
			cfgName.append(
					ZephyrApplicationMakefilesBuildConfiguration.CONFIG_NAME);
		} else if (cmakeGenerator
				.equals(ZephyrConstants.CMAKE_GENERATOR_NINJA)) {
			cfgName.append(
					ZephyrApplicationNinjaBuildConfiguration.CONFIG_NAME);
		}

		cfgName.append("#"); //$NON-NLS-1$
		cfgName.append(project.getName());

		IBuildConfiguration config =
				workspace.newBuildConfig(project.getName(), cfgName.toString());

		String[] configNames = {
			config.getName()
		};

		desc.setBuildConfigs(configNames);
		desc.setActiveBuildConfig(config.getName());
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

		CCorePlugin.getIndexManager()
				.addIndexerSetupParticipant(indexerSetupParticipant);

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

		/* Setup project */
		IProjectDescription desc = project.getDescription();
		setupProject(desc);
		project.setDescription(desc, monitor);
	}

	public void setCMakeGenerator(String cmakeGenerator) {
		this.cmakeGenerator = cmakeGenerator;
	}

	public void notifyProjectCreationComplete(ICProject cproject) {
		projectCreationComplete = true;
		indexerSetupParticipant.notifyIndexerSetup(cproject);
		CCorePlugin.getIndexManager()
				.removeIndexerSetupParticipant(indexerSetupParticipant);
	}

}
