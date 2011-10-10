package emediaplayerplugin.ui;

import java.io.File;
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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import emediaplayerplugin.model.IMediaPlayerListener;
import emediaplayerplugin.model.MediaFile;
import emediaplayerplugin.model.MediaLibrary;
import emediaplayerplugin.model.MediaPlayer;

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
		createLibrarySection();
		addDisposeListeners();
	}

	private Job syncJob = new Job("Refresh") {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask("Refreshing library", IProgressMonitor.UNKNOWN);
			mediaLibrary.syncAll();
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					libraryViewer.setInput(mediaLibrary);
					refreshLibraryView();
					libraryViewer.getTree().setEnabled(true);
				}
			});

			return Status.OK_STATUS;
		}

	};

	public void refreshLibraryView() {
		Object[] elements = libraryViewer.getExpandedElements();
		libraryViewer.refresh();
		libraryViewer.setExpandedElements(elements);

	}

	private void createLibrarySection() {
		TabItem libraryTab = new TabItem(container, SWT.NONE);
		libraryTab.setText("Library");

		mediaLibrary = new MediaLibrary("D:\\Prasad\\Music\\Test\\local", "D:\\Prasad\\Music\\Test\\remote");
		Composite libraryComposite = new Composite(container, SWT.NONE);
		libraryTab.setControl(libraryComposite);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(libraryComposite);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(libraryComposite);

		Composite filterComposite = new Composite(libraryComposite, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).span(2,1).applyTo(filterComposite);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(filterComposite);
		
		Label label = new Label(filterComposite, SWT.NONE);
		label.setText("Filter");
		GridDataFactory.swtDefaults().applyTo(label);
		
		musicLibraryFilterText = new Text(filterComposite, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(musicLibraryFilterText);
		musicLibraryFilterText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				refreshLibraryView();
			}
		});

		libraryViewer = new TreeViewer(libraryComposite);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(libraryViewer.getControl());
		libraryViewer.setContentProvider(new LibraryContentProvider());
		libraryViewer.setLabelProvider(new LibraryLabelProvider());
		libraryViewer.setSorter(new ViewerSorter());
		addLibrarySectionButtons(libraryComposite);
		libraryViewer.setFilters(new ViewerFilter[] { libraryFilter });

		libraryViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Object element = ((IStructuredSelection) libraryViewer.getSelection()).getFirstElement();
				if (libraryViewer.isExpandable(element)) {
					libraryViewer.setExpandedState(element, !libraryViewer.getExpandedState(element));
				} else if (element instanceof File) {
					addToPlayList(Arrays.asList((File) element), true);
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
		boolean hasFolders = false;
		for (Object object : structuredSelection.toList()) {
			if (object instanceof String) {
				hasFolders = true;
				break;
			}
		}
		final List<File> selectedFiles = getSelectedFiles(structuredSelection);

		boolean isEnabled = !hasFolders || structuredSelection.size() == 1;
		if (isEnabled) {
			manager.add(new Action("Eneque") {
				@Override
				public void run() {
					addToPlayList(selectedFiles, false);
				}
			});

			boolean canExport = false;
			for (File file : selectedFiles) {
				if (!mediaLibrary.isRemoteFile(file)) {
					canExport = true;
				}
			}

			if (canExport) {
				manager.add(new Action("Share") {
					@Override
					public void run() {
						shareFiles(selectedFiles);
					}
				});
			}

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

	private void addLibrarySectionButtons(Composite parent) {
		Composite buttonsComposite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(buttonsComposite);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(buttonsComposite);

		Button syncAll = new Button(buttonsComposite, SWT.PUSH);
		syncAll.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ELCL_SYNCED));
		syncAll.setToolTipText("Sync Libraries");
		syncAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				libraryViewer.getTree().setEnabled(false);
				syncJob.schedule();
			}
		});

	}

	private void addToPlayList(final List<File> files, final boolean play) {
		Job downloadJob = new Job("Downloading...") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				for (File file : files) {
					try {
						final boolean isLocal = mediaLibrary.isLocalFile(file);
						if (!isLocal) {
							monitor.beginTask("Downloading files from shared library", IProgressMonitor.UNKNOWN);
						}
						final File localFile = mediaLibrary.getLocalFile(file);
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								mediaPlayer.addToPlayList(localFile.getAbsolutePath(), false);
								if (play) {
									mediaPlayer.playItem(playListViewer.getTable().getItemCount() - 1);
								}
								if (!isLocal) {
									refreshLibraryView();
								}
							}
						});
					} catch (Exception e) {
						EMediaPlayerActivator.getDefault().logException(null, e);
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
						EMediaPlayerActivator.getDefault().logException(null, e);
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
				return mediaLibrary.getFolders().toArray();
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

	private class LibraryLabelProvider implements ILabelProvider {
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
				return ((File) element).getName();
			}
			return element.toString();
		}

		@Override
		public Image getImage(Object element) {
			if (element instanceof File) {
				File file = (File) element;
				return mediaLibrary.isLocalFile(file) ? MediaLibrary.FILE : MediaLibrary.REMOTE_FILE;
			} else if (element instanceof String) {
				String key = element.toString();
				return mediaLibrary.isLocalFolder(key) ? MediaLibrary.FOLDER : MediaLibrary.REMOTE_FOLDER;
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
						newIndex++;
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
		playListViewer.setColumnProperties(new String[] { NAME, DURATION, URL });

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
		mediaPlayer.savePlaylist();
		super.dispose();
	}

}
