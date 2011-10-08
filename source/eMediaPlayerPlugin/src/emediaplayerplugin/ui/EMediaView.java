package emediaplayerplugin.ui;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.ole.win32.OLE;
import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.OleClientSite;
import org.eclipse.swt.ole.win32.OleFrame;
import org.eclipse.swt.ole.win32.Variant;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.part.ViewPart;

public class EMediaView extends ViewPart {

	private TabFolder container;
	private OleClientSite wmPlayerSite;

	public EMediaView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		 parent.setLayout(new GridLayout());
         container = new TabFolder(parent, SWT.NONE);
         container.setLayout(new GridLayout());
         container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
         createPlayerSection();
         addFileMenu();
	}

	private void createPlayerSection() {
		TabItem playerTab = new TabItem(container, SWT.NONE);
		playerTab.setText("Player");
		try {
			OleFrame frame = new OleFrame(container, SWT.NONE);
			wmPlayerSite = new OleClientSite(frame, SWT.NONE, "WMPlayer.OCX");
			wmPlayerSite.doVerb(OLE.OLEIVERB_PRIMARY);
			playerTab.setControl(frame);
		} catch (SWTError e) {
			System.out.println("Unable to open activeX control");
			return;
		}
	}

	
	private void addFileMenu() {
		IToolBarManager tookBarManager = getViewSite().getActionBars().getToolBarManager();
		tookBarManager.add(new Action("Open") {
			@Override
			public void run() {
				fileOpen();
			}
		});

		
	}

	private void fileOpen() {
		FileDialog dialog = new FileDialog(wmPlayerSite.getShell(), SWT.OPEN);
		dialog.setFilterExtensions(new String[] { "*.mp3" });
		String filename = dialog.open();
		if (filename != null) {
			OleAutomation player = new OleAutomation(wmPlayerSite);
			OleAutomation playList = getProperty(player, "currentPlaylist");
			Variant media = invoke(player, "newMedia", filename);
			if (playList != null) {
				Variant count = getSimpleProperty(playList, "count");
				invoke(playList, "appendItem", media);
				if (count.getInt() == 0) {
					invoke(getProperty(player, "controls"), "play");
				}
			} else { 
				int playURL[] = player.getIDsOfNames(new String[] { "URL" });
				if (playURL != null) {
					Variant theFile = new Variant(filename);
					player.setProperty(playURL[0], theFile);
				}
			}
			
			player.dispose();
		}
	}
	
	private static Variant getSimpleProperty(OleAutomation auto, String name) {
		Variant varResult = auto.getProperty(property(auto, name));
		if (varResult != null && varResult.getType() != OLE.VT_EMPTY) {
			return varResult;
		}
		return null;
	}
	
	private static OleAutomation getProperty(OleAutomation auto, String name) {
		Variant varResult = auto.getProperty(property(auto, name));
		if (varResult != null && varResult.getType() != OLE.VT_EMPTY) {
			OleAutomation result = null;
			try {
				result = varResult.getAutomation();
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println(varResult);
			}
			varResult.dispose();
			return result;
		}
		return null;
	}
	
	private static int property(OleAutomation auto, String name) {
		return auto.getIDsOfNames(new String[] { name })[0];
	}

	private static Variant invoke(OleAutomation auto, String command,
			Variant value) {
		return auto.invoke(property(auto, command),
				new Variant[] { value });
	}

	
	private static Variant invoke(OleAutomation auto, String command,
			String value) {
		return auto.invoke(property(auto, command),
				new Variant[] { new Variant(value) });
	}

	private static Variant invoke(OleAutomation auto, String command) {
		return auto.invoke(property(auto, command));
	}
	
	@Override
	public void setFocus() {

	}

}
