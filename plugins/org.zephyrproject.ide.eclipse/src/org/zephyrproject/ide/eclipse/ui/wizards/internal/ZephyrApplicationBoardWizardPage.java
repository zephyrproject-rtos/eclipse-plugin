/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.ui.wizards.internal;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.zephyrproject.ide.eclipse.core.internal.ZephyrHelpers;
import org.zephyrproject.ide.eclipse.core.preferences.ZephyrProjectPreferences;
import org.zephyrproject.ide.eclipse.core.preferences.ZephyrProjectPreferences.ZephyrBase;

/**
 * Wizard page to specify which board to build for.
 */
public class ZephyrApplicationBoardWizardPage extends WizardPage {

	private Text boardText;

	private Button useListsBtn;

	private List archList;

	private List boardList;

	private ZephyrApplicationMainWizardPage mainPage;

	private HashMap<String, String[]> boardConfigs;

	/**
	 * Runnable to fetch board names from Zephyr Base directory.
	 */
	private class FetchBoardConfigs implements IRunnableWithProgress {

		private ArrayList<String> explorePath(File dir, SubMonitor monitor) {
			ArrayList<String> boards = new ArrayList<>();

			File[] entries = dir.listFiles();

			SubMonitor subMon = monitor.newChild(entries.length);

			for (File f : entries) {
				if (f.isDirectory()) {
					boards.addAll(explorePath(f, monitor));
				} else if (f.isFile()) {
					String name = f.getName();
					if (name.toLowerCase().endsWith(".yaml")) { //$NON-NLS-1$
						int idx = name.lastIndexOf(".");
						boards.add(name.substring(0, idx));
					}
				}
				subMon.worked(1);
			}

			subMon.done();

			return boards;
		}

		/**
		 * Gather board names.
		 *
		 * @param monitor Progress monitor
		 */
		@Override
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			/* Check if ZEPHYR_BASE is valid */
			String zBase = mainPage.getZephyrBaseLocation();
			if (!ZephyrHelpers.checkValidZephyrBase(zBase)) {
				setErrorMessage(
						ZephyrBase.DIRECTORY_DESCRIPTION + " is not valid");
				return;
			}

			/* Check if "boards/" exists */
			File zBaseBoards = new File(zBase, "boards"); //$NON-NLS-1$
			if (!zBaseBoards.isDirectory()) {
				setErrorMessage(
						ZephyrBase.DIRECTORY_DESCRIPTION + " is not valid");
				return;
			}

			setErrorMessage(null);

			SubMonitor monArch = SubMonitor.convert(monitor);

			File[] archFiles = zBaseBoards.listFiles();
			monArch.beginTask("Gathering board configurations",
					archFiles.length);
			for (File oneArch : archFiles) {
				if (oneArch.isDirectory()) {
					ArrayList<String> bList = explorePath(oneArch, monArch);
					if (!bList.isEmpty()) {
						boardConfigs.put(oneArch.getName(),
								bList.toArray(new String[0]));
					}
				}
			}
			monArch.done();

			String[] archs = boardConfigs.keySet().toArray(new String[0]);
			Arrays.sort(archs);
			archList.setItems(archs);
		}

	}

	/**
	 * Listener to determine whether this page has valid data to move forward.
	 */
	private class zBoardTextModifyListener implements Listener {
		@Override
		public void handleEvent(Event event) {
			setPageComplete(validatePage());
		}
	};

	public ZephyrApplicationBoardWizardPage(String pageName,
			ZephyrApplicationMainWizardPage main) {
		super(pageName);
		setPageComplete(false);

		this.mainPage = main;
		this.boardConfigs = new HashMap<>(10);
	}

	@Override
	public void createControl(Composite parent) {
		GridData gridData;
		Composite composite = new Composite(parent, SWT.NULL);

		initializeDialogUnits(parent);

		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		composite.setFont(parent.getFont());

		/* Label: "Board" */
		Label boardLabel = new Label(composite, SWT.NONE);
		gridData = new GridData();
		boardLabel.setLayoutData(gridData);
		boardLabel.setText("Board Name:");
		boardLabel.setFont(parent.getFont());

		/* Input field for board name */
		boardText = new Text(composite, SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		boardText.setLayoutData(gridData);
		boardText.setFont(parent.getFont());
		boardText.addListener(SWT.Modify, new zBoardTextModifyListener());

		String prevBoard =
				getDialogSettings().get(ZephyrProjectPreferences.BOARD);
		if (prevBoard != null) {
			boardText.setText(prevBoard);
		}

		/* Checkbox to enable choosing from lists */
		useListsBtn = new Button(composite, SWT.CHECK | SWT.LEFT);
		useListsBtn.setText("Select from available board configurations under "
				+ ZephyrBase.DIRECTORY_DESCRIPTION);
		useListsBtn.setSelection(false);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		useListsBtn.setLayoutData(gridData);
		useListsBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				boolean sel = useListsBtn.getSelection();
				boardText.setEnabled(!sel);
				archList.setEnabled(sel);
				boardList.setEnabled(sel);

				if (sel && boardConfigs.isEmpty()) {
					try {
						getContainer().run(false, false,
								new FetchBoardConfigs());
					} catch (InvocationTargetException
							| InterruptedException e) {
						throw new RuntimeException(e);
					}
				} else {
					setErrorMessage(null);
				}
			}
		});

		createBoardLists(composite);

		setPageComplete(validatePage());

		setErrorMessage(null);
		setMessage(null);
		setControl(composite);
		Dialog.applyDialogFont(composite);
	}

	/**
	 * Create the UI lists for choosing board configurations.
	 *
	 * @param parent
	 */
	private void createBoardLists(Composite parent) {
		GridData gridData;

		Composite composite = new Composite(parent, SWT.NULL);
		composite.setFont(parent.getFont());

		gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.horizontalSpan = 2;
		composite.setLayoutData(gridData);

		GridLayout layout = new GridLayout(3, true);
		layout.marginWidth = 0;
		composite.setLayout(layout);

		/* Label: "Architecture" */
		Label archLabel = new Label(composite, SWT.NULL);
		gridData = new GridData();
		archLabel.setText("Architecture:");
		archLabel.setFont(parent.getFont());
		archLabel.setLayoutData(gridData);

		/* Label: "Board Configurations" */
		Label boardLabel = new Label(composite, SWT.NULL);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		boardLabel.setText("Board Configurations:");
		boardLabel.setFont(parent.getFont());
		boardLabel.setLayoutData(gridData);

		/* Control containing the list of architectures */
		archList = new List(composite, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
		gridData = new GridData(GridData.FILL_BOTH);
		archList.setLayoutData(gridData);
		archList.setEnabled(false);
		archList.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				handleArchSelection();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

		});

		/* Control containing the list of boards of a particular architecture */
		boardList = new List(composite, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
		gridData = new GridData(GridData.FILL_BOTH);
		gridData.horizontalSpan = 2;
		boardList.setLayoutData(gridData);
		boardList.setEnabled(false);
		boardList.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				String[] sel = boardList.getSelection();
				if (sel.length == 1) {
					boardText.setText(sel[0]);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

		});
	}

	private boolean validatePage() {
		return (!boardText.getText().isEmpty());
	}

	/**
	 * Handle selection in architecture list.
	 *
	 * This changes the list of board configurations depending on which
	 * architecture is selected.
	 */
	private void handleArchSelection() {
		if (boardConfigs.isEmpty()) {
			return;
		}

		boardList.removeAll();

		String[] archMultiSel = archList.getSelection();
		if (archMultiSel.length != 1) {
			return;
		}

		String archSel = archMultiSel[0];
		String[] boards = boardConfigs.get(archSel);
		Arrays.sort(boards);
		boardList.setItems(boards);
	}

	/**
	 * Perform actions associated with finishing the wizard.
	 *
	 * This saves the board name in the project preference store.
	 *
	 * @param project
	 * @throws IOException
	 */
	public void performFinish(IProject project) throws IOException {
		String board = boardText.getText();

		ScopedPreferenceStore pStore =
				ZephyrHelpers.getProjectPreferenceStore(project);

		pStore.putValue(ZephyrProjectPreferences.BOARD, board);

		pStore.save();

		getDialogSettings().put(ZephyrProjectPreferences.BOARD, board);
	}

	/**
	 * @return Board name specified
	 */
	public String getBoardName() {
		return boardText.getText();
	}
}
