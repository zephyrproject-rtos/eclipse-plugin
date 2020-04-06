/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.ui.launch.tabs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.launch.ui.CMainTab2;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.zephyrproject.ide.eclipse.core.ZephyrApplicationNature;
import org.zephyrproject.ide.eclipse.core.ZephyrPlugin;
import org.zephyrproject.ide.eclipse.core.build.ZephyrApplicationBuildConfiguration;
import org.zephyrproject.ide.eclipse.core.internal.ZephyrHelpers;
import org.zephyrproject.ide.eclipse.core.launch.ZephyrLaunchConstants;
import org.zephyrproject.ide.eclipse.core.launch.ZephyrProcessFactory;

public class EmulatorDebugLaunchMainTab extends CMainTab2 {

	public static final String TAB_ID =
			ZephyrPlugin.PLUGIN_ID + ".ui.launch.emulator.debug.mainTab"; //$NON-NLS-1$

	public static final String MENU_ID = "Main";

	private Button btnDbgSrvNone;

	private Button btnDbgSrvBuildSys;

	private Button btnDbgSrvWest;

	private Text westDbgSrvArgsText;

	public EmulatorDebugLaunchMainTab() {
		super(0);
	}

	@Override
	public String getName() {
		return MENU_ID;
	}

	@Override
	public String getId() {
		return TAB_ID;
	}

	@Override
	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);

		GridLayout topLayout = new GridLayout();
		comp.setLayout(topLayout);

		createVerticalSpacer(comp, 1);
		createProjectGroup(comp, 1);
		createExeFileGroup(comp, 1);

		createVerticalSpacer(comp, 1);
		createCommandSelectionGroup(comp, 1);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		super.setDefaults(configuration);

		try {
			IResource[] resources = configuration.getMappedResources();
			if ((resources != null) && (resources.length > 0)) {
				IProject project = resources[0].getProject();
				ZephyrApplicationBuildConfiguration appBuildCfg =
						ZephyrHelpers.Launch
								.getZephyrBuildConfiguration(project);

				IContainer buildContainer = appBuildCfg.getBuildContainer();
				if (buildContainer instanceof IFolder) {
					IFolder buildFolder = (IFolder) buildContainer;

					IFile bin = buildFolder.getFile("zephyr/zephyr.elf"); //$NON-NLS-1$
					configuration.setAttribute(
							ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME,
							bin.getProjectRelativePath().toString());
				}

				String cfgName = getLaunchConfigurationDialog()
						.generateName(String.format("%s %s", //$NON-NLS-1$
								project.getName(), "emulator")); //$NON-NLS-1$
				configuration.rename(cfgName);
			}
		} catch (CoreException e) {
		}

		/* Build before running */
		configuration.setAttribute(
				ICDTLaunchConfigurationConstants.ATTR_BUILD_BEFORE_LAUNCH, 1);

		/* Use default command to start debug server */
		configuration.setAttribute(ZephyrLaunchConstants.ATTR_DBGSERVER_CMD_SEL,
				ZephyrLaunchConstants.DBGSERVER_CMD_SEL_BUILDSYS);
		configuration.setAttribute(
				ZephyrLaunchConstants.ATTR_DBGSERVER_CMD_WEST_ARGS,
				EMPTY_STRING);

		/* Use our own process factory */
		configuration.setAttribute(DebugPlugin.ATTR_PROCESS_FACTORY_ID,
				ZephyrProcessFactory.ID);
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		super.initializeFrom(configuration);

		try {
			String cmdSel = configuration.getAttribute(
					ZephyrLaunchConstants.ATTR_DBGSERVER_CMD_SEL,
					ZephyrLaunchConstants.DBGSERVER_CMD_SEL_BUILDSYS);

			if (cmdSel
					.equals(ZephyrLaunchConstants.DBGSERVER_CMD_SEL_BUILDSYS)) {
				btnDbgSrvNone.setSelection(false);
				btnDbgSrvBuildSys.setSelection(true);
				btnDbgSrvWest.setSelection(false);
				westDbgSrvArgsText.setEnabled(false);
			} else if (cmdSel
					.equals(ZephyrLaunchConstants.DBGSERVER_CMD_SEL_WEST)) {
				btnDbgSrvNone.setSelection(false);
				btnDbgSrvBuildSys.setSelection(false);
				btnDbgSrvWest.setSelection(true);
				westDbgSrvArgsText.setEnabled(true);
			} else {
				/* Also "DBGSERVER_CMD_SEL_NONE" */
				btnDbgSrvNone.setSelection(true);
				btnDbgSrvBuildSys.setSelection(false);
				btnDbgSrvWest.setSelection(false);
				westDbgSrvArgsText.setEnabled(false);
			}

			String westArgs = configuration.getAttribute(
					ZephyrLaunchConstants.ATTR_DBGSERVER_CMD_WEST_ARGS,
					EMPTY_STRING);
			westDbgSrvArgsText.setText(westArgs);
		} catch (CoreException e) {
			/* Default */
			btnDbgSrvNone.setSelection(true);
			btnDbgSrvBuildSys.setSelection(false);
			btnDbgSrvWest.setSelection(false);
			westDbgSrvArgsText.setEnabled(false);
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		super.performApply(configuration);

		if (btnDbgSrvBuildSys.getSelection()) {
			configuration.setAttribute(
					ZephyrLaunchConstants.ATTR_DBGSERVER_CMD_SEL,
					ZephyrLaunchConstants.DBGSERVER_CMD_SEL_BUILDSYS);
		} else if (btnDbgSrvWest.getSelection()) {
			configuration.setAttribute(
					ZephyrLaunchConstants.ATTR_DBGSERVER_CMD_SEL,
					ZephyrLaunchConstants.DBGSERVER_CMD_SEL_WEST);

			configuration.setAttribute(
					ZephyrLaunchConstants.ATTR_DBGSERVER_CMD_WEST_ARGS,
					westDbgSrvArgsText.getText());
		} else {
			configuration.setAttribute(
					ZephyrLaunchConstants.ATTR_DBGSERVER_CMD_SEL,
					ZephyrLaunchConstants.DBGSERVER_CMD_SEL_NONE);
		}
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		return true;
	}

	@Override
	protected ICProject[] getCProjects() throws CModelException {
		ICProject[] cprojects = super.getCProjects();

		/* Filter out projects which does not have the Zephyr nature */
		List<ICProject> newList = new ArrayList<>();
		for (ICProject cproj : cprojects) {
			try {
				if (cproj.getProject()
						.hasNature(ZephyrApplicationNature.NATURE_ID)) {
					newList.add(cproj);
				}
			} catch (CoreException e) {
			}
		}

		return newList.toArray(new ICProject[0]);
	}

	private void createCommandSelectionGroup(Composite parent, int colSpan) {
		Composite cmdSelGrp = new Composite(parent, SWT.NONE);

		GridLayout cmdSelLayout = new GridLayout();
		cmdSelLayout.numColumns = 2;
		cmdSelLayout.marginHeight = 0;
		cmdSelLayout.marginWidth = 0;
		cmdSelGrp.setLayout(cmdSelLayout);

		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = colSpan;
		cmdSelGrp.setLayoutData(gridData);

		Label cmdSelLabel = new Label(cmdSelGrp, SWT.NONE);
		cmdSelLabel.setText("Debug Server:"); //$NON-NLS-1$
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		cmdSelLabel.setLayoutData(gridData);

		btnDbgSrvNone = new Button(cmdSelGrp, SWT.RADIO);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		btnDbgSrvNone.setLayoutData(gridData);
		btnDbgSrvNone.setText("Do not start debug server"); //$NON-NLS-1$
		btnDbgSrvNone.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				updateCommandSelection();
			}
		});

		btnDbgSrvBuildSys = new Button(cmdSelGrp, SWT.RADIO);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		btnDbgSrvBuildSys.setLayoutData(gridData);
		btnDbgSrvBuildSys.setText("Invoke build system to start debug server"); //$NON-NLS-1$
		btnDbgSrvBuildSys.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				updateCommandSelection();
			}
		});

		btnDbgSrvWest = new Button(cmdSelGrp, SWT.RADIO);
		btnDbgSrvWest.setText("Invoke West to start debug server:"); //$NON-NLS-1$
		btnDbgSrvWest.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				updateCommandSelection();
			}
		});

		westDbgSrvArgsText = new Text(cmdSelGrp, SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		westDbgSrvArgsText.setLayoutData(gridData);
		westDbgSrvArgsText.setEnabled(false);
		westDbgSrvArgsText.setMessage("(additional arguments to West)"); //$NON-NLS-1$
		westDbgSrvArgsText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
	}

	private void updateCommandSelection() {
		westDbgSrvArgsText.setEnabled(btnDbgSrvWest.getSelection());

		updateLaunchConfigurationDialog();
	}

}
