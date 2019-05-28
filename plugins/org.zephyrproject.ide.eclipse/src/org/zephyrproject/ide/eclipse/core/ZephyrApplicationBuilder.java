/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core;

import java.util.Map;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.build.ICBuildConfiguration;
import org.eclipse.cdt.core.build.ICBuildConfigurationManager;
import org.eclipse.cdt.core.build.ICBuildConfigurationProvider;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.zephyrproject.ide.eclipse.core.build.CMakeGeneratorBuildConfiguration;
import org.zephyrproject.ide.eclipse.core.build.ZephyrApplicationBuildConfiguration;
import org.zephyrproject.ide.eclipse.core.build.ZephyrApplicationBuildConfigurationProvider;
import org.zephyrproject.ide.eclipse.core.build.makefiles.ZephyrApplicationMakefilesBuildConfiguration;
import org.zephyrproject.ide.eclipse.core.build.ninja.ZephyrApplicationNinjaBuildConfiguration;
import org.zephyrproject.ide.eclipse.core.internal.ZephyrHelpers;

public class ZephyrApplicationBuilder extends IncrementalProjectBuilder {

	public static final String BUILDER_ID =
			ZephyrPlugin.PLUGIN_ID + ".zephyrApplicationBuilder"; //$NON-NLS-1$

	/**
	 * Adjust build kind if needed.
	 *
	 * This looks at the resource delta to determine whether a new kind value
	 * is needed to be passed to the builder. For example, if CMakeLists.txt
	 * has changed, a {@code FULL_BUILD} is needed to regenerate cmake files.
	 *
	 * @param oldKind Current kind value.
	 * @param delta Resource delta.
	 * @return New kind value to be passed to the builder.
	 */
	private int adjustKind(int oldKind, IResourceDelta delta) {
		if (delta == null) {
			return FULL_BUILD;
		}

		int newKind = oldKind;
		IResourceDelta[] children = delta.getAffectedChildren(
				IResourceDelta.CHANGED | IResourceDelta.CONTENT);

		for (IResourceDelta oneDelta : children) {
			IResource res = oneDelta.getResource();
			if (res instanceof IFile) {
				IFile file = (IFile) res;

				/* If CMakeLists.txt has changed, force a full build */
				if (file.getName().equals("CMakeLists.txt")) { //$NON-NLS-1$
					newKind = FULL_BUILD;
				}

				/* If prj.conf has changed, force a full build */
				if (file.getName().equals("prj.conf")) { //$NON-NLS-1$
					newKind = FULL_BUILD;
				}
			}
		}

		return newKind;
	}

	private ICBuildConfiguration fixBuildConfig(IBuildConfiguration config)
			throws CoreException {
		ICBuildConfigurationManager configManager =
				CCorePlugin.getService(ICBuildConfigurationManager.class);

		if (configManager == null) {
			throw new CoreException(ZephyrHelpers.errorStatus(
					"Cannot get build configuration manager!", null));
		}

		ICBuildConfigurationProvider provider = configManager
				.getProvider(ZephyrApplicationBuildConfigurationProvider.ID);

		if (provider == null) {
			throw new CoreException(ZephyrHelpers.errorStatus(
					"Cannot get build configuration provider!", null));
		}

		String cmake = ZephyrHelpers.getCMakeGenerator(config.getProject());
		ICBuildConfiguration buildCfg = null;

		if (cmake.equals(ZephyrConstants.CMAKE_GENERATOR_MAKEFILE)) {
			buildCfg = provider.getCBuildConfiguration(config,
					ZephyrApplicationMakefilesBuildConfiguration.CONFIG_NAME);
		} else if (cmake.equals(ZephyrConstants.CMAKE_GENERATOR_NINJA)) {
			buildCfg = provider.getCBuildConfiguration(config,
					ZephyrApplicationNinjaBuildConfiguration.CONFIG_NAME);
		}

		if ((buildCfg == null)
				|| !(buildCfg instanceof ZephyrApplicationBuildConfiguration)) {
			throw new CoreException(ZephyrHelpers.errorStatus(
					"Unable to retrieve build configuration!", null));
		}

		return buildCfg;
	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args,
			IProgressMonitor monitor) throws CoreException {
		IResourceDelta delta = getDelta(getProject());
		int buildType = adjustKind(kind, delta);
		IProject project = getProject();
		IBuildConfiguration buildCfg = getBuildConfig();

		SubMonitor subMonitor = SubMonitor.convert(monitor, 101);

		/* Setup console */
		IConsole console = CCorePlugin.getDefault().getConsole();
		console.start(project);

		/* Grab a build configuration from the provider */
		ICBuildConfiguration appBuildCfg =
				buildCfg.getAdapter(ICBuildConfiguration.class);
		if ((appBuildCfg == null)
				|| !(appBuildCfg instanceof ZephyrApplicationBuildConfiguration)) {
			appBuildCfg = fixBuildConfig(buildCfg);
		}

		ZephyrApplicationBuildConfiguration zAppBuildCfg =
				(ZephyrApplicationBuildConfiguration) appBuildCfg;

		/* Setup build configuration to run CMake */
		ICBuildConfiguration cmakeBuildCfg =
				new CMakeGeneratorBuildConfiguration(buildCfg, zAppBuildCfg);

		cmakeBuildCfg.build(buildType, args, console, subMonitor.newChild(1));

		/* Invoke application build */
		zAppBuildCfg.build(buildType, args, console, subMonitor.newChild(100));

		return new IProject[] {
			project
		};
	}

	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		IProject project = getProject();
		IBuildConfiguration buildCfg = getBuildConfig();

		/* Setup console */
		IConsole console = CCorePlugin.getDefault().getConsole();
		console.start(project);

		/* Grab a build configuration from the provider */
		ICBuildConfiguration appBuildCfg =
				buildCfg.getAdapter(ICBuildConfiguration.class);
		if ((appBuildCfg == null)
				|| !(appBuildCfg instanceof ZephyrApplicationBuildConfiguration)) {
			appBuildCfg = fixBuildConfig(buildCfg);
		}

		ZephyrApplicationBuildConfiguration zAppBuildCfg =
				(ZephyrApplicationBuildConfiguration) appBuildCfg;

		/* Invoke application clean */
		zAppBuildCfg.clean(console, monitor);

		/* Setup build configuration to run CMake */
		ICBuildConfiguration cmakeBuildCfg =
				new CMakeGeneratorBuildConfiguration(buildCfg, zAppBuildCfg);

		cmakeBuildCfg.clean(console, monitor);
	}

}
