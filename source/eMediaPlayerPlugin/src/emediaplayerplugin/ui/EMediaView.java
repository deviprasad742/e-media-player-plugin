package emediaplayerplugin.ui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.ole.win32.OLE;
import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.OleControlSite;
import org.eclipse.swt.ole.win32.OleFrame;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import emediaplayerplugin.EMediaPlayerActivator;
import emediaplayerplugin.model.EMediaConstants;
import emediaplayerplugin.model.FavMedia;
import emediaplayerplugin.model.FavouritesRepository;
import emediaplayerplugin.model.IListener;
import emediaplayerplugin.model.MediaFile;
import emediaplayerplugin.model.MediaLibrary;
import emediaplayerplugin.model.MediaPlayer;

/**
 * 
 * @author Prasad
 * 
 */

public class EMediaView extends ViewPart {

	public static final String ID = "emediaplayerplugin.ui.EMediaView";

	private TabFolder container;
	private OleControlSite wmPlayerSite;
	private MediaPlayer mediaPlayer;
	private TableViewer playListViewer;
	private TreeViewer libraryViewer;
	private Text musicLibraryFilterText;

	private MediaLibrary mediaLibrary;
	public static final String NAME = "Name";
	public static final String DURATION = "Duration";
	public static final String ALBUM = "Album";
	public static final String URL = "Location";
	public static final int FOLDER_LIMIT = 300;

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
		createLibrarySection();
		addDisposeListeners();
	}

	private Job syncJob = new Job("Refresh") {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask("Refreshing library", IProgressMonitor.UNKNOWN);
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					setLibraryButtonsEnabled(false);
				}
			});
			mediaLibrary.syncAll();
			try {
				favouritesRepository.syncRepositories();
			} catch (Exception e) {
				EMediaPlayerActivator.getDefault().logException(e);
			}
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					setLibraryButtonsEnabled(true);
					libraryViewer.setInput(mediaLibrary);
				}
			});

			return Status.OK_STATUS;
		}

	};

	private FavouritesRepository favouritesRepository;

	private void setLibraryButtonsEnabled(boolean enable) {
		syncAllButton.setEnabled(enable);
		pathsButton.setEnabled(enable);
	}

	public void refreshLibraryView() {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				Object[] elements = libraryViewer.getExpandedElements();
				libraryViewer.refresh();
				libraryViewer.setExpandedElements(elements);
			}
		});

	}

	private void createLibrarySection() {
		TabItem libraryTab = new TabItem(container, SWT.NONE);
		libraryTab.setText("Library");

		mediaLibrary = new MediaLibrary(LibraryPathsDialog.getLocalPath(), LibraryPathsDialog.getRemotePath());
		mediaLibrary.setListener(new IListener() {
			@Override
			public void handleEvent(int eventKind) {
				refreshLibraryView();
			}
		});

		// TODO:
		favouritesRepository = new FavouritesRepository(mediaLibrary.getRemotePath(), mediaLibrary);
		favouritesRepository.setListener(new IListener() {
			@Override
			public void handleEvent(int eventKind) {
				refreshLibraryView();
			}
		});

		Composite libraryComposite = new Composite(container, SWT.NONE);
		libraryTab.setControl(libraryComposite);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(libraryComposite);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(libraryComposite);

		Composite filterComposite = new Composite(libraryComposite, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(filterComposite);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(filterComposite);

		String toolTip = "Type text and press 'Enter' key to filter songs based on the text. Files will be automatically filtered without pressing enter key if there are optimum number of folders";
		Label label = new Label(filterComposite, SWT.NONE);
		label.setText("Filter: ");
		label.setToolTipText(toolTip);
		GridDataFactory.swtDefaults().applyTo(label);

		musicLibraryFilterText = new Text(filterComposite, SWT.BORDER);
		musicLibraryFilterText.setToolTipText(toolTip);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(musicLibraryFilterText);
		musicLibraryFilterText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (!isEnterMode) {
					refreshLibraryView();
				}
			}
		});

		musicLibraryFilterText.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == '\r' && isEnterMode) {
					refreshLibraryView();
				}
			}
		});

		libraryViewer = new TreeViewer(libraryComposite);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(libraryViewer.getControl());
		libraryViewer.setContentProvider(new LibraryContentProvider());
		libraryViewer.setLabelProvider(new LibraryLabelProvider());
		libraryViewer.setSorter(new ViewerSorter());
		addLibraryButtonSection(libraryComposite);
		libraryViewer.setFilters(new ViewerFilter[] { libraryFilter });
		ColumnViewerToolTipSupport.enableFor(libraryViewer);

		libraryViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Object element = ((IStructuredSelection) libraryViewer.getSelection()).getFirstElement();
				if (libraryViewer.isExpandable(element)) {
					libraryViewer.setExpandedState(element, !libraryViewer.getExpandedState(element));
				} else if (element instanceof File) {
					downloadFiles(Arrays.asList((File) element), true, true);
				}
			}
		});

		syncJob.schedule();
		hookContextMenu();
	}

	private ViewerFilter libraryFilter = new ViewerFilter() {

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			String filter = musicLibraryFilterText.getText().trim();
			ILabelProvider labelProvider = (ILabelProvider) libraryViewer.getLabelProvider();
			if (!filter.isEmpty()) {
				if (element instanceof String) {
					if (isMatch(filter, labelProvider.getText(element))) {
						return true;
					}
					Object[] children = ((ITreeContentProvider) libraryViewer.getContentProvider()).getChildren(element);
					for (Object child : children) {
						String text = labelProvider.getText(child);
						if (isMatch(filter, text)) {
							return true;
						}
					}
				} else if (element instanceof File) {
					String text = labelProvider.getText(element);
					String parentText = labelProvider.getText(parentElement);
					return isMatch(filter, text) || isMatch(filter, parentText);
				}
			}
			return filter.isEmpty();
		}

		private boolean isMatch(String filter, String text) {
			return text.toLowerCase().contains(filter.toLowerCase());
		}
	};

	private Button syncAllButton;

	private Button pathsButton;

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fillMediaLibraryContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(libraryViewer.getControl());
		libraryViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, libraryViewer);
	}

	private void fillMediaLibraryContextMenu(IMenuManager manager) {
		final IStructuredSelection structuredSelection = (IStructuredSelection) libraryViewer.getSelection();
		final List<File> selectedFiles = getSelectedFiles(structuredSelection);
		boolean singleSelection = structuredSelection.size() == 1;
		manager.add(new Action("Play") {
			@Override
			public void run() {
				downloadFiles(selectedFiles, true, true);
			}
		});

		manager.add(new Action("Eneque") {
			@Override
			public void run() {
				downloadFiles(selectedFiles, true, false);
			}
		});
		manager.add(new Separator());

		final List<File> favFiles2Add = new ArrayList<File>();
		final List<File> favFiles2Remove = new ArrayList<File>();
		for (File file : selectedFiles) {
			if (favouritesRepository.isLocalFavMedia(file.getAbsolutePath())) {
				favFiles2Remove.add(file);
			} else {
				favFiles2Add.add(file);
			}
		}

		if (!favFiles2Add.isEmpty()) {
			manager.add(new Action("Add To Favourites") {
				@Override
				public void run() {
					for (File file : favFiles2Add) {
						try {
							favouritesRepository.addToFavourites(file);
						} catch (Exception e) {
							EMediaPlayerActivator.getDefault().logException(e);
						}
					}
				}
			});
		}

		if (!favFiles2Remove.isEmpty()) {
			manager.add(new Action("Remove From Favourites") {
				@Override
				public void run() {
					for (File file : favFiles2Remove) {
						try {
							favouritesRepository.removeFromFavourites(file);
						} catch (Exception e) {
							EMediaPlayerActivator.getDefault().logException(e);
						}
					}
				}
			});
		}

		manager.add(new Separator());

		final List<File> files2Download = new ArrayList<File>();
		for (File file : selectedFiles) {
			if (!mediaLibrary.isLocalFile(file)) {
				files2Download.add(file);
			}
		}

		if (!files2Download.isEmpty()) {
			manager.add(new Action("Download") {
				@Override
				public void run() {
					downloadFiles(files2Download, false, false);
				}
			});
			manager.add(new Separator());
		}

		final List<File> files2Delete = new ArrayList<File>();
		for (File file : selectedFiles) {
			if (mediaLibrary.isLocalFile(file)) {
				files2Delete.add(file);
			}
		}

		if (!files2Delete.isEmpty()) {
			manager.add(new Action("Delete") {
				@Override
				public void run() {
					boolean proceed = MessageDialog.openQuestion(Display.getDefault().getActiveShell(), "Confirm Delete",
							"Are you sure you want to delete the selected '" + files2Delete.size() + "' file(s)?");
					if (proceed) {
						mediaLibrary.removeLocalFiles(files2Delete);
					}
				}
			});
			manager.add(new Separator());
		}
		final List<File> files2Share = new ArrayList<File>();
		for (File file : selectedFiles) {
			if (mediaLibrary.isRemoteShareRequired(file)) {
				files2Share.add(file);
			}
		}

		if (!files2Share.isEmpty()) {
			manager.add(new Action("Share") {
				@Override
				public void run() {
					shareFiles(files2Share);
				}
			});
			manager.add(new Separator());
		}

		if (singleSelection) {
			manager.add(new Action("Open Location") {
				@Override
				public void run() {
					File file = selectedFiles.get(0);
					try {
						Desktop.getDesktop().browse(file.getParentFile().toURI());
					} catch (IOException e) {
						EMediaPlayerActivator.getDefault().logException(e);
					}
				}
			});
			manager.add(new Separator());
		}

	}

	private List<File> getSelectedFiles(IStructuredSelection structuredSelection) {
		List<File> files = new ArrayList<File>();
		for (Object object : structuredSelection.toList()) {
			if (object instanceof File) {
				files.add((File) object);
			} else {
				files.addAll(mediaLibrary.getMusicFiles(object.toString()));
			}
		}
		return files;
	}

	private void addLibraryButtonSection(Composite parent) {
		Composite buttonsComposite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(buttonsComposite);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(buttonsComposite);

		Button addButton = new Button(buttonsComposite, SWT.PUSH);
		addButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD));
		addButton.setToolTipText("Add To Library");
		addButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				importFiles(true);
			}
		});

		syncAllButton = new Button(buttonsComposite, SWT.PUSH);
		syncAllButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ELCL_SYNCED));
		syncAllButton.setToolTipText("Sync Libraries");
		syncAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				syncJob.schedule();
			}
		});

		pathsButton = new Button(buttonsComposite, SWT.PUSH);
		pathsButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_HOME_NAV));
		pathsButton.setToolTipText("Library Paths");
		pathsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				LibraryPathsDialog dialog = new LibraryPathsDialog(mediaLibrary.getLocalPath(), mediaLibrary.getRemotePath());
				if (dialog.open() == IDialogConstants.OK_ID) {
					mediaLibrary.setLocal(dialog.getLocal());
					mediaLibrary.setRemote(dialog.getRemote());
					syncJob.schedule();
				}
			}
		});

	}

	private void downloadFiles(final List<File> files2Download, final boolean addToPlayList, final boolean play) {
		Job downloadJob = new Job("Downloading...") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				for (File file : files2Download) {
					try {
						final boolean isLocal = mediaLibrary.isLocalFile(file);
						if (!isLocal && !mediaLibrary.isRemoteLocal()) {
							monitor.beginTask("Downloading files from shared library", IProgressMonitor.UNKNOWN);
						}

						File localFile = mediaLibrary.getLocalFile(file);
						if (addToPlayList) {
							mediaPlayer.addToPlayList(localFile.getAbsolutePath(), play);
						}
					} catch (Exception e) {
						EMediaPlayerActivator.getDefault().logException(e);
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								MessageDialog.openError(Display.getDefault().getActiveShell(), "Failed", "Failed to play file. See logs for details");
							}
						});

					}
				}
				return Status.OK_STATUS;
			}
		};
		downloadJob.schedule();
	}

	private void shareFiles(final List<File> files) {
		Job shareJob = new Job("Sharing...") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				for (File file : files) {
					try {
						mediaLibrary.addToRemoteRepository(file);
					} catch (Exception e) {
						EMediaPlayerActivator.getDefault().logException(e);
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								MessageDialog
										.openError(Display.getDefault().getActiveShell(), "Failed", "Failed to share file. See logs for details");
							}
						});

					}
				}
				return Status.OK_STATUS;
			}
		};
		shareJob.schedule();
	}

	private boolean isEnterMode;
	private int folderCount;

	private class LibraryContentProvider implements ITreeContentProvider {
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

		}

		@Override
		public void dispose() {

		}

		@Override
		public boolean hasChildren(Object element) {
			return element instanceof String;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof MediaLibrary) {
				Object[] array = mediaLibrary.getFolders().toArray();
				isEnterMode = (folderCount = array.length) > FOLDER_LIMIT;
				return array;
			}
			return new Object[] {};
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof String) {
				return mediaLibrary.getMusicFiles((String) parentElement).toArray();
			}
			return null;
		}
	};

	private class LibraryLabelProvider extends CellLabelProvider implements ILabelProvider, IFontProvider, IColorProvider {
		@Override
		public void removeListener(ILabelProviderListener listener) {

		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void dispose() {

		}

		@Override
		public void addListener(ILabelProviderListener listener) {

		}

		@Override
		public String getText(Object element) {
			if (element instanceof File) {
				FavMedia favMedia = favouritesRepository.getFavMedia(((File) element).getAbsolutePath());
			    String tail = "";
			    if (favMedia != null && favMedia.isValid()) {
			    	tail = " (+" + favMedia.getMembers().size() + ")";
			    }
				return ((File) element).getName() + tail;
			}
			return element.toString();
		}

		@Override
		public Image getImage(Object element) {
			return mediaLibrary.getElementType(element).getImage();
		}

		@Override
		public void update(ViewerCell cell) {
			Object element = cell.getElement();
			cell.setText(getText(element));
			Image image = getImage(element);
			cell.setImage(image);
			cell.setBackground(getBackground(element));
			cell.setForeground(getForeground(element));
			cell.setFont(getFont(element));
		}

		@Override
		public int getToolTipDisplayDelayTime(Object object) {
			return 100;
		}

		@Override
		public int getToolTipTimeDisplayed(Object object) {
			return 10000;
		}

		@Override
		public String getToolTipText(Object element) {
			if (element instanceof File) {
				return ((File) element).getAbsolutePath();
			} else {
				return "Total Folders: " + folderCount;
			}
		}

		@Override
		public Color getForeground(Object element) {
			if (element instanceof File) {
				if (favouritesRepository.isFavMedia(((File) element).getAbsolutePath())) {
					return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN);
				}
			}
			return null;
		}

		@Override
		public Color getBackground(Object element) {
			return null;
		}

		@Override
		public Font getFont(Object element) {
			if (element instanceof File) {
				if (favouritesRepository.isLocalFavMedia(((File) element).getAbsolutePath())) {
					return JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
				}
			}
			return null;
		}

	};
	

	private void addDisposeListeners() {
		getSite().getWorkbenchWindow().getWorkbench().addWorkbenchListener(new IWorkbenchListener() {

			@Override
			public boolean preShutdown(IWorkbench workbench, boolean forced) {
				try {
					mediaPlayer.savePlaylist();
					favouritesRepository.saveAllFavourites();
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
		addPlaylistActionsAndButtons(buttonsComposite);
	}

	private void addPlaylistActionsAndButtons(Composite buttonsComposite) {
		Button addButton = new Button(buttonsComposite, SWT.PUSH);
		addButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD));
		addButton.setToolTipText("Add To Playlist");
		addButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				addToPlaylistAction.run();
			}
		});

		Button deleteFromPlaylistButton = new Button(buttonsComposite, SWT.PUSH);
		deleteFromPlaylistButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_DELETE));
		deleteFromPlaylistButton.setToolTipText("Remove From Playlist");
		deleteFromPlaylistButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				removeFromPlaylistAction.run();
			}

		});

		Button clearPlaylistButton = new Button(buttonsComposite, SWT.PUSH);
		clearPlaylistButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_CLEAR));
		clearPlaylistButton.setToolTipText("Clear Playlist");
		clearPlaylistButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				clearPlayListAction.run();
			}
		});

		Button saveButton = new Button(buttonsComposite, SWT.PUSH);
		saveButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_SAVE_EDIT));
		saveButton.setToolTipText("Save Playlist");
		saveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				savePlaylistAction.run();
			}
		});

		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fillPlaylistContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(playListViewer.getControl());
		playListViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, playListViewer);

	}

	private void fillPlaylistContextMenu(IMenuManager manager) {
		final IStructuredSelection structuredSelection = (IStructuredSelection) playListViewer.getSelection();
		if (structuredSelection.size() == 1) {
			manager.add(playFileAction);
		}
		manager.add(addToPlaylistAction);

		final String clipboardURL = getClipboardURL();
		if (clipboardURL != null && (MediaLibrary.isWebUrl(clipboardURL))) {
			manager.add(new Action("Play Clipboard URL") {
				@Override
				public void run() {
					mediaPlayer.addToPlayList(clipboardURL, true);
					mediaPlayer.playItem(playListViewer.getTable().getItemCount() - 1);
				}
			});
		}

		manager.add(new Separator());
		if (!structuredSelection.isEmpty()) {
			manager.add(removeFromPlaylistAction);
		}

		if (playListViewer.getTable().getItemCount() != 0) {
			manager.add(clearPlayListAction);
		}
		manager.add(new Separator());

		repeatAction.setChecked(mediaPlayer.isRepeat());
		manager.add(repeatAction);
		shuffleAction.setChecked(mediaPlayer.isShuffle());
		manager.add(shuffleAction);
		manager.add(new Separator());

		final List<File> files2Share = new ArrayList<File>();
		for (Object object : structuredSelection.toList()) {
			MediaFile mediaFile = (MediaFile) object;
			String url = mediaFile.getUrl();
			File file = new File(url);
			if (!mediaFile.isWebUrl() && mediaLibrary.isRemoteShareRequired(file)) {
				files2Share.add(file);
			}
		}

		if (!files2Share.isEmpty()) {
			manager.add(new Action("Share") {
				@Override
				public void run() {
					shareFiles(files2Share);
				}
			});
		}
		manager.add(new Separator());

		if (structuredSelection.size() == 1 && !((MediaFile) structuredSelection.getFirstElement()).isWebUrl()) {
			manager.add(new Action("Open Location") {
				public void run() {
					MediaFile mediaFile = (MediaFile) structuredSelection.getFirstElement();
					try {
						Desktop.getDesktop().browse(new File(mediaFile.getUrl()).getParentFile().toURI());
					} catch (IOException e) {
						EMediaPlayerActivator.getDefault().logException(e);
					}
				};
			});
			manager.add(new Separator());
		}

	}

	Action repeatAction = new Action("Repeat") {
		public void run() {
			mediaPlayer.setRepeat(!mediaPlayer.isRepeat());
		};
	};

	Action shuffleAction = new Action("Shuffle") {
		public void run() {
			mediaPlayer.setShuffle(!mediaPlayer.isShuffle());
		};
	};

	private String getClipboardURL() {
		Clipboard clipboard = new Clipboard(Display.getDefault());
		try {
			Object contents = clipboard.getContents(TextTransfer.getInstance());
			if (contents != null) {
				return contents.toString();
			}
		} finally {
			clipboard.dispose();
		}
		return null;
	}

	private Action savePlaylistAction = new Action("Save Playlist") {
		public void run() {
			try {
				mediaPlayer.savePlaylist();
			} catch (IOException e) {
				EMediaPlayerActivator.getDefault().logException(e);
			}
		};
	};

	private Action playFileAction = new Action("Play") {
		public void run() {
			int index = playListViewer.getTable().getSelectionIndex();
			mediaPlayer.playItem(index);
		};
	};

	private Action addToPlaylistAction = new Action("Add") {
		public void run() {
			importFiles(false);
		};
	};

	private Action clearPlayListAction = new Action("Clear") {
		public void run() {
			int count = playListViewer.getTable().getItemCount();
			if (count > 0) {
				boolean proceed = MessageDialog.openQuestion(Display.getDefault().getActiveShell(), "Confirm",
						"Are you sure you want to clear the playlist?");
				if (proceed) {
					while (count > 0) {
						mediaPlayer.removeItem(--count);
					}
				}
			}
		};
	};

	private Action removeFromPlaylistAction = new Action("Remove") {
		public void run() {

			int[] selectionIndices = playListViewer.getTable().getSelectionIndices();
			int length = selectionIndices.length;
			while (length > 0) {
				mediaPlayer.removeItem(selectionIndices[0]);
				length--;
			}
			if (selectionIndices.length > 0) {
				int newIndex = selectionIndices[selectionIndices.length - 1] - 1;
				if (newIndex == -1) {
					newIndex++;
				}
				playListViewer.getTable().select(newIndex);
			}

		};
	};

	private void importFiles(boolean library) {
		FileDialog dialog = new FileDialog(wmPlayerSite.getShell(), SWT.OPEN | SWT.MULTI);
		dialog.setFilterExtensions(new String[] { "*.*" });
		String file = dialog.open();
		if (file != null) {
			String[] fileNames = dialog.getFileNames();
			for (String filename : fileNames) {
				String fileURL = dialog.getFilterPath() + File.separator + filename;
				if (library) {
					try {
						if (!mediaLibrary.isPicture(fileURL)) {
							mediaLibrary.addToLocalRepository(new File(fileURL));
						}
					} catch (Exception e) {
						EMediaPlayerActivator.getDefault().logException(e);
					}
				} else {
					mediaPlayer.addToPlayList(fileURL, mediaPlayer.getPlayList().size() == 0);
				}
			}
		}
	}

	private void addPlayListViewer(Composite playListComposite) {
		playListViewer = new TableViewer(playListComposite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(playListViewer.getTable());
		playListViewer.getTable().setHeaderVisible(true);
		playListViewer.getTable().setLinesVisible(true);

		TableViewerColumn nameColumn = new TableViewerColumn(playListViewer, SWT.NONE);
		nameColumn.getColumn().setText(NAME);
		nameColumn.getColumn().setWidth(150);

		TableViewerColumn durationColumn = new TableViewerColumn(playListViewer, SWT.NONE);
		durationColumn.getColumn().setText(DURATION);
		durationColumn.getColumn().setWidth(60);

		TableViewerColumn albumColumn = new TableViewerColumn(playListViewer, SWT.NONE);
		albumColumn.getColumn().setText(ALBUM);
		albumColumn.getColumn().setWidth(150);

		TableViewerColumn urlColumn = new TableViewerColumn(playListViewer, SWT.NONE);
		urlColumn.getColumn().setText(URL);
		urlColumn.getColumn().setWidth(300);
		playListViewer.setColumnProperties(new String[] { NAME, DURATION, URL });

		playListViewer.setContentProvider(new ArrayContentProvider());
		playListViewer.setLabelProvider(new PlaylistLabelProvider());
		playListViewer.setInput(mediaPlayer.getPlayList());

		mediaPlayer.setListener(new IListener() {
			@Override
			public void handleEvent(int eventKind) {
				Display.getDefault().asyncExec(new Runnable() {

					@Override
					public void run() {
						playListViewer.refresh();
					}
				});
			}
		});

		playListViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				playFileAction.run();
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
			if (columnIndex == 0) {
				return EMediaConstants.IMAGE_MUSIC_FILE;
			}
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
				case 2:
					if (mediaFile.isWebUrl()) {
						return "Web File";
					} else {
						return new File(mediaFile.getUrl()).getParentFile().getName();
					}
				default:
					return mediaFile.getUrl();
				}

			}
			return null;
		}

		@Override
		public Color getForeground(Object element) {
			if (element instanceof MediaFile) {
				String url = ((MediaFile) element).getUrl();
				if (!MediaLibrary.isWebUrl(url)) {
					File file = new File(url);
					if (!file.exists()) {
						return Display.getDefault().getSystemColor(SWT.COLOR_RED);
					}
				}
			}
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
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			return (EMediaView) activePage.showView(ID);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void dispose() {
		super.dispose();
	}

}
