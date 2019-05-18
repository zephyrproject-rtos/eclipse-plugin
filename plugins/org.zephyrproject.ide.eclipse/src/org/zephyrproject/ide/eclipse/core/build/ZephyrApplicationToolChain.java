/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.build;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.build.gcc.core.GCCToolChain;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.build.IToolChainProvider;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.IExtendedScannerInfo;
import org.eclipse.core.resources.IBuildConfiguration;

/**
 * Wrapper of GCCToolChain to build Zephyr Applications.
 */
public class ZephyrApplicationToolChain extends GCCToolChain {

	public static final String TOOLCHAIN_ID = "zephyr.default"; //$NON-NLS-1$
	public static final String TOOLCHAIN_OS = "zephyr"; //$NON-NLS-1$

	private String cCompilerPath;

	private String cxxCompilerPath;

	private String makeProgramPath;

	public ZephyrApplicationToolChain(IToolChainProvider provider) {
		super(provider, TOOLCHAIN_ID, "");
		super.setProperty(ATTR_OS, TOOLCHAIN_OS);
		this.cCompilerPath = null;
		this.cxxCompilerPath = null;
		this.makeProgramPath = null;
	}

	ZephyrApplicationToolChain(IToolChainProvider provider,
			IBuildConfiguration config) {
		super(provider, TOOLCHAIN_ID, "");
		super.setProperty(ATTR_OS, TOOLCHAIN_OS);
		this.cCompilerPath = null;
		this.cxxCompilerPath = null;
		this.makeProgramPath = null;
	}

	public ZephyrApplicationToolChain(IToolChainProvider provider, String id,
			String version) {
		super(provider, id, version);
		super.setProperty(ATTR_OS, TOOLCHAIN_OS);
		this.cCompilerPath = null;
		this.cxxCompilerPath = null;
		this.makeProgramPath = null;
	}

	@Override
	public String getBinaryParserId() {
		return CCorePlugin.PLUGIN_ID + ".ELF"; //$NON-NLS-1$
	}

	@Override
	public String[] getCompileCommands() {
		List<String> compilers = new ArrayList<>();

		if (this.cCompilerPath != null) {
			compilers.add(this.cCompilerPath);
		}

		if (this.cxxCompilerPath != null) {
			compilers.add(this.cxxCompilerPath);
		}

		return compilers.toArray(new String[0]);
	}

	@Override
	public String[] getCompileCommands(ILanguage language) {
		if (GPPLanguage.ID.equals(language.getId())
				&& (this.cxxCompilerPath != null)) {
			return new String[] {
				this.cxxCompilerPath
			};
		} else if (GCCLanguage.ID.equals(language.getId())
				&& (this.cCompilerPath != null)) {
			return new String[] {
				this.cCompilerPath
			};
		} else {
			return new String[0];
		}
	}

	@Override
	public IExtendedScannerInfo getDefaultScannerInfo(
			IBuildConfiguration buildConfig,
			IExtendedScannerInfo baseScannerInfo, ILanguage language,
			URI buildDirectoryURI) {
		return null;
	}

	public String getCCompiler() {
		return cCompilerPath;
	}

	public void setCCompiler(String cCompilerPath) {
		this.cCompilerPath = cCompilerPath;
	}

	public String getCXXCompiler() {
		return cxxCompilerPath;
	}

	public void setCXXCompiler(String cxxCompilerPath) {
		this.cxxCompilerPath = cxxCompilerPath;
	}

	public String getMakeProgram() {
		return makeProgramPath;
	}

	public void setMakeProgram(String makeProgramPath) {
		this.makeProgramPath = makeProgramPath;
	}

	@Override
	protected void addDiscoveryOptions(List<String> command) {
		super.addDiscoveryOptions(command);
	}

}
