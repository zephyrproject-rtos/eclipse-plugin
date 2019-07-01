package org.zephyrproject.ide.eclipse.ui.launchbar;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.launchbar.core.target.ILaunchTarget;
import org.eclipse.swt.graphics.Image;
import org.zephyrproject.ide.eclipse.core.ZephyrPlugin;
import org.zephyrproject.ide.eclipse.ui.ZephyrUIPlugin;

public class ZephyrApplicationLaunchBarTargetLabelProvider
		extends LabelProvider {

	@Override
	public Image getImage(Object element) {
		return ZephyrPlugin.getDefault().getImageRegistry()
				.get(ZephyrUIPlugin.IMG_ZEPHYR_KITE16);
	}

	@Override
	public String getText(Object element) {
		if (element instanceof ILaunchTarget) {
			return ((ILaunchTarget) element).getId();
		}
		return super.getText(element);
	}

}
