package emediaplayerplugin.ui.notifications;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;

/**
 * 
 * @author Prasad
 *
 */

public class EventNotifier {

	public static void notifyInfo(String title, String message) {
		notifyInfo(title, message, null);
	}

	public static void notifyInfo(String title, String message, Runnable onClick) {
		notify(title, message, onClick, NotificationType.INFO);
	}
	
	public static void notifyWarning(String title, String message) {
		notifyWarning(title, message, null);
	}

	public static void notifyWarning(String title, String message, Runnable onClick) {
		notify(title, message, onClick, NotificationType.WARN);
	}
	
	public static void notifyError(String title, String message) {
		notifyError(title, message, null);
	}

	public static void notifyError(String title, String message, Runnable onClick) {
		notify(title, message, onClick, NotificationType.ERROR);
	}
	
	public static void notify(String message, final NotificationType notificationType) {
		notify(null, message, notificationType);
	}

	public static void notify(String title, String message, final NotificationType notificationType) {
		notify(title, message, null, notificationType);
	}

	public static void notify(final String title, final String message, final Runnable onClick, final NotificationType notificationType) {
		Display.getDefault().asyncExec(new  Runnable() {
			@Override
			public void run() {
				NotifierDialog.notify(title, message, onClick, notificationType);
			}
		});
	}
	
	public static void test(){
		notifyInfo(null, "This is a very lengthy info message. This is a very lengthy info message. This is a very lengthy info message" );
		notifyWarning("Warning", "This is a lengthy warning message. This is a lengthy warning message" );
	   	notifyError("Error", "This is a short error message" );
    	notify("This is a message for connection event", NotificationType.CONNECTED);
    	notify("Disconnnected", "This is a message for disconnected event", NotificationType.DISCONNNECTED);
	}
	
	public static Runnable showViewRunnable(final String viewId) {
		return new Runnable() {
			@Override
			public void run() {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(viewId);
						if (view != null) {
							PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().bringToTop(view);
						}
					}
				});
			}
		};
	}
	
}
