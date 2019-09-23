/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.internal.build;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.cdt.core.parser.ExtendedScannerInfo;
import org.eclipse.cdt.core.parser.IExtendedScannerInfo;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.zephyrproject.ide.eclipse.core.ZephyrPlugin;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Scanner Information Caching class
 */
public class ZephyrScannerInfoCache {

	private static final String SCANNER_INFO_CACHE = "scannerInfoCache";

	private Map<Integer, ExtendedScannerInfo> cacheMap;

	private Map<String, ExtendedScannerInfo> defaultCacheMap;

	private IBuildConfiguration config;

	public ZephyrScannerInfoCache(IBuildConfiguration config) {
		this.cacheMap = new TreeMap<>();
		this.defaultCacheMap = new TreeMap<>();
		this.config = config;

		getScannerInfoCachePath().toFile().mkdirs();
		readCache();
	}

	/**
	 * @return Path to store the scanner info cache files.
	 */
	private IPath getScannerInfoCachePath() {
		return config.getProject().getWorkingLocation(ZephyrPlugin.PLUGIN_ID)
				.append(SCANNER_INFO_CACHE);
	}

	/**
	 * Read the cache files from storage.
	 */
	private void readCache() {
		File cacheFile = getScannerInfoCachePath().append("cacheMap").toFile(); //$NON-NLS-1$

		if (cacheFile.exists()) {
			try (FileReader reader = new FileReader(cacheFile)) {
				Gson gson = new Gson();
				synchronized (this) {
					Type mapType =
							new TypeToken<Map<Integer, ExtendedScannerInfo>>() {
							}.getType();
					cacheMap = gson.fromJson(reader, mapType);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		cacheFile =
				getScannerInfoCachePath().append("defaultCacheMap").toFile(); //$NON-NLS-1$

		if (cacheFile.exists()) {
			try (FileReader reader = new FileReader(cacheFile)) {
				Gson gson = new Gson();
				synchronized (this) {
					Type mapType =
							new TypeToken<Map<String, ExtendedScannerInfo>>() {
							}.getType();
					defaultCacheMap = gson.fromJson(reader, mapType);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Write the cached info object into storage.
	 */
	public void writeCache() {
		File cacheFile = getScannerInfoCachePath().append("cacheMap").toFile(); //$NON-NLS-1$

		try (FileWriter writer = new FileWriter(cacheFile)) {
			Gson gson = new Gson();
			synchronized (this) {
				gson.toJson(cacheMap, writer);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		cacheFile =
				getScannerInfoCachePath().append("defaultCacheMap").toFile(); //$NON-NLS-1$

		try (FileWriter writer = new FileWriter(cacheFile)) {
			Gson gson = new Gson();
			synchronized (this) {
				gson.toJson(defaultCacheMap, writer);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/**
	 * Retrieve a scanner info object associated with a file.
	 *
	 * @param resource Resource object to the file.
	 * @return IExtendedScannerInfo object if file is cached, null otherwise.
	 */
	public IExtendedScannerInfo getScannerInfo(IResource resource) {
		if ((resource != null) && (resource instanceof IFile)) {
			Integer hash = resource.getLocation().toOSString().hashCode();
			IExtendedScannerInfo info;
			synchronized (this) {
				info = cacheMap.get(hash);
			}

			return info;
		}
		return null;
	}

	/**
	 * Put a scanner info object associated with a file into cache.
	 *
	 * @param resource Resource object to the file.
	 * @param info Scanner info object associated with the file.
	 */
	public void setScannerInfo(IResource resource, IExtendedScannerInfo info) {
		if ((resource != null) && (info != null) && (resource instanceof IFile)
				&& (info instanceof ExtendedScannerInfo)) {
			Integer hash = resource.getLocation().toOSString().hashCode();
			ExtendedScannerInfo esi = (ExtendedScannerInfo) info;
			synchronized (this) {
				cacheMap.put(hash, esi);
			}
		}
	}

	/**
	 * Retrieve a generic scanner info object using the file extension of
	 * the resource.
	 *
	 * @param resource Resource object to the file.
	 * @return IExtendedScannerInfo object if file is cached, null otherwise.
	 */
	public IExtendedScannerInfo getDefaultScannerInfo(IResource resource) {
		if ((resource != null) && (resource instanceof IFile)) {
			String ext = resource.getLocation().getFileExtension();
			IExtendedScannerInfo info = null;

			if (ext != null) {
				/* If there is an extension... */
				synchronized (this) {
					info = defaultCacheMap.get(ext.toLowerCase());
				}
			}

			if (info == null) {
				/* The ".c" scanner is the default one. */
				synchronized (this) {
					info = defaultCacheMap.get("c"); //$NON-NLS-1$
				}
			}

			return info;
		}

		return null;
	}

	/**
	 * Put a generic scanner info object into cache.
	 *
	 * @param resource Resource object to the file.
	 * @param info Scanner info object associated with the file extension.
	 */
	public void setDefaultScannerInfo(IResource resource,
			IExtendedScannerInfo info) {
		if ((resource != null) && (info != null) && (resource instanceof IFile)
				&& (info instanceof ExtendedScannerInfo)) {

			String ext = resource.getLocation().getFileExtension();
			ExtendedScannerInfo esi = (ExtendedScannerInfo) info;
			if (ext != null) {
				/* If there is an extension... */
				synchronized (this) {
					defaultCacheMap.put(ext.toLowerCase(), esi);
				}
			}
		}
	}
}
