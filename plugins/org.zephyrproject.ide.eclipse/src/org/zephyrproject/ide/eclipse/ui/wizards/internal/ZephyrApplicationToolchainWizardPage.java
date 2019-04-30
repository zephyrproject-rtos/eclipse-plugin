/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.zephyrproject.ide.eclipse.ui.wizards.internal;

import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_DESC_LIST;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_CROSS_COMPILE;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_CROSS_COMPILE_DESC;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_CROSS_COMPILE_DESC_PREFIX;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_CROSS_COMPILE_ENV;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_CUSTOM_DESC;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_CUSTOM_DESC_DIR;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_CUSTOM_ENV;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB_DESC;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB_DESC_DIR;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB_ENV;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_ISSM;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_ISSM_DESC;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_ISSM_DESC_DIR;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_ISSM_ENV;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS_DESC;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS_DESC_DIR;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS_ENV;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR_DESC;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR_DESC_DIR;
import static org.zephyrproject.ide.eclipse.core.ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR_ENV;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.equinox.bidi.StructuredTextTypeHandlerFactory;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.BidiUtils;
import org.eclipse.jface.wizard.WizardPage;
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
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.zephyrproject.ide.eclipse.core.ZephyrPlugin;
import org.zephyrproject.ide.eclipse.core.ZephyrStrings;
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
		GridData gridData;
		boolean zephyr = false;
		boolean xtools = false;
		boolean gnuarmemb = false;
		boolean issm = false;
		boolean crossCompile = false;
		boolean external = false;

		if (selection.equals(ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR_DESC)) {
			tcVariant.setText(ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR);
			tcVariant.setEnabled(false);
			zephyr = true;
		} else if (selection.equals(ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS_DESC)) {
			tcVariant.setText(ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS);
			tcVariant.setEnabled(false);
			xtools = true;
		} else if (selection.equals(ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB_DESC)) {
			tcVariant.setText(ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB);
			tcVariant.setEnabled(false);
			gnuarmemb = true;
		} else if (selection.equals(ZEPHYR_TOOLCHAIN_VARIANT_ISSM_DESC)) {
			tcVariant.setText(ZEPHYR_TOOLCHAIN_VARIANT_ISSM);
			tcVariant.setEnabled(false);
			issm = true;
		} else if (selection
				.equals(ZEPHYR_TOOLCHAIN_VARIANT_CROSS_COMPILE_DESC)) {
			tcVariant.setText(ZEPHYR_TOOLCHAIN_VARIANT_CROSS_COMPILE);
			tcVariant.setEnabled(false);
			crossCompile = true;
		} else if (selection.equals(ZEPHYR_TOOLCHAIN_VARIANT_CUSTOM_DESC)) {
			tcVariant.setText(ZephyrStrings.EMPTY_STRING);
			tcVariant.setEnabled(true);
			external = true;
		}

		grpZephyr.setVisible(zephyr);
		gridData = (GridData) grpZephyr.getLayoutData();
		gridData.exclude = !zephyr;

		grpXTools.setVisible(xtools);
		gridData = (GridData) grpXTools.getLayoutData();
		gridData.exclude = !xtools;

		grpGnuArmEmb.setVisible(gnuarmemb);
		gridData = (GridData) grpGnuArmEmb.getLayoutData();
		gridData.exclude = !gnuarmemb;

		grpISSM.setVisible(issm);
		gridData = (GridData) grpISSM.getLayoutData();
		gridData.exclude = !issm;

		grpCrossCompile.setVisible(crossCompile);
		gridData = (GridData) grpCrossCompile.getLayoutData();
		gridData.exclude = !crossCompile;

		grpCustom.setVisible(external);
		gridData = (GridData) grpCustom.getLayoutData();
		gridData.exclude = !external;

		composite.layout(true, true);

		setPageComplete(validatePage());
	}

	/**
	 * Create a composite to group various controls for easier manipulation.
	 *
	 * @return A composite
	 */
	private Composite createOneControlGroup() {
		Composite grp = new Composite(composite, SWT.NULL);

		grp.setFont(composite.getFont());

		GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
		gridData.horizontalSpan = 2;
		grp.setLayoutData(gridData);

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

		createLabel(grp, ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR_DESC_DIR + " (" //$NON-NLS-1$
				+ ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR_ENV + "):"); //$NON-NLS-1$

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

		createLabel(grp, ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS_DESC_DIR + " (" //$NON-NLS-1$
				+ ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS_ENV + "):"); //$NON-NLS-1$

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

		createLabel(grp, ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB_DESC_DIR + " (" //$NON-NLS-1$
				+ ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB_ENV + "):"); //$NON-NLS-1$

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

		createLabel(grp, ZEPHYR_TOOLCHAIN_VARIANT_ISSM_DESC_DIR + " (" //$NON-NLS-1$
				+ ZEPHYR_TOOLCHAIN_VARIANT_ISSM_ENV + "):"); //$NON-NLS-1$

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

		createLabel(grp, ZEPHYR_TOOLCHAIN_VARIANT_CROSS_COMPILE_DESC_PREFIX
				+ " (" + ZEPHYR_TOOLCHAIN_VARIANT_CROSS_COMPILE_ENV + "):"); //$NON-NLS-1$ //$NON-NLS-2$

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

		createLabel(grp, ZEPHYR_TOOLCHAIN_VARIANT_CUSTOM_DESC_DIR + " (" //$NON-NLS-1$
				+ ZEPHYR_TOOLCHAIN_VARIANT_CUSTOM_ENV + "):"); //$NON-NLS-1$

		customRoot = createTextField(grp);

		createBrowseBtn(grp, customRoot);
	};

	@Override
	public void createControl(Composite parent) {
		GridData gridData;
		composite = new Composite(parent, SWT.NULL);

		initializeDialogUnits(parent);

		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		/* Label: "Toolchain Variant" */
		Label tcLabel = new Label(composite, SWT.NONE);
		gridData = new GridData();
		tcLabel.setLayoutData(gridData);
		tcLabel.setText("Toolchain Variant:");
		tcLabel.setFont(composite.getFont());

		/* Combo box for toolchain list */
		tcSelection = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
		gridData = new GridData();
		tcSelection.setLayoutData(gridData);
		tcSelection.setFont(composite.getFont());
		tcSelection.setItems(ZEPHYR_TOOLCHAIN_DESC_LIST);
		tcSelection.select(0);
		tcSelection.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				handleToolchainSelection();
			}
		});

		/* Label and Text for ZEPHYR_TOOLCHAIN_VARIANT */
		Composite grpVariant = createOneControlGroup();

		Label tcVariantLabel = new Label(grpVariant, SWT.NONE);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		tcVariantLabel.setLayoutData(gridData);
		tcVariantLabel.setText(ZEPHYR_TOOLCHAIN_VARIANT + " ="); //$NON-NLS-1$
		tcVariantLabel.setFont(composite.getFont());

		tcVariant = new Text(grpVariant, SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		tcVariant.setLayoutData(gridData);
		tcVariant.setFont(composite.getFont());

		/* Create various groups for toolchains */
		grpZephyr = createOneControlGroup();
		createGroupZephyrSDK();

		grpXTools = createOneControlGroup();
		createGroupXtools();

		grpGnuArmEmb = createOneControlGroup();
		createGroupGnuArmEmb();

		grpISSM = createOneControlGroup();
		createGroupISSM();

		grpCrossCompile = createOneControlGroup();
		createGroupCrossCompile();

		grpCustom = createOneControlGroup();
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

		if (selection.equals(ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR_DESC)) {
			String dir = zephyrSdkInstallDir.getText();

			valid = ZephyrHelpers.checkValidZephyrSdkInstallDir(dir);

			if (dir.isEmpty()) {
				setErrorMessage(null);
				setMessage(ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR_DESC_DIR
						+ " must be specified");
			} else {
				setErrorMessage(ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR_DESC_DIR
						+ " is not valid");
				setMessage(null);
			}
		} else if (selection.equals(ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS_DESC)) {
			String dir = xtoolsDir.getText();

			valid = ZephyrHelpers.checkValidXToolsDirectory(dir);

			if (dir.isEmpty()) {
				setErrorMessage(null);
				setMessage(ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS_DESC_DIR
						+ " must be specified");
			} else {
				setErrorMessage(ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS_DESC_DIR
						+ " is not valid");
				setMessage(null);
			}
		} else if (selection.equals(ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB_DESC)) {
			String dir = gnuArmEmbDir.getText();

			valid = ZephyrHelpers.checkValidDirectory(dir);

			if (dir.isEmpty()) {
				setErrorMessage(null);
				setMessage(ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB_DESC_DIR
						+ " must be specified");
			} else {
				setErrorMessage(ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB_DESC_DIR
						+ " is not valid");
				setMessage(null);
			}
		} else if (selection.equals(ZEPHYR_TOOLCHAIN_VARIANT_ISSM_DESC)) {
			String dir = issmDir.getText();

			valid = ZephyrHelpers.checkValidDirectory(dir);

			if (dir.isEmpty()) {
				setErrorMessage(null);
				setMessage(ZEPHYR_TOOLCHAIN_VARIANT_ISSM_DESC_DIR
						+ " must be specified");
			} else {
				setErrorMessage(ZEPHYR_TOOLCHAIN_VARIANT_ISSM_DESC_DIR
						+ " is not valid");
				setMessage(null);
			}
		} else if (selection
				.equals(ZEPHYR_TOOLCHAIN_VARIANT_CROSS_COMPILE_DESC)) {
			String prefix = crossCompilePrefix.getText();
			if (!prefix.isEmpty()) {
				valid = true;
			} else {
				setErrorMessage(
						ZEPHYR_TOOLCHAIN_VARIANT_CROSS_COMPILE_DESC_PREFIX
								+ " must be specified");
				setMessage(null);
			}
		} else if (selection.equals(ZEPHYR_TOOLCHAIN_VARIANT_CUSTOM_DESC)) {
			String variant = tcVariant.toString();
			if (!variant.isEmpty()) {
				String tcRoot = customRoot.getText();
				if (!tcRoot.isEmpty()) {
					valid = true;
				} else {
					setErrorMessage(ZEPHYR_TOOLCHAIN_VARIANT_CUSTOM_DESC_DIR
							+ " must be specified");
					setMessage(null);
				}
			} else {
				setErrorMessage(
						ZEPHYR_TOOLCHAIN_VARIANT + " must be specified");
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
		ScopedPreferenceStore pStore = new ScopedPreferenceStore(
				new ProjectScope(project), ZephyrPlugin.PLUGIN_ID);

		String variant = tcVariant.getText();
		pStore.putValue(ZEPHYR_TOOLCHAIN_VARIANT, variant);

		if (variant.equals(ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR)) {
			String dir = zephyrSdkInstallDir.getText();
			pStore.putValue(ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR_ENV, dir);
		} else if (variant.equals(ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS)) {
			String dir = xtoolsDir.getText();
			pStore.putValue(ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS_ENV, dir);
		} else if (variant.equals(ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB)) {
			String dir = gnuArmEmbDir.getText();
			pStore.putValue(ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB_ENV, dir);
		} else if (variant.equals(ZEPHYR_TOOLCHAIN_VARIANT_ISSM)) {
			String dir = issmDir.getText();
			pStore.putValue(ZEPHYR_TOOLCHAIN_VARIANT_ISSM_ENV, dir);
		} else if (variant.equals(ZEPHYR_TOOLCHAIN_VARIANT_CROSS_COMPILE)) {
			String prefix = crossCompilePrefix.getText();
			pStore.putValue(ZEPHYR_TOOLCHAIN_VARIANT_CROSS_COMPILE_ENV, prefix);
		} else {
			String tcRoot = customRoot.getText();
			pStore.putValue(ZEPHYR_TOOLCHAIN_VARIANT_CUSTOM_ENV, tcRoot);
		}

		pStore.save();
	}
}
