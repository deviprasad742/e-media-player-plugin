package emediaplayerplugin.ui.notifications;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class NotifierDialog {

	// how long the the tray popup is displayed after fading in (in
	// milliseconds)
	private static int DISPLAY_TIME = 3000;
	// how long each tick is when fading in (in ms)
	private static final int FADE_TIMER = 50;
	// how long each tick is when fading out (in ms)
	private static final int FADE_IN_STEP = 30;
	// how many tick steps we use when fading out
	private static final int FADE_OUT_STEP = 8;

	// how high the alpha value is when we have finished fading in
	private static final int FINAL_ALPHA = 225;

	// contains list of all active popup shells
	private static List<Shell> activeShells = new ArrayList<Shell>();

	// image used when drawing

	/**
	 * Creates and shows a notification dialog with a specific title, message and a
	 * 
	 * @param b
	 * 
	 * @param title
	 * @param message
	 * @param type
	 */
	static void notify(String title, String message, Runnable onClick, final NotificationType notificationType) {
		final Shell thisShell = new Shell(Display.getDefault().getActiveShell(), SWT.NO_FOCUS | SWT.ON_TOP);
		// thisShell.setText(scoreNode.getScore());
		thisShell.setLayout(new FillLayout());
		thisShell.setBackgroundMode(SWT.INHERIT_DEFAULT);

        int noOfColumns = 1;
		noOfColumns = title != null ? ++noOfColumns : noOfColumns;
		
		Composite container = new Composite(thisShell, SWT.NONE);
		GridLayout containerLayout = new GridLayout(noOfColumns, false);
		containerLayout.marginLeft = 5;
		containerLayout.marginTop = 0;
		containerLayout.marginRight = 5;
		containerLayout.marginBottom = 5;
		containerLayout.verticalSpacing = 0;

		container.setLayout(containerLayout);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		thisShell.addListener(SWT.Resize, new Listener() {

			public void handleEvent(Event e) {
				try {
					// get the size of the drawing area
					Rectangle rect = thisShell.getClientArea();
					// create a new image with that size
					final Image newImage = new Image(Display.getDefault(), Math.max(1, rect.width), rect.height);
					// create a GC object we can use to draw with
					GC gc = new GC(newImage);

					// fill background
					gc.setForeground(NotificationColorMapper.getForeGround(notificationType));
					gc.setBackground(NotificationColorMapper.getBackGround(notificationType));
					gc.fillGradientRectangle(rect.x, rect.y, rect.width, rect.height, true);

					// draw shell edge
					gc.setLineWidth(2);
					gc.setForeground(NotificationColorMapper.getBorder(notificationType));
					gc.drawRectangle(rect.x + 1, rect.y + 1, rect.width - 2, rect.height - 2);
					// remember to dipose the GC object!
					gc.dispose();

					// now set the background image on the shell
					thisShell.setBackgroundImage(newImage);
					thisShell.addDisposeListener(new DisposeListener() {
						
						@Override
						public void widgetDisposed(DisposeEvent e) {
							newImage.dispose();
						}
					});

				} catch (Exception err) {
					err.printStackTrace();
				}
			}
		});

		
		if (title != null) {
			Label titleLabel = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(titleLabel);
			titleLabel.setText(title);
			titleLabel.setForeground(NotificationColorMapper.getFontColor(notificationType));
			Font tf = titleLabel.getFont();
			FontData tfd = tf.getFontData()[0];
			tfd.height = 9;
			tfd.setStyle(SWT.BOLD);
			titleLabel.setFont(FontCache.getFont(tfd));
		}
		
		Button closeButton = new Button(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.END, SWT.TOP).applyTo(closeButton);
		closeButton.setText("X");
		closeButton.setForeground(container.getForeground());
		closeButton.setBackground(container.getBackground());
		closeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				disposeShell(thisShell);
			}
		});

		Rectangle clientArea = Display.getDefault().getClientArea();
		int minWidth = (int) (clientArea.width/5.5) ;
		
		Label messageLabel = new Label(container, SWT.WRAP);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).span(noOfColumns, 1).hint(minWidth, SWT.DEFAULT).applyTo(messageLabel);
		messageLabel.setText(message);
		messageLabel.setForeground(NotificationColorMapper.getFontColor(notificationType));
		Font sf = messageLabel.getFont();
		FontData sfd = sf.getFontData()[0];
		sfd.height = 9;
		sfd.setStyle(SWT.ITALIC);
		messageLabel.setFont(FontCache.getFont(sfd));

		minWidth = minWidth + closeButton.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		int minHeight = Math.max(container.computeSize(SWT.DEFAULT, SWT.DEFAULT).y, clientArea.height/10);
		thisShell.setSize(minWidth, minHeight);

		int startX =  clientArea.width - (minWidth + 2);
		int height_offset = minHeight + 2 + getDefaultOffset();
		int startY = clientArea.height - height_offset;
		// move other shells up
		moveShells(thisShell, clientArea, activeShells);

		thisShell.setLocation(startX, startY);
		thisShell.setAlpha(0);
		thisShell.setVisible(true);
		activeShells.add(thisShell);

		container.addMouseListener(getMouseListener(thisShell, onClick));
		messageLabel.addMouseListener(getMouseListener(thisShell, onClick));
		fadeIn(thisShell, notificationType);
	}

	private static void moveShells(final Shell thisShell, Rectangle clientArea, List<Shell> shells2Move) {
		if (!shells2Move.isEmpty()) {
			List<Shell> modifiable = new ArrayList<Shell>(shells2Move);
			Collections.reverse(modifiable);
			for (Shell shell : modifiable) {
				if (!shell.isDisposed()) {
					Point curLoc = shell.getLocation();
					int height = thisShell.getBounds().height;
					int y = curLoc.y - height;
					shell.setLocation(curLoc.x, y);
					boolean dispose = curLoc.y - height < 0;
					if (dispose) {
						shells2Move.remove(shell);
						shell.dispose();
					}
				}
			}
		}
	}

//	private static void createLabel(final Shell thisShell, final Composite container, String labelText, boolean bowler) {
//		Label label = new Label(container, SWT.WRAP);
//		Font font = label.getFont();
//		FontData tlfd = font.getFontData()[0];
//		tlfd.height = 10;
//		if (bowler) {
//			tlfd.setStyle(SWT.NORMAL);
//		}
//		label.setFont(FontCache.getFont(tlfd));
//		GridData gd = new GridData(GridData.FILL_BOTH);
//		gd.horizontalSpan = 2;
//		label.setLayoutData(gd);
//		label.setText(labelText);
//		label.addMouseListener(getMouseListener(thisShell));
//		label.setToolTipText(getMultiLine(scoreNode.getCommentary()));
//	}

	
	private static MouseListener getMouseListener(final Shell thisShell, final Runnable onClick) {
		return new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				if (onClick != null) {
					onClick.run();
				} 
			}
		};
	}

	private static void fadeIn(final Shell _shell, final NotificationType notificationType) {
		Runnable run = new Runnable() {

			public void run() {
				try {
					if (_shell == null || _shell.isDisposed()) {
						return;
					}

					int cur = _shell.getAlpha();
					cur += FADE_IN_STEP;

					if (cur > FINAL_ALPHA) {
						_shell.setAlpha(FINAL_ALPHA);
						startTimer(_shell, notificationType);
						return;
					}

					_shell.setAlpha(cur);
					Display.getDefault().timerExec(FADE_TIMER, this);
				} catch (Exception err) {
					err.printStackTrace();
				}
			}

		};
		Display.getDefault().timerExec(FADE_TIMER, run);
	}

	private static void startTimer(final Shell _shell, NotificationType notificationType) {
		Runnable run = new Runnable() {

			public void run() {
				try {
					if (_shell == null || _shell.isDisposed()) {
						return;
					}

					fadeOut(_shell);
				} catch (Exception err) {
					err.printStackTrace();
				}
			}

		};
		Display.getDefault().timerExec(notificationType == NotificationType.ERROR ? DISPLAY_TIME * 3 : DISPLAY_TIME, run);

	}

	private static void fadeOut(final Shell _shell) {
		final Runnable run = new Runnable() {

			public void run() {
				try {
					if (_shell == null || _shell.isDisposed()) {
						return;
					}

					int cur = _shell.getAlpha();
					cur -= FADE_OUT_STEP;

					if (cur <= 0) {
						disposeShell(_shell);
						return;
					}

					_shell.setAlpha(cur);

					if (_shell.getAlpha() != cur) {
						disposeShell(_shell);
						return;
					}

					Display.getDefault().timerExec(FADE_TIMER, this);

				} catch (Exception err) {
					err.printStackTrace();
				}
			}

		};
		Display.getDefault().timerExec(FADE_TIMER, run);

	}

	private static void disposeShell(final Shell _shell) {
		_shell.setAlpha(0);
		_shell.dispose();
		if (activeShells.contains(_shell)) {
			activeShells.remove(_shell);
		}
	}

	private static int getDefaultOffset() {
		String os = System.getProperty("os.name").toLowerCase();
		boolean isLinux = os.indexOf("nux") >= 0;
		if (isLinux) {
			Rectangle clientArea = Display.getDefault().getClientArea();
			return clientArea.height / 25;
		}
		return 0;
	}

}
