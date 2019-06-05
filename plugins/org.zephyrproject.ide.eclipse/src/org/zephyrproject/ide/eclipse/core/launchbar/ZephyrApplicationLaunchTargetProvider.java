package org.zephyrproject.ide.eclipse.core.launchbar;

import org.eclipse.launchbar.core.target.ILaunchTarget;
import org.eclipse.launchbar.core.target.ILaunchTargetManager;
import org.eclipse.launchbar.core.target.ILaunchTargetProvider;
import org.eclipse.launchbar.core.target.ILaunchTargetWorkingCopy;
import org.eclipse.launchbar.core.target.TargetStatus;
import org.zephyrproject.ide.eclipse.core.ZephyrConstants.Launch;

public class ZephyrApplicationLaunchTargetProvider
		implements ILaunchTargetProvider {

	@Override
	public void init(ILaunchTargetManager targetManager) {
		if (targetManager.getLaunchTarget(Launch.LAUNCH_TARGET_EMULATOR_TYPE_ID,
				Launch.LAUNCH_TARGET_EMULATOR_NAME) == null) {
			ILaunchTarget target = targetManager.addLaunchTarget(
					Launch.LAUNCH_TARGET_EMULATOR_TYPE_ID,
					Launch.LAUNCH_TARGET_EMULATOR_NAME);
			ILaunchTargetWorkingCopy wc = target.getWorkingCopy();
			wc.setAttribute(ILaunchTarget.ATTR_OS, Launch.LAUNCH_TARGET_OS);
			wc.setAttribute(ILaunchTarget.ATTR_ARCH, Launch.LAUNCH_TARGET_ARCH);
			wc.save();
		}

		if (targetManager.getLaunchTarget(Launch.LAUNCH_TARGET_HARDWARE_TYPE_ID,
				Launch.LAUNCH_TARGET_HARDWARE_NAME) == null) {
			ILaunchTarget target = targetManager.addLaunchTarget(
					Launch.LAUNCH_TARGET_HARDWARE_TYPE_ID,
					Launch.LAUNCH_TARGET_HARDWARE_NAME);
			ILaunchTargetWorkingCopy wc = target.getWorkingCopy();
			wc.setAttribute(ILaunchTarget.ATTR_OS, Launch.LAUNCH_TARGET_OS);
			wc.setAttribute(ILaunchTarget.ATTR_ARCH, Launch.LAUNCH_TARGET_ARCH);
			wc.save();
		}
	}

	@Override
	public TargetStatus getStatus(ILaunchTarget arg0) {
		return TargetStatus.OK_STATUS;
	}

}
