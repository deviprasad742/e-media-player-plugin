package emediaplayerplugin.actions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

import emediaplayerplugin.EMediaPlayerActivator;
import emediaplayerplugin.ui.EMediaView;
import emediaplayerplugin.ui.notifications.EventNotifier;
import emediaplayerplugin.ui.notifications.NotificationType;

public class CheckForUpdatesAction extends Action {
	private static final String PLUGIN_LOCATION_URL = "http://e-media-player-plugin.googlecode.com/svn/update-site/eMediaPlayerUpdateSite/plugins/";
	private static final String PLUGIN_REGEX = ".*(eMediaPlayerPlugin_(.*).jar).*";
	
	public CheckForUpdatesAction() {
		super("Check For Updates");
	}

	@Override
	public void run() {
		Job checkForUpdatesJob = new Job("Find eMediaPlayerPlugin Updates") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Checking for latest version of the software", IProgressMonitor.UNKNOWN);
				checkForUpdates();
				return Status.OK_STATUS;
			}
		};
		checkForUpdatesJob.schedule();
	}
	
	private void checkForUpdates() {
		Bundle bundle = EMediaPlayerActivator.getDefault().getBundle();
		String current_version = (String)bundle.getHeaders().get("Bundle-Version");
		String onlineVersion = getPluginVersion(PLUGIN_LOCATION_URL);
		if (!current_version.equals(onlineVersion)) {
			Runnable onClick = new Runnable() {
				@Override
				public void run() {
					try {
						PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(PLUGIN_LOCATION_URL));
					} catch (Exception e) {
						EMediaPlayerActivator.getDefault().logException(e);
					}
				}
			};
			
			String symbolicName = EMediaPlayerActivator.getDefault().getBundle().getSymbolicName();
			String message = "A newer version of the '" + symbolicName + "' is available. Click here to view the plugin.";
		    EventNotifier.notify("Update Available", message, onClick, NotificationType.DISCONNNECTED);
		}
	}
	
	private static String getPluginVersion(String urlString) {
		try {
			URL url = new URL(urlString);
			HttpURLConnection connection = null;
			try {
				connection = (HttpURLConnection) url.openConnection();
				connection.connect();
				InputStreamReader inStream = new InputStreamReader(connection.getInputStream());
				BufferedReader buff = null;
				try {
					buff = new BufferedReader(inStream);
					while (true) {
						String nextLine = buff.readLine();
						if (nextLine != null) {
							Pattern pattern = Pattern.compile(PLUGIN_REGEX);
							Matcher matcher = pattern.matcher(nextLine);
							if (matcher.matches()) {
								return matcher.group(2).trim();
							}
						} else {
							break;
						}
					}
				} finally {
					if (buff != null) {
						buff.close();
					}
				}
			} catch (IOException e) {
				EMediaView.showAndLogError("Find Updates", null, e);
			} finally {
				connection.disconnect();
			}

		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		return null;
	}
	
	public static void main(String[] args) {
		String test_plugin = "<li><a href=\"eMediaPlayerPlugin_1.0.0.201110261759.jar\">eMediaPlayerPlugin_1.0.0.201110261759.jar</a></li>";
	    Matcher matcher = Pattern.compile(PLUGIN_REGEX).matcher(test_plugin);
	    System.out.println(matcher.matches());
	    System.out.println(matcher.group(2));
	   
	}
	
}
