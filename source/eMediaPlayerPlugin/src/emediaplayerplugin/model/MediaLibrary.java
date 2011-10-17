package emediaplayerplugin.model;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

/**
 * 
 * @author Prasad
 *
 */

public class MediaLibrary {

	private boolean isRemoteLocal;
	private File local;
	private File remote;
	private Map<String, Map<File, String>> localLib = new HashMap<String, Map<File, String>>();
	private Map<String, Map<File, String>> remoteLib = new HashMap<String, Map<File, String>>();
	private Map<String, List<File>> musicMap = new HashMap<String, List<File>>();
	

	public MediaLibrary(String local, String remote) {
		setLocal(local);
		setRemote(remote);
	}

	public synchronized void setLocal(String local) {
		this.local = new File(local);
	}

	public synchronized void setRemote(String remote) {
		isRemoteLocal = !remote.startsWith("\\\\");
		isRemoteLocal = false;
		this.remote = new File(remote);
	}

	public void syncAll() {
		syncLocalRepository();
		syncRemoteRepository();
	}

	public List<String> syncLocalRepository() {
		return syncRepository(localLib, local);
	}

	public List<String> syncRemoteRepository() {
		return syncRepository(remoteLib, remote);
	}

	private List<String> syncRepository(Map<String, Map<File, String>> lib, File root) {
		List<String> added = new ArrayList<String>();
		Map<String, Map<File, String>> localLib = new HashMap<String, Map<File, String>>();
		populateFiles(localLib, root);
		synchronized (this) {
			added.addAll(localLib.keySet());
			added.removeAll(lib.keySet());
			lib.clear();
			lib.putAll(localLib);
			musicMap.clear();
		}
		notifyListener();
		return added;
	}

	private void populateFiles(Map<String, Map<File, String>> localLib, File root) {
		if(!root.exists()) {
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
					map = new HashMap<File, String>();
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

	public synchronized Collection<String> getFolders() {
		Set<String> folders = new HashSet<String>();
		folders.addAll(localLib.keySet());
		folders.addAll(remoteLib.keySet());
		return folders;
	}

	public synchronized Collection<File> getMusicFiles(String folderName) {
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

	
	public synchronized File getLocalFile(File remoteFile) throws Exception {
		if (!isLocalFile(remoteFile)) {
			if (isRemoteLocal) {
				// return same file if remote is a local folder
				return remoteFile;
			}
			
			addToLocalRepository(remoteFile);
		}
		File localFile = getLocalFile(remoteFile.getParentFile().getName(), remoteFile.getName());
		if (localFile != null) {
			return localFile;
		}
		throw new RuntimeException("Something went wrong");
	}
	
	public synchronized File getLocalFile(String folder, String fileName) {
		Map<File, String> songs = localLib.get(folder);
		for (Entry<File, String> entry : songs.entrySet()) {
			if (entry.getValue().equals(fileName)) {
				return entry.getKey();
			}
		}
		return null;
	}
	
	public synchronized File getFile(String folder, String fileName) {
		Collection<File> files = getMusicFiles(folder);
		for (File file : files) {
			if (file.getName().equals(fileName)) {
				return file;
			}
		}
		return null;
	}
	
	public synchronized boolean isLocalFile(File file) {
		Map<File, String> files = localLib.get(file.getParentFile().getName());
		return files != null && files.containsValue(file.getName());
	}
	
	public synchronized boolean isRemoteFile(File file) {
		Map<File, String> files = remoteLib.get(file.getParentFile().getName());
		return files != null && files.containsValue(file.getName());
	}

	public synchronized boolean isLocalFolder(String folderName) {
		return localLib.containsKey(folderName);
	}

	public synchronized boolean isRemoteShareRequired(File file) {
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

	public synchronized void addToLocalRepository(File remote) throws Exception {
		// check before adding
		if (!isLocalFile(remote)) {
			writeToRepository(local, localLib, remote);
		}
	}

	public synchronized void addToRemoteRepository(File local) throws Exception {
		if (isRemoteShareRequired(local)) {
			writeToRepository(remote, remoteLib, local);
		}
	}

	private void writeToRepository(File root, Map<String, Map<File, String>> lib, File file2Copy) throws Exception {
		String key = file2Copy.getParentFile().getName();
		Map<File, String> filesMap = lib.get(key);
		String repoRelativeFolderPath = EMediaConstants.EMEDIA_SHARED_FOLDER + File.separator + key;		
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
        File destination = new File(root, repoRelativeFolderPath + File.separator + file2Copy.getName());
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
			filesMap = new HashMap<File, String>();
			lib.put(key, filesMap);
		}
		filesMap.put(destination, destination.getName());
		musicMap.remove(destination.getParentFile().getName());
		notifyListener();
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
				File parent = file.getParentFile();
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
				}
			}
		}
		notifyListener();
	}
	
	
}
