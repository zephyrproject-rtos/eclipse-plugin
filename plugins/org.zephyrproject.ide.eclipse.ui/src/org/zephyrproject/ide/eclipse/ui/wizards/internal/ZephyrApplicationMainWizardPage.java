/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.ui.wizards.internal;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.equinox.bidi.StructuredTextTypeHandlerFactory;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.BidiUtils;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.zephyrproject.ide.eclipse.core.ZephyrPlugin;
import org.zephyrproject.ide.eclipse.core.ZephyrStrings;
import org.zephyrproject.ide.eclipse.core.build.CMakeConstants;
import org.zephyrproject.ide.eclipse.core.internal.ZephyrHelpers;
import org.zephyrproject.ide.eclipse.core.preferences.ZephyrPreferenceConstants;
import org.zephyrproject.ide.eclipse.core.preferences.ZephyrProjectPreferences;
import org.zephyrproject.ide.eclipse.core.preferences.ZephyrProjectPreferences.ZephyrBase;

/**
 * Main wizard page for Zephyr Application wizards.
 *
 * This contains controls to specify the project name and where to find
 * Zephyr Base directory.
 */
public class ZephyrApplicationMainWizardPage
		extends WizardNewProjectCreationPage {

	private Text zBaseTextField;

	private Button zBaseUseDefaultBtn = null;

	private Button zBaseBrowseBtn;

	private String zephyrBaseLocation = null;

	private String zbUserLoc;

	private Combo zCMakeGenerator;

	private Text buildFolderTextField;

	private String buildFolder;

	public ZephyrApplicationMainWizardPage(String pageName) {
		super(pageName);

		this.buildFolder = ZephyrProjectPreferences.BUILD_DIR_DEFAULT;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.ui.dialogs.WizardNewProjectCreationPage#createControl(org.
	 * eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);

		zbUserLoc = getDialogSettings().get(ZephyrBase.ZEPHYR_BASE_LOCATION);
		if (zbUserLoc == null) {
			zbUserLoc = ZephyrStrings.EMPTY_STRING;
		}

		createZephyrBaseGroup((Composite) getControl());
		createCMakeGeneratorGroup((Composite) getControl());
		createFolderSelectionGroup((Composite) getControl());
		setPageComplete(validatePage());
		Dialog.applyDialogFont(getControl());
	}

	/**
	 * @return The Zephyr Base directory.
	 */
	public String getZephyrBaseLocation() {
		return this.zephyrBaseLocation;
	}

	/**
	 * @return True if use the Zephyr Base defined in preference page, false
	 *         otherwise.
	 */
	public boolean getZephyrBaseUseDefault() {
		if (zBaseUseDefaultBtn != null) {
			return zBaseUseDefaultBtn.getSelection();
		} else {
			return false;
		}
	}

	/**
	 * @return The selected CMake Generator.
	 */
	public String getCMakeGenerator() {
		return zCMakeGenerator.getText();
	}

	public String getBuildDirectory() {
		return this.buildFolder;
	}

	public String getSourceDirectory() {
		return ZephyrProjectPreferences.SOURCE_DIR_DEFAULT;
	}

	/**
	 * @return The default Zephyr Base defined in preference page.
	 */
	private String getPrefZephyrBaseDefault() {
		IPreferenceStore zTopPref =
				ZephyrPlugin.getDefault().getPreferenceStore();

		return zTopPref.getString(ZephyrPreferenceConstants.P_ZEPHYR_BASE);

	}

	/**
	 * Listener to determine whether this page has valid data to move forward.
	 */
	private class zBaseLocModifyListener implements Listener {
		@Override
		public void handleEvent(Event event) {
			setPageComplete(validatePage());
		}
	};

	private void setZephyrBaseLocControlsEnabled(boolean enabled) {
		zBaseTextField.setEnabled(enabled);
		zBaseBrowseBtn.setEnabled(enabled);
	}

	/**
	 * Create the controls to specify Zephyr Base directory.
	 *
	 * @param parent
	 */
	private final void createZephyrBaseGroup(Composite parent) {
		GridData gridData;
		Composite zBaseGroup = new Composite(parent, SWT.NONE);
		String zBasePref = getPrefZephyrBaseDefault();
		boolean zBaseLocUseDefault = !zBasePref.isEmpty();

		/* Create a grid with 3 columns */
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		zBaseGroup.setLayout(layout);
		zBaseGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		/* Label: "Zephyr Base" */
		Label zBaseLabel = new Label(zBaseGroup, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 3;
		zBaseLabel.setLayoutData(gridData);
		zBaseLabel.setText(ZephyrBase.DIRECTORY_DESCRIPTION + " (" //$NON-NLS-1$
				+ ZephyrBase.ZEPHYR_BASE + "):"); //$NON-NLS-1$
		zBaseLabel.setFont(zBaseGroup.getFont());

		/* Checkbox: "Use default" for Zephyr Base */
		if (!zBasePref.isEmpty()) {
			zBaseUseDefaultBtn = new Button(zBaseGroup, SWT.CHECK | SWT.RIGHT);
			zBaseUseDefaultBtn.setText("Use default");
			zBaseUseDefaultBtn.setSelection(zBaseLocUseDefault);
			gridData = new GridData();
			gridData.horizontalSpan = 3;
			zBaseUseDefaultBtn.setLayoutData(gridData);
			zBaseUseDefaultBtn.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					handleZephyrBaseUseDefaultBtn();
				}
			});
		}

		/* Label: "Location" */
		Label locationLabel = new Label(zBaseGroup, SWT.NONE);
		locationLabel.setText("Location:");

		/* Text field for the ZEPHYR_BASE location */
		zBaseTextField = new Text(zBaseGroup, SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		zBaseTextField.setLayoutData(gridData);
		zBaseTextField.setFont(zBaseGroup.getFont());
		BidiUtils.applyBidiProcessing(zBaseTextField,
				StructuredTextTypeHandlerFactory.FILE);
		zBaseTextField.addListener(SWT.Modify, new zBaseLocModifyListener());

		/* Start ZEPHYR_BASE with location set in main preference page */
		if (!zBasePref.isEmpty()) {
			zBaseTextField.setText(zBasePref);
		} else {
			zBaseTextField.setText(zbUserLoc);
		}

		/* "Browse" button for ZEPHYR_BASE location */
		zBaseBrowseBtn = new Button(zBaseGroup, SWT.PUSH);
		zBaseBrowseBtn.setText(ZephyrStrings.BROWSE_BTN_TEXT);
		zBaseBrowseBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				handleZephyrBaseBrowseBtn();
			}
		});

		setZephyrBaseLocControlsEnabled(!zBaseLocUseDefault);

		Dialog.applyDialogFont(zBaseGroup);
	}

	/**
	 * Create the controls to specify the CMake generator.
	 *
	 * @param parent
	 */
	private final void createCMakeGeneratorGroup(Composite parent) {
		GridData gridData;
		Composite cmakeGroup = new Composite(parent, SWT.NONE);

		/* Create a grid with 3 columns */
		GridLayout layout = new GridLayout(3, false);
		cmakeGroup.setLayout(layout);
		cmakeGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		/* Label: "CMake Generator" */
		Label generator = new Label(cmakeGroup, SWT.NONE);
		gridData = new GridData();
		generator.setLayoutData(gridData);
		generator.setText(CMakeConstants.CMAKE_GENERATOR_DESC + ":"); //$NON-NLS-1$
		generator.setFont(cmakeGroup.getFont());

		/* Combo box for CMake Generator */
		zCMakeGenerator = new Combo(cmakeGroup, SWT.BORDER | SWT.READ_ONLY);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		zCMakeGenerator.setLayoutData(gridData);
		zCMakeGenerator.setItems(CMakeConstants.CMAKE_GENERATOR_LIST);
		zCMakeGenerator.select(0);
		zCMakeGenerator.setFont(cmakeGroup.getFont());

		Dialog.applyDialogFont(cmakeGroup);
	}

	/**
	 * Create the controls to specify various folder names.
	 *
	 * @param parent
	 */
	private final void createFolderSelectionGroup(Composite parent) {
		GridData gridData;
		Composite zFolderGroup = new Composite(parent, SWT.NONE);

		/* Create a grid with 3 columns */
		GridLayout layout = new GridLayout(2, false);
		layout.numColumns = 3;
		zFolderGroup.setLayout(layout);
		zFolderGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		/* Label: "Build folder" */
		Label buildFolderLabel = new Label(zFolderGroup, SWT.NONE);
		buildFolderLabel.setText("Build folder:");

		/* Text field for the ZEPHYR_BASE location */
		buildFolderTextField = new Text(zFolderGroup, SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		buildFolderTextField.setLayoutData(gridData);
		buildFolderTextField.setFont(zFolderGroup.getFont());
		BidiUtils.applyBidiProcessing(zBaseTextField,
				StructuredTextTypeHandlerFactory.FILE);
		buildFolderTextField.addListener(SWT.Modify,
				new zBaseLocModifyListener());
		buildFolderTextField.setText(this.buildFolder);

		Dialog.applyDialogFont(zFolderGroup);
	}

	/**
	 * Handle the event from the "Browse" button for ZEPHYR_BASE.
	 */
	private void handleZephyrBaseBrowseBtn() {
		DirectoryDialog dialog =
				new DirectoryDialog(zBaseTextField.getShell(), SWT.SHEET);
		String zBaseLoc = zBaseTextField.getText();
		if (!zBaseLoc.isEmpty()) {
			dialog.setFilterPath(zBaseLoc);
		}
		String selection = dialog.open();
		if (selection != null) {
			zBaseTextField.setText(selection);
		}
	}

	/**
	 * Handle the event from the use default checkbox for Zephyr Base.
	 */
	private void handleZephyrBaseUseDefaultBtn() {
		boolean checked = zBaseUseDefaultBtn.getSelection();

		if (checked) {
			zbUserLoc = zBaseTextField.getText();
			zBaseTextField.setText(getPrefZephyrBaseDefault());
		} else {
			zBaseTextField.setText(TextProcessor.process(zbUserLoc));
		}
		setZephyrBaseLocControlsEnabled(!checked);
	}

	@Override
	protected boolean validatePage() {
		/* Validate project name and location first (in super class) */
		if (!super.validatePage()) {
			return false;
		}

		/* Is ZEPHYR_BASE pointing to a valid repo? */
		String zBaseLoc = zBaseTextField.getText();
		if (zBaseLoc.isEmpty()) {
			setErrorMessage(
					ZephyrBase.DIRECTORY_DESCRIPTION + " must be specified");
			return false;
		}

		try {
			/* Check if VERSION file exists */
			if (!ZephyrHelpers.checkValidZephyrBase(zBaseLoc)) {
				setErrorMessage(
						ZephyrBase.DIRECTORY_DESCRIPTION + " is not valid");
				return false;
			}
		} catch (SecurityException se) {
			/* No read access to the VERSION file */
			setErrorMessage(
					"Cannot access " + ZephyrBase.DIRECTORY_DESCRIPTION);
			return false;
		}

		/* Build folder is not empty */
		String bFolder = buildFolderTextField.getText().trim();
		if (bFolder.isEmpty()) {
			return false;
		}

		/* TODO: actually verify content of VERSION */

		this.zephyrBaseLocation = zBaseLoc;
		this.buildFolder = bFolder;

		setErrorMessage(null);
		return true;
	}

	/**
	 * Perform actions associated with finishing the wizard.
	 *
	 * This saves the Zephyr Base directory location in the project preference
	 * store.
	 *
	 * @param project
	 * @throws IOException
	 */
	public void performFinish(IProject project) throws IOException {
		String zBaseLoc = zBaseTextField.getText();

		ScopedPreferenceStore pStore =
				ZephyrHelpers.getProjectPreferenceStore(project);

		/* Save user specified Zephyr Base for use in the future */
		if (!getZephyrBaseUseDefault()) {
			getDialogSettings().put(ZephyrBase.ZEPHYR_BASE_LOCATION, zBaseLoc);
		}

		pStore.putValue(ZephyrBase.ZEPHYR_BASE_LOCATION, zBaseLoc);
		pStore.putValue(ZephyrProjectPreferences.BUILD_DIR,
				getBuildDirectory());
		pStore.putValue(ZephyrProjectPreferences.SOURCE_DIR,
				getSourceDirectory());

		/* Store the chosen CMake generator */
		String generator = zCMakeGenerator.getText();
		pStore.putValue(CMakeConstants.CMAKE_GENERATOR, generator);

		pStore.save();
	}

}
