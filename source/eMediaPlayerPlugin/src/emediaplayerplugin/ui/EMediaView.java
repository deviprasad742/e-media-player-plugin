package emediaplayerplugin.ui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
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
import org.eclipse.swt.dnd.Transfer;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
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
import emediaplayerplugin.ui.notifications.EventNotifier;

/**
 * 
 * @author Prasad
 * 
 */

public class EMediaView extends ViewPart {

	private static final String ADD_TO_LIBRARY_LABEL = "Add To Library";
	private static final String SHARE_LABEL = "Share";


	public static final String ID = "emediaplayerplugin.ui.EMediaView";

	private TabFolder container;
	private OleControlSite wmPlayerSite;
	private MediaPlayer mediaPlayer;
	private TableViewer playListViewer;
	private TableViewer favouritesViewer;

	private TreeViewer libraryViewer;
	private Text musicLibraryFilterText;

	private MediaLibrary mediaLibrary;
	private boolean confirmClear = true;


	private Text favFilterText;
	private Combo favUsersCombo;
	public static final String NAME = "Name";
	public static final String DURATION = "Duration";
	public static final String ALBUM = "Album";
	public static final String HITS = "Hits";
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
		createFavouritesSection();
		fillToolbar();
		addDisposeListeners();
	}

	private void fillToolbar() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalToolBar(IToolBarManager toolBarManager) {
		toolBarManager.add(new PlayURLFilesAction());
	}

	private void fillLocalPullDown(IMenuManager menuManager) {
		
	}

	private void createFavouritesSection() {
		TabItem favTab = new TabItem(container, SWT.NONE);
		favTab.setText("Favourites");
		
		favouritesRepository = new FavouritesRepository(mediaLibrary.getRemotePath(), mediaLibrary);
		favouritesRepository.setListener(new IListener() {
			@Override
			public void handleEvent(int eventKind) {
				refreshAll();
			}
		});
		
		Composite favComposite = new Composite(container, SWT.NONE);
		favTab.setControl(favComposite);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(favComposite);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(favComposite);

		Composite filterComposite = new Composite(favComposite, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(filterComposite);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(filterComposite);

		String tooltip = "Type file name or album name or hit count(Ex:+3) to filter the items";
		Label label = new Label(filterComposite, SWT.NONE);
		label.setText("Filter: ");
		label.setToolTipText(tooltip);
		GridDataFactory.swtDefaults().applyTo(label);

	    favFilterText = new Text(filterComposite, SWT.BORDER);
	    favFilterText.setToolTipText(tooltip);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(favFilterText);
		favFilterText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				refreshFavouritesView();
			}
		});
		
		Label usersLabel = new Label(filterComposite, SWT.NONE);
		usersLabel.setText("User: ");
		usersLabel.setToolTipText(tooltip);
		GridDataFactory.swtDefaults().applyTo(usersLabel);
		
	    favUsersCombo = new Combo(filterComposite, SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(favUsersCombo);
		populateComboUsers();
		
		favUsersCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				refreshFavouritesView();
			}
		});
		
		favouritesViewer = new TableViewer(favComposite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(favouritesViewer.getControl());
		favouritesViewer.getTable().setHeaderVisible(true);
		favouritesViewer.getTable().setLinesVisible(true);
		
		TableViewerColumn nameColumn = new TableViewerColumn(favouritesViewer, SWT.NONE);
		nameColumn.getColumn().setText(NAME);
		nameColumn.getColumn().setWidth(150);

		TableViewerColumn albumColumn = new TableViewerColumn(favouritesViewer, SWT.NONE);
		albumColumn.getColumn().setText(ALBUM);
		albumColumn.getColumn().setWidth(150);
		
		TableViewerColumn durationColumn = new TableViewerColumn(favouritesViewer, SWT.NONE);
		durationColumn.getColumn().setText(HITS);
		durationColumn.getColumn().setWidth(60);

		TableViewerColumn urlColumn = new TableViewerColumn(favouritesViewer, SWT.NONE);
		urlColumn.getColumn().setText(URL);
		urlColumn.getColumn().setWidth(300);
		
		favouritesViewer.setContentProvider(new ArrayContentProvider());
		favouritesViewer.setLabelProvider(new FavouritesLabelProvider());
		favouritesViewer.setSorter(new ViewerSorter() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				if (e1 instanceof FavMedia && e2 instanceof FavMedia) {
					FavMedia f1 = (FavMedia) e1;
					FavMedia f2 = (FavMedia) e2;
					int compare =  ("" + f2.getMembers().size()).compareTo("" + f1.getMembers().size());
					if (compare == 0) {
						return f1.getFile().getName().compareTo(f2.getFile().getName());
					}
					return compare;
				}
				return super.compare(viewer, e1, e2);
			}
		});
		favouritesViewer.setFilters(new ViewerFilter[] { favouritesFilter });
		favouritesViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Object element = ((IStructuredSelection) favouritesViewer.getSelection()).getFirstElement();
				if (element instanceof FavMedia) {
					downloadFiles(Arrays.asList(((FavMedia) element).getFile()), true, true);
				}
			}
		});
		hookLibAndFavContextMenu(false);

	}
	
	private void populateComboUsers() {
		String user = favUsersCombo.getText();
		favUsersCombo.deselectAll();
		List<String> members = favouritesRepository.getMembers();
		members.remove(favouritesRepository.getUserName());
		Collections.sort(members);
		members.add(0, EMediaConstants.FAV_MEMBER_LOCAL);
		members.add(0, EMediaConstants.FAV_MEMBER_All);
		favUsersCombo.setItems(members.toArray(new String[0]));
        favUsersCombo.select(members.indexOf(user));
        if (favUsersCombo.getText().isEmpty()) {
        	favUsersCombo.select(0);
        }
	}

	private void refreshAll() {
         refreshPlayListView();
         refreshLibraryView();
         refreshFavouritesView();
	}
	
	public void refreshLibraryView() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				Object[] elements = libraryViewer.getExpandedElements();
				libraryViewer.refresh();
				libraryViewer.setExpandedElements(elements);
			}
		};
		safelyRunInUI(runnable);
	}
	
	private void refreshFavouritesView() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				favouritesViewer.setInput(favouritesRepository.getFavMedias());
			}
		};
		safelyRunInUI(runnable);
	}
	
	private void refreshPlayListView() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				playListViewer.refresh();
			}
		};
		safelyRunInUI(runnable);
	}
	
	private void safelyRunInUI(final Runnable runnable) {
		if (Display.getDefault().getThread() == Thread.currentThread()) {
			runnable.run();
		} else {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					runnable.run();
				}
			});
		}
	}
	
	Job syncFavouritesJob = new Job("Syncing Favourites") {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				if (favouritesRepository != null) {
					favouritesRepository.setRemoteSettingsPath(mediaLibrary.getRemotePath());
					favouritesRepository.syncRepositories();
					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							populateComboUsers();
						}
					};
					safelyRunInUI(runnable);
				}

			} catch (Exception e) {
				showAndLogError(getName(), "Failed to synchronize favourites.", e);
			}
			return Status.OK_STATUS;
		}
	};
	
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
			syncFavouritesJob.schedule();
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
		libraryViewer.setSorter(new LibrarySorter());
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
		hookLibAndFavContextMenu(true);
	}

	private class LibrarySorter extends ViewerSorter{
		
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			int compareTo = mediaLibrary.getElementType(e1).compareTo(mediaLibrary.getElementType(e2));
			return compareTo == 0 ? super.compare(viewer, e1, e2) : compareTo;
		}
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

	};
	
	private ViewerFilter favouritesFilter = new ViewerFilter() {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			String favFilter = favFilterText.getText().trim();
			if (element instanceof FavMedia) {
				FavMedia favMedia = (FavMedia) element;
				boolean isFileMatch = isMatch(favFilter, favMedia.getKey());
				boolean isHitsMatch = isMatch(favFilter, "+" + favMedia.getMembers().size());
				boolean isUserFiltered = false;
				String user = favUsersCombo.getText();
				if (user.equals(EMediaConstants.FAV_MEMBER_All)) {
					isUserFiltered = true;
				} else if (user.equals(EMediaConstants.FAV_MEMBER_LOCAL)) {
					isUserFiltered = favMedia.isLocal();
				} else {
					isUserFiltered = favMedia.getMembers().contains(user);
				}
				return (isFileMatch || isHitsMatch || favFilter.isEmpty()) && isUserFiltered;
			}
			return false;
		}

	};
	
	private boolean isMatch(String filter, String text) {
		return text.toLowerCase().contains(filter.toLowerCase());
	}

	private Button syncAllButton;

	private Button pathsButton;

	private void hookLibAndFavContextMenu(final boolean isLibrary) {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager, isLibrary, false);
			}
		});
		Viewer viewer = isLibrary ? libraryViewer : favouritesViewer;
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void fillContextMenu(IMenuManager manager, boolean isLibraryMenu, boolean isPlayListMenu) {
	    IStructuredSelection structuredSelection = null;
		if (isLibraryMenu) {
			structuredSelection = (IStructuredSelection) libraryViewer.getSelection();
		} else if (isPlayListMenu) {
			structuredSelection = (IStructuredSelection) playListViewer.getSelection();
		} else {
			structuredSelection = (IStructuredSelection) favouritesViewer.getSelection();
		}
		
		final List<File> selectedFiles = getSelectedFiles(structuredSelection);
		boolean singleSelection = structuredSelection.size() == 1;
		
		
		if (isPlayListMenu) {

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

		}
		
		if (!isPlayListMenu) {
			EnqueSelectedFilesAction playFilesAction  = new EnqueSelectedFilesAction(selectedFiles, true);
			EnqueSelectedFilesAction enequeFilesAction  = new EnqueSelectedFilesAction(selectedFiles, false);
			manager.add(playFilesAction);
			manager.add(enequeFilesAction);
			manager.add(new Separator());
		}
		
		AddToFavAction addFavAction = new AddToFavAction(selectedFiles);
		if (addFavAction.isEnabled()) {
			manager.add(addFavAction);
		}

		RemoveFromFavAction removeFavAction = new RemoveFromFavAction(selectedFiles);
		if (removeFavAction.isEnabled()) {
			manager.add(removeFavAction);
		}
		manager.add(new Separator());

        if (isPlayListMenu) {
        	repeatAction.setChecked(mediaPlayer.isRepeat());
        	manager.add(repeatAction);
        	shuffleAction.setChecked(mediaPlayer.isShuffle());
        	manager.add(shuffleAction);
        	manager.add(new Separator());
        }
		
		
		final List<File> files2Delete = new ArrayList<File>();
		for (File file : selectedFiles) {
			if (mediaLibrary.isLocalFile(file)) {
				files2Delete.add(file);
			}
		}
		
		if (!files2Delete.isEmpty() && isLibraryMenu) {
			manager.add(new Action("Delete") {
				@Override
				public void run() {
					boolean proceed = MessageDialog.openQuestion(Display.getDefault().getActiveShell(), "Confirm Delete",
							"Are you sure you want to delete the selected '" + files2Delete.size() + "' file(s)?");
					if (proceed) {
						mediaLibrary.removeLocalFiles(files2Delete);
						new RemoveFromFavAction(files2Delete).run();
					}
				}
			});
			manager.add(new Separator());
		}
		
		
		ShareFilesAction shareFilesAction = new ShareFilesAction(selectedFiles);
		DownloadFilesAction downloadFilesAction = new DownloadFilesAction(selectedFiles);
		ShareFilesURLAction shareFilesURLAction = new ShareFilesURLAction(selectedFiles);

		if (shareFilesAction.isEnabled()) {
			manager.add(shareFilesAction);
		}
		
		if (downloadFilesAction.isEnabled()) {
			manager.add(downloadFilesAction);
		}
		
		if (shareFilesURLAction.isEnabled()) {
			manager.add(shareFilesURLAction);
		}
		
		if (shareFilesAction.isEnabled() || downloadFilesAction.isEnabled() || shareFilesURLAction.isEnabled()) {
			manager.add(new Separator());
		}
		
		
		if (!isLibraryMenu && !isPlayListMenu) {
			manager.add(syncFavouritesAction);
			manager.add(new Separator());
		}

		if (singleSelection) {
			OpenFileLocationAction openFileLocationAction = new OpenFileLocationAction(selectedFiles);
			OpenSharedFileLocationAction openSharedFileLocationAction = new OpenSharedFileLocationAction(selectedFiles);
			if (openFileLocationAction.isEnabled()) {
				manager.add(openFileLocationAction);
			}
			if (openSharedFileLocationAction.isEnabled()) {
				manager.add(openSharedFileLocationAction);
			}
			manager.add(new Separator());
		}

	}
	
	private class OpenFileLocationAction extends Action {
		private File file;

		public OpenFileLocationAction(List<File> selectedFiles) {
			super("Open File Location");
			for (File file : selectedFiles) {
				if (mediaLibrary.isLocalFile(file)) {
					this.file = file;
					break;
				}
			}
		}

		public void run() {
			try {
				Desktop.getDesktop().browse(file.getParentFile().toURI());
			} catch (IOException e) {
				showAndLogError(getText(), "Failed to open location: " + file.getParentFile().getAbsolutePath(), e);
			}
		}

		@Override
		public boolean isEnabled() {
			return file != null;
		}
	}
	private class OpenSharedFileLocationAction extends Action {
		private File file;

		public OpenSharedFileLocationAction(List<File> selectedFiles) {
			super("Open Shared File Location");
			for (File file : selectedFiles) {
				if (mediaLibrary.isRemoteFile(file)) {
					this.file = mediaLibrary.getRemoteFile(file.getParentFile().getName(), file.getName());
					break;
				}
			}
		}

		public void run() {
			try {
				PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(file.getParentFile().toURI().toURL());
			} catch (Exception e) {
				showAndLogError(getText(), "Failed to open shared location: "  + file.getParentFile().getAbsolutePath(), e);
			}
		}
		
		@Override
		public boolean isEnabled() {
			return file != null;
		}
	}
	
	private class ShareFilesURLAction extends Action {
		private List<File> files2Share = new ArrayList<File>();
		private List<File> selectedFiles;

		public ShareFilesURLAction(List<File> selectedFiles) {
			super("Copy URL");
			this.selectedFiles = selectedFiles;
			for (File file : selectedFiles) {
				if (mediaLibrary.isRemoteShareRequired(file)) {
					files2Share.add(file);
				}
			}
			if (!files2Share.isEmpty()) {
				setText("Share And Copy URL");
			}
		}

		public void run() {
			ShareJob shareJob = new ShareJob(files2Share);
			shareJob.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent event) {
					final StringBuilder builder = new StringBuilder(EMediaConstants.EMEDIA_SHARED_URL);
					boolean inRepo = false;
					for (File file : selectedFiles) {
						if (mediaLibrary.isRemoteFile(file)) {
							inRepo = true;
							builder.append(FavMedia.getKey(file));
							builder.append(EMediaConstants.SEPARATOR);
						}
					}
					if (inRepo) {
						Runnable runnable = new Runnable() {
							@Override
							public void run() {
								String shareUrl = builder.substring(0, builder.length() - 1);
								Clipboard clipboard = new Clipboard(Display.getDefault());
								try {
									Transfer[] transfers = new Transfer[]{TextTransfer.getInstance()};
									Object[] data = new Object[]{shareUrl};
									clipboard.setContents(data, transfers);
								} finally {
									clipboard.dispose();
								}
							}
						};
						safelyRunInUI(runnable);
					}
				}
			});
			shareJob.schedule();
		}
		
		@Override
		public boolean isEnabled() {
			return !selectedFiles.isEmpty();
		}
	}
	
	
	
	private class ShareFilesAction extends Action {
		private List<File> files2Share = new ArrayList<File>();

		public ShareFilesAction(List<File> selectedFiles) {
			super(SHARE_LABEL);
			for (File file : selectedFiles) {
				if (mediaLibrary.isRemoteShareRequired(file)) {
					files2Share.add(file);
				}
			}
		}

		public void run() {
			ShareJob shareJob = new ShareJob(files2Share);
			shareJob.schedule();
		}

		@Override
		public boolean isEnabled() {
			return !files2Share.isEmpty();
		}
	}
	
	private class DownloadFilesAction extends Action {
		private List<File> files2Download = new ArrayList<File>();

		public DownloadFilesAction(List<File> selectedFiles) {
			super("Download");
			for (File file : selectedFiles) {
				if (!mediaLibrary.isLocalFile(file)) {
					files2Download.add(file);
				}
			}
		}

		public void run() {
			downloadFiles(files2Download, false, false);
		}
		
		@Override
		public boolean isEnabled() {
			return !files2Download.isEmpty();
		}
	}
	
	
	private class EnqueSelectedFilesAction extends Action {
		private List<File> selectedFiles = new ArrayList<File>();
        private boolean play;
		
		public EnqueSelectedFilesAction(List<File> selectedFiles, boolean play) {
			super(play ? "Play" : "Enque");
			this.selectedFiles = selectedFiles;
			this.play = play;
		}

		public void run() {
			downloadFiles(selectedFiles, true, play);
		}
		
		@Override
		public boolean isEnabled() {
			return !selectedFiles.isEmpty();
		}
	}
	
	private class PlayURLFilesAction extends Action {

		public PlayURLFilesAction() {
			setImageDescriptor(ImageDescriptor.createFromImage(EMediaConstants.IMAGE_PLAY_URL));
			setText("Play URL");
			setToolTipText("Plays files present in the clipboard if it represents a valid url");
		}
		
		public void run() {
			final String[] fileKeys = getClipFileKeys();
			
			if (fileKeys == null || fileKeys.length == 0) {
				String message = "Clipboard doesnt contain a valid url which contains the files to play";
				MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Invalid URL", message);
				return;
			}
			
			boolean isSyncRequired = false;
			for (String fileKey : fileKeys) {
				File file = mediaLibrary.getFile(FavMedia.getFolderName(fileKey), FavMedia.getFileName(fileKey));
				if (file == null) {
					isSyncRequired = true;
					break;
				}
			}

			if (isSyncRequired) {
				syncJob.addJobChangeListener(new JobChangeAdapter() {
					public void done(IJobChangeEvent event) {
                        downloadAndPlayFiles(fileKeys);
                        syncJob.removeJobChangeListener(this);
					};
				});
				syncJob.schedule();
			} else {
                downloadAndPlayFiles(fileKeys);
			}
		}
		
		private void downloadAndPlayFiles(String[] fileKeys) {
			List<File> selectedFiles = new ArrayList<File>();
			for (String fileKey : fileKeys) {
				File file = mediaLibrary.getFile(FavMedia.getFolderName(fileKey), FavMedia.getFileName(fileKey));
				if (file != null) {
					selectedFiles.add(file);
				}
			}
			downloadFiles(selectedFiles, true, true);
		}
		
		
		@Override
		public boolean isEnabled() {
			return true;
		}
		
	}
	
	private String[] getClipFileKeys() {
		Clipboard clipboard = new Clipboard(Display.getDefault());
		try {
			Object contents = clipboard.getContents(TextTransfer.getInstance());
			if (contents != null) {
				String shareUrl = contents.toString();
				int index;
				if ((index = shareUrl.indexOf(EMediaConstants.EMEDIA_SHARED_URL)) != -1) {
					int beginIndex = index + EMediaConstants.EMEDIA_SHARED_URL.length();
					if (beginIndex <= shareUrl.length()) {
						String string = shareUrl.substring(beginIndex);
						String[] keys = string.split(EMediaConstants.SEPARATOR);
						return keys;
					}
				}
			}
		} finally {
			clipboard.dispose();
		}
		return null;
	}
	
	private Action syncFavouritesAction = new Action("Sync Favourites"){
		@Override
		public void run() {
			syncFavouritesJob.schedule();
		}
	};
	
	
	Job shareFavouritesJob = new Job("Sharing Local Favourites") {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				favouritesRepository.saveRemoteFavourites();
			} catch (Exception e) {
				showAndLogError(getName(), "Failed to share local favourites to repository", e);
			}
			return Status.OK_STATUS;
		}
	};
	
	private class AddToFavAction extends Action {
		private List<File> favFiles = new ArrayList<File>();

		public AddToFavAction(List<File> selectedFiles) {
			super("Add To Favourites");
			for (File file : selectedFiles) {
				if (!favouritesRepository.isLocalFavMedia(file.getAbsolutePath())) {
					favFiles.add(file);
				}
			}
		}

		public void run() {
			int i = 0;
			try {
				for (File file : favFiles) {
					i++;
					favouritesRepository.addToFavourites(file, i == favFiles.size());
				}
			} catch (Exception e) {
				showAndLogError(getText(), "Failed to add to favourites", e);
			}
			shareFavouritesJob.schedule();
		}
		
		@Override
		public boolean isEnabled() {
			return !favFiles.isEmpty();
		}
	}
	
	private class RemoveFromFavAction extends Action {
		private List<File> favFiles = new ArrayList<File>();
		
		public RemoveFromFavAction(List<File> selectedFiles) {
			super("Remove From Favourites");
			for (File file : selectedFiles) {
				if (favouritesRepository.isLocalFavMedia(file.getAbsolutePath())) {
					favFiles.add(file);
				}
			}
		}
		
		public void run() {
			int i = 0;
			try {
				for (File file : favFiles) {
					i++;
					favouritesRepository.removeFromFavourites(file, i == favFiles.size());
				}
			} catch (Exception e) {
				showAndLogError(getText(), "Failed to remove from favourites", e);
			}
			shareFavouritesJob.schedule();
		}
		
		@Override
		public boolean isEnabled() {
			return !favFiles.isEmpty();
		}
	}

	private List<File> getSelectedFiles(IStructuredSelection structuredSelection) {
		List<File> files = new ArrayList<File>();
		for (Object object : structuredSelection.toList()) {
			if (object instanceof File) {
				files.add((File) object);
			} else if (object instanceof String){
				files.addAll(mediaLibrary.getMusicFiles(object.toString()));
			} else if (object instanceof IAdaptable) {
				File file = (File) ((IAdaptable) object).getAdapter(File.class);
				if (file != null) {
					files.add(file);
				}
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
		addButton.setToolTipText(ADD_TO_LIBRARY_LABEL);
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
				int i = 0;
				try {
					for (File file : files2Download) {
						i++;
						final boolean isLocal = mediaLibrary.isLocalFile(file);
						if (!isLocal && !mediaLibrary.isRemoteLocal()) {
							monitor.beginTask("Downloading files from shared library", IProgressMonitor.UNKNOWN);
						}

						File localFile = mediaLibrary.downloadLocalFile(file, files2Download.size() == i);
						if (addToPlayList) {
							mediaPlayer.addToPlayList(localFile.getAbsolutePath(), play && i == 1);
						}
					}
				} catch (Exception e) {
					showAndLogError("Failed Operation", "Failed to download files from repository", e);
				}
				return Status.OK_STATUS;
			}
		};
		downloadJob.schedule();
	}

	private class ShareJob extends Job {
		private List<File> files;
		
		public ShareJob(List<File> files) {
			super("Sharing...");
			this.files = files;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			int i = 0;
			for (File file : files) {
				i++;
				try {
					mediaLibrary.addToRemoteRepository(file, i == files.size());
				} catch (Exception e) {
					showAndLogError(SHARE_LABEL, "Failed to share files to remote repository", e);
				}
			}
			return Status.OK_STATUS;
		}
	};
	

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
					mediaPlayer.getControl().stop();
					confirmClear = false;
					clearPlayListAction.run();
					mediaPlayer.dispose();
				} catch (Exception e) {
					EMediaPlayerActivator.getDefault().logException(e);
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
				fillContextMenu(manager, false, true);
			}
		});
		Menu menu = menuMgr.createContextMenu(playListViewer.getControl());
		playListViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, playListViewer);

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
				showAndLogError(getText(), "Failed to save playlist", e);

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
				boolean proceed = !confirmClear
						|| MessageDialog.openQuestion(Display.getDefault().getActiveShell(), "Confirm",
								"Are you sure you want to clear the playlist?");
				if (proceed) {
					while (count > 0) {
						mediaPlayer.removeItem(--count);
					}
				}
			}
		}

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
							mediaLibrary.addToLocalRepository(new File(fileURL), true);
						}
					} catch (Exception e) {
						showAndLogError(ADD_TO_LIBRARY_LABEL, "Failed to add files to library", e);
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

		playListViewer.setContentProvider(new ArrayContentProvider());
		playListViewer.setLabelProvider(new PlaylistLabelProvider());
		playListViewer.setInput(mediaPlayer.getPlayList());

		mediaPlayer.setListener(new IListener() {
			@Override
			public void handleEvent(int eventKind) {
				Display.getDefault().asyncExec(new Runnable() {

					@Override
					public void run() {
						if (!playListViewer.getControl().isDisposed()) {
							playListViewer.refresh();
						}
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
				MediaFile mediaFile = (MediaFile) element;
                if (favouritesRepository != null && favouritesRepository.isFavMedia(mediaFile.getUrl())) {
                	return EMediaConstants.FAV_MUSIC_FILE;
                }
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
	
	private class FavouritesLabelProvider implements ITableLabelProvider, IColorProvider, IFontProvider {

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
				return EMediaConstants.FAV_MUSIC_FILE;
			}
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof FavMedia) {
				FavMedia favMedia = (FavMedia) element;
				switch (columnIndex) {
				case 0:
					return favMedia.getFile().getName();
				case 1:
					return favMedia.getFile().getParentFile().getName();
				case 2:
					return "+" + favMedia.getMembers().size();
				default:
					return favMedia.getFile().getAbsolutePath();
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

		@Override
		public Font getFont(Object element) {
			if (element instanceof FavMedia) {
				if (((FavMedia) element).isLocal()) {
					return JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
				}
			}
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
	
	public static void showAndLogError(String title, String message, Exception e) {
	    if (message == null) {
	    	message = e.getMessage();
	    } else {
	    	message = message + ". Reason: " + e.getMessage();
	    }
	    EMediaPlayerActivator.getDefault().logException(message, e);
		EventNotifier.notifyError(title, message, EventNotifier.showViewRunnable("org.eclipse.pde.runtime.LogView"));
	}

}
