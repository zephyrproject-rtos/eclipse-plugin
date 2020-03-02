package org.zephyrproject.ide.eclipse.ui.property;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.zephyrproject.ide.eclipse.core.internal.ZephyrHelpers;
import org.zephyrproject.ide.eclipse.core.preferences.ZephyrProjectPreferences;

public class ZephyrApplicationTopPropertyPage extends PropertyPage
		implements IWorkbenchPropertyPage {

	private Text boardNameText;

	public ZephyrApplicationTopPropertyPage() {
	}

	@Override
	protected Control createContents(Composite parent) {
		IProject project = getElement().getAdapter(IProject.class);

		noDefaultButton();

		GridData gridData;
		Composite composite = new Composite(parent, SWT.NONE);

		/* Create a grid with 2 columns */
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		/* Show board name */
		Label boardNameLabel = new Label(composite, SWT.NONE);
		gridData = new GridData();
		boardNameLabel.setLayoutData(gridData);
		boardNameLabel.setText("Board Name:"); //$NON-NLS-1$

		/* Input field for board name */
		boardNameText = new Text(composite, SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
		boardNameText.setLayoutData(gridData);
		boardNameText.setFont(parent.getFont());
		boardNameText.setText(ZephyrHelpers.getBoardName(project));
		boardNameText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				updateApplyButton();
			}
		});

		return composite;
	}

	@Override
	public boolean isValid() {
		boolean valid = true;

		if (boardNameText.getText().trim().isEmpty()) {
			valid = false;
			setErrorMessage("Board name must be specified"); //$NON-NLS-1$
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

		pStore.setValue(ZephyrProjectPreferences.BOARD,
				boardNameText.getText());

		try {
			pStore.save();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean performOk() {
		performApply();
		return true;
	}

}
