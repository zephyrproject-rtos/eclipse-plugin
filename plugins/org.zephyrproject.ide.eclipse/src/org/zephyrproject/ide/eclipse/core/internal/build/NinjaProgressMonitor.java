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
public class NinjaProgressMonitor implements IConsoleParser {

	private IProgressMonitor monitor;

	private int lastPercentage;

	private Pattern linePattern;

	public NinjaProgressMonitor(IProgressMonitor m) {
		this.monitor = m;
		this.lastPercentage = 0;
		this.linePattern = Pattern.compile("\\[([0-9]+)/([0-9]+)\\] (.+)");
	}

	@Override
	public boolean processLine(String line) {
		if (!line.startsWith("[")) {
			return false;
		}

		Matcher matcher = linePattern.matcher(line);
		if (matcher.matches()) {
			String sCurStep = matcher.group(1);
			String sAllSteps = matcher.group(2);
			String msg = matcher.group(3);

			try {
				int curStep = Integer.valueOf(sCurStep);
				int allSteps = Integer.valueOf(sAllSteps);

				int percentage = (curStep * 100) / allSteps;

				if (percentage != lastPercentage) {
					monitor.worked(percentage - lastPercentage);
					lastPercentage = percentage;
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
