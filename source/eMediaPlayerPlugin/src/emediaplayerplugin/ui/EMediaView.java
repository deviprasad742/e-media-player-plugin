package emediaplayerplugin.ui;

import java.io.File;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.ole.win32.OLE;
import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.OleControlSite;
import org.eclipse.swt.ole.win32.OleFrame;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import emediaplayerplugin.model.IMediaPlayerListener;
import emediaplayerplugin.model.MediaFile;
import emediaplayerplugin.model.MediaPlayer;

public class EMediaView extends ViewPart {

	public static final String ID = "emediaplayerplugin.ui.EMediaView";
	
	private TabFolder container;
	private OleControlSite wmPlayerSite;
	private MediaPlayer mediaPlayer;
	private TableViewer playListViewer;
	public static final String NAME = "Name";
	public static final String DURATION = "Duration";
	public static final String URL = "Location";

	public EMediaView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		 parent.setLayout(new GridLayout());
         container = new TabFolder(parent, SWT.NONE);
         container.setLayout(new GridLayout());
         container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
         createPlayerSection();
         createPlayListSection();
         addDisposeListeners();
	}


	private void addDisposeListeners() {
		getSite().getWorkbenchWindow().getWorkbench().addWorkbenchListener(new IWorkbenchListener() {
			
			@Override
			public boolean preShutdown(IWorkbench workbench, boolean forced) {
				try {
					mediaPlayer.savePlaylist();
					mediaPlayer.dispose();
				} catch (Exception e) {
					e.printStackTrace();
				}
				return true;
			}
			
			@Override
			public void postShutdown(IWorkbench workbench) {
				
			}
		});
	}

	private void createPlayerSection() {
		TabItem playerTab = new TabItem(container, SWT.NONE);
		playerTab.setText("Player");
		try {
			OleFrame frame = new OleFrame(container, SWT.NONE);
			wmPlayerSite = new OleControlSite(frame, SWT.NONE, "WMPlayer.OCX");
			wmPlayerSite.doVerb(OLE.OLEIVERB_PRIMARY);
			mediaPlayer = new MediaPlayer(new OleAutomation(wmPlayerSite));
			playerTab.setControl(frame);
		} catch (SWTError e) {
			System.out.println("Unable to open activeX control");
			return;
		}
	}

	private void createPlayListSection() {
		TabItem playListTab = new TabItem(container, SWT.NONE);
		playListTab.setText("Playlist");

		Composite playListComposite = new Composite(container, SWT.NONE);
		playListTab.setControl(playListComposite);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(playListComposite);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(playListComposite);
		
		addPlayListViewer(playListComposite);
		
		Composite buttonsComposite = new Composite(playListComposite, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(buttonsComposite);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(buttonsComposite);
		addButtons(buttonsComposite);
	}

	private void addButtons(Composite buttonsComposite) {
		Button addButton = new Button(buttonsComposite, SWT.PUSH);
		addButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD));
		addButton.setToolTipText("Add To Playlist");
		addButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(wmPlayerSite.getShell(), SWT.OPEN | SWT.MULTI);
				dialog.setFilterExtensions(new String[] { "*.mp3" });
				String file = dialog.open();
				if (file != null) {
					String[] fileNames = dialog.getFileNames();
					for (String filename : fileNames) {
						mediaPlayer.addToPlayList(dialog.getFilterPath() + File.separator + filename, true);
					}
				}
			
			}
		});
		
		Button deleteButton = new Button(buttonsComposite, SWT.PUSH);
		deleteButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_DELETE));
		deleteButton.setToolTipText("Remove From Playlist");
		deleteButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int[] selectionIndices = playListViewer.getTable().getSelectionIndices();
				int length = selectionIndices.length;
				while (length > 0) {
					mediaPlayer.removeItem(selectionIndices[0]);
					length--;
				}
				if (selectionIndices.length > 0) {
					int newIndex = selectionIndices[selectionIndices.length - 1] - 1;
					if (newIndex == -1) {
						newIndex ++;
					}
					playListViewer.getTable().select(newIndex);
				}
			}
		});
		
		Button clearButton = new Button(buttonsComposite, SWT.PUSH);
		clearButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_CLEAR));
		clearButton.setToolTipText("Clear Playlist");
		clearButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int count = playListViewer.getTable().getItemCount();
				while (count > 0) {
					mediaPlayer.removeItem(--count);
				}
				
			}
		});

	}
	

	private void addPlayListViewer(Composite playListComposite) {
		playListViewer = new TableViewer(playListComposite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(playListViewer.getTable());
		playListViewer.getTable().setHeaderVisible(true);
		playListViewer.getTable().setLinesVisible(true);
		
		TableViewerColumn nameColumn = new TableViewerColumn(playListViewer, SWT.NONE);
		nameColumn.getColumn().setText(NAME);
		nameColumn.getColumn().setWidth(300);
		
		TableViewerColumn durationColumn = new TableViewerColumn(playListViewer, SWT.NONE);
		durationColumn.getColumn().setText(DURATION);
		durationColumn.getColumn().setWidth(100);
		
		TableViewerColumn urlColumn = new TableViewerColumn(playListViewer, SWT.NONE);
		urlColumn.getColumn().setText(URL);
		urlColumn.getColumn().setWidth(300);
		playListViewer.setColumnProperties(new String[] {NAME, DURATION, URL});

		playListViewer.setContentProvider(new ArrayContentProvider());
		playListViewer.setLabelProvider(new PlaylistLabelProvider());
		playListViewer.setInput(mediaPlayer.getPlayList());
		
		mediaPlayer.setListener(new IMediaPlayerListener() {
			@Override
			public void handleEvent(int eventKind) {
				playListViewer.refresh();
			}
		});
		
		playListViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				int index = playListViewer.getTable().getSelectionIndex();
				mediaPlayer.playItem(index);
			}
		});
		
	}
	

	private class PlaylistLabelProvider implements ITableLabelProvider, IColorProvider {
	
		@Override
		public void addListener(ILabelProviderListener listener) {
			
		}

		@Override
		public void dispose() {
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
			
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof MediaFile) {
				MediaFile mediaFile = (MediaFile) element;
				switch (columnIndex) {
				case 0:
					return mediaFile.getName();
				case 1:
					return mediaFile.getDuration();
				default:
					return mediaFile.getUrl();
				}
				
			}
			return null;
		}

		@Override
		public Color getForeground(Object element) {
			return null;
		}

		@Override
		public Color getBackground(Object element) {
			return null;
		}
		
	}
	
	public MediaPlayer getMediaPlayer() {
		return mediaPlayer;
	}
	
	@Override
	public void setFocus() {

	}
	
	public static EMediaView getMediaView() {
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IViewPart findView = activePage.findView(ID);
		return (EMediaView) findView;
	}
	
	public static EMediaView showView() {
		IWorkbenchPage activePage = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();
		try {
			return (EMediaView) activePage.showView(ID);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		return null;
	}

}
