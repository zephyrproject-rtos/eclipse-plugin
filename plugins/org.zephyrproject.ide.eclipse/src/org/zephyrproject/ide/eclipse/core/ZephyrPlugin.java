/*
 * Copyright (c) 2019 Intel Corporation
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.zephyrproject.ide.eclipse.core;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * The activator class controls the plug-in life cycle
 */
public class ZephyrPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.zephyrproject.ide.eclipse"; //$NON-NLS-1$

	public static final String IMG_ZEPHYR_KITE16 = PLUGIN_ID + "zephyr.kite.16"; //$NON-NLS-1$

	public static final String IMG_ZEPHYR_KITE32 = PLUGIN_ID + "zephyr.kite.32"; //$NON-NLS-1$

	public static final String IMG_ZEPHYR_KITE48 = PLUGIN_ID + "zephyr.kite.48"; //$NON-NLS-1$

	// The shared instance
	private static ZephyrPlugin plugin;

	/**
	 * The constructor
	 */
	public ZephyrPlugin() {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.
	 * BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.
	 * BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static ZephyrPlugin getDefault() {
		return plugin;
	}

	public static String getId() {
		return plugin.getBundle().getSymbolicName();
	}

	public static <T> T getService(Class<T> service) {
		BundleContext context = plugin.getBundle().getBundleContext();
		ServiceReference<T> ref = context.getServiceReference(service);
		return ref != null ? context.getService(ref) : null;
	}

	@Override
	protected ImageRegistry createImageRegistry() {
		ImageRegistry registry = super.createImageRegistry();

		registry.put(IMG_ZEPHYR_KITE16, imageDescriptorFromPlugin(PLUGIN_ID,
				"icons/zephyr-kite-logo_16x16.png")); //$NON-NLS-1$

		registry.put(IMG_ZEPHYR_KITE32, imageDescriptorFromPlugin(PLUGIN_ID,
				"icons/zephyr-kite-logo_32x32.png")); //$NON-NLS-1$

		registry.put(IMG_ZEPHYR_KITE48, imageDescriptorFromPlugin(PLUGIN_ID,
				"icons/zephyr-kite-logo_48x48.png")); //$NON-NLS-1$

		return registry;
	}
}
