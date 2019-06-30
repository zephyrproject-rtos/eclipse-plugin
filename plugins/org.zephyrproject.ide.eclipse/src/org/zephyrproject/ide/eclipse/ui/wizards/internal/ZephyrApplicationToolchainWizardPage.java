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
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
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
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.zephyrproject.ide.eclipse.core.ZephyrPlugin;
import org.zephyrproject.ide.eclipse.core.ZephyrStrings;
import org.zephyrproject.ide.eclipse.core.build.ZephyrToolChainConstants;
import org.zephyrproject.ide.eclipse.core.build.ZephyrToolChainConstants.CrossCompileToolChain;
import org.zephyrproject.ide.eclipse.core.build.ZephyrToolChainConstants.CrosstoolsToolChain;
import org.zephyrproject.ide.eclipse.core.build.ZephyrToolChainConstants.CustomToolChain;
import org.zephyrproject.ide.eclipse.core.build.ZephyrToolChainConstants.GnuArmEmbToolChain;
import org.zephyrproject.ide.eclipse.core.build.ZephyrToolChainConstants.IssmToolChain;
import org.zephyrproject.ide.eclipse.core.build.ZephyrToolChainConstants.ZephyrSdkToolChain;
import org.zephyrproject.ide.eclipse.core.internal.ZephyrHelpers;
import org.zephyrproject.ide.eclipse.ui.preferences.ZephyrPreferenceConstants;

/**
 * Wizard page to specify toolchain related information.
 */
public class ZephyrApplicationToolchainWizardPage extends WizardPage {

	/**
	 * Listener to determine whether this page has valid data to move forward.
	 */
	private final class fieldModifyListener implements Listener {
		@Override
		public void handleEvent(Event event) {
			setPageComplete(validatePage());
		}
	};

	private final fieldModifyListener modifyListener =
			new fieldModifyListener();

	private IPreferenceStore zTopPref =
			ZephyrPlugin.getDefault().getPreferenceStore();

	private Composite composite;

	private Combo tcSelection;

	private Text tcVariant;

	private Composite tcParams;
	private StackLayout tcParamsLayout;

	private Composite grpZephyr;
	private Text zephyrSdkInstallDir;

	private Composite grpXTools;
	private Text xtoolsDir;

	private Composite grpGnuArmEmb;
	private Text gnuArmEmbDir;

	private Composite grpISSM;
	private Text issmDir;

	private Composite grpCrossCompile;
	private Text crossCompilePrefix;

	private Composite grpCustom;
	private Text customRoot;

	public ZephyrApplicationToolchainWizardPage(String pageName) {
		super(pageName);
		setPageComplete(false);
	}

	/**
	 * Handle the drop-down box selection of toolchain variant.
	 *
	 * This changes the visibilities of the controls according to variant
	 * selection. Only the necessary controls related to a particular toolchain
	 * variant are visible.
	 */
	private void handleToolchainSelection() {
		String selection = tcSelection.getText();

		if (selection.equals(ZephyrSdkToolChain.DESCRIPTION)) {
			tcVariant.setText(ZephyrSdkToolChain.VARIANT);
			tcVariant.setEnabled(false);
			tcParamsLayout.topControl = grpZephyr;
		} else if (selection.equals(CrosstoolsToolChain.DESCRIPTION)) {
			tcVariant.setText(CrosstoolsToolChain.VARIANT);
			tcVariant.setEnabled(false);
			tcParamsLayout.topControl = grpXTools;
		} else if (selection.equals(GnuArmEmbToolChain.DESCRIPTION)) {
			tcVariant.setText(GnuArmEmbToolChain.VARIANT);
			tcVariant.setEnabled(false);
			tcParamsLayout.topControl = grpGnuArmEmb;
		} else if (selection.equals(IssmToolChain.DESCRIPTION)) {
			tcVariant.setText(IssmToolChain.VARIANT);
			tcVariant.setEnabled(false);
			tcParamsLayout.topControl = grpISSM;
		} else if (selection.equals(CrossCompileToolChain.DESCRIPTION)) {
			tcVariant.setText(CrossCompileToolChain.VARIANT);
			tcVariant.setEnabled(false);
			tcParamsLayout.topControl = grpCrossCompile;
		} else if (selection.equals(CustomToolChain.DESCRIPTION)) {
			tcVariant.setText(ZephyrStrings.EMPTY_STRING);
			tcVariant.setEnabled(true);
			tcParamsLayout.topControl = grpCustom;
		}

		tcParams.layout();

		setPageComplete(validatePage());
	}

	/**
	 * Create a composite to group various controls for easier manipulation.
	 *
	 * @return A composite
	 */
	private Composite createOneControlGroup(Composite parent) {
		Composite grp = new Composite(parent, SWT.NULL);

		grp.setFont(parent.getFont());

		grp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		GridLayout layout = new GridLayout();
		layout.marginTop = 15;
		layout.marginWidth = 0;
		grp.setLayout(layout);

		return grp;
	}

	/**
	 * Handle browse button action.
	 *
	 * @param field The text field associated with the browse button.
	 */
	private void handleDirBrowseBtn(Text field) {
		DirectoryDialog dialog =
				new DirectoryDialog(field.getShell(), SWT.SHEET);
		String loc = field.getText();
		if (!loc.isEmpty()) {
			dialog.setFilterPath(loc);
		}
		String selection = dialog.open();
		if (selection != null) {
			field.setText(selection);
		}
	}

	/**
	 * Helper function to create a label.
	 *
	 * @param grp Composite.
	 * @param txt Text of label.
	 * @return The new Label object.
	 */
	private Label createLabel(Composite grp, String txt) {
		Label desc = new Label(grp, SWT.NULL);
		GridData gridData = new GridData();
		desc.setLayoutData(gridData);
		desc.setText(txt);
		desc.setFont(grp.getFont());

		return desc;
	}

	/**
	 * Helper function to create a "Browse" button.
	 *
	 * @param grp Composite.
	 * @param field Text field associated with the browse button.
	 * @return The new Button object.
	 */
	private Button createBrowseBtn(Composite grp, Text field) {
		Button btn = new Button(grp, SWT.PUSH);
		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
		btn.setLayoutData(gridData);
		btn.setText(ZephyrStrings.BROWSE_BTN_TEXT);
		btn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				handleDirBrowseBtn(field);
			}
		});

		return btn;
	}

	/**
	 * Helper function to create a generic text field.
	 *
	 * @param grp Composite
	 * @return The new Text object.
	 */
	private Text createTextField(Composite grp) {
		Text field = new Text(grp, SWT.BORDER);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		field.setLayoutData(gridData);
		field.setFont(grp.getFont());
		field.addListener(SWT.Modify, modifyListener);

		return field;
	}

	/**
	 * Helper function to create a text field for directory location.
	 *
	 * @param grp Composite
	 * @return The new Text object.
	 */
	private Text createDirField(Composite grp) {
		Text field = createTextField(grp);
		BidiUtils.applyBidiProcessing(field,
				StructuredTextTypeHandlerFactory.FILE);

		return field;
	}

	/**
	 * Create a group for official Zephyr SDK.
	 */
	private void createGroupZephyrSDK() {
		Composite grp = grpZephyr;

		createLabel(grp, ZephyrSdkToolChain.DIRECTORY_DESCRIPTION + " (" //$NON-NLS-1$
				+ ZephyrSdkToolChain.ENV + "):"); //$NON-NLS-1$

		zephyrSdkInstallDir = createDirField(grp);

		zephyrSdkInstallDir.setText(zTopPref
				.getString(ZephyrPreferenceConstants.P_ZEPHYR_SDK_INSTALL_DIR));

		createBrowseBtn(grp, zephyrSdkInstallDir);
	};

	/**
	 * Create a group for Crosstool-NG toolchain.
	 */
	private void createGroupXtools() {
		Composite grp = grpXTools;

		createLabel(grp, CrosstoolsToolChain.DIRECTORY_DESCRIPTION + " (" //$NON-NLS-1$
				+ CrosstoolsToolChain.ENV + "):"); //$NON-NLS-1$

		xtoolsDir = createDirField(grp);

		xtoolsDir.setText(zTopPref
				.getString(ZephyrPreferenceConstants.P_XTOOLS_TOOLCHAIN_PATH));

		createBrowseBtn(grp, xtoolsDir);
	};

	/**
	 * Create a group for GNU ARM Embedded toolchain.
	 */
	private void createGroupGnuArmEmb() {
		Composite grp = grpGnuArmEmb;

		createLabel(grp, GnuArmEmbToolChain.DIRECTORY_DESCRIPTION + " (" //$NON-NLS-1$
				+ GnuArmEmbToolChain.ENV + "):"); //$NON-NLS-1$

		gnuArmEmbDir = createDirField(grp);

		gnuArmEmbDir.setText(zTopPref.getString(
				ZephyrPreferenceConstants.P_GNUARMEMB_TOOLCHAIN_PATH));

		createBrowseBtn(grp, gnuArmEmbDir);
	};

	/**
	 * Create a group for Intel System Studio for Microcontrollers toolchain.
	 */
	private void createGroupISSM() {
		Composite grp = grpISSM;

		createLabel(grp, IssmToolChain.DIRECTORY_DESCRIPTION + " (" //$NON-NLS-1$
				+ IssmToolChain.ENV + "):"); //$NON-NLS-1$

		issmDir = createDirField(grp);

		issmDir.setText(zTopPref
				.getString(ZephyrPreferenceConstants.P_ISSM_INSTALLATION_PATH));

		createBrowseBtn(grp, issmDir);
	};

	/**
	 * Create a group for specifying CROSS_COMPILE toolchain.
	 */
	private void createGroupCrossCompile() {
		Composite grp = grpCrossCompile;

		createLabel(grp, CrossCompileToolChain.PREFIX_DESCRIPTION + " (" //$NON-NLS-1$
				+ CrossCompileToolChain.ENV + "):"); //$NON-NLS-1$

		crossCompilePrefix = createTextField(grp);

		crossCompilePrefix.setText(zTopPref
				.getString(ZephyrPreferenceConstants.P_CROSS_COMPILE_PREFIX));

		createBrowseBtn(grp, crossCompilePrefix);
	};

	/**
	 * Create a group for specifying custom CMake toolchain.
	 */
	private void createGroupCustom() {
		Composite grp = grpCustom;

		createLabel(grp, CustomToolChain.DIRECTORY_DESCRIPTION + " (" //$NON-NLS-1$
				+ CustomToolChain.ENV + "):"); //$NON-NLS-1$

		customRoot = createTextField(grp);

		createBrowseBtn(grp, customRoot);
	};

	@Override
	public void createControl(Composite parent) {
		GridData gridData;
		composite = new Composite(parent, SWT.NULL);

		initializeDialogUnits(parent);

		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		/* Group for toolchain selection */
		Composite grpSelection = new Composite(composite, SWT.NULL);
		grpSelection.setLayout(new GridLayout(2, false));
		grpSelection.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		/* Label: "Toolchain Variant" */
		Label tcLabel = new Label(grpSelection, SWT.NONE);
		tcLabel.setLayoutData(new GridData());
		tcLabel.setText("Toolchain Variant:");
		tcLabel.setFont(grpSelection.getFont());

		/* Combo box for toolchain list */
		tcSelection = new Combo(grpSelection, SWT.BORDER | SWT.READ_ONLY);
		tcSelection.setLayoutData(new GridData());
		tcSelection.setFont(grpSelection.getFont());
		tcSelection
				.setItems(ZephyrToolChainConstants.ZEPHYR_TOOLCHAIN_DESC_LIST);
		tcSelection.select(0);
		tcSelection.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				handleToolchainSelection();
			}
		});

		/* Label and Text for ZEPHYR_TOOLCHAIN_VARIANT */
		Composite grpVariant = createOneControlGroup(composite);

		Label tcVariantLabel = new Label(grpVariant, SWT.NONE);
		gridData = new GridData();
		tcVariantLabel.setLayoutData(gridData);
		tcVariantLabel.setText(
				ZephyrToolChainConstants.ZEPHYR_TOOLCHAIN_VARIANT + " ="); //$NON-NLS-1$
		tcVariantLabel.setFont(composite.getFont());

		tcVariant = new Text(grpVariant, SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		tcVariant.setLayoutData(gridData);
		tcVariant.setFont(composite.getFont());

		tcParams = new Composite(composite, SWT.NO_TRIM | SWT.NO_FOCUS);
		tcParamsLayout = new StackLayout();
		tcParams.setLayout(tcParamsLayout);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		tcParams.setLayoutData(gridData);

		/* Create various groups for toolchains */
		grpZephyr = createOneControlGroup(tcParams);
		createGroupZephyrSDK();

		grpXTools = createOneControlGroup(tcParams);
		createGroupXtools();

		grpGnuArmEmb = createOneControlGroup(tcParams);
		createGroupGnuArmEmb();

		grpISSM = createOneControlGroup(tcParams);
		createGroupISSM();

		grpCrossCompile = createOneControlGroup(tcParams);
		createGroupCrossCompile();

		grpCustom = createOneControlGroup(tcParams);
		createGroupCustom();

		handleToolchainSelection();
		setPageComplete(validatePage());

		setErrorMessage(null);
		setMessage(null);
		setControl(composite);
		Dialog.applyDialogFont(composite);
	}

	protected boolean validatePage() {
		String selection = tcSelection.getText();
		boolean valid = false;

		if (selection.equals(ZephyrSdkToolChain.DESCRIPTION)) {
			String dir = zephyrSdkInstallDir.getText();

			valid = ZephyrHelpers.checkValidZephyrSdkInstallDir(dir);

			if (dir.isEmpty()) {
				setErrorMessage(null);
				setMessage(ZephyrSdkToolChain.DIRECTORY_DESCRIPTION
						+ " must be specified");
			} else if (!valid) {
				setErrorMessage(ZephyrSdkToolChain.DIRECTORY_DESCRIPTION
						+ " is not valid");
				setMessage(null);
			}
		} else if (selection.equals(CrosstoolsToolChain.DESCRIPTION)) {
			String dir = xtoolsDir.getText();

			valid = ZephyrHelpers.checkValidXToolsDirectory(dir);

			if (dir.isEmpty()) {
				setErrorMessage(null);
				setMessage(CrosstoolsToolChain.DIRECTORY_DESCRIPTION
						+ " must be specified");
			} else if (!valid) {
				setErrorMessage(CrosstoolsToolChain.DIRECTORY_DESCRIPTION
						+ " is not valid");
				setMessage(null);
			}
		} else if (selection.equals(GnuArmEmbToolChain.DESCRIPTION)) {
			String dir = gnuArmEmbDir.getText();

			valid = ZephyrHelpers.checkValidDirectory(dir);

			if (dir.isEmpty()) {
				setErrorMessage(null);
				setMessage(GnuArmEmbToolChain.DIRECTORY_DESCRIPTION
						+ " must be specified");
			} else if (!valid) {
				setErrorMessage(GnuArmEmbToolChain.DIRECTORY_DESCRIPTION
						+ " is not valid");
				setMessage(null);
			}
		} else if (selection.equals(IssmToolChain.DESCRIPTION)) {
			String dir = issmDir.getText();

			valid = ZephyrHelpers.checkValidDirectory(dir);

			if (dir.isEmpty()) {
				setErrorMessage(null);
				setMessage(IssmToolChain.DIRECTORY_DESCRIPTION
						+ " must be specified");
			} else if (!valid) {
				setErrorMessage(
						IssmToolChain.DIRECTORY_DESCRIPTION + " is not valid");
				setMessage(null);
			}
		} else if (selection.equals(CrossCompileToolChain.DESCRIPTION)) {
			String prefix = crossCompilePrefix.getText();
			if (!prefix.isEmpty()) {
				valid = true;
			} else {
				setErrorMessage(CrossCompileToolChain.PREFIX_DESCRIPTION
						+ " must be specified");
				setMessage(null);
			}
		} else if (selection.equals(CustomToolChain.DESCRIPTION)) {
			String variant = tcVariant.toString();
			if (!variant.isEmpty()) {
				String tcRoot = customRoot.getText();
				if (!tcRoot.isEmpty()) {
					valid = true;
				} else {
					setErrorMessage(CustomToolChain.DIRECTORY_DESCRIPTION
							+ " must be specified");
					setMessage(null);
				}
			} else {
				setErrorMessage(
						ZephyrToolChainConstants.ZEPHYR_TOOLCHAIN_VARIANT
								+ " must be specified");
				setMessage(null);
			}
		}

		if (valid) {
			setErrorMessage(null);
			setMessage(null);
		}
		return valid;
	}

	/**
	 * Perform actions associated with finishing the wizard.
	 *
	 * This saves the toolchain related information in the project preference
	 * store.
	 *
	 * @param project
	 * @throws IOException
	 */
	public void performFinish(IProject project) throws IOException {
		ScopedPreferenceStore pStore =
				ZephyrHelpers.getProjectPreferenceStore(project);

		String variant = tcVariant.getText();
		pStore.putValue(ZephyrToolChainConstants.ZEPHYR_TOOLCHAIN_VARIANT,
				variant);

		if (variant.equals(ZephyrSdkToolChain.VARIANT)) {
			String dir = zephyrSdkInstallDir.getText();
			pStore.putValue(ZephyrSdkToolChain.ENV, dir);
		} else if (variant.equals(CrosstoolsToolChain.VARIANT)) {
			String dir = xtoolsDir.getText();
			pStore.putValue(CrosstoolsToolChain.ENV, dir);
		} else if (variant.equals(GnuArmEmbToolChain.VARIANT)) {
			String dir = gnuArmEmbDir.getText();
			pStore.putValue(GnuArmEmbToolChain.ENV, dir);
		} else if (variant.equals(IssmToolChain.VARIANT)) {
			String dir = issmDir.getText();
			pStore.putValue(IssmToolChain.ENV, dir);
		} else if (variant.equals(CrossCompileToolChain.VARIANT)) {
			String prefix = crossCompilePrefix.getText();
			pStore.putValue(CrossCompileToolChain.ENV, prefix);
		} else {
			String tcRoot = customRoot.getText();
			pStore.putValue(CustomToolChain.ENV, tcRoot);
		}

		pStore.save();
	}
}
