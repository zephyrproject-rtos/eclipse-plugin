package org.zephyrproject.ide.eclipse.ui.property;

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
import java.util.Arrays;

import org.eclipse.core.resources.IProject;
import org.eclipse.equinox.bidi.StructuredTextTypeHandlerFactory;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.util.BidiUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.zephyrproject.ide.eclipse.core.ZephyrConstants;
import org.zephyrproject.ide.eclipse.core.ZephyrStrings;
import org.zephyrproject.ide.eclipse.core.internal.ZephyrHelpers;

public class ZephyrApplicationToolchainPropertyPage extends PropertyPage
		implements IWorkbenchPropertyPage {

	private Combo tcSelection;

	private Composite tcParams;

	private StackLayout tcParamsLayout;

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

	/**
	 * Listener to determine whether this page has valid data to move forward.
	 */
	private final class fieldModifyListener implements Listener {
		@Override
		public void handleEvent(Event event) {
			updateApplyButton();
		}
	};

	private final fieldModifyListener modifyListener =
			new fieldModifyListener();

	public ZephyrApplicationToolchainPropertyPage() {
	}

	@Override
	protected Control createContents(Composite parent) {
		IProject project = getElement().getAdapter(IProject.class);
		ScopedPreferenceStore pStore =
				ZephyrHelpers.getProjectPreferenceStore(project);
		noDefaultButton();

		GridData gridData;
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		/* Group for toolchain selection */
		Composite grpSelection = new Composite(composite, SWT.NULL);
		grpSelection.setLayout(new GridLayout(2, false));
		grpSelection.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		/* Label: "Toolchain Variant" */
		Label tcLabel = new Label(grpSelection, SWT.NONE);
		gridData = new GridData();
		tcLabel.setLayoutData(gridData);
		tcLabel.setText("Toolchain Variant:");
		tcLabel.setFont(grpSelection.getFont());

		/* Combo box for toolchain list */
		tcSelection = new Combo(grpSelection, SWT.BORDER | SWT.READ_ONLY);
		gridData = new GridData();
		tcSelection.setLayoutData(gridData);
		tcSelection.setFont(grpSelection.getFont());
		tcSelection.setItems(ZephyrConstants.ZEPHYR_TOOLCHAIN_DESC_LIST);
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
		tcVariantLabel.setText(ZEPHYR_TOOLCHAIN_VARIANT + " ="); //$NON-NLS-1$
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

		/* Selection the toolchain from project */
		String storedTcVariant =
				pStore.getString(ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT);
		tcVariant.setText(storedTcVariant);
		String tcDesc;
		if (storedTcVariant
				.equals(ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR)) {
			tcDesc = ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR_DESC;
		} else if (storedTcVariant
				.equals(ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS)) {
			tcDesc = ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS_DESC;
		} else if (storedTcVariant
				.equals(ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB)) {
			tcDesc = ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB_DESC;
		} else if (storedTcVariant
				.equals(ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_ISSM)) {
			tcDesc = ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_ISSM_DESC;
		} else if (storedTcVariant.equals(
				ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_CROSS_COMPILE)) {
			tcDesc = ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_CROSS_COMPILE_DESC;
		} else {
			tcDesc = ZephyrConstants.ZEPHYR_TOOLCHAIN_VARIANT_CUSTOM_DESC;
		}
		int selection =
				Arrays.asList(ZephyrConstants.ZEPHYR_TOOLCHAIN_DESC_LIST)
						.indexOf(tcDesc);
		tcSelection.select(selection);

		/* Create the toolchain parameter controls */
		tcParams = new Composite(composite, SWT.NO_TRIM | SWT.NO_FOCUS);
		tcParamsLayout = new StackLayout();
		tcParams.setLayout(tcParamsLayout);
		tcParams.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		/* Parameters for Zephyr SDK */
		grpZephyr = createOneControlGroup(tcParams);
		createGroupZephyrSDK(pStore);

		grpXTools = createOneControlGroup(tcParams);
		createGroupXtools(pStore);

		grpGnuArmEmb = createOneControlGroup(tcParams);
		createGroupGnuArmEmb(pStore);

		grpISSM = createOneControlGroup(tcParams);
		createGroupISSM(pStore);

		grpCrossCompile = createOneControlGroup(tcParams);
		createGroupCrossCompile(pStore);

		grpCustom = createOneControlGroup(tcParams);
		createGroupCustom(pStore);

		handleToolchainSelection();

		return composite;
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
	private void createGroupZephyrSDK(ScopedPreferenceStore pStore) {
		Composite grp = grpZephyr;

		createLabel(grp, ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR_DESC_DIR + " (" //$NON-NLS-1$
				+ ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR_ENV + "):"); //$NON-NLS-1$

		zephyrSdkInstallDir = createDirField(grp);

		zephyrSdkInstallDir
				.setText(pStore.getString(ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR_ENV));

		createBrowseBtn(grp, zephyrSdkInstallDir);
	};

	/**
	 * Create a group for Crosstool-NG toolchain.
	 */
	private void createGroupXtools(ScopedPreferenceStore pStore) {
		Composite grp = grpXTools;

		createLabel(grp, ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS_DESC_DIR + " (" //$NON-NLS-1$
				+ ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS_ENV + "):"); //$NON-NLS-1$

		xtoolsDir = createDirField(grp);

		xtoolsDir
				.setText(pStore.getString(ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS_ENV));

		createBrowseBtn(grp, xtoolsDir);
	};

	/**
	 * Create a group for GNU ARM Embedded toolchain.
	 */
	private void createGroupGnuArmEmb(ScopedPreferenceStore pStore) {
		Composite grp = grpGnuArmEmb;

		createLabel(grp, ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB_DESC_DIR + " (" //$NON-NLS-1$
				+ ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB_ENV + "):"); //$NON-NLS-1$

		gnuArmEmbDir = createDirField(grp);

		gnuArmEmbDir.setText(
				pStore.getString(ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB_ENV));

		createBrowseBtn(grp, gnuArmEmbDir);
	};

	/**
	 * Create a group for Intel System Studio for Microcontrollers toolchain.
	 */
	private void createGroupISSM(ScopedPreferenceStore pStore) {
		Composite grp = grpISSM;

		createLabel(grp, ZEPHYR_TOOLCHAIN_VARIANT_ISSM_DESC_DIR + " (" //$NON-NLS-1$
				+ ZEPHYR_TOOLCHAIN_VARIANT_ISSM_ENV + "):"); //$NON-NLS-1$

		issmDir = createDirField(grp);

		issmDir.setText(pStore.getString(ZEPHYR_TOOLCHAIN_VARIANT_ISSM_ENV));

		createBrowseBtn(grp, issmDir);
	};

	/**
	 * Create a group for specifying CROSS_COMPILE toolchain.
	 */
	private void createGroupCrossCompile(ScopedPreferenceStore pStore) {
		Composite grp = grpCrossCompile;

		createLabel(grp, ZEPHYR_TOOLCHAIN_VARIANT_CROSS_COMPILE_DESC_PREFIX
				+ " (" + ZEPHYR_TOOLCHAIN_VARIANT_CROSS_COMPILE_ENV + "):"); //$NON-NLS-1$ //$NON-NLS-2$

		crossCompilePrefix = createTextField(grp);

		crossCompilePrefix.setText(
				pStore.getString(ZEPHYR_TOOLCHAIN_VARIANT_CROSS_COMPILE_ENV));

		createBrowseBtn(grp, crossCompilePrefix);
	};

	/**
	 * Create a group for specifying custom CMake toolchain.
	 */
	private void createGroupCustom(ScopedPreferenceStore pStore) {
		Composite grp = grpCustom;

		createLabel(grp, ZEPHYR_TOOLCHAIN_VARIANT_CUSTOM_DESC_DIR + " (" //$NON-NLS-1$
				+ ZEPHYR_TOOLCHAIN_VARIANT_CUSTOM_ENV + "):"); //$NON-NLS-1$

		customRoot = createTextField(grp);

		createBrowseBtn(grp, customRoot);
	};

	/**
	 * Handle the drop-down box selection of toolchain variant.
	 *
	 * This changes the visibilities of the controls according to variant
	 * selection. Only the necessary controls related to a particular toolchain
	 * variant are visible.
	 */
	private void handleToolchainSelection() {
		String selection = tcSelection.getText();

		if (selection.equals(ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR_DESC)) {
			tcVariant.setText(ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR);
			tcVariant.setEnabled(false);
			tcParamsLayout.topControl = grpZephyr;
		} else if (selection.equals(ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS_DESC)) {
			tcVariant.setText(ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS);
			tcVariant.setEnabled(false);
			tcParamsLayout.topControl = grpXTools;
		} else if (selection.equals(ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB_DESC)) {
			tcVariant.setText(ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB);
			tcVariant.setEnabled(false);
			tcParamsLayout.topControl = grpGnuArmEmb;
		} else if (selection.equals(ZEPHYR_TOOLCHAIN_VARIANT_ISSM_DESC)) {
			tcVariant.setText(ZEPHYR_TOOLCHAIN_VARIANT_ISSM);
			tcVariant.setEnabled(false);
			tcParamsLayout.topControl = grpISSM;
		} else if (selection
				.equals(ZEPHYR_TOOLCHAIN_VARIANT_CROSS_COMPILE_DESC)) {
			tcVariant.setText(ZEPHYR_TOOLCHAIN_VARIANT_CROSS_COMPILE);
			tcVariant.setEnabled(false);
			tcParamsLayout.topControl = grpCrossCompile;
		} else if (selection.equals(ZEPHYR_TOOLCHAIN_VARIANT_CUSTOM_DESC)) {
			tcVariant.setText(ZephyrStrings.EMPTY_STRING);
			tcVariant.setEnabled(true);
			tcParamsLayout.topControl = grpCustom;
		}

		tcParams.layout();

		updateApplyButton();
	}

	@Override
	public boolean isValid() {
		String selection = tcSelection.getText();
		boolean valid = false;

		if (selection.equals(ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR_DESC)) {
			String dir = zephyrSdkInstallDir.getText();

			valid = ZephyrHelpers.checkValidZephyrSdkInstallDir(dir);

			if (dir.isEmpty()) {
				setErrorMessage(ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR_DESC_DIR
						+ " must be specified");
			} else if (!valid) {
				setErrorMessage(ZEPHYR_TOOLCHAIN_VARIANT_ZEPHYR_DESC_DIR
						+ " is not valid");
			}
		} else if (selection.equals(ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS_DESC)) {
			String dir = xtoolsDir.getText();

			valid = ZephyrHelpers.checkValidXToolsDirectory(dir);

			if (dir.isEmpty()) {
				setErrorMessage(ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS_DESC_DIR
						+ " must be specified");
			} else if (!valid) {
				setErrorMessage(ZEPHYR_TOOLCHAIN_VARIANT_XTOOLS_DESC_DIR
						+ " is not valid");
			}
		} else if (selection.equals(ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB_DESC)) {
			String dir = gnuArmEmbDir.getText();

			valid = ZephyrHelpers.checkValidDirectory(dir);

			if (dir.isEmpty()) {
				setErrorMessage(ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB_DESC_DIR
						+ " must be specified");
			} else if (!valid) {
				setErrorMessage(ZEPHYR_TOOLCHAIN_VARIANT_GNUARMEMB_DESC_DIR
						+ " is not valid");
			}
		} else if (selection.equals(ZEPHYR_TOOLCHAIN_VARIANT_ISSM_DESC)) {
			String dir = issmDir.getText();

			valid = ZephyrHelpers.checkValidDirectory(dir);

			if (dir.isEmpty()) {
				setErrorMessage(ZEPHYR_TOOLCHAIN_VARIANT_ISSM_DESC_DIR
						+ " must be specified");
			} else if (!valid) {
				setErrorMessage(ZEPHYR_TOOLCHAIN_VARIANT_ISSM_DESC_DIR
						+ " is not valid");
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
				}
			} else {
				setErrorMessage(
						ZEPHYR_TOOLCHAIN_VARIANT + " must be specified");
			}
		}

		if (valid) {
			setErrorMessage(null);
		}

		return valid;
	}

	@Override
	protected void performApply() {
		IProject project = getElement().getAdapter(IProject.class);
		ScopedPreferenceStore pStore =
				ZephyrHelpers.getProjectPreferenceStore(project);

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

		try {
			pStore.save();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

}
