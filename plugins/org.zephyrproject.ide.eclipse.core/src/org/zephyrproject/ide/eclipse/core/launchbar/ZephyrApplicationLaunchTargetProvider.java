package org.zephyrproject.ide.eclipse.core.launchbar;

import org.eclipse.launchbar.core.target.ILaunchTarget;
import org.eclipse.launchbar.core.target.ILaunchTargetManager;
import org.eclipse.launchbar.core.target.ILaunchTargetProvider;
import org.eclipse.launchbar.core.target.ILaunchTargetWorkingCopy;
import org.eclipse.launchbar.core.target.TargetStatus;
import org.zephyrproject.ide.eclipse.core.launch.ZephyrLaunchConstants;

public class ZephyrApplicationLaunchTargetProvider
		implements ILaunchTargetProvider {

	@Override
	public void init(ILaunchTargetManager targetManager) {
		if (targetManager.getLaunchTarget(
				ZephyrLaunchConstants.LAUNCH_TARGET_EMULATOR_RUN_TYPE_ID,
				ZephyrLaunchConstants.LAUNCH_TARGET_EMULATOR_RUN_NAME) == null) {
			ILaunchTarget target = targetManager.addLaunchTarget(
					ZephyrLaunchConstants.LAUNCH_TARGET_EMULATOR_RUN_TYPE_ID,
					ZephyrLaunchConstants.LAUNCH_TARGET_EMULATOR_RUN_NAME);
			ILaunchTargetWorkingCopy wc = target.getWorkingCopy();
			wc.setAttribute(ILaunchTarget.ATTR_OS,
					ZephyrLaunchConstants.LAUNCH_TARGET_OS);
			wc.setAttribute(ILaunchTarget.ATTR_ARCH,
					ZephyrLaunchConstants.LAUNCH_TARGET_ARCH);
			wc.save();
		}

		if (targetManager.getLaunchTarget(
				ZephyrLaunchConstants.LAUNCH_TARGET_HARDWARE_RUN_TYPE_ID,
				ZephyrLaunchConstants.LAUNCH_TARGET_HARDWARE_RUN_NAME) == null) {
			ILaunchTarget target = targetManager.addLaunchTarget(
					ZephyrLaunchConstants.LAUNCH_TARGET_HARDWARE_RUN_TYPE_ID,
					ZephyrLaunchConstants.LAUNCH_TARGET_HARDWARE_RUN_NAME);
			ILaunchTargetWorkingCopy wc = target.getWorkingCopy();
			wc.setAttribute(ILaunchTarget.ATTR_OS,
					ZephyrLaunchConstants.LAUNCH_TARGET_OS);
			wc.setAttribute(ILaunchTarget.ATTR_ARCH,
					ZephyrLaunchConstants.LAUNCH_TARGET_ARCH);
			wc.save();
		}
	}

	@Override
	public TargetStatus getStatus(ILaunchTarget arg0) {
		return TargetStatus.OK_STATUS;
	}

}
