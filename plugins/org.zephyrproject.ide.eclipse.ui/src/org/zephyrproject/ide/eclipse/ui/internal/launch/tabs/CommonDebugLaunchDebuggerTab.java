/*
 * Copyright (c) 2019-2020 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.ui.internal.launch.tabs;

import org.eclipse.cdt.core.build.IToolChain;
import org.eclipse.cdt.debug.gdbjtag.core.IGDBJtagConstants;
import org.eclipse.cdt.debug.gdbjtag.ui.GDBJtagDSFDebuggerTab;
import org.eclipse.cdt.dsf.gdb.IGDBLaunchConfigurationConstants;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.zephyrproject.ide.eclipse.core.build.ZephyrApplicationBuildConfiguration;
import org.zephyrproject.ide.eclipse.core.build.toolchain.ZephyrGCCToolChain;
import org.zephyrproject.ide.eclipse.core.internal.ZephyrHelpers;
import org.zephyrproject.ide.eclipse.ui.internal.debug.JTagDeviceDesc;

public abstract class CommonDebugLaunchDebuggerTab
		extends GDBJtagDSFDebuggerTab {

	protected String defaultJtagDevice = JTagDeviceDesc.GENERIC_TCPIP_DEVICE;
	protected String defaultHost = JTagDeviceDesc.IP_ADDR_LOCALHOST;
	protected Integer defaultPort = 10000;
	protected String defaultConnection = null;

	protected JTagDeviceDesc[] jtagDevices = new JTagDeviceDesc[] {

		/* Emulator targets */

		new JTagDeviceDesc("arc-nsim", //$NON-NLS-1$
				"org.zephyrproject.ide.eclipse.core.debug.jtagdevice.arcnSimDevice", //$NON-NLS-1$
				JTagDeviceDesc.IP_ADDR_LOCALHOST, 3333),
		new JTagDeviceDesc("qemu", //$NON-NLS-1$
				"org.zephyrproject.ide.eclipse.core.debug.jtagdevice.qemuDevice", //$NON-NLS-1$
				JTagDeviceDesc.IP_ADDR_LOCALHOST, 1234),

		/* Hardware targets */

		new JTagDeviceDesc("blackmagicprobe", //$NON-NLS-1$
				"org.zephyrproject.ide.eclipse.core.debug.jtagdevice.blackMagicProbe", //$NON-NLS-1$
				"/dev/ttyACM0"), //$NON-NLS-1$
		new JTagDeviceDesc("intel_s1000", //$NON-NLS-1$
				"org.zephyrproject.ide.eclipse.core.debug.jtagdevice.xtOCDDevice", //$NON-NLS-1$
				JTagDeviceDesc.IP_ADDR_LOCALHOST, 20000),
		new JTagDeviceDesc("JLink", //$NON-NLS-1$
				"org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.SeggerJLink", //$NON-NLS-1$
				JTagDeviceDesc.IP_ADDR_LOCALHOST, 2331),
		new JTagDeviceDesc("nios2", //$NON-NLS-1$
				"org.zephyrproject.ide.eclipse.core.debug.jtagdevice.nios2Device", //$NON-NLS-1$
				JTagDeviceDesc.IP_ADDR_LOCALHOST, 3333),
		new JTagDeviceDesc("openocd", //$NON-NLS-1$
				"org.eclipse.cdt.debug.gdbjtag.core.jtagdevice.OpenOCDSocket", //$NON-NLS-1$
				JTagDeviceDesc.IP_ADDR_LOCALHOST, 3333),
		new JTagDeviceDesc("openipc", //$NON-NLS-1$
				"org.zephyrproject.ide.eclipse.core.debug.jtagdevice.openIPCDevice", //$NON-NLS-1$
				JTagDeviceDesc.IP_ADDR_LOCALHOST, 8086),
	};

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		super.setDefaults(configuration);

		configuration.setAttribute(IGDBJtagConstants.ATTR_USE_REMOTE_TARGET,
				true);

		String device = defaultJtagDevice;
		String connection = defaultConnection;
		String ipAddr = defaultHost;
		Integer ipPort = defaultPort;

		/* Grab GDB path and runner from CMake */
		try {
			IResource[] resources = configuration.getMappedResources();
			if ((resources != null) && (resources.length > 0)) {
				IProject project = resources[0].getProject();
				ZephyrApplicationBuildConfiguration appBuildCfg =
						ZephyrHelpers.Launch
								.getZephyrBuildConfiguration(project);

				IToolChain toolchain = appBuildCfg.getToolChain();
				if (toolchain instanceof ZephyrGCCToolChain) {
					ZephyrGCCToolChain zToolChain =
							((ZephyrGCCToolChain) toolchain);

					String gdb = zToolChain.getGdbProgramPath();
					if ((gdb != null) && !gdb.trim().isEmpty()) {
						configuration.setAttribute(
								IGDBLaunchConfigurationConstants.ATTR_DEBUG_NAME,
								gdb);
					}

					/* Setup reasonable defaults for connection */
					String runner = zToolChain.getDebugRunner();
					if (runner != null) {
						for (JTagDeviceDesc jtag : jtagDevices) {
							if (runner.equals(jtag.id)) {
								device = jtag.jtag;

								if (jtag.connection != null) {
									connection = jtag.connection;
								} else {
									ipAddr = jtag.host;
									ipPort = jtag.port;
								}

								break;
							}
						}

					}
				}
			}
		} catch (CoreException e) {
		}

		configuration.setAttribute(IGDBJtagConstants.ATTR_JTAG_DEVICE_ID,
				device);
		if (connection != null) {
			configuration.setAttribute(IGDBJtagConstants.ATTR_CONNECTION,
					connection);
		} else {
			configuration.setAttribute(IGDBJtagConstants.ATTR_IP_ADDRESS,
					ipAddr);

			configuration.setAttribute(IGDBJtagConstants.ATTR_PORT_NUMBER,
					ipPort);
		}
	}

}
