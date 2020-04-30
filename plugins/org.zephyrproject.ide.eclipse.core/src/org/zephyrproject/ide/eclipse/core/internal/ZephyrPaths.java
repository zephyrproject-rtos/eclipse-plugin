/*
 * Copyright (c) 2020 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core.internal;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class ZephyrPaths {

	public static IPath[] generateZephyrBaseExclusionList() {

		return new Path[] {
			/*
			 * Files in these directories are not source code.
			 */
			new Path(".git/**"), //$NON-NLS-1$
			new Path(".github/**"), //$NON-NLS-1$
			new Path(".known-issues/**"), //$NON-NLS-1$
			new Path("cmake/**"), //$NON-NLS-1$
			new Path("doc/**"), //$NON-NLS-1$
			new Path("scripts/**"), //$NON-NLS-1$

			/*
			 * Default build and sanity check directories are ignored.
			 */
			new Path("**/build/**"), //$NON-NLS-1$
			new Path("sanity-out/**"), //$NON-NLS-1$

			/*
			 * samples/** and tests/** are excluded because code here is
			 * almost never called in applications.
			 */
			new Path("samples/**"), //$NON-NLS-1$
			new Path("tests/**"), //$NON-NLS-1$

			/*
			 * Ignore non-code files.
			 */
			new Path("CODEOWNERS"), //$NON-NLS-1$
			new Path("CODE_OF_CONDUCT.md"), //$NON-NLS-1$
			new Path("LICENSE"), //$NON-NLS-1$
			new Path("VERSION"), //$NON-NLS-1$
			new Path("dts/**"), //$NON-NLS-1$
			new Path("share/**"), //$NON-NLS-1$
			new Path("**/*_defconfig"), //$NON-NLS-1$
			new Path("**/CMakeLists.txt"), //$NON-NLS-1$
			new Path("**/Kconfig*"), //$NON-NLS-1$
			new Path("**/Makefile"), // $NON-NLS
			new Path("**/README*"), // $NON-NLS
			new Path("**/*.cmake"), //$NON-NLS-1$
			new Path("**/*.cmd"), //$NON-NLS-1$
			new Path("**/*.dts"), //$NON-NLS-1$
			new Path("**/*.dtsi"), //$NON-NLS-1$
			new Path("**/*.h.in"), //$NON-NLS-1$
			new Path("**/*.ld"), //$NON-NLS-1$
			new Path("**/*.py"), //$NON-NLS-1$
			new Path("**/*.pyc"), //$NON-NLS-1$
			new Path("**/*.rst"), //$NON-NLS-1$
			new Path("**/*.sh"), //$NON-NLS-1$
			new Path("**/*.txt"), //$NON-NLS-1$
			new Path("**/*.yml"), //$NON-NLS-1$
			new Path("**/*.yaml"), //$NON-NLS-1$
		};
	}
}
