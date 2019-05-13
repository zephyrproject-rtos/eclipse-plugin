/*
 * Copyright (c) 2015, 2016 QNX Software Systems and others.
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ConsoleOutputStream;
import org.eclipse.cdt.core.ErrorParserManager;
import org.eclipse.cdt.core.IConsoleParser;
import org.eclipse.cdt.core.build.CBuildConfiguration;
import org.eclipse.cdt.core.build.IToolChain;
import org.eclipse.cdt.core.model.ICModelMarker;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.zephyrproject.ide.eclipse.core.ZephyrConstants;
import org.zephyrproject.ide.eclipse.core.ZephyrPlugin;
import org.zephyrproject.ide.eclipse.core.internal.ZephyrHelpers;
import org.zephyrproject.ide.eclipse.core.internal.build.CMakeCache;
import org.zephyrproject.ide.eclipse.core.internal.build.MakefileProgressMonitor;

/**
 * Build configuration for Zephyr Application
 *
 * This contains code to actually build and clean the project.
 *
 * Originally from org.eclipse.cdt.cmake.core.internal.CMakeBuildConfiguration.
 */
public class ZephyrApplicationBuildConfiguration extends CBuildConfiguration {

	public static final String DEFAULT_CONFIG_NAME =
			ZephyrApplicationBuildConfigurationProvider.ID
					+ "/zephyr.app.build.config"; //$NON-NLS-1$

	private String cmakeMakeProgram;

	private CMakeCache cmakeCache;

	public ZephyrApplicationBuildConfiguration(IBuildConfiguration config,
			IToolChain toolChain) {
		super(config, DEFAULT_CONFIG_NAME, toolChain);
	}

	public ZephyrApplicationBuildConfiguration(IBuildConfiguration config,
			String name, IToolChain toolChain) {
		super(config, name, toolChain);

		this.cmakeMakeProgram = "make"; //$NON-NLS-1$
		this.cmakeCache = null;
	}

	@Override
	public IContainer getBuildContainer() throws CoreException {
		IProject project = getProject();
		IFolder buildFolder =
				project.getFolder(ZephyrConstants.DEFAULT_BUILD_DIR);
		if (!buildFolder.exists()) {
			buildFolder.create(IResource.FORCE | IResource.DERIVED, true,
					new NullProgressMonitor());
		}

		return buildFolder;
	}

	/**
	 * Get the board name to be built for.
	 *
	 * @param project The Project.
	 * @return Name of board to be built for.
	 */
	private String getBoardName(IProject project) {
		ScopedPreferenceStore pStore = new ScopedPreferenceStore(
				new ProjectScope(project), ZephyrPlugin.PLUGIN_ID);

		return pStore.getString(ZephyrConstants.ZEPHYR_BOARD);
	}

	private void parseCMakeCache(IProject project, Path buildDir)
			throws IOException, CoreException {
		Path cachePath = buildDir.resolve("CMakeCache.txt"); //$NON-NLS-1$
		boolean done = false;
		if (Files.exists(cachePath)) {
			CMakeCache cache = new CMakeCache(project);
			done = cache.parseFile(cachePath.toFile());
			if (done) {
				this.cmakeCache = cache;
				updateToolChain(this.cmakeCache);
			}
		}

		if (!done) {
			throw new CoreException(ZephyrHelpers.errorStatus(
					"Build not configured properly.", new Exception()));
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.cdt.core.build.ICBuildConfiguration#build(int,
	 * java.util.Map, org.eclipse.cdt.core.resources.IConsole,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public IProject[] build(int kind, Map<String, String> args,
			IConsole console, IProgressMonitor monitor) throws CoreException {
		IProject project = getProject();

		try {
			monitor.beginTask("Building...", 100);

			/* Remove C-related warnings/errors */
			project.deleteMarkers(ICModelMarker.C_MODEL_PROBLEM_MARKER, false,
					IResource.DEPTH_INFINITE);

			ConsoleOutputStream consoleOut = console.getOutputStream();

			Path buildDir = getBuildDirectory();
			IFolder buildFolder = (IFolder) getBuildContainer();

			if ((cmakeCache == null)
					|| (kind == IncrementalProjectBuilder.FULL_BUILD)) {
				parseCMakeCache(project, buildDir);
			}

			String boardName = getBoardName(project);

			if (!Files.exists(buildDir.resolve("Makefile"))) { //$NON-NLS-1$
				console.getErrorStream().write("Makefile does not exist\n");
				return new IProject[0];
			}

			try (ErrorParserManager epm =
					new ErrorParserManager(project, getBuildDirectoryURI(),
							this, getToolChain().getErrorParserIds())) {
				MakefileProgressMonitor buildProgress =
						new MakefileProgressMonitor(monitor);

				consoleOut.write(String.format(
						"----- Building for board %s in %s\n", boardName,
						buildFolder.getProjectRelativePath().toString()));

				String[] command = {
					this.cmakeMakeProgram
				};

				Path cmdPath = findCommand(command[0]);
				if (cmdPath != null) {
					command[0] = cmdPath.toString();
				}

				ProcessBuilder processBuilder = new ProcessBuilder(command)
						.directory(buildDir.toFile());
				Map<String, String> env = processBuilder.environment();
				ZephyrHelpers.setupBuildCommandEnvironment(project, env);
				setBuildEnvironment(env);
				Process process = processBuilder.start();
				consoleOut.write(String.join(" ", command) + '\n'); //$NON-NLS-1$
				watchProcess(process, new IConsoleParser[] {
					epm,
					buildProgress
				}, console);
			}

			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);

			monitor.done();

			return new IProject[] {
				project
			};
		} catch (IOException eio) {
			throw new CoreException(ZephyrHelpers.errorStatus(String.format(
					"Error building project %s!", project.getName()), eio));
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.cdt.core.build.ICBuildConfiguration#clean(org.eclipse.cdt.
	 * core.resources.IConsole, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void clean(IConsole console, IProgressMonitor monitor)
			throws CoreException {
		IProject project = getProject();

		try {
			/* Remove C-related warnings/errors */
			project.deleteMarkers(ICModelMarker.C_MODEL_PROBLEM_MARKER, false,
					IResource.DEPTH_INFINITE);

			Path buildDir = getBuildDirectory();
			IFolder buildFolder = (IFolder) getBuildContainer();

			ConsoleOutputStream consoleOut = console.getOutputStream();

			if (!Files.exists(buildDir.resolve("CMakeFiles"))) {
				/* Haven't run CMake yet, so nothing to clean */
				console.getErrorStream()
						.write("----- Need to run CMake first.\n");
				return;
			}

			if (!Files.exists(buildDir.resolve("Makefile"))) { //$NON-NLS-1$
				/* Makefile does not exist, 'make clean' won't work */
				console.getErrorStream()
						.write("----- Makefile does not exist.\n");
				return;
			}

			if (cmakeCache == null) {
				parseCMakeCache(project, buildDir);
			}

			consoleOut.write(String.format("----- Cleaning in %s\n",
					buildFolder.getProjectRelativePath().toString()));

			String[] command = {
				this.cmakeMakeProgram,
				"clean" //$NON-NLS-1$
			};

			Path cmdPath = findCommand(command[0]);
			if (cmdPath != null) {
				command[0] = cmdPath.toString();
			}

			ProcessBuilder processBuilder =
					new ProcessBuilder(command).directory(buildDir.toFile());
			Map<String, String> env = processBuilder.environment();
			ZephyrHelpers.setupBuildCommandEnvironment(project, env);
			setBuildEnvironment(env);
			Process process = processBuilder.start();
			consoleOut.write(String.join(" ", command) + '\n'); //$NON-NLS-1$
			watchProcess(process, new IConsoleParser[0], console);

			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		} catch (IOException eio) {
			throw new CoreException(ZephyrHelpers.errorStatus(String.format(
					"Error cleaning Zephyr Application project %s!",
					project.getName()), eio));
		}
	}

	@Override
	public String getBinaryParserId() throws CoreException {
		return CCorePlugin.PLUGIN_ID + ".ELF"; //$NON-NLS-1$
	}

	@Override
	public Path findCommand(String command) {
		return super.findCommand(command);
	}

	private void updateToolChain(CMakeCache cache) throws CoreException {
		/* Make sure it is of known toolchain class */
		IToolChain iTC = getToolChain();
		if ((iTC == null) || !(iTC instanceof ZephyrApplicationToolChain)) {
			throw new CoreException(ZephyrHelpers.errorStatus(
					"Toolchain not configured properly.", new Exception()));
		}

		ZephyrApplicationToolChain toolChain = (ZephyrApplicationToolChain) iTC;

		/* Populate compiler paths */
		String str;
		str = cache.getCCompiler();
		if (str != null) {
			toolChain.setCCompiler(str);
		}

		str = cache.getCXXCompiler();
		if (str != null) {
			toolChain.setCXXCompiler(str);
		}

		str = cache.getMakeProgram();
		if (str != null) {
			this.cmakeMakeProgram = str;
		}
	}

}
