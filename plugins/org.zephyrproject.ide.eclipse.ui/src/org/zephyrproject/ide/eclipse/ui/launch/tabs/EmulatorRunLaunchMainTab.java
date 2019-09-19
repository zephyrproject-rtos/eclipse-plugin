/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.ui.launch.tabs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.launch.ui.CAbstractMainTab;
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
import org.zephyrproject.ide.eclipse.core.launch.ZephyrLaunchConstants;
import org.zephyrproject.ide.eclipse.core.launch.ZephyrProcessFactory;

public class EmulatorRunLaunchMainTab extends CAbstractMainTab {

	public static final String TAB_ID =
			ZephyrPlugin.PLUGIN_ID + ".ui.launch.emulator.run.mainTab"; //$NON-NLS-1$

	public static final String MENU_ID = "Main";

	private Button btnEmulatorRunBuildSys;

	private Button btnEmulatorRunWest;

	private Text westRunArgsText;

	public EmulatorRunLaunchMainTab() {
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

		createVerticalSpacer(comp, 1);
		createCommandSelectionGroup(comp, 1);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		/*
		 * These are going to be set by initializeCProject().
		 * Provide defaults if the function is not called.
		 */
		configuration.setAttribute(
				ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME,
				EMPTY_STRING);
		configuration.setAttribute(
				ICDTLaunchConfigurationConstants.ATTR_PROJECT_BUILD_CONFIG_ID,
				EMPTY_STRING);

		/* Initialize configuration */
		ICElement cElement = null;
		cElement = getContext(configuration, getPlatform(configuration));
		if (cElement != null) {
			initializeCProject(cElement, configuration);

			/* To have a meaningful name instead of "New_configuration" */
			if (cElement instanceof ICProject) {
				String cfgName = getLaunchConfigurationDialog()
						.generateName(String.format("%s %s", //$NON-NLS-1$
								cElement.getElementName(), "emulator")); //$NON-NLS-1$
				configuration.rename(cfgName);
			}
		} else {
			configuration.setMappedResources(null);
		}

		/* Build before running */
		configuration.setAttribute(
				ICDTLaunchConfigurationConstants.ATTR_BUILD_BEFORE_LAUNCH, 1);

		/* Use default command to run emulator */
		configuration.setAttribute(
				ZephyrLaunchConstants.ATTR_EMULATOR_RUN_CMD_SEL,
				ZephyrLaunchConstants.EMULATOR_RUN_CMD_SEL_BUILDSYS);
		configuration.setAttribute(ZephyrLaunchConstants.ATTR_RUN_CMD_WEST_ARGS,
				EMPTY_STRING);

		/* Use our own process factory */
		configuration.setAttribute(DebugPlugin.ATTR_PROCESS_FACTORY_ID,
				ZephyrProcessFactory.ID);
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		updateProjectFromConfig(configuration);

		try {
			String cmdSel = configuration.getAttribute(
					ZephyrLaunchConstants.ATTR_EMULATOR_RUN_CMD_SEL,
					EMPTY_STRING);

			if (cmdSel.equals(
					ZephyrLaunchConstants.EMULATOR_RUN_CMD_SEL_BUILDSYS)) {
				btnEmulatorRunBuildSys.setSelection(true);
				btnEmulatorRunWest.setSelection(false);
				westRunArgsText.setEnabled(false);
			} else if (cmdSel
					.equals(ZephyrLaunchConstants.EMULATOR_RUN_CMD_SEL_WEST)) {
				btnEmulatorRunBuildSys.setSelection(false);
				btnEmulatorRunWest.setSelection(true);
				westRunArgsText.setEnabled(true);
			} else {
				btnEmulatorRunBuildSys.setSelection(true);
				btnEmulatorRunWest.setSelection(false);
				westRunArgsText.setEnabled(false);
			}

			String westArgs = configuration.getAttribute(
					ZephyrLaunchConstants.ATTR_RUN_CMD_WEST_ARGS, EMPTY_STRING);
			westRunArgsText.setText(westArgs);
		} catch (CoreException e) {
			/* Default */
			btnEmulatorRunBuildSys.setSelection(true);
			btnEmulatorRunWest.setSelection(false);
			westRunArgsText.setEnabled(false);
		}
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

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		super.performApply(configuration);
		ICProject cProject = this.getCProject();
		if (cProject != null && cProject.exists()) {
			configuration.setMappedResources(new IResource[] {
				cProject.getProject()
			});
		} else {
			configuration.setMappedResources(null);
		}

		configuration.setAttribute(
				ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME,
				fProjText.getText());

		if (btnEmulatorRunBuildSys.getSelection()) {
			configuration.setAttribute(
					ZephyrLaunchConstants.ATTR_EMULATOR_RUN_CMD_SEL,
					ZephyrLaunchConstants.EMULATOR_RUN_CMD_SEL_BUILDSYS);
		} else if (btnEmulatorRunWest.getSelection()) {
			configuration.setAttribute(
					ZephyrLaunchConstants.ATTR_EMULATOR_RUN_CMD_SEL,
					ZephyrLaunchConstants.EMULATOR_RUN_CMD_SEL_WEST);

			configuration.setAttribute(
					ZephyrLaunchConstants.ATTR_RUN_CMD_WEST_ARGS,
					westRunArgsText.getText());
		} else {
			configuration.setAttribute(
					ZephyrLaunchConstants.ATTR_EMULATOR_RUN_CMD_SEL,
					ZephyrLaunchConstants.EMULATOR_RUN_CMD_SEL_NONE);
		}
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		return true;
	}

	@Override
	protected void handleSearchButtonSelected() {
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
		cmdSelLabel.setText("Command to Run:"); //$NON-NLS-1$
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		cmdSelLabel.setLayoutData(gridData);

		btnEmulatorRunBuildSys = new Button(cmdSelGrp, SWT.RADIO);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		btnEmulatorRunBuildSys.setLayoutData(gridData);
		btnEmulatorRunBuildSys
				.setText("Invoke build system to run emulator"); //$NON-NLS-1$
		btnEmulatorRunBuildSys.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				updateCommandSelection();
			}
		});

		btnEmulatorRunWest = new Button(cmdSelGrp, SWT.RADIO);
		btnEmulatorRunWest.setText("Invoke West to run emulator:"); //$NON-NLS-1$
		btnEmulatorRunWest.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				updateCommandSelection();
			}
		});

		westRunArgsText = new Text(cmdSelGrp, SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		westRunArgsText.setLayoutData(gridData);
		westRunArgsText.setEnabled(false);
		westRunArgsText.setMessage("(additional arguments to West)"); //$NON-NLS-1$
		westRunArgsText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
	}

	private void updateCommandSelection() {
		westRunArgsText.setEnabled(btnEmulatorRunWest.getSelection());

		updateLaunchConfigurationDialog();
	}
}
