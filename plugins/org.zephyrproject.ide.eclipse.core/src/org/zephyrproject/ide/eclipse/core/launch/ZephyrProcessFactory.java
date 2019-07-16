/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.launch;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.eclipse.cdt.dsf.gdb.IGdbDebugConstants;
import org.eclipse.cdt.dsf.gdb.launching.GDBProcess;
import org.eclipse.cdt.dsf.gdb.launching.InferiorRuntimeProcess;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.IProcessFactory;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.zephyrproject.ide.eclipse.core.ZephyrPlugin;
import org.zephyrproject.ide.eclipse.core.internal.ZephyrHelpers;

public class ZephyrProcessFactory implements IProcessFactory {

	public static final String ID = ZephyrPlugin.PLUGIN_ID + ".processFactory"; //$NON-NLS-1$

	private static Constructor<?> getRuntimeProcessConstructor() {
		Constructor<?> runtime = null;
		Class<?> runtimeClass = null;
		String className =
				String.format("%s.internal.launch.%s.ZephyrRuntimeProcess", //$NON-NLS-1$
						ZephyrPlugin.PLUGIN_ID, Platform.getOS());
		try {
			ClassLoader loader = ZephyrHelpers.class.getClassLoader();
			runtimeClass = loader.loadClass(className);
		} catch (ClassNotFoundException cnfe) {
			runtimeClass = null;
		}

		if (runtimeClass != null) {
			try {
				/* Grab the constructor */
				Class<?> args[] = new Class[4];
				args[0] = ILaunch.class;
				args[1] = Process.class;
				args[2] = String.class;
				args[3] = Map.class;

				runtime = runtimeClass.getConstructor(args);
			} catch (NoSuchMethodException | SecurityException e) {
				runtime = null;
			}
		}

		return runtime;
	}

	@Override
	public IProcess newProcess(ILaunch launch, Process process, String label,
			Map<String, String> attributes) {
		if (attributes != null) {
			if (IGdbDebugConstants.GDB_PROCESS_CREATION_VALUE.equals(attributes
					.get(IGdbDebugConstants.PROCESS_TYPE_CREATION_ATTR))) {
				return new GDBProcess(launch, process, label, attributes);
			}

			if (IGdbDebugConstants.INFERIOR_PROCESS_CREATION_VALUE
					.equals(attributes.get(
							IGdbDebugConstants.PROCESS_TYPE_CREATION_ATTR))) {
				return new InferiorRuntimeProcess(launch, process, label,
						attributes);
			}
		}

		Constructor<?> runtime = getRuntimeProcessConstructor();
		IProcess iproc = null;
		if (runtime != null) {
			try {
				iproc = (IProcess) runtime.newInstance(launch, process, label,
						attributes);
			} catch (InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException e) {
				iproc = null;
			}
		}

		if (iproc == null) {
			iproc = new RuntimeProcess(launch, process, label, attributes);
		}

		return iproc;
	}

}
