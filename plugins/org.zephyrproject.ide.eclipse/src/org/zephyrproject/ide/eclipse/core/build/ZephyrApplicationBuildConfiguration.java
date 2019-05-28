/*
 * Copyright (c) 2015, 2016 QNX Software Systems and others.
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.build;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ConsoleOutputStream;
import org.eclipse.cdt.core.ErrorParserManager;
import org.eclipse.cdt.core.IConsoleParser;
import org.eclipse.cdt.core.build.CBuildConfiguration;
import org.eclipse.cdt.core.build.IToolChain;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICModelMarker;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.core.parser.IExtendedScannerInfo;
import org.eclipse.cdt.core.parser.IScannerInfo;
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
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.zephyrproject.ide.eclipse.core.ZephyrConstants;
import org.zephyrproject.ide.eclipse.core.ZephyrPlugin;
import org.zephyrproject.ide.eclipse.core.internal.ZephyrHelpers;
import org.zephyrproject.ide.eclipse.core.internal.build.CMakeCache;
import org.zephyrproject.ide.eclipse.core.internal.build.CompileCommand;
import org.zephyrproject.ide.eclipse.core.internal.build.MakefileProgressMonitor;
import org.zephyrproject.ide.eclipse.core.internal.build.NinjaProgressMonitor;
import org.zephyrproject.ide.eclipse.core.internal.build.ZephyrScannerInfoCache;

import com.google.gson.Gson;

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

	private ScopedPreferenceStore pStore;

	private ZephyrScannerInfoCache scannerInfoCache;

	public ZephyrApplicationBuildConfiguration(IBuildConfiguration config,
			IToolChain toolChain) {
		this(config, DEFAULT_CONFIG_NAME, toolChain);
	}

	public ZephyrApplicationBuildConfiguration(IBuildConfiguration config,
			String name, IToolChain toolChain) {
		super(config, name, toolChain);

		this.cmakeCache = null;
		this.scannerInfoCache = new ZephyrScannerInfoCache(config);

		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			this.cmakeMakeProgram = "ninja.exe"; //$NON-NLS-1$
		} else {
			this.cmakeMakeProgram = "ninja"; //$NON-NLS-1$
		}

		this.pStore = new ScopedPreferenceStore(
				new ProjectScope(config.getProject()), ZephyrPlugin.PLUGIN_ID);
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
	 * @return Name of board to be built for.
	 */
	private String getBoardName() {
		return pStore.getString(ZephyrConstants.ZEPHYR_BOARD);
	}

	@Override
	public void setBuildEnvironment(Map<String, String> env) {
		ZephyrHelpers.setupBuildCommandEnvironment(pStore, env);
		super.setBuildEnvironment(env);
	}

	/**
	 * @return The CMake Generator identifier
	 */
	private String getCMakeGenerator() {
		String generator = pStore.getString(ZephyrConstants.CMAKE_GENERATOR);

		if (generator.trim().isEmpty()) {
			return ZephyrHelpers.getDefaultCMakeGenerator();
		}

		return generator;
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

			String boardName = getBoardName();
			String cmakeGenerator = getCMakeGenerator();

			IConsoleParser buildProgress = null;
			if (cmakeGenerator
					.equals(ZephyrConstants.CMAKE_GENERATOR_MAKEFILE)) {
				if (!Files.exists(buildDir.resolve("Makefile"))) { //$NON-NLS-1$
					/* Makefile does not exist, 'make' won't work */
					console.getErrorStream().write("Makefile does not exist\n");
					return null;
				}

				buildProgress = new MakefileProgressMonitor(monitor);
			} else if (cmakeGenerator
					.equals(ZephyrConstants.CMAKE_GENERATOR_NINJA)) {
				if (!Files.exists(buildDir.resolve("build.ninja"))) { //$NON-NLS-1$
					/* build.ninja does not exist, 'ninja' won't work */
					console.getErrorStream()
							.write("build.ninja does not exist\n");
					return null;
				}

				buildProgress = new NinjaProgressMonitor(monitor);
			} else {
				throw new CoreException(ZephyrHelpers.errorStatus(
						"Unknonw CMake Generator specified", new Exception()));
			}

			try (ErrorParserManager epm =
					new ErrorParserManager(project, getBuildDirectoryURI(),
							this, getToolChain().getErrorParserIds())) {
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
				setBuildEnvironment(processBuilder.environment());
				Process process = processBuilder.start();
				consoleOut.write(String.join(" ", command) + '\n'); //$NON-NLS-1$
				watchProcess(process, new IConsoleParser[] {
					epm,
					buildProgress
				}, console);

				consoleOut.write(String.format(
						"----- Done building for board %s in %s\n", boardName,
						buildFolder.getProjectRelativePath().toString()));
			}

			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);

			processCompileCommandsFile(monitor);

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

			String cmakeGenerator = getCMakeGenerator();

			if (cmakeGenerator
					.equals(ZephyrConstants.CMAKE_GENERATOR_MAKEFILE)) {
				if (!Files.exists(buildDir.resolve("Makefile"))) { //$NON-NLS-1$
					/* Makefile does not exist, 'make clean' won't work */
					console.getErrorStream().write("Makefile does not exist\n");
					return;
				}
			} else if (cmakeGenerator
					.equals(ZephyrConstants.CMAKE_GENERATOR_NINJA)) {
				if (!Files.exists(buildDir.resolve("build.ninja"))) { //$NON-NLS-1$
					/* build.ninja does not exist, 'ninja clean' won't work */
					console.getErrorStream()
							.write("build.ninja does not exist\n");
					return;
				}
			} else {
				throw new CoreException(ZephyrHelpers.errorStatus(
						"Unknonw CMake Generator specified", new Exception()));
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
			setBuildEnvironment(processBuilder.environment());
			Process process = processBuilder.start();
			consoleOut.write(String.join(" ", command) + '\n'); //$NON-NLS-1$
			watchProcess(process, new IConsoleParser[0], console);

			consoleOut.write(String.format("----- Done cleaning in %s\n",
					buildFolder.getProjectRelativePath().toString()));

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
		Path cmd = null;

		try {
			cmd = super.findCommand(command);
		} catch (NullPointerException npe) {
			/*
			 * super.findCommand() may throw NullPointerException if
			 * PATH is not in environment.
			 */
		}

		return cmd;
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

	@Override
	public IScannerInfo getScannerInformation(IResource resource) {
		IExtendedScannerInfo info = scannerInfoCache.getScannerInfo(resource);

		if (info == null) {
			ICElement celement =
					CCorePlugin.getDefault().getCoreModel().create(resource);
			if (celement instanceof ITranslationUnit) {
				info = scannerInfoCache
						.getDefaultScannerInfo(celement.getResource());
			}
		}
		return info;
	}

	@Override
	public boolean processLine(String line) {
		return false;
	}

	public void processCompileCommand(String line,
			ArrayList<ICElement> tuSelection) throws CoreException {

		List<String> command = Arrays.asList(line.split("\\s+")); //$NON-NLS-1$

		/* Only work with known toolchain object */
		IToolChain iTC = getToolChain();
		if (!(iTC instanceof ZephyrApplicationToolChain)) {
			return;
		}
		ZephyrApplicationToolChain toolChain = (ZephyrApplicationToolChain) iTC;

		/* Make sure it's a compiler command */
		String[] compileCommands = toolChain.getCompileCommands();
		loop: for (String arg : command) {
			if (arg.startsWith("-")) { //$NON-NLS-1$
				/* compiler option found, gone too far into the list */
				return;
			}

			for (String cc : compileCommands) {
				if (arg.endsWith(cc)) {
					break loop;
				}
			}
		}

		/* Add artifacts to the tuSelection so indexer can update these later */
		IResource[] resources = getToolChain().getResourcesFromCommand(command,
				getBuildDirectoryURI());
		if (resources != null) {
			for (IResource resource : resources) {
				Path commandPath = findCommand(command.get(0));
				command.set(0, commandPath.toString());

				/*
				 * Note that this does not use the reference scanner info
				 * object, as the command line already has everything to
				 * parse the file correctly.
				 */
				IExtendedScannerInfo info = getToolChain().getScannerInfo(
						getBuildConfiguration(), command, null, resource,
						getBuildDirectoryURI());
				scannerInfoCache.setScannerInfo(resource, info);
				scannerInfoCache.setDefaultScannerInfo(resource, info);

				ICElement element = CCorePlugin.getDefault().getCoreModel()
						.create(resource);
				if (element != null) {
					tuSelection.add(element);
				}
			}
		}
	}

	@Override
	public void shutdown() {
		/* Nothing to do here. */
	}

	private void processCompileCommandsFile(IProgressMonitor monitor)
			throws CoreException {
		IProject project = getProject();
		Path commandsFile =
				getBuildDirectory().resolve("compile_commands.json"); //$NON-NLS-1$
		if (Files.exists(commandsFile)) {
			try (FileReader reader = new FileReader(commandsFile.toFile())) {
				Gson gson = new Gson();
				ArrayList<ICElement> tuSelection = new ArrayList<>();

				CompileCommand[] commands =
						gson.fromJson(reader, CompileCommand[].class);
				for (CompileCommand command : commands) {
					processCompileCommand(command.getCommand(), tuSelection);
				}

				if (!tuSelection.isEmpty()) {
					CCorePlugin.getIndexManager().update(
							tuSelection.toArray(new ICElement[0]),
							IIndexManager.UPDATE_CHECK_TIMESTAMPS
									| IIndexManager.UPDATE_CHECK_CONFIGURATION
									| IIndexManager.UPDATE_EXTERNAL_FILES_FOR_PROJECT
									| IIndexManager.UPDATE_CHECK_CONTENTS_HASH
									| IIndexManager.UPDATE_UNRESOLVED_INCLUDES);
				}

				scannerInfoCache.writeCache();
			} catch (IOException e) {
				throw new CoreException(ZephyrHelpers.errorStatus(String.format(
						"Cannot parse compiler commands from CMake for project %s",
						project.getName()), e));
			}
		}
	}

}
