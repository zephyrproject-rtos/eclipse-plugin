/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.build;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
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

import org.eclipse.cdt.build.gcc.core.GCCToolChain;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.build.IToolChainProvider;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.ExtendedScannerInfo;
import org.eclipse.cdt.core.parser.IExtendedScannerInfo;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.zephyrproject.ide.eclipse.core.ZephyrStrings;
import org.zephyrproject.ide.eclipse.core.internal.ZephyrHelpers;
import org.zephyrproject.ide.eclipse.core.internal.build.CMakeCache;

/**
 * Wrapper of GCCToolChain to build Zephyr Applications.
 */
public abstract class ZephyrApplicationToolChain extends GCCToolChain {

	public static final String TOOLCHAIN_OS = "zephyr"; //$NON-NLS-1$

	private HashMap<String, String> cmakeCacheMap;

	public ZephyrApplicationToolChain(IToolChainProvider provider, String id) {
		this(provider, id, ZephyrStrings.EMPTY_STRING);
	}

	public ZephyrApplicationToolChain(IToolChainProvider provider, String id,
			IBuildConfiguration config) {
		this(provider, id, ZephyrStrings.EMPTY_STRING);
	}

	public ZephyrApplicationToolChain(IToolChainProvider provider, String id,
			String version) {
		super(provider, id, version);
		super.setProperty(ATTR_OS, TOOLCHAIN_OS);
		this.cmakeCacheMap = new HashMap<>();
	}

	@Override
	public String getProperty(String key) {
		String value = super.getProperty(key);
		if (value != null) {
			return value;
		}

		return null;
	}

	@Override
	public String getBinaryParserId() {
		return CCorePlugin.PLUGIN_ID + ".ELF"; //$NON-NLS-1$
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

	public String getCCompiler() {
		return cmakeCacheMap.get(CMakeCache.CMAKE_C_COMPILER);
	}

	public String getCXXCompiler() {
		return cmakeCacheMap.get(CMakeCache.CMAKE_CXX_COMPILER);
	}

	public String getMakeProgram() {
		return cmakeCacheMap.get(CMakeCache.CMAKE_MAKE_PROGRAM);
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
		}
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

			List<String> commandLine = new ArrayList<>();
			commandLine.add(commandStrings.get(0)); /* path to compiler */

			addFromBaseScannerInfo(baseScannerInfo, commandLine);
			addDiscoveryOptions(commandLine);
			commandLine
					.addAll(commandStrings.subList(1, commandStrings.size()));

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
			int i = 1;
			while (i < commandLine.size()) {
				/*
				 * There is no need to parse -I for include paths, because
				 * they are recovered from running the compiler with
				 * discovery options.
				 */
				if (commandLine.get(i).equals("-imacros")) { //$NON-NLS-1$
					/* Don't replace macro files */
					i++;
					macroFiles.add(commandLine.get(i));
					i++;
					continue;
				} else if (commandLine.get(i).equals("-include")) { //$NON-NLS-1$
					/* Don't replace include files */
					i++;
					includeFiles.add(commandLine.get(i));
					i++;
					continue;
				} else if (!commandLine.get(i).startsWith("-")) { //$NON-NLS-1$
					Path filePath = buildDirectory.resolve(commandLine.get(i));
					IFile[] files = ResourcesPlugin.getWorkspace().getRoot()
							.findFilesForLocationURI(filePath.toUri());
					if (files.length > 0) {
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
				}
				i++;
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
		String value = ZephyrHelpers.getPrefStringOrNull(pStore, key);

		if (value != null) {
			cmakeCacheMap.put(key, value);
		}
	}

	public void initCMakeVarsFromProjectPerfStore(IProject project) {
		ScopedPreferenceStore pStore =
				ZephyrHelpers.getProjectPreferenceStore(project);

		storeCMakeCacheVarHelper(pStore, CMakeCache.CMAKE_C_COMPILER);
		storeCMakeCacheVarHelper(pStore, CMakeCache.CMAKE_CXX_COMPILER);
		storeCMakeCacheVarHelper(pStore, CMakeCache.CMAKE_MAKE_PROGRAM);
	}

}
