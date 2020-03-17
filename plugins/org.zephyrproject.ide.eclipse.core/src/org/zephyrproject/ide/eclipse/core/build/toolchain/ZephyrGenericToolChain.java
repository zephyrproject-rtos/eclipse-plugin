/*
 * Copyright (c) 2020 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.build.toolchain;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.build.IToolChain;
import org.eclipse.cdt.core.build.IToolChainManager;
import org.eclipse.cdt.core.build.IToolChainProvider;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.zephyrproject.ide.eclipse.core.ZephyrPlugin;
import org.zephyrproject.ide.eclipse.core.build.ZephyrApplicationToolChainProvider;

/**
 * A Generic Zephyr toolchain which only provides bare minimum.
 */
public class ZephyrGenericToolChain extends PlatformObject
		implements IToolChain {

	public static final String TOOLCHAIN_OS = "zephyr"; //$NON-NLS-1$

	public static final String TYPE_ID =
			ZephyrPlugin.PLUGIN_STEM + ".toolchain"; //$NON-NLS-1$

	public static final String TOOLCHAIN_ID = "zephyr.toolchain.generic"; //$NON-NLS-1$

	private final IToolChainProvider provider;
	private final IEnvironmentVariable[] envVars;
	private final Map<String, String> properties = new HashMap<>();

	public ZephyrGenericToolChain() {
		super();

		IToolChainProvider tcP = null;
		try {
			IToolChainManager toolChainManager =
					ZephyrPlugin.getService(IToolChainManager.class);
			tcP = toolChainManager
					.getProvider(ZephyrApplicationToolChainProvider.ID);
		} catch (CoreException e) {
		}

		this.provider = tcP;
		this.envVars = new IEnvironmentVariable[0];
	}

	@Override
	public IToolChainProvider getProvider() {
		return provider;
	}

	@Override
	public String getTypeId() {
		return TYPE_ID;
	}

	@Override
	public String getId() {
		return TOOLCHAIN_ID;
	}

	@Override
	public String getVersion() {
		return "";
	}

	@Override
	public String getName() {
		return TOOLCHAIN_ID;
	}

	@Override
	public String getProperty(String key) {
		String value = properties.get(key);
		if (value != null) {
			return value;
		}

		switch (key) {
		case ATTR_OS:
			return Platform.getOS();
		case ATTR_ARCH:
			if (Platform.getOS().equals(getProperty(ATTR_OS))) {
				return Platform.getOSArch();
			}
		}

		return null;
	}

	@Override
	public void setProperty(String key, String value) {
		properties.put(key, value);
	}

	@Override
	public IEnvironmentVariable[] getVariables() {
		return envVars;
	}

	@Override
	public IEnvironmentVariable getVariable(String name) {
		return null;
	}

	@Override
	public String[] getErrorParserIds() {
		return new String[] {
			"org.eclipse.cdt.core.CWDLocator" //$NON-NLS-1$
		};
	}

	@Override
	public String getBinaryParserId() {
		return CCorePlugin.PLUGIN_ID + ".ELF"; //$NON-NLS-1$
	}

	@Override
	public Path getCommandPath(Path command) {
		return null;
	}

	@Override
	public String[] getCompileCommands() {
		return null;
	}

}
