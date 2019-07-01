/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.internal.build;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.IConsoleParser;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Simple console parser to update progress due to build output
 */
public class MakefileProgressMonitor implements IConsoleParser {

	private IProgressMonitor monitor;

	private int lastPercentage;

	private Pattern linePattern;

	public MakefileProgressMonitor(IProgressMonitor m) {
		this.monitor = m;
		this.lastPercentage = 0;
		this.linePattern = Pattern.compile("\\[[ ]?([0-9]+)%\\] (.+)");
	}

	@Override
	public boolean processLine(String line) {
		if (!line.startsWith("[")) {
			return false;
		}

		Matcher matcher = linePattern.matcher(line);
		if (matcher.matches()) {
			String percentage = matcher.group(1);
			String msg = matcher.group(2);

			try {
				int p = Integer.valueOf(percentage);
				if (p != lastPercentage) {
					monitor.worked(p - lastPercentage);
					lastPercentage = p;
				}
			} catch (NumberFormatException nfe) {

			}

			monitor.subTask(msg);

			return true;
		}

		return false;
	}

	@Override
	public void shutdown() {
	}

}
