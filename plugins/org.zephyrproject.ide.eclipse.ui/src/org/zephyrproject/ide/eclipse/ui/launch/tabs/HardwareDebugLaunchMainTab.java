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

public class HardwareDebugLaunchMainTab extends CMainTab2 {

	public static final String TAB_ID =
			ZephyrPlugin.PLUGIN_ID + ".ui.launch.hardware.debug.mainTab"; //$NON-NLS-1$

	public static final String MENU_ID = "Main";

	private Button btnFlashTargetNone;

	private Button btnFlashTargetDefault;

	private Button btnFlashTargetCustomCmd;

	private Text flashTargetCustomCommandText;

	private Button btnDbgSrvNone;

	private Button btnDbgSrvDefault;

	private Button btnDbgSrvCustomCmd;

	private Text dbgSrvCustomCommandText;

	public HardwareDebugLaunchMainTab() {
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
		createFlashTargetCommandSelectionGroup(comp, 1);

		createVerticalSpacer(comp, 1);
		createDbgSrvCommandSelectionGroup(comp, 1);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		super.setDefaults(configuration);

		try {
			IResource[] resources = configuration.getMappedResources();
			if ((resources != null) && (resources.length > 0)) {
				IProject project = resources[0].getProject();
				ZephyrApplicationBuildConfiguration appBuildCfg =
						ZephyrHelpers.Launch.getBuildConfiguration(project);

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
								project.getName(), "hardware")); //$NON-NLS-1$
				configuration.rename(cfgName);
			}
		} catch (CoreException e) {
		}

		/* Build before running */
		configuration.setAttribute(
				ICDTLaunchConfigurationConstants.ATTR_BUILD_BEFORE_LAUNCH, 1);

		/* Use default command to flash hardware target */
		configuration.setAttribute(ZephyrLaunchConstants.ATTR_FLASH_CMD_SEL,
				ZephyrLaunchConstants.FLASH_CMD_SEL_DFLT);
		configuration.setAttribute(
				ZephyrLaunchConstants.ATTR_FLASH_CUSTOM_COMMAND, EMPTY_STRING);

		/* Use default command to start debug server */
		configuration.setAttribute(ZephyrLaunchConstants.ATTR_DBGSERVER_CMD_SEL,
				ZephyrLaunchConstants.DBGSERVER_CMD_SEL_DEFAULT);
		configuration.setAttribute(
				ZephyrLaunchConstants.ATTR_DBGSERVER_CUSTOM_COMMAND,
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
					ZephyrLaunchConstants.DBGSERVER_CMD_SEL_DEFAULT);

			if (cmdSel.equals(
					ZephyrLaunchConstants.DBGSERVER_CMD_SEL_CUSTOM_COMMAND)) {
				btnDbgSrvNone.setSelection(false);
				btnDbgSrvDefault.setSelection(false);
				btnDbgSrvCustomCmd.setSelection(true);
				dbgSrvCustomCommandText.setEnabled(true);
			} else if (cmdSel
					.equals(ZephyrLaunchConstants.DBGSERVER_CMD_SEL_NONE)) {
				btnDbgSrvNone.setSelection(true);
				btnDbgSrvDefault.setSelection(false);
				btnDbgSrvCustomCmd.setSelection(false);
				dbgSrvCustomCommandText.setEnabled(false);
			} else {
				btnDbgSrvNone.setSelection(false);
				btnDbgSrvDefault.setSelection(true);
				btnDbgSrvCustomCmd.setSelection(false);
				dbgSrvCustomCommandText.setEnabled(false);
			}

			String customCmd = configuration.getAttribute(
					ZephyrLaunchConstants.ATTR_EMULATOR_RUN_CUSTOM_COMMAND,
					EMPTY_STRING);
			dbgSrvCustomCommandText.setText(customCmd);
		} catch (CoreException e) {
			/* Default */
			btnDbgSrvNone.setSelection(false);
			btnDbgSrvDefault.setSelection(true);
			btnDbgSrvCustomCmd.setSelection(false);
			dbgSrvCustomCommandText.setEnabled(false);
		}

		try {
			String cmdSel = configuration.getAttribute(
					ZephyrLaunchConstants.ATTR_FLASH_CMD_SEL,
					ZephyrLaunchConstants.FLASH_CMD_SEL_DFLT);

			if (cmdSel.equals(ZephyrLaunchConstants.FLASH_CMD_SEL_CUSTOM_CMD)) {
				btnFlashTargetNone.setSelection(false);
				btnFlashTargetDefault.setSelection(false);
				btnFlashTargetCustomCmd.setSelection(true);
				flashTargetCustomCommandText.setEnabled(true);
			} else if (cmdSel
					.equals(ZephyrLaunchConstants.FLASH_CMD_SEL_NONE)) {
				btnFlashTargetNone.setSelection(true);
				btnFlashTargetDefault.setSelection(false);
				btnFlashTargetCustomCmd.setSelection(false);
				flashTargetCustomCommandText.setEnabled(false);
			} else {
				btnFlashTargetNone.setSelection(false);
				btnFlashTargetDefault.setSelection(true);
				btnFlashTargetCustomCmd.setSelection(false);
				flashTargetCustomCommandText.setEnabled(false);
			}

			String customCmd = configuration.getAttribute(
					ZephyrLaunchConstants.ATTR_FLASH_CUSTOM_COMMAND,
					EMPTY_STRING);
			flashTargetCustomCommandText.setText(customCmd);
		} catch (CoreException e) {
			/* Default */
			btnFlashTargetNone.setSelection(false);
			btnFlashTargetDefault.setSelection(true);
			btnFlashTargetCustomCmd.setSelection(false);
			flashTargetCustomCommandText.setEnabled(false);
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		super.performApply(configuration);

		if (btnDbgSrvCustomCmd.getSelection()) {
			configuration.setAttribute(
					ZephyrLaunchConstants.ATTR_DBGSERVER_CMD_SEL,
					ZephyrLaunchConstants.DBGSERVER_CMD_SEL_CUSTOM_COMMAND);
			configuration.setAttribute(
					ZephyrLaunchConstants.ATTR_DBGSERVER_CUSTOM_COMMAND,
					dbgSrvCustomCommandText.getText());
		} else if (btnDbgSrvDefault.getSelection()) {
			configuration.setAttribute(
					ZephyrLaunchConstants.ATTR_DBGSERVER_CMD_SEL,
					ZephyrLaunchConstants.DBGSERVER_CMD_SEL_DEFAULT);
		} else if (btnDbgSrvNone.getSelection()) {
			configuration.setAttribute(
					ZephyrLaunchConstants.ATTR_DBGSERVER_CMD_SEL,
					ZephyrLaunchConstants.DBGSERVER_CMD_SEL_NONE);
		}

		if (btnFlashTargetCustomCmd.getSelection()) {
			configuration.setAttribute(ZephyrLaunchConstants.ATTR_FLASH_CMD_SEL,
					ZephyrLaunchConstants.FLASH_CMD_SEL_CUSTOM_CMD);
			configuration.setAttribute(
					ZephyrLaunchConstants.ATTR_FLASH_CUSTOM_COMMAND,
					flashTargetCustomCommandText.getText());
		} else if (btnFlashTargetDefault.getSelection()) {
			configuration.setAttribute(ZephyrLaunchConstants.ATTR_FLASH_CMD_SEL,
					ZephyrLaunchConstants.FLASH_CMD_SEL_DFLT);
		} else if (btnFlashTargetNone.getSelection()) {
			configuration.setAttribute(ZephyrLaunchConstants.ATTR_FLASH_CMD_SEL,
					ZephyrLaunchConstants.FLASH_CMD_SEL_NONE);
		}
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		if (btnDbgSrvCustomCmd.getSelection()) {
			String customCmd = dbgSrvCustomCommandText.getText();
			if (customCmd.trim().isEmpty()) {
				return false;
			}
		}

		if (btnFlashTargetCustomCmd.getSelection()) {
			String customCmd = flashTargetCustomCommandText.getText();
			if (customCmd.trim().isEmpty()) {
				return false;
			}
		}

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

	private void createDbgSrvCommandSelectionGroup(Composite parent,
			int colSpan) {
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

		btnDbgSrvDefault = new Button(cmdSelGrp, SWT.RADIO);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		btnDbgSrvDefault.setLayoutData(gridData);
		btnDbgSrvDefault.setText("Use default command to start debug server"); //$NON-NLS-1$
		btnDbgSrvDefault.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				updateCommandSelection();
			}
		});

		btnDbgSrvCustomCmd = new Button(cmdSelGrp, SWT.RADIO);
		btnDbgSrvCustomCmd.setText("Custom command:"); //$NON-NLS-1$
		btnDbgSrvCustomCmd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				updateCommandSelection();
			}
		});

		dbgSrvCustomCommandText = new Text(cmdSelGrp, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		dbgSrvCustomCommandText.setLayoutData(gridData);
		dbgSrvCustomCommandText.setEnabled(false);
		dbgSrvCustomCommandText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
	}

	private void createFlashTargetCommandSelectionGroup(Composite parent,
			int colSpan) {
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
		cmdSelLabel.setText("Command to Run:"); //$NON-NLS-1$
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		cmdSelLabel.setLayoutData(gridData);

		btnFlashTargetNone = new Button(cmdSelGrp, SWT.RADIO);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		btnFlashTargetNone.setLayoutData(gridData);
		btnFlashTargetNone.setText("Do not flash hardware target"); //$NON-NLS-1$
		btnFlashTargetNone.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				updateCommandSelection();
			}
		});

		btnFlashTargetDefault = new Button(cmdSelGrp, SWT.RADIO);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		btnFlashTargetDefault.setLayoutData(gridData);
		btnFlashTargetDefault
				.setText("Default command to flash hardware target"); //$NON-NLS-1$
		btnFlashTargetDefault.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				updateCommandSelection();
			}
		});

		btnFlashTargetCustomCmd = new Button(cmdSelGrp, SWT.RADIO);
		btnFlashTargetCustomCmd.setText("Custom command:"); //$NON-NLS-1$
		btnFlashTargetCustomCmd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				updateCommandSelection();
			}
		});

		flashTargetCustomCommandText = new Text(cmdSelGrp, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		flashTargetCustomCommandText.setLayoutData(gridData);
		flashTargetCustomCommandText.setEnabled(false);
		flashTargetCustomCommandText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
	}

	private void updateCommandSelection() {
		dbgSrvCustomCommandText.setEnabled(btnDbgSrvCustomCmd.getSelection());
		flashTargetCustomCommandText
				.setEnabled(btnFlashTargetCustomCmd.getSelection());

		updateLaunchConfigurationDialog();
	}

}
