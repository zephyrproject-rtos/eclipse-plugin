/*
 * Copyright (c) 2015, 2016 QNX Software Systems and others.
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.build;

import java.io.File;
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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.zephyrproject.ide.eclipse.core.build.toolchain.ZephyrGCCToolChain;
import org.zephyrproject.ide.eclipse.core.build.toolchain.ZephyrGenericToolChain;
import org.zephyrproject.ide.eclipse.core.internal.ZephyrHelpers;
import org.zephyrproject.ide.eclipse.core.internal.build.CMakeCache;
import org.zephyrproject.ide.eclipse.core.internal.build.CompileCommand;
import org.zephyrproject.ide.eclipse.core.internal.build.MakefileProgressMonitor;
import org.zephyrproject.ide.eclipse.core.internal.build.NinjaProgressMonitor;
import org.zephyrproject.ide.eclipse.core.internal.build.ZephyrScannerInfoCache;
import org.zephyrproject.ide.eclipse.core.preferences.ZephyrProjectPreferences;

import com.google.gson.Gson;

/**
 * Build configuration for Zephyr Application
 *
 * This contains code to actually build and clean the project.
 *
 * Originally from org.eclipse.cdt.cmake.core.internal.CMakeBuildConfiguration.
 */
public class ZephyrApplicationBuildConfiguration extends CBuildConfiguration {

	public static final String CONFIG_NAME =
			ZephyrApplicationBuildConfigurationProvider.ID
					+ "/zephyr.app.build.config"; //$NON-NLS-1$

	private String cmakeCmd;

	private String cmakeMakeProgram;

	private ScopedPreferenceStore pStore;

	private ZephyrScannerInfoCache scannerInfoCache;

	public ZephyrApplicationBuildConfiguration(IBuildConfiguration config,
			String name, IToolChain toolChain) {
		super(config, name, toolChain);

		this.scannerInfoCache = new ZephyrScannerInfoCache(config);

		/* Default is to use cmake and ninja */
		this.cmakeCmd = "cmake"; // $NON-NLS-1$
		this.cmakeMakeProgram = "ninja"; //$NON-NLS-1$

		this.pStore =
				ZephyrHelpers.getProjectPreferenceStore(config.getProject());
	}

	@Override
	public IContainer getBuildContainer() throws CoreException {
		IProject project = getProject();
		IFolder buildFolder = project
				.getFolder(ZephyrProjectPreferences.getBuildDirectory(project));
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
		return ZephyrHelpers.getBoardName(pStore);
	}

	@Override
	public void setBuildEnvironment(Map<String, String> env) {
		super.setBuildEnvironment(env);
		ZephyrHelpers.setupBuildCommandEnvironment(pStore, env);
	}

	/**
	 * @return The CMake Generator identifier
	 */
	private String getCMakeGenerator() {
		String generator = pStore.getString(CMakeConstants.CMAKE_GENERATOR);

		if (generator.trim().isEmpty()) {
			return ZephyrHelpers.getDefaultCMakeGenerator();
		}

		return generator;
	}

	/**
	 * Build the application by invoking make or ninja.
	 *
	 * @see org.eclipse.cdt.core.build.ICBuildConfiguration#build(int,
	 *      java.util.Map, org.eclipse.cdt.core.resources.IConsole,
	 *      org.eclipse.core.runtime.IProgressMonitor)
	 */
	private IProject appBuild(int kind, Map<String, String> args,
			IConsole console, IProgressMonitor monitor) throws CoreException {
		IProject project = getProject();

		try {
			monitor.beginTask("Building...", 100);

			ConsoleOutputStream consoleOut = console.getOutputStream();

			Path buildDir = getBuildDirectory();
			IFolder buildFolder = (IFolder) getBuildContainer();

			String boardName = getBoardName();
			String cmakeGenerator = getCMakeGenerator();

			IConsoleParser buildProgress = null;
			if (cmakeGenerator
					.equals(CMakeConstants.CMAKE_GENERATOR_MAKEFILE)) {
				if (!Files.exists(buildDir.resolve("Makefile"))) { //$NON-NLS-1$
					/* Makefile does not exist, 'make' won't work */
					console.getErrorStream().write(
							"Makefile does not exist" + System.lineSeparator());
					return null;
				}

				buildProgress = new MakefileProgressMonitor(monitor);
			} else if (cmakeGenerator
					.equals(CMakeConstants.CMAKE_GENERATOR_NINJA)) {
				if (!Files.exists(buildDir.resolve("build.ninja"))) { //$NON-NLS-1$
					/* build.ninja does not exist, 'ninja' won't work */
					console.getErrorStream().write("build.ninja does not exist"
							+ System.lineSeparator());
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
						"----- Building for board %s in %s%n", boardName,
						buildFolder.getProjectRelativePath().toString()));
				epm.setOutputStream(console.getOutputStream());

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
				consoleOut.write(
						String.join(" ", command) + System.lineSeparator());
				watchProcess(process, new IConsoleParser[] {
					epm,
					buildProgress
				});

				consoleOut.write(String.format(
						"----- Done building for board %s in %s%n", boardName,
						buildFolder.getProjectRelativePath().toString()));
			}

			monitor.done();

			processCompileCommandsFile(monitor);

			return project;
		} catch (IOException eio) {
			throw new CoreException(ZephyrHelpers.errorStatus(String.format(
					"Error building project %s!", project.getName()), eio));
		}
	}

	/**
	 * Run CMake to configure the application.
	 *
	 * @see org.eclipse.cdt.core.build.ICBuildConfiguration#build(int,
	 *      java.util.Map, org.eclipse.cdt.core.resources.IConsole,
	 *      org.eclipse.core.runtime.IProgressMonitor)
	 */
	private IProject cmakeBuild(int kind, Map<String, String> args,
			IConsole console, IProgressMonitor monitor) throws CoreException {
		IProject project = getProject();

		IPath prjIPath = project.getLocation();

		/* Make sure CMakeLists.txt is there */
		if (!prjIPath.append("CMakeLists.txt").toFile().exists()) { //$NON-NLS-1$
			throw new CoreException(ZephyrHelpers.errorStatus(
					"File CMakeLists.txt does not exist under project root",
					null));
		}

		/* Make sure prj.conf is there */
		if (!prjIPath.append("prj.conf").toFile().exists()) { //$NON-NLS-1$
			throw new CoreException(ZephyrHelpers.errorStatus(
					"File prj.conf does not exist under project root", null));
		}

		try {
			monitor.beginTask("Running CMake...", 1);

			ConsoleOutputStream consoleOut = console.getOutputStream();

			Path buildDir = getBuildDirectory();
			IFolder buildFolder = (IFolder) getBuildContainer();

			String boardName = getBoardName();

			String projectAbsPath =
					new File(project.getLocationURI()).getAbsolutePath();

			if (!Files.exists(buildDir.resolve("CMakeFiles")) //$NON-NLS-1$
					|| (kind == IncrementalProjectBuilder.FULL_BUILD)) {

				consoleOut.write(String.format(
						"----- Generating CMake files for board %s in %s%n",
						boardName,
						buildFolder.getProjectRelativePath().toString()));

				List<String> command = new ArrayList<>();

				Path cmakePath = findCommand(cmakeCmd); // $NON-NLS-1$
				if (cmakePath != null) {
					command.add(cmakePath.toString());
				} else {
					/*
					 * findCommand() may return null if path is not setup
					 * correctly. Here assume the environment has been setup
					 * correctly and cmake can be called anywhere here.
					 */
					command.add(cmakeCmd);
				}

				command.add(String.format("-DBOARD=%s", boardName)); //$NON-NLS-1$

				command.add("-G"); //$NON-NLS-1$
				command.add(getCMakeGenerator());

				command.add("-DCMAKE_EXPORT_COMPILE_COMMANDS=ON"); //$NON-NLS-1$

				command.add(projectAbsPath);

				ProcessBuilder processBuilder = new ProcessBuilder(command)
						.directory(buildDir.toFile());
				setBuildEnvironment(processBuilder.environment());
				Process process = processBuilder.start();
				consoleOut.write(
						String.join(" ", command) + System.lineSeparator());

				watchProcess(process, console);

				consoleOut.write(String.format(
						"----- Done generating CMake files for board %s in %s%n",
						boardName,
						buildFolder.getProjectRelativePath().toString()));
			}

			/*
			 * For previously created projects without those CMake variables
			 * saved into project preference space, do this here to jumpstart
			 * the process. Or else the actual build would fail as those
			 * variables would result in NullPointerException later in
			 * appBuild().
			 */
			parseCMakeCache(project, buildDir);

			monitor.worked(1);
			monitor.done();

			/*
			 * Return this project so that we get resource delta next time
			 * this is invoked.
			 */
			return project;
		} catch (IOException eio) {
			throw new CoreException(ZephyrHelpers.errorStatus(
					String.format("Error invoking CMake for project %s!",
							project.getName()),
					eio));
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

		SubMonitor subMonitor = SubMonitor.convert(monitor, 103);

		/* Remove C-related warnings/errors */
		project.deleteMarkers(ICModelMarker.C_MODEL_PROBLEM_MARKER, false,
				IResource.DEPTH_INFINITE);

		updateToolChain();

		if (cmakeBuild(kind, args, console, subMonitor.newChild(1)) == null) {
			return null;
		}

		updateCMakeVariables();

		if (appBuild(kind, args, console, subMonitor.newChild(101)) == null) {
			return null;
		}

		project.refreshLocal(IResource.DEPTH_INFINITE, subMonitor.newChild(1));

		return new IProject[] {
			project
		};
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
				console.getErrorStream().write("----- Need to run CMake first."
						+ System.lineSeparator());
				return;
			}

			updateToolChain();
			updateCMakeVariables();

			String cmakeGenerator = getCMakeGenerator();

			if (cmakeGenerator
					.equals(CMakeConstants.CMAKE_GENERATOR_MAKEFILE)) {
				if (!Files.exists(buildDir.resolve("Makefile"))) { //$NON-NLS-1$
					/* Makefile does not exist, 'make clean' won't work */
					console.getErrorStream().write(
							"Makefile does not exist" + System.lineSeparator());
					return;
				}
			} else if (cmakeGenerator
					.equals(CMakeConstants.CMAKE_GENERATOR_NINJA)) {
				if (!Files.exists(buildDir.resolve("build.ninja"))) { //$NON-NLS-1$
					/* build.ninja does not exist, 'ninja clean' won't work */
					console.getErrorStream().write("build.ninja does not exist"
							+ System.lineSeparator());
					return;
				}
			} else {
				throw new CoreException(ZephyrHelpers.errorStatus(
						"Unknonw CMake Generator specified", new Exception()));
			}

			consoleOut.write(String.format("----- Cleaning in %s%n",
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
			consoleOut
					.write(String.join(" ", command) + System.lineSeparator());
			watchProcess(process, console);

			consoleOut.write(String.format("----- Done cleaning in %s%n",
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

	private void updateToolChain() throws CoreException {
		/* Make sure it is of known toolchain classes */
		IToolChain iTC = getToolChain();
		if (iTC instanceof ZephyrGenericToolChain) {
			/* nothing to do here */
		} else if (iTC instanceof ZephyrGCCToolChain) {
			ZephyrGCCToolChain toolChain = (ZephyrGCCToolChain) iTC;
			toolChain.initCMakeVarsFromProjectPerfStore(getProject());
		} else {
			throw new CoreException(ZephyrHelpers.errorStatus(
					"Toolchain not configured properly.", new Exception()));
		}
	}

	private void updateCMakeVariables() {
		this.cmakeMakeProgram = ZephyrHelpers.getProjectPreference(pStore,
				CMakeCache.CMAKE_MAKE_PROGRAM);
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
		if (!(iTC instanceof ZephyrGCCToolChain)) {
			return;
		}
		ZephyrGCCToolChain toolChain = (ZephyrGCCToolChain) iTC;

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
				if (commandPath == null) {
					continue;
				}
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
		monitor.beginTask("Parsing compile commands...", 1);

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

		monitor.worked(1);
		monitor.done();
	}

	private void parseCMakeCache(IProject project, Path buildDir)
			throws IOException, CoreException {
		Path cachePath = buildDir.resolve("CMakeCache.txt"); //$NON-NLS-1$
		boolean done = false;
		if (Files.exists(cachePath)) {
			CMakeCache cache = new CMakeCache(project);
			done = cache.parseFile(cachePath.toFile());
			if (done) {
				ScopedPreferenceStore pStore =
						ZephyrHelpers.getProjectPreferenceStore(project);

				pStore.putValue(CMakeCache.CMAKE_C_COMPILER,
						cache.getCCompiler());
				pStore.putValue(CMakeCache.CMAKE_CXX_COMPILER,
						cache.getCXXCompiler());
				pStore.putValue(CMakeCache.CMAKE_MAKE_PROGRAM,
						cache.getMakeProgram());
				pStore.putValue(CMakeCache.CMAKE_GDB, cache.getGdb());

				pStore.putValue(CMakeCache.WEST, cache.getWest());

				String val = cache.getDebugRunner();
				if ((val != null) && !val.isEmpty()) {
					pStore.putValue(CMakeCache.ZEPHYR_BOARD_DEBUG_RUNNER,
							cache.getDebugRunner());
				}

				pStore.save();
			}
		}

		if (!done) {
			throw new CoreException(ZephyrHelpers.errorStatus(
					"Build not configured properly.", new Exception()));
		}
	}

}
