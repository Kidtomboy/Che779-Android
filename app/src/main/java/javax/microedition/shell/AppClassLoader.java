/*
 * Copyright 2015-2016 Nickolay Savchenko
 * Copyright 2017-2018 Nikita Shakarun
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.microedition.shell;

import android.util.Log;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import dalvik.system.DexClassLoader;
import ru.playsoftware.j2meloader.BuildConfig;
import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.util.FileUtils;
import ru.playsoftware.j2meloader.util.IOUtils;

public class AppClassLoader extends DexClassLoader {
	private static volatile int loadLogCount;
	private static final String TAG = AppClassLoader.class.getName();


	AppClassLoader(String paths, String tmpDir, ClassLoader parent, File appDir) {
		super(paths, tmpDir, null, parent);
		if (appDir == null)
			throw new NullPointerException("App path is null");
		Session session = Session.current();
		session.oldResDir = new File(appDir, Config.MIDLET_RES_DIR);
		session.classLoader = this;
		setDataDir(appDir);
		File jar = new File(appDir, Config.MIDLET_RES_FILE);
		session.resZip = jar.exists() ? new ZipFile(jar) : null;
	}

	public static void setDataDir(File appDir) {
		Session session = Session.current();
		session.dataDir = appDir.getParentFile().getParent() + Config.MIDLET_DATA_DIR + appDir.getName()
				+ session.dataSuffix;
	}

	public static InputStream getResourceAsStream(Class<?> resClass, String resName) {
		Log.d(TAG, "CUSTOM GET RES CALLED WITH PATH: " + resName);
		if (resName == null || resName.equals("")) {
			Log.w(TAG, "Can't load res on empty path");
			return null;
		}
		// Add support for Siemens file path
		String normName = resName.replace('\\', '/');
		// Remove double slashes
		normName = normName.replaceAll("//+", "/");
		if (normName.charAt(0) != '/' && resClass != null && resClass.getPackage() != null) {
			String className = resClass.getPackage().getName().replace('.', '/');
			normName = className + "/" + normName;
		}
		// Remove leading slash
		if (normName.charAt(0) == '/') {
			normName = normName.substring(1);
		}
		byte[] data = getResourceBytes(normName);
		if (data == null) {
			Log.w(TAG, "Can't load res: " + resName);
			return null;
		}
		return new ByteArrayInputStream(data);
	}

	public static String getDataDir() {
		return Session.current().dataDir;
	}

	public static byte[] getResourceAsBytes(String resName) {
		if (resName == null || resName.equals("")) {
			Log.w(TAG, "Can't load res on empty path");
			return null;
		}
		// Add support for Siemens file path
		String normName = resName.replace('\\', '/');
		// Remove double slashes
		normName = normName.replaceAll("//+", "/");
		// Remove leading slash
		if (normName.charAt(0) == '/') {
			normName = normName.substring(1);
		}
		byte[] data = getResourceBytes(normName);
		if (data == null) {
			Log.w(TAG, "Can't load res: " + resName);
			return null;
		}
		return data;
	}

	private static byte[] getResourceBytes(String name) {
		if (name.equals("")) {
			Log.w(TAG, "Can't load res on empty path");
			return null;
		}
		if (!BuildConfig.FULL_EMULATOR) {
			try {
				InputStream stream = AppClassLoader.class.getClassLoader().getResourceAsStream(name);
				if (stream != null) {
					return IOUtils.toByteArray(stream);
				}
				Log.e(TAG, "getResourceBytes: resource not found " + name);
				return null;
			} catch (Exception e) {
				Log.e(TAG, "getResourceBytes: " + name, e);
				return null;
			}
		}
		Session session = Session.current();
		ZipFile zipFile = session.resZip;
		if (zipFile == null) {
			final File file = new File(session.oldResDir, name);
			try {
				return FileUtils.getBytes(file);
			} catch (Exception e) {
				Log.w(TAG, "getResourceBytes: from file=" + file, e);
				return null;
			}
		}
		DataInputStream dis = null;
		try {
			FileHeader header = zipFile.getFileHeader(name);
			if (header == null) {
				return null;
			}
			dis = new DataInputStream(zipFile.getInputStream(header));
			byte[] data = new byte[(int) header.getUncompressedSize()];
			dis.readFully(data);
			return data;
		} catch (Exception e) {
			Log.e(TAG, "getResourceBytes: ", e);
		} finally {
			if (dis != null) {
				try {
					dis.close();
				} catch (IOException e) {
					Log.e(TAG, "getResourceBytes: error close stream", e);
				}
			}
		}
		return null;
	}

	public static AppClassLoader getInstance() {
		return Session.current().classLoader;
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		// First, check if the class is already loaded
		Class<?> loadedClass = findLoadedClass(name);
		if (loadedClass != null) {
			return loadedClass;
		}

		try {
			// Try to load the class from the URLs of this classloader
			Class<?> localClass = findClass(name);
			if (resolve) {
				resolveClass(localClass);
			}
			if (loadLogCount++ < 1500) {
				Che779Diag.log("load " + name);
			}
			return localClass;
		} catch (ClassNotFoundException e) {
			// Class not found in this classloader, delegate to parent classloader
			try {
				return super.loadClass(name, resolve);
			} catch (ClassNotFoundException e2) {
				Che779Diag.log("CLASS NOT FOUND " + name);
				throw e2;
			}
		} catch (LinkageError e) {
			Che779Diag.error("class " + name, e);
			throw e;
		}
	}
}
