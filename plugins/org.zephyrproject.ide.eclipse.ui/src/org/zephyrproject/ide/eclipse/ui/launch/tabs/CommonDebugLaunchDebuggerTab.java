/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.ui.launch.tabs;

import org.eclipse.cdt.core.build.IToolChain;
import org.eclipse.cdt.debug.gdbjtag.core.IGDBJtagConstants;
import org.eclipse.cdt.debug.gdbjtag.ui.GDBJtagDSFDebuggerTab;
import org.eclipse.cdt.dsf.gdb.IGDBLaunchConfigurationConstants;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.zephyrproject.ide.eclipse.core.build.ZephyrApplicationBuildConfiguration;
import org.zephyrproject.ide.eclipse.core.build.ZephyrApplicationToolChain;
import org.zephyrproject.ide.eclipse.core.internal.ZephyrHelpers;

public class CommonDebugLaunchDebuggerTab extends GDBJtagDSFDebuggerTab {

	private static final String DEVICE_SERIAL = "Generic Serial"; //$NON-NLS-1$

	private static final String SERIAL_ACM0 = "/dev/ACM0"; //$NON-NLS-1$

	private static final String DEVICE_TCPIP = "Generic TCP/IP"; //$NON-NLS-1$

	private static final String LOCALHOST = "localhost"; //$NON-NLS-1$

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		super.setDefaults(configuration);

		configuration.setAttribute(IGDBJtagConstants.ATTR_USE_REMOTE_TARGET,
				true);
		configuration.setAttribute(IGDBJtagConstants.ATTR_JTAG_DEVICE,
				"Generic TCP/IP"); //$NON-NLS-1$
		configuration.setAttribute(IGDBJtagConstants.ATTR_IP_ADDRESS,
				"localhost"); //$NON-NLS-1$
		configuration.setAttribute(IGDBJtagConstants.ATTR_PORT_NUMBER, 1234);

		/* Default path GDB program according to CMake */
		try {
			IResource[] resources = configuration.getMappedResources();
			if ((resources != null) && (resources.length > 0)) {
				IProject project = resources[0].getProject();
				ZephyrApplicationBuildConfiguration appBuildCfg =
						ZephyrHelpers.Launch.getBuildConfiguration(project);

				IToolChain toolchain = appBuildCfg.getToolChain();
				if (toolchain instanceof ZephyrApplicationToolChain) {
					ZephyrApplicationToolChain zToolChain =
							((ZephyrApplicationToolChain) toolchain);

					String gdb = zToolChain.getGdbProgramPath();
					if ((gdb != null) && !gdb.trim().isEmpty()) {
						configuration.setAttribute(
								IGDBLaunchConfigurationConstants.ATTR_DEBUG_NAME,
								gdb);
					}

					/* Setup reasonable defaults for connection */
					String runner = zToolChain.getDebugRunner();
					if (runner != null) {
						String device = null;
						String connection = null;
						String ipAddr = null;
						Integer ipPort = null;

						if (runner.equals("arc-nsim") //$NON-NLS-1$
								|| runner.equals("em-starterkit")) { //$NON-NLS-1$
							device = DEVICE_TCPIP;
							ipAddr = LOCALHOST;
							ipPort = 3333;
						} else if (runner.equals("blackmagicprobe")) { //$NON-NLS-1$
							device = DEVICE_SERIAL; // $NON-NLS-1$
							connection = SERIAL_ACM0; // $NON-NLS-1$
						} else if (runner.equals("jlink")) { //$NON-NLS-1$
							device = DEVICE_TCPIP;
							ipAddr = LOCALHOST;
							ipPort = 2331;
						} else if (runner.equals("nios2")) { //$NON-NLS-1$
							device = DEVICE_TCPIP;
							ipAddr = LOCALHOST;
							ipPort = 3333;
						} else if (runner.equals("openocd") //$NON-NLS-1$
								|| runner.equals("pyocd")) { //$NON-NLS-1$
							device = DEVICE_TCPIP;
							ipAddr = LOCALHOST;
							ipPort = 3333;
						} else if (runner.equals("intel_s1000") //$NON-NLS-1$
								|| runner.equals("xtensa")) { //$NON-NLS-1$
							device = DEVICE_TCPIP;
							ipAddr = LOCALHOST;
							ipPort = 20000;
						} else {
							/* QEMU and others */
							device = DEVICE_TCPIP;
							ipAddr = LOCALHOST;
							ipPort = 1234;
						}

						if (device != null) {
							configuration.setAttribute(
									IGDBJtagConstants.ATTR_JTAG_DEVICE, device);
						}
						if (connection != null) {
							configuration.setAttribute(
									IGDBJtagConstants.ATTR_CONNECTION,
									connection);
						}
						if (ipAddr != null) {
							configuration.setAttribute(
									IGDBJtagConstants.ATTR_IP_ADDRESS, ipAddr);
						}
						if (ipPort != null) {
							configuration.setAttribute(
									IGDBJtagConstants.ATTR_PORT_NUMBER, ipPort);
						}
					}
				}
			}
		} catch (CoreException e) {
		}
	}

}
