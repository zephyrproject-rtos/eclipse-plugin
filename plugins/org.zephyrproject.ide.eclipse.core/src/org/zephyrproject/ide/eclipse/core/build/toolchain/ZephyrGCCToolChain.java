/*
 * Copyright (c) 2019-2020 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.build.toolchain;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.build.IToolChain;
import org.eclipse.cdt.core.build.IToolChainManager;
import org.eclipse.cdt.core.build.IToolChainProvider;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.ExtendedScannerInfo;
import org.eclipse.cdt.core.parser.IExtendedScannerInfo;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.zephyrproject.ide.eclipse.core.ZephyrPlugin;
import org.zephyrproject.ide.eclipse.core.internal.ZephyrHelpers;
import org.zephyrproject.ide.eclipse.core.internal.build.CMakeCache;

/**
 * GCC-compatible Zephyr toolchain.
 */
public class ZephyrGCCToolChain extends PlatformObject implements IToolChain {

	public static final String TOOLCHAIN_OS = "zephyr"; //$NON-NLS-1$

	public static final String TOOLCHAIN_ARCH = "unknown"; //$NON-NLS-1$

	public static final String TYPE_ID =
			ZephyrPlugin.PLUGIN_STEM + ".toolchain"; //$NON-NLS-1$

	public static final String TOOLCHAIN_ID = "zephyr.toolchain.gcc"; //$NON-NLS-1$

	private final IToolChainProvider provider;
	private final String id;
	private final IEnvironmentVariable[] envVars;
	private final Map<String, String> properties = new HashMap<>();
	private final HashMap<String, String> cmakeCacheMap = new HashMap<>();

	public ZephyrGCCToolChain(String id) {
		this.id = id;

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
	public String getProperty(String key) {
		String value = properties.get(key);
		if (value != null) {
			return value;
		}

		switch (key) {
		case ATTR_OS:
			return TOOLCHAIN_OS;
		case ATTR_ARCH:
			return TOOLCHAIN_ARCH;
		}

		return null;
	}

	@Override
	public void setProperty(String key, String value) {
		properties.put(key, value);
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
		return id;
	}

	@Override
	public String getVersion() {
		return ""; //$NON-NLS-1$
	}

	@Override
	public String getName() {
		return TOOLCHAIN_ID;
	}

	@Override
	public String getBinaryParserId() {
		return CCorePlugin.PLUGIN_ID + ".ELF"; //$NON-NLS-1$
	}

	@Override
	public String[] getErrorParserIds() {
		return new String[] {
			"org.eclipse.cdt.core.GCCErrorParser", //$NON-NLS-1$
			"org.eclipse.cdt.core.GASErrorParser", //$NON-NLS-1$
			"org.eclipse.cdt.core.GLDErrorParser", //$NON-NLS-1$
			"org.eclipse.cdt.core.GmakeErrorParser", //$NON-NLS-1$
			"org.eclipse.cdt.core.CWDLocator" //$NON-NLS-1$
		};
	}

	@Override
	public IEnvironmentVariable getVariable(String name) {
		return null;
	}

	@Override
	public IEnvironmentVariable[] getVariables() {
		return envVars;
	}

	@Override
	public Path getCommandPath(Path command) {
		if (command.isAbsolute()) {
			return command;
		}

		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			if (!command.toString().endsWith(".exe") //$NON-NLS-1$
					&& !command.toString().endsWith(".bat")) { //$NON-NLS-1$
				command = Paths.get(command.toString() + ".exe"); //$NON-NLS-1$
			}
		}

		// Look for it in the path environment var
		IEnvironmentVariable myPath = getVariable("PATH"); //$NON-NLS-1$
		String path =
				myPath != null ? myPath.getValue() : System.getenv("PATH"); //$NON-NLS-1$
		for (String entry : path.split(File.pathSeparator)) {
			Path entryPath = Paths.get(entry);
			Path cmdPath = entryPath.resolve(command);
			if (Files.isExecutable(cmdPath)) {
				return cmdPath;
			}
		}

		return null;
	}

	@Override
	public String[] getCompileCommands() {
		List<String> compilers = new ArrayList<>();

		if (cmakeCacheMap.containsKey(CMakeCache.CMAKE_C_COMPILER)) {
			compilers.add(cmakeCacheMap.get(CMakeCache.CMAKE_C_COMPILER));
		}

		if (cmakeCacheMap.containsKey(CMakeCache.CMAKE_CXX_COMPILER)) {
			compilers.add(cmakeCacheMap.get(CMakeCache.CMAKE_CXX_COMPILER));
		}

		return compilers.toArray(new String[0]);
	}

	@Override
	public String[] getCompileCommands(ILanguage language) {
		if (GPPLanguage.ID.equals(language.getId())
				&& (cmakeCacheMap.containsKey(CMakeCache.CMAKE_CXX_COMPILER))) {
			return new String[] {
				cmakeCacheMap.get(CMakeCache.CMAKE_CXX_COMPILER)
			};
		} else if (GCCLanguage.ID.equals(language.getId())
				&& (cmakeCacheMap.containsKey(CMakeCache.CMAKE_C_COMPILER))) {
			return new String[] {
				cmakeCacheMap.get(CMakeCache.CMAKE_C_COMPILER)
			};
		} else {
			return new String[0];
		}
	}

	@Override
	public IResource[] getResourcesFromCommand(List<String> cmd,
			URI buildDirectoryURI) {
		/* Start at the back looking for arguments */
		List<IResource> resources = new ArrayList<>();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		for (int i = cmd.size() - 1; i >= 0; --i) {
			String arg = cmd.get(i);
			if (arg.startsWith("-")) { //$NON-NLS-1$
				/* ran into an option, we're done. */
				break;
			}
			if (i > 1 && cmd.get(i - 1).equals("-o")) { //$NON-NLS-1$
				/* this is an output file */
				--i;
				continue;
			}
			try {
				Path srcPath = Paths.get(arg);
				URI uri;
				if (srcPath.isAbsolute()) {
					uri = srcPath.toUri();
				} else {
					uri = Paths.get(buildDirectoryURI).resolve(srcPath).toUri()
							.normalize();
				}

				for (IFile resource : root.findFilesForLocationURI(uri)) {
					resources.add(resource);
				}
			} catch (IllegalArgumentException e) {
				/* Bad URI */
				continue;
			}
		}

		return resources.toArray(new IResource[resources.size()]);
	}

	@Override
	public List<String> stripCommand(List<String> command,
			IResource[] resources) {
		List<String> newCommand = new ArrayList<>();

		for (int i = 0; i < command.size() - resources.length; ++i) {
			String arg = command.get(i);
			if (arg.startsWith("-o")) { //$NON-NLS-1$
				if (arg.equals("-o")) { //$NON-NLS-1$
					i++;
				}
				continue;
			}
			newCommand.add(arg);
		}

		return newCommand;
	}

	public String getCCompiler() {
		return cmakeCacheMap.get(CMakeCache.CMAKE_C_COMPILER);
	}

	public String getCXXCompiler() {
		return cmakeCacheMap.get(CMakeCache.CMAKE_CXX_COMPILER);
	}

	public String getMakeProgram() {
		return cmakeCacheMap.get(CMakeCache.CMAKE_MAKE_PROGRAM);
	}

	public String getGdbProgramPath() {
		return cmakeCacheMap.get(CMakeCache.CMAKE_GDB);
	}

	public String getDebugRunner() {
		return cmakeCacheMap.get(CMakeCache.ZEPHYR_BOARD_DEBUG_RUNNER);
	}

	private void addFromBaseScannerInfo(IExtendedScannerInfo baseScannerInfo,
			List<String> commandLine) {
		if (baseScannerInfo != null) {
			if (baseScannerInfo.getIncludePaths() != null) {
				for (String includePath : baseScannerInfo.getIncludePaths()) {
					commandLine.add("-I" + includePath); //$NON-NLS-1$
				}
			}

			if (baseScannerInfo.getMacroFiles() != null) {
				for (String macroFile : baseScannerInfo.getMacroFiles()) {
					commandLine.add("-imacros"); // $NON-NLS-1$
					commandLine.add(macroFile);
				}
			}

			if (baseScannerInfo.getIncludeFiles() != null) {
				for (String includeFile : baseScannerInfo.getIncludeFiles()) {
					commandLine.add("-include"); // $NON-NLS-1$
					commandLine.add(includeFile);
				}
			}

			if (baseScannerInfo.getDefinedSymbols() != null) {
				for (Map.Entry<String, String> macro : baseScannerInfo
						.getDefinedSymbols().entrySet()) {
					if (macro.getValue() != null
							&& !macro.getValue().isEmpty()) {
						commandLine.add(
								"-D" + macro.getKey() + '=' + macro.getValue()); //$NON-NLS-1$
					} else {
						commandLine.add("-D" + macro.getKey()); //$NON-NLS-1$
					}
				}
			}
		}
	}

	protected void addDiscoveryOptions(List<String> command) {
		command.add("-E"); //$NON-NLS-1$
		command.add("-P"); //$NON-NLS-1$
		command.add("-v"); //$NON-NLS-1$
		command.add("-dD"); //$NON-NLS-1$
	}

	@Override
	public IExtendedScannerInfo getDefaultScannerInfo(
			IBuildConfiguration buildConfig,
			IExtendedScannerInfo baseScannerInfo, ILanguage language,
			URI buildDirectoryURI) {
		/* Default scanner info is handled in build configuration instead. */
		return null;
	}

	@Override
	public IExtendedScannerInfo getScannerInfo(IBuildConfiguration buildConfig,
			List<String> commandStrings, IExtendedScannerInfo baseScannerInfo,
			IResource resource, URI buildDirectoryURI) {

		try {
			Path buildDirectory = Paths.get(buildDirectoryURI);

			int offset = 0;
			Path command = Paths.get(commandStrings.get(offset));

			/* look for ccache being used, and skip it */
			if (command.toString().contains("ccache")) { //$NON-NLS-1$
				command = Paths.get(commandStrings.get(++offset));
			}

			List<String> commandLine = new ArrayList<>();
			if (command.isAbsolute()) {
				commandLine.add(command.toString());
			} else {
				commandLine.add(getCommandPath(command).toString());
			}

			addFromBaseScannerInfo(baseScannerInfo, commandLine);
			addDiscoveryOptions(commandLine);
			commandLine.addAll(
					commandStrings.subList(offset + 1, commandStrings.size()));

			/* Strip surrounding quotes from the args on Windows */
			if (Platform.OS_WIN32.equals(Platform.getOS())) {
				for (int i = 0; i < commandLine.size(); i++) {
					String arg = commandLine.get(i);
					if (arg.startsWith("\"") && arg.endsWith("\"")) { //$NON-NLS-1$ //$NON-NLS-2$
						commandLine.set(i, arg.substring(1, arg.length() - 1));
					}
				}
			}
			/* Change output to stdout */
			boolean haveOut = false;
			for (int i = 1; i < commandLine.size() - 1; ++i) {
				if (commandLine.get(i).equals("-o")) { //$NON-NLS-1$
					commandLine.set(i + 1, "-"); //$NON-NLS-1$
					haveOut = true;
					break;
				}
			}
			if (!haveOut) {
				commandLine.add("-o"); //$NON-NLS-1$
				commandLine.add("-"); //$NON-NLS-1$
			}

			Set<String> includePaths;
			Set<String> macroFiles;
			Set<String> includeFiles;
			if (baseScannerInfo != null) {
				includePaths = new HashSet<>(
						Arrays.asList(baseScannerInfo.getIncludePaths()));
				macroFiles = new HashSet<>(
						Arrays.asList(baseScannerInfo.getMacroFiles()));
				includeFiles = new HashSet<>(
						Arrays.asList(baseScannerInfo.getIncludeFiles()));
			} else {
				includePaths = new HashSet<>();
				macroFiles = new HashSet<>();
				includeFiles = new HashSet<>();
			}

			/* Change source file to a tmp file (needs to be empty) */
			Path tmpFile = null;
			for (int i = 1; i < commandLine.size(); ++i) {
				String arg = commandLine.get(i);
				if (!arg.startsWith("-")) { //$NON-NLS-1$
					Path filePath;
					try {
						filePath = buildDirectory.resolve(arg).normalize();
					} catch (InvalidPathException e) {
						continue;
					}

					IFile[] files = ResourcesPlugin.getWorkspace().getRoot()
							.findFilesForLocationURI(filePath.toUri());
					if ((files.length > 0) && files[0].exists()) {
						/* replace it with a tmp file */
						String extension = files[0].getFileExtension();
						if (extension == null) {
							extension = ".c"; //$NON-NLS-1$
						} else {
							extension = '.' + extension;
						}
						tmpFile = Files.createTempFile(buildDirectory, ".sc", //$NON-NLS-1$
								extension);
						commandLine.set(i, tmpFile.toString());
					}
				} else {
					switch (arg) {
					case "-o": //$NON-NLS-1$
					case "-D": //$NON-NLS-1$
					case "-I": //$NON-NLS-1$
						i++;
						continue;
					case "-imacros": //$NON-NLS-1$
						macroFiles.add(commandLine.get(++i));
						continue;
					case "-isystem": //$NON-NLS-1$
					case "-include": //$NON-NLS-1$
						includePaths.add(commandLine.get(++i));
						continue;
					}

					if (arg.startsWith("-imacros=")) {
						String path = arg.substring("-imacros=".length());
						macroFiles.add(path);
						continue;
					} else if (arg.startsWith("-imacros")) {
						String path = arg.substring("-imacros".length());
						macroFiles.add(path);
						continue;
					}
				}
			}
			if (tmpFile == null) {
				/* No source file found in command line, so skip */
				return null;
			}
			return getScannerInfo(buildConfig, commandLine, buildDirectory,
					tmpFile, includePaths, macroFiles, includeFiles);
		} catch (IOException e) {
			return null;
		}
	}

	private IExtendedScannerInfo getScannerInfo(IBuildConfiguration buildConfig,
			List<String> commandLine, Path buildDirectory, Path tmpFile,
			Set<String> moreIncludePaths, Set<String> moreMacroFiles,
			Set<String> moreIncludeFiles) throws IOException {
		Files.createDirectories(buildDirectory);

		/* Run the command */
		ProcessBuilder processBuilder = new ProcessBuilder(commandLine)
				.directory(buildDirectory.toFile()).redirectErrorStream(true);
		Map<String, String> buildEnv = processBuilder.environment();
		ZephyrHelpers.setupBuildCommandEnvironment(buildConfig.getProject(),
				buildEnv);
		CCorePlugin.getDefault().getBuildEnvironmentManager()
				.setEnvironment(buildEnv, buildConfig, true);
		Process process = processBuilder.start();

		/* Scan for the scanner info */
		Map<String, String> symbols = new HashMap<>();
		Set<String> includePath =
				(moreIncludePaths == null) ? new HashSet<>() : moreIncludePaths;
		Pattern definePattern = Pattern.compile("#define (.*)\\s(.*)"); //$NON-NLS-1$
		boolean inIncludePaths = false;
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(process.getInputStream()))) {
			for (String line = reader.readLine(); line != null; line =
					reader.readLine()) {
				if (inIncludePaths) {
					if (line.equals("End of search list.")) { //$NON-NLS-1$
						inIncludePaths = false;
					} else {
						includePath.add(line.trim());
					}
				} else if (line.startsWith("#define ")) { //$NON-NLS-1$
					Matcher matcher = definePattern.matcher(line);
					if (matcher.matches()) {
						symbols.put(matcher.group(1), matcher.group(2));
					}
				} else if (line.equals("#include <...> search starts here:")) { //$NON-NLS-1$
					inIncludePaths = true;
				}
			}
		}

		try {
			process.waitFor();
		} catch (InterruptedException e) {
		}
		Files.delete(tmpFile);

		Set<String> macroFiles =
				(moreMacroFiles == null) ? new HashSet<>() : moreMacroFiles;
		Set<String> includeFiles =
				(moreIncludeFiles == null) ? new HashSet<>() : moreIncludeFiles;

		return new ExtendedScannerInfo(symbols,
				includePath.toArray(new String[0]),
				macroFiles.toArray(new String[0]),
				includeFiles.toArray(new String[0]));
	}

	private void storeCMakeCacheVarHelper(ScopedPreferenceStore pStore,
			String key) {
		String value = ZephyrHelpers.getProjectPreference(pStore, key);

		if (value != null) {
			cmakeCacheMap.put(key, value);
		}
	}

	public void updateCMakeVarsFromProjectPerfStore(IProject project) {
		ScopedPreferenceStore pStore =
				ZephyrHelpers.getProjectPreferenceStore(project);

		storeCMakeCacheVarHelper(pStore, CMakeCache.CMAKE_C_COMPILER);
		storeCMakeCacheVarHelper(pStore, CMakeCache.CMAKE_CXX_COMPILER);
		storeCMakeCacheVarHelper(pStore, CMakeCache.CMAKE_MAKE_PROGRAM);
		storeCMakeCacheVarHelper(pStore, CMakeCache.CMAKE_GDB);
		storeCMakeCacheVarHelper(pStore, CMakeCache.ZEPHYR_BOARD_DEBUG_RUNNER);
	}

}
