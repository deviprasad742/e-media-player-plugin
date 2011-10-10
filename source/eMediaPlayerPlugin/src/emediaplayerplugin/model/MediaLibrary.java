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
import java.util.Queue;
import java.util.Set;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import emediaplayerplugin.EMediaPlayerActivator;

public class MediaLibrary {

	public static final Image FOLDER = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
	public static final Image FILE = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
	public static final Image REMOTE_FOLDER = ImageDescriptor.createFromFile(EMediaPlayerActivator.class, "icons/remote-folder.png").createImage();
	public static final Image REMOTE_FILE = ImageDescriptor.createFromFile(EMediaPlayerActivator.class, "icons/web.png").createImage();
	private final static String MP3 = ".mp3";
	private final static String WMV = ".wmv";

	private File local;
	private File remote;
	private Map<String, List<File>> localLib = new HashMap<String, List<File>>();
	private Map<String, List<File>> remoteLib = new HashMap<String, List<File>>();

	public MediaLibrary(String local, String remote) {
		setLocal(new File(local));
		setRemote(new File(remote));
	}

	public synchronized void setLocal(File local) {
		this.local = local;
	}

	public synchronized void setRemote(File remote) {
		this.remote = remote;
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

	private List<String> syncRepository(Map<String, List<File>> lib, File root) {
		List<String> added = new ArrayList<String>();
		Map<String, List<File>> localLib = new HashMap<String, List<File>>();
		populateFiles(localLib, root);
		synchronized (this) {
			added.addAll(localLib.keySet());
			added.removeAll(lib.keySet());
			lib.clear();
			lib.putAll(localLib);
		}
		return added;
	}

	private void populateFiles(Map<String, List<File>> lib, File root) {
		Queue<File> files = new LinkedList<File>();
		files.add(root);
		while (files.peek() != null) {
			File current = files.poll();
			File[] folders = current.listFiles(directoryFilter);
			files.addAll(Arrays.asList(folders));
			File[] songs = current.listFiles(songsFilter);
			if (songs.length > 0) {
				lib.put(current.getName(), new ArrayList<File>(Arrays.asList(songs)));
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
			return name.endsWith(MP3) || name.endsWith(WMV);
		}
	};

	public synchronized Collection<String> getFolders() {
		Set<String> folders = new HashSet<String>();
		folders.addAll(localLib.keySet());
		folders.addAll(remoteLib.keySet());
		return folders;
	}

	public synchronized Collection<File> getMusicFiles(String folderName) {
	    Set<String> fileNames = new HashSet<String>();
		List<File> songs = new ArrayList<File>();
		List<File> localSongs = localLib.get(folderName);
		if (localSongs != null) {
			songs.addAll(localSongs);
			for (File localFile : localSongs) {
				fileNames.add(localFile.getName());
			}
		}
		List<File> remoteSongs = remoteLib.get(folderName);
		if (remoteSongs != null) {
			for (File remoteFile : remoteSongs) {
				if (!fileNames.contains(remoteFile.getName())) {
					songs.add(remoteFile);
				}
			}
		}
		return songs;
	}

	
	public synchronized File getLocalFile(File remoteFile) throws Exception {
		if (!isLocalFile(remoteFile)) {
			addToLocalRepository(remoteFile);
		}
		List<File> songs = localLib.get(remoteFile.getParentFile().getName());
		for (File file : songs) {
			if (file.getName().equals(remoteFile.getName())) {
				return file;
			}
		}
		throw new RuntimeException("Something went wrong");
	}
	
	public synchronized boolean isLocalFile(File file) {
		List<File> files = localLib.get(file.getParentFile().getName());
		return files != null && files.contains(file);
	}

	public synchronized boolean isLocalFolder(String folderName) {
		return localLib.containsKey(folderName);
	}

	public synchronized boolean isRemoteFile(File file) {
		List<File> files = remoteLib.get(file.getParentFile().getName());
		if (files != null) {
			for (File remoteFile : files) {
				if (remoteFile.getName().equals(file.getName())) {
					return true;
				}
			}
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
		if (!isRemoteFile(local)) {
			writeToRepository(remote, remoteLib, local);
		}
	}

	private void writeToRepository(File root, Map<String, List<File>> lib, File file2Copy) throws Exception {
		File destination = new File(root, file2Copy.getParentFile().getName() + File.separator + file2Copy.getName());
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
			} else if (out != null) {
				out.close();
			}
		}

		String key = destination.getParentFile().getName();
		List<File> list = lib.get(key);
		if (list == null) {
			list = new ArrayList<File>();
			lib.put(key, list);
		}
		list.add(destination);

	}

}
