package emediaplayerplugin;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class EMediaPlayerActivator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "eMediaPlayerPlugin"; //$NON-NLS-1$

	// The shared instance
	private static EMediaPlayerActivator plugin;
	
	/**
	 * The constructor
	 */
	public EMediaPlayerActivator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static EMediaPlayerActivator getDefault() {
		return plugin;
	}
	
	public void logException(String message, Exception e) {
		message = message == null ? e.getMessage() : message;
		getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, e));
	}

}
