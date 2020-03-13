/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.ui.wizards;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IPathEntry;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.tools.templates.core.IGenerator;
import org.eclipse.tools.templates.ui.TemplateWizard;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.zephyrproject.ide.eclipse.core.ZephyrApplicationNewProjectGenerator;
import org.zephyrproject.ide.eclipse.core.ZephyrPlugin;
import org.zephyrproject.ide.eclipse.core.ZephyrStrings;
import org.zephyrproject.ide.eclipse.core.preferences.ZephyrProjectPreferences.ZephyrBase;
import org.zephyrproject.ide.eclipse.ui.wizards.internal.ZephyrApplicationBoardWizardPage;
import org.zephyrproject.ide.eclipse.ui.wizards.internal.ZephyrApplicationMainWizardPage;
import org.zephyrproject.ide.eclipse.ui.wizards.internal.ZephyrApplicationToolchainWizardPage;

public class ZephyrApplicationEmptyProjectWizard extends TemplateWizard {

	private ZephyrApplicationMainWizardPage mainPage;

	private ZephyrApplicationToolchainWizardPage toolchainPage;

	private ZephyrApplicationBoardWizardPage boardPage;

	private static final String WIZARD_NAME =
			ZephyrStrings.ZEPHYR_APPLICATION_PROJECT + " Wizard";

	private ZephyrApplicationNewProjectGenerator generator;

	public ZephyrApplicationEmptyProjectWizard() {
		super();
		setDialogSettings(ZephyrPlugin.getDefault().getDialogSettings());
		setNeedsProgressMonitor(true);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.jface.wizard.Wizard#setContainer(org.eclipse.jface.wizard.
	 * IWizardContainer)
	 */
	@Override
	public void setContainer(IWizardContainer wizardContainer) {
		super.setContainer(wizardContainer);
		setWindowTitle(ZephyrStrings.ZEPHYR_APPLICATION);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		mainPage = new ZephyrApplicationMainWizardPage(WIZARD_NAME);
		mainPage.setTitle(ZephyrStrings.ZEPHYR_APPLICATION_PROJECT);
		mainPage.setDescription(
				"Create a new " + ZephyrStrings.ZEPHYR_APPLICATION);
		addPage(mainPage);

		toolchainPage = new ZephyrApplicationToolchainWizardPage(WIZARD_NAME);
		toolchainPage.setTitle(ZephyrStrings.ZEPHYR_APPLICATION_PROJECT
				+ " - Toolchain Selection");
		toolchainPage.setDescription(
				"Specify the Toolchain to Build this Application");
		addPage(toolchainPage);

		boardPage = new ZephyrApplicationBoardWizardPage(WIZARD_NAME, mainPage);
		boardPage.setTitle(ZephyrStrings.ZEPHYR_APPLICATION_PROJECT
				+ " - Target Board Configuration");
		boardPage.setDescription("Specify the target board configuration");
		addPage(boardPage);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.tools.templates.ui.TemplateWizard#getGenerator()
	 */
	@Override
	protected IGenerator getGenerator() {
		if (generator != null) {
			return generator;
		}

		generator = new ZephyrApplicationNewProjectGenerator(null);
		generator.setProjectName(mainPage.getProjectName());
		generator.setCMakeGenerator(mainPage.getCMakeGenerator());
		generator.setSourceDirectory(mainPage.getSourceDirectory());
		if (!mainPage.useDefaults()) {
			generator.setLocationURI(mainPage.getLocationURI());
		}
		return generator;
	}

	/**
	 * Show an error dialog and delete the project from workspace.
	 *
	 * Project must be created in the workspace before configuration of
	 * the project can take place. The configuration phase may not complete
	 * entirely, so this is to avoid creating an incomplete/invalid project in
	 * the workspace.
	 *
	 * @param msg The message to be displayed in the dialog.
	 * @param t Throwable that can be displayed in the dialog.
	 */
	private void showErrorDialogAndDeleteProject(String msg, Throwable t) {
		Status status = new Status(IStatus.ERROR, ZephyrPlugin.PLUGIN_ID, 0,
				t.getLocalizedMessage(), t);
		ErrorDialog.openError(getShell(), "Error", msg, status);

		try {
			mainPage.getProjectHandle().delete(false, false, null);
		} catch (CoreException ce) {
			/* ignore */
		}
	}

	/**
	 * Perform actions associated with finishing the wizard.
	 */
	@Override
	public boolean performFinish() {
		/*
		 * TemplateWizard.performFinish() always return true, but would throw
		 * RuntimeException.
		 */
		try {
			super.performFinish();
		} catch (RuntimeException e) {
			showErrorDialogAndDeleteProject("Cannot create project files", e);
			return false;
		}

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project =
				workspace.getRoot().getProject(mainPage.getProjectName());
		ICProject cProj =
				CCorePlugin.getDefault().getCoreModel().create(project);

		List<IPathEntry> entries = new ArrayList<>();
		try {
			entries = new ArrayList<>(Arrays.asList(cProj.getRawPathEntries()));
		} catch (CModelException e) {
			showErrorDialogAndDeleteProject("Error getting paths from CDT", e);
			return false;
		}

		/*
		 * The project root path is designated as both source and output by
		 * default. Remove them to avoid confusing the indexer.
		 */
		Iterator<IPathEntry> iter = entries.iterator();
		while (iter.hasNext()) {
			IPathEntry path = iter.next();

			if (((path.getEntryKind() == IPathEntry.CDT_SOURCE)
					|| (path.getEntryKind() == IPathEntry.CDT_OUTPUT))
					&& (path.getPath().equals(project.getFullPath()))) {
				iter.remove();
			}
		}

		/*
		 * Create the build directory, and let CDT know where the build (output)
		 * directory is, excluding CMake directories.
		 */
		IFolder buildFolder = project.getFolder(mainPage.getBuildDirectory());
		if (!buildFolder.exists()) {
			try {
				buildFolder.create(IResource.FORCE | IResource.DERIVED, true,
						new NullProgressMonitor());
			} catch (CoreException e) {
				showErrorDialogAndDeleteProject("Cannot create build directory",
						e);
				return false;
			}
		}

		entries.add(CoreModel.newOutputEntry(buildFolder.getFullPath(),
				new IPath[] {
					new Path("**/CMakeFiles/**") //$NON-NLS-1$
				}));

		/*
		 * Create a link to ZEPHYR_BASE so the indexer can also index the Zephyr
		 * core code.
		 */
		IFolder zBase = project.getFolder(ZephyrBase.ZEPHYR_BASE); // $NON-NLS-1$
		String zBaseLoc = mainPage.getZephyrBaseLocation();
		IPath zBaseLink = new Path(zBaseLoc);

		if (zBase.exists() && zBase.isLinked()) {
			/*
			 * The project itself might be deleted from workspace metadata, but
			 * project files still exist on storage.
			 */
			try {
				zBase.delete(false, new NullProgressMonitor());
			} catch (CoreException e) {
				showErrorDialogAndDeleteProject(String.format(
						"Cannot create project due to pre-existing linked resource '%s'",
						zBase.getFullPath()), e);
				return false;
			}
		}

		IStatus zBaseLinkValid =
				workspace.validateLinkLocation(zBase, zBaseLink);
		if (zBaseLinkValid.isOK() || zBaseLinkValid.matches(IStatus.WARNING)) {
			/*
			 * WARNING means the linked resource also appears in another
			 * project, so not exactly an issue.
			 */
			try {
				zBase.createLink(zBaseLink, IResource.NONE, null);
			} catch (CoreException e) {
				showErrorDialogAndDeleteProject(
						String.format("Error creating linked resource to %s",
								ZephyrBase.DIRECTORY_DESCRIPTION),
						e);
				return false;
			}
		} else {
			RuntimeException e = new RuntimeException("Link not valid");
			showErrorDialogAndDeleteProject(
					String.format("Error creating linked resource to %s",
							ZephyrBase.DIRECTORY_DESCRIPTION),
					e);
			return false;
		}

		/*
		 * Also need to tell CDT ZEPHYR_BASE is source so it will index the
		 * source inside, for code completion and references/declarations
		 * searching.
		 */
		IPath[] exclusion = new Path[] {
			/*
			 * Parsing all boards is not useful as we only need one.
			 */
			new Path("boards/**"), //$NON-NLS-1$

			/*
			 * Files in these directories are ignored to speed up
			 * indexing a bit by not looking at those files.
			 */
			new Path("cmake/**"), //$NON-NLS-1$
			new Path("doc/**"), //$NON-NLS-1$
			new Path("scripts/**"), //$NON-NLS-1$

			/*
			 * Code in ext/ is usually architecture specific and should not
			 * be included unconditionally.
			 */
			new Path("ext/**"), //$NON-NLS-1$

			/*
			 * Default build and sanity check directories are ignored.
			 */
			new Path("**/build/**"), //$NON-NLS-1$
			new Path("sanity-out/**"), //$NON-NLS-1$

			/*
			 * Ignore non-code files.
			 */
			new Path("CODEOWNERS"), //$NON-NLS-1$ -1$
			new Path("CODE_OF_CONDUCT.md"), //$NON-NLS-1$ -1$
			new Path("LICENSE"), //$NON-NLS-1$ -1$
			new Path("VERSION"), //$NON-NLS-1$ -1$
			new Path("**/CMakeLists.txt"), //$NON-NLS-1$
			new Path("**/Kconfig*"), //$NON-NLS-1$ -1$
			new Path("**/Makefile"), // $NON-NLS
			new Path("**/*.cmd"), //$NON-NLS-1$
			new Path("**/*.dts"), //$NON-NLS-1$
			new Path("**/*.dtsi"), //$NON-NLS-1$
			new Path("**/*.h.in"), //$NON-NLS-1$
			new Path("**/*.ld"), //$NON-NLS-1$
			new Path("**/*.py"), //$NON-NLS-1$
			new Path("**/*.pyc"), //$NON-NLS-1$
			new Path("**/*.rst"), //$NON-NLS-1$
			new Path("**/*.sh"), //$NON-NLS-1$
			new Path("**/*.yml"), //$NON-NLS-1$

			/*
			 * samples/** and tests/** are excluded because code here is
			 * almost never called in applications.
			 */
			new Path("samples/**"), //$NON-NLS-1$
			new Path("tests/**"), //$NON-NLS-1$
		};

		entries.add(CoreModel.newSourceEntry(zBase.getFullPath(), exclusion));

		try {
			cProj.setRawPathEntries(entries.toArray(new IPathEntry[0]), null);
		} catch (CModelException e) {
			showErrorDialogAndDeleteProject("Error setting paths to CDT", e);
			return false;
		}

		try {
			mainPage.performFinish(project);
			toolchainPage.performFinish(project);
			boardPage.performFinish(project);
		} catch (IOException e) {
			showErrorDialogAndDeleteProject("Cannot save project settings", e);
			return false;
		}

		if (generator != null) {
			generator.notifyProjectCreationComplete(cProj);
		}

		return true;
	}

	/**
	 * This sets the icon for the wizard page.
	 */
	@Override
	protected void initializeDefaultPageImageDescriptor() {
		ImageDescriptor desc = AbstractUIPlugin.imageDescriptorFromPlugin(
				ZephyrPlugin.PLUGIN_ID, "icons/wizard.png"); //$NON-NLS-1$
		setDefaultPageImageDescriptor(desc);
	}

}
