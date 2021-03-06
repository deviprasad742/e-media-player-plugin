package emediaplayerplugin.model;

import java.awt.Desktop;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import org.eclipse.ui.PlatformUI;

import emediaplayerplugin.EMediaPlayerActivator;
import emediaplayerplugin.ui.notifications.EventNotifier;
import emediaplayerplugin.ui.notifications.NotificationType;

/**
 * 
 * @author Prasad
 * 
 */

public class MediaLibrary {

	private boolean isRemoteLocal;
	private File local;
	private File remote;
	private Map<String, Map<File, String>> localLib = Collections.synchronizedMap(new HashMap<String, Map<File, String>>());
	private Map<String, Map<File, String>> remoteLib = Collections.synchronizedMap(new HashMap<String, Map<File, String>>());
	private Map<String, List<File>> musicMap = Collections.synchronizedMap(new HashMap<String, List<File>>());

	public MediaLibrary(String local, String remote) {
		setLocal(local);
		setRemote(remote);
	}

	public void setLocal(String local) {
		this.local = new File(local);
	}

	public void setRemote(String remote) {
		isRemoteLocal = isLocalPath(remote);
		this.remote = new File(remote);
	}

	public void syncAll() {
		syncLocalRepository();
		syncRemoteRepository();
	}

	public List<String> syncLocalRepository() {
		return syncRepository(localLib, local, false);
	}

	public List<String> syncRemoteRepository() {
		return syncRepository(remoteLib, remote, false);
	}

	public List<String> syncRepository(Map<String, Map<File, String>> lib, File root, boolean isExternal) {
		List<String> added = new ArrayList<String>();
		Map<String, Map<File, String>> localLib = new HashMap<String, Map<File, String>>();
		populateFiles(localLib, root);
		synchronized (this) {
			added.addAll(localLib.keySet());
			added.removeAll(lib.keySet());
			lib.clear();
			lib.putAll(localLib);
			if (!isExternal) {
				musicMap.clear();
			}
		}
		if (!isExternal) {
			notifyListener();
		}
		return added;
	}

	private void populateFiles(Map<String, Map<File, String>> localLib, File root) {
		if (!root.exists()) {
			return;
		}
		Queue<File> files = new LinkedList<File>();
		files.add(root);
		while (files.peek() != null) {
			File current = files.poll();
			File[] folders = current.listFiles(directoryFilter);
			files.addAll(Arrays.asList(folders));
			File[] songs = current.listFiles(songsFilter);
			if (songs.length > 0) {
				Map<File, String> map = localLib.get(current.getName());
				if (map == null) {
					map = Collections.synchronizedMap(new HashMap<File, String>());
					localLib.put(current.getName(), map);
				}
				for (File file : songs) {
					if (!map.containsValue(file.getName())) {
						map.put(file, file.getName());
					}
				}
			}
		}
	}

	private FileFilter directoryFilter = new FileFilter() {
		@Override
		public boolean accept(File file) {
			return file.isDirectory();
		}
	};

	private FileFilter songsFilter = new FileFilter() {
		@Override
		public boolean accept(File file) {
			String name = file.getName();
			int indexOf = name.lastIndexOf(".");
			if (indexOf != -1) {
				String ext = name.substring(indexOf + 1);
				return EMediaConstants.SUPPORTED_FORMATS.contains(ext.toLowerCase());
			}
			return false;
		}
	};
	private IListener listener;

	public Collection<String> getFolders() {
		Set<String> folders = new HashSet<String>();
		folders.addAll(localLib.keySet());
		folders.addAll(remoteLib.keySet());
		return folders;
	}

	public Collection<File> getMusicFiles(String folderName) {
		List<File> songs = musicMap.get(folderName);
		if (songs == null) {
			songs = new ArrayList<File>();
			musicMap.put(folderName, songs);
			Set<String> fileNames = new HashSet<String>();
			Map<File, String> localSongs = localLib.get(folderName);
			if (localSongs != null) {
				songs.addAll(localSongs.keySet());
				fileNames.addAll(localSongs.values());
			}
			Map<File, String> remoteSongs = remoteLib.get(folderName);
			if (remoteSongs != null) {
				for (Entry<File, String> entry : remoteSongs.entrySet()) {
					if (!fileNames.contains(entry.getValue())) {
						songs.add(entry.getKey());
					}
				}
			}
		}
		return songs;
	}

	public File downloadLocalFile(File remoteFile, boolean notify) throws Exception {
		if (!isLocalFile(remoteFile)) {
			if (isRemoteLocal) {
				// return same file if remote is a local folder
				return remoteFile;
			}

			addToLocalRepository(remoteFile, notify);
		}
		File localFile = getLocalFile(remoteFile.getParentFile().getName(), remoteFile.getName());
		if (localFile != null) {
			return localFile;
		}
		throw new RuntimeException("Something went wrong");
	}

	public File getLocalFile(String folder, String fileName) {
		return getFile(folder, fileName, localLib);
	}	
	
	public File getRemoteFile(String folder, String fileName) {
		return getFile(folder, fileName, remoteLib);
	}
	
	private File getFile(String folder, String fileName, Map<String, Map<File, String>> lib) {
		Map<File, String> songs = lib.get(folder);
		if (songs != null) {
			for (Entry<File, String> entry : songs.entrySet()) {
				if (entry.getValue().equals(fileName)) {
					return entry.getKey();
				}
			}
		}
		return null;
	}

	public File getFile(String folder, String fileName) {
		Collection<File> files = getMusicFiles(folder);
		for (File file : files) {
			if (file.getName().equals(fileName)) {
				return file;
			}
		}
		return null;
	}

	public boolean isLocalFile(File file) {
		Map<File, String> files = localLib.get(file.getParentFile().getName());
		return files != null && files.containsValue(file.getName());
	}

	public boolean isRemoteFile(File file) {
		Map<File, String> files = remoteLib.get(file.getParentFile().getName());
		return files != null && files.containsValue(file.getName());
	}

	public boolean isLocalFolder(String folderName) {
		return localLib.containsKey(folderName);
	}

	public boolean isRemoteShareRequired(File file) {
		if (!isRemoteLocal) {
			Map<File, String> files = remoteLib.get(file.getParentFile().getName());
			if (files != null) {
				if (files.containsValue(file.getName())) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public void addToLocalRepository(File remote, boolean notify) throws Exception {
		// check before adding
		if (!isLocalFile(remote)) {
			writeToRepository(local, localLib, remote, notify, false);
		}
	}

	public void addToRemoteRepository(File local, boolean notify) throws Exception {
		if (isRemoteShareRequired(local)) {
			writeToRepository(remote, remoteLib, local, notify, false);
		}
	}

	public void writeToRepository(final File root, Map<String, Map<File, String>> lib, File file2Copy, boolean notify, boolean isExternal) throws Exception {
		String key = file2Copy.getParentFile().getName();
		Map<File, String> filesMap = lib.get(key);
		String repoRelativeFolderPath = isExternal ? key : EMediaConstants.EMEDIA_SHARED_FOLDER + File.separator + key;
		if (filesMap != null) {
			File firstFile = null;
			for (Entry<File, String> entry : filesMap.entrySet()) {
				firstFile = entry.getKey();
				break;
			}
			if (firstFile != null) {
				String parentPath = firstFile.getParentFile().getAbsolutePath();
				String rootPath = root.getAbsolutePath();
				repoRelativeFolderPath = parentPath.substring(parentPath.indexOf(rootPath) + rootPath.length());
			}
		}
		final File destination = new File(root, repoRelativeFolderPath + File.separator + file2Copy.getName());
		destination.getParentFile().mkdirs();
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new FileInputStream(file2Copy);
			out = new FileOutputStream(destination);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
		} finally {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
		}

		if (filesMap == null) {
			filesMap = Collections.synchronizedMap(new HashMap<File, String>());
			lib.put(key, filesMap);
		}
		filesMap.put(destination, destination.getName());
		if (!isExternal) {
			musicMap.remove(destination.getParentFile().getName());
		}
		

		if (EMediaConstants.canNotify()) {
			Runnable locationRunnable = new Runnable() {
				@Override
				public void run() {
					try {
						if (isLocalPath(destination.getAbsolutePath())) {
							Desktop.getDesktop().browse(destination.getParentFile().toURI());
						} else {
							PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(destination.getParentFile().toURI().toURL());
						}
					} catch (Exception e) {
						EMediaPlayerActivator.getDefault().logException(e);
					}
				}
			};
			String message = "File '" + destination.getName() + "' is successfully copied to repository '" + root.getName() + "' .";
			EventNotifier.notify("File Added", message, locationRunnable, NotificationType.CONNECTED);
		}
		
		if (notify) {
			notifyListener();
		}
	}

	public String getLocalPath() {
		return local.getAbsolutePath();
	}

	public String getRemotePath() {
		return remote.getAbsolutePath();
	}

	public boolean isRemoteLocal() {
		return isRemoteLocal;
	}

	public boolean isPicture(String url) {
		return url.endsWith(EMediaConstants.EXT_JPEG) || url.endsWith(EMediaConstants.EXT_JPG);
	}

	public static boolean isWebUrl(String url) {
		return url.startsWith("http://");
	}

	public ElementType getElementType(Object element) {
		if (element instanceof File) {
			File file = (File) element;
			boolean isLocal = existsInLib(localLib, file);
			boolean isRemote = existsInLib(remoteLib, file);
			if (isRemote && isLocal) {
				return ElementType.FILE_SYNCED;
			} else if (isRemote) {
				return ElementType.FILE_REMOTE;
			} else {
				return ElementType.FILE_NORMAL;
			}
		} else {
			boolean isLocal = localLib.containsKey(element);
			boolean isRemote = remoteLib.containsKey(element);
			if (isRemote && isLocal) {
				return ElementType.FOLDER_SYNCED;
			} else if (isRemote) {
				return ElementType.FOLDER_REMOTE;
			} else {
				return ElementType.FOLDER_NORMAL;
			}
		}
	}

	private boolean existsInLib(Map<String, Map<File, String>> lib, File file) {
		String parent = file.getParentFile().getName();
		return lib.get(parent) != null && lib.get(parent).containsValue(file.getName());
	}

	public void setListener(IListener listener) {
		this.listener = listener;
	}

	private void notifyListener() {
		if (listener != null) {
			listener.handleEvent(IListener.EVENT_DEFAULT);
		}
	}

	public void removeLocalFiles(List<File> files) {
		for (File file : files) {
			if (isLocalFile(file)) {
				final File parent = file.getParentFile();
				boolean success = file.delete();
				if (success) {
					String[] list = parent.list();
					if (list.length == 0) {
						parent.delete();
						localLib.remove(parent.getName());
					} else {
						localLib.get(parent.getName()).remove(file);
					}
					musicMap.remove(parent.getName());

					if (EMediaConstants.canNotify()) {

						Runnable locationRunnable = new Runnable() {
							@Override
							public void run() {
								try {
									URI uri = parent.exists() ? parent.toURI() : parent.getParentFile().toURI();
									Desktop.getDesktop().browse(uri);
								} catch (Exception e) {
									EMediaPlayerActivator.getDefault().logException(e);
								}
							}
						};
						String message = "File '" + file.getName() + "' is successfully removed from folder '" + parent.getName() + "' .";
						EventNotifier.notify("File Deleted", message, locationRunnable, NotificationType.CONNECTED);
					}
				
				}
			}
		}
		notifyListener();
	}
	
	public static boolean isLocalPath(String path) {
		return !path.startsWith("\\\\");
	}

}
