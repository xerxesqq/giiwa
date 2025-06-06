/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.bean;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.conf.Config;
import org.giiwa.conf.Global;
import org.giiwa.dao.Comment;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.dfile.DFile;
import org.giiwa.misc.IOUtil;
import org.giiwa.task.Consumer;
import org.giiwa.task.Task;
import org.giiwa.web.Language;

/**
 * Create Temporary file, which can be accessed by web api, please refer
 * model("/temp")
 * 
 * @author joe
 *
 */
@Comment(text = "临时文件")
public final class Temp {

	static Log log = LogFactory.getLog(Temp.class);

//	public static long MAX = 1024 * 1024 * 1024 * 2; // 2G

	public static String ROOT = "/data/temp";

	/**
	 * Initialize the Temp object, this will be invoke in giiwa startup.
	 *
	 * @param conf the conf
	 */
	public static void init(Configuration conf) {

		if (conf != null) {
			ROOT = conf.getString("temp.path", ROOT);
			if (X.isEmpty(ROOT)) {
				ROOT = "/data/temp";
			}
		}

		log.warn("temp is init [" + ROOT + "] ...");

		File f1 = new File(ROOT);
		try {
			if (!f1.exists()) {
				X.IO.mkdirs(f1);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		if (!f1.exists() || !f1.canWrite()) {
			ROOT = "./temp/";
		}

	}

	private String id = null;
	public String name = null;
	private boolean memory = true;

	@Comment()
	public long size() {
		try {
			return this.bb == null ? length() : this.bb.length;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return 0;
	}

	private Temp() {

	}

	public static Temp create(String id, String name) {
		return create(id, name, false);
	}

	/**
	 * 创建临时对象
	 * 
	 * @param id
	 * @param name
	 * @param inmemory true 内存对象， false 文件对象
	 * @return
	 */
	public static Temp create(String id, String name, boolean inmemory) {
		Temp t = new Temp();

		t.id = id;// UID.id(Global.now(), UID.random(), name);
		t.name = name;
		t.memory = inmemory;

		return t;
	}

	/**
	 * get the Temp file for name.
	 *
	 * @param name the file name
	 * @return the Temp
	 */
	public static Temp create(String name) {
		String id = UID.id(Global.now(), UID.random(), name);
		return create(id, name, false);
	}

	/**
	 * 创建临时对象
	 * 
	 * @param name
	 * @param inmemory true 内存对象， false 文件对象
	 * @return
	 */
	public static Temp create(String name, boolean inmemory) {
		String id = UID.id(Global.now(), UID.random(), name);
		return create(id, name, inmemory);
	}

	/**
	 * get the Id
	 * 
	 * @return String of id
	 */
	public String getId() {
		return id;
	}

	/**
	 * get the web access uri directly
	 * 
	 * @return String of uri
	 */
	public String getUri(Language lang) {

		String filename = "/temp/" + lang.format(Global.now(), "yyyy/MM/dd") + "/"
				+ Global.now() + "/" + id + "/" + name;
		try {
			DFile f1 = Disk.seek(filename);
			f1.upload(this.getInputStream());
			return "/f/d/" + f1.getId() + "/" + name;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return null;
	}

	/**
	 * refer DFile.upload
	 * 
	 * @param f
	 * @throws Exception
	 */
	@Comment()
	public void save(@Comment(text = "file") DFile f) throws Exception {
		f.upload(this.getInputStream());
	}

	/**
	 * refer DFile.upload
	 * 
	 * @param filename
	 * @throws Exception
	 */
//	public void upload(String filename) throws Exception {
//		DFile f = Disk.seek(filename);
//		save(f);
//	}

	/**
	 * update the temp to dfile
	 * 
	 * @return the filename
	 * @throws IOException
	 */
	@Comment(text = "上传本地临时文件到文件仓库临时文件")
	public DFile upload() throws IOException {
		DFile f1 = get(id, name);
		f1.upload(this.getInputStream());
		return f1;
	}

	@Comment(text = "上传字节到文件仓库临时文件")
	public DFile upload(@Comment(text = "bytes") byte[] bb) throws IOException {
		DFile f1 = get(id, name);
		f1.upload(bb);
		return f1;
	}

	/**
	 * （可能造成安全问题）
	 * 
	 * @deprecated
	 * 
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	public String saveas(String filename) throws Exception {

		if (X.callBy("nashorn", "jython", "groovy")) {
			throw new IOException("access deny!");
		}

		filename = X.getCanonicalPath(filename);
		for (String s : new String[] { "/etc/", "/dev/", "/usr/sbin/", "/usr/bin/", "/sbin/", "/bin/" }) {
			if (filename.startsWith(s)) {
				throw new Exception("禁止访问系统文件目录!");
			}
		}

		File f1 = new File(filename);
		X.IO.mkdirs(f1.getParentFile());
		X.IO.copy(this.getInputStream(), new FileOutputStream(f1));

		return filename;
	}

	@Comment(text = "上传输入流到文件仓库临时文件")
	public DFile upload(@Comment(text = "in") InputStream in) throws IOException {
		DFile f1 = get(id, name);
		f1.upload(in);
		return f1;
	}

	@Comment(text = "复制输入流到本地临时文件")
	public long copy(InputStream in) throws IOException {
		return IOUtil.copy(in, this.getOutputStream());
	}

	@Comment(text = "获取压缩输出流")
	public ZipOutputStream getZipOutputStream() throws Exception {
		return new ZipOutputStream(this.getOutputStream());
	}

	@Comment(text = "获取压缩输出流")
	public GZIPOutputStream getGZIPOutputStream() throws Exception {
		return new GZIPOutputStream(this.getOutputStream());
	}

	private byte[] bb = null;
	private boolean newdata = false;

	@Comment(text = "获取输出Writer")
	public Writer getWriter() throws IOException {
		return new OutputStreamWriter(this.getOutputStream());
	}

	@Comment(text = "获取输出流")
	public OutputStream getOutputStream() throws IOException {

		if (memory) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			return new OutputStream() {

				@Override
				public void write(int b) throws IOException {
//				if (out.size() > MAX)
//					throw new IOException("exceed max [" + MAX + "]");
					out.write(b);
				}

				@Override
				public void write(byte[] b) throws IOException {
//				if (out.size() > MAX)
//					throw new IOException("exceed max [" + MAX + "]");
					out.write(b);
				}

				@Override
				public void write(byte[] b, int off, int len) throws IOException {
//				if (out.size() > MAX)
//					throw new IOException("exceed max [" + MAX + "]");
					if (log.isDebugEnabled())
						log.debug("write, off=" + off + ", len=" + len);

					out.write(b, off, len);
				}

				@Override
				public void close() throws IOException {
					out.close();
					bb = out.toByteArray();
					newdata = true;
				}

			};
		}

		File f1 = this.getFile();
		X.IO.mkdirs(f1.getParentFile());
		f1.createNewFile();
		return new FileOutputStream(f1);

	}

	@Comment(text = "获取输入流")
	public InputStream getInputStream() throws IOException {
		if (memory) {
			if (bb != null) {
				// 1, create in from memory first
				ByteArrayInputStream in = new ByteArrayInputStream(bb);
				return in;
			} else {
				// 2, from local temp file
				File f1 = this.getFile();
				if (f1.exists()) {
					return new FileInputStream(f1);
				}
				// from distributed file system
				DFile f2 = this.getDFile();
				if (f2 != null && f2.exists()) {
					return f2.getInputStream();
				}
			}
		}

		return new FileInputStream(this.getFile());
	}

	public long zipcopy(String name, InputStream in) throws IOException {

		ZipOutputStream out = new ZipOutputStream(this.getOutputStream());
		try {
			ZipEntry e = new ZipEntry(name);
			out.putNextEntry(e);
			return IOUtil.copy(in, out, false);
		} finally {
			out.closeEntry();
			X.close(in, out);
		}

	}

	@Comment(text = "要是当前文件或文件夹")
	public Temp zip() throws Exception {

		Temp t = Temp.create(name + ".zip");
		File f = this.getFile();
		if (f.isFile()) {

			File f1 = t.getFile();
			ZipOutputStream out = null;
			InputStream in = null;

			try {
				in = new FileInputStream(f);
				out = new ZipOutputStream(new FileOutputStream(f1));

				ZipEntry e = new ZipEntry(f.getName());
				out.putNextEntry(e);
				IOUtil.copy(in, out, false);
				out.closeEntry();
			} finally {
				X.close(in, out);
			}

			return t;

		} else if (f.isDirectory()) {

			File f1 = t.getFile();
			ZipOutputStream out = null;

			try {
				out = new ZipOutputStream(new FileOutputStream(f1));

				ZipEntry e = new ZipEntry(f.getName());
				out.putNextEntry(e);
				out.closeEntry();

				File[] ff = f1.listFiles();
				if (ff != null) {
					for (File f2 : ff) {
						_zip(out, f2, f1.getName());
					}
				}

			} finally {
				X.close(out);
			}

			return t;

		}

		return null;

	}

	public void zip(File f1, Consumer<String> func) throws Exception {

		ZipOutputStream zip = this.getZipOutputStream();

		zip(zip, f1, ".", func);

		X.close(zip);
	}

	public void zip(ZipOutputStream out, File f, String relative, Consumer<String> func) throws Exception {

		if (f.isFile()) {

			ZipEntry e = new ZipEntry(f.getName());
			out.putNextEntry(e);

			InputStream in = new FileInputStream(f);

			try {
				if (func != null) {
					func.accept(f.getName());
				}

				IOUtil.copy(in, out, false);
			} finally {
				X.close(in);
				out.closeEntry();
			}

		} else if (f.isDirectory()) {

			ZipEntry e = new ZipEntry(f.getName());
			out.putNextEntry(e);
			out.closeEntry();

			try {

				File[] ff = f.listFiles();
				if (ff != null) {
					for (File f2 : ff) {
						zip(out, f2, relative + "/" + f.getName(), func);
					}
				}

			} finally {
				X.close(out);
			}

		}

	}

	public void zip(DFile f1, Consumer<String> func) throws Exception {

		ZipOutputStream zip = this.getZipOutputStream();

		_zip(zip, f1, ".", func);

		X.close(zip);
	}

	private void _zip(ZipOutputStream out, DFile f, String relative, Consumer<String> func) throws Exception {

		if (f.isFile()) {

			ZipEntry e = new ZipEntry(relative + "/" + f.getName());
			out.putNextEntry(e);
			InputStream in = f.getInputStream();
			try {
				if (func != null) {
					func.accept(f.getName());
				}

				IOUtil.copy(in, out, false);
			} finally {
				X.close(in);
				out.closeEntry();
			}

		} else if (f.isDirectory()) {

//			ZipEntry e = new ZipEntry(f.getName());
//			out.putNextEntry(e);
//			out.closeEntry();

			DFile[] ff = f.listFiles();
			if (ff != null) {
				for (DFile f2 : ff) {
					_zip(out, f2, relative + "/" + f.getName(), func);
				}
			}

		}

	}

	private void _zip(ZipOutputStream out, File f, String path) throws Exception {
		if (f.isFile()) {
			InputStream in = null;

			try {
				in = new FileInputStream(f);

				ZipEntry e = new ZipEntry(path + "/" + f.getName());
				out.putNextEntry(e);
				IOUtil.copy(in, out, false);
				out.closeEntry();

			} finally {
				X.close(in);
			}

		} else if (f.isDirectory()) {

			ZipEntry e = new ZipEntry(path + "/" + f.getName());
			out.putNextEntry(e);
			out.closeEntry();

			File[] ff = f.listFiles();
			if (ff != null) {
				for (File f1 : ff) {
					_zip(out, f1, path + "/" + f1.getName());
				}
			}

		}
	}

	public File unzip() throws Exception {

		Temp t2 = Temp.create(name);

		File f2 = t2.getFile();
		X.IO.mkdirs(f2);

		ZipInputStream in = null;

		try {
			in = new ZipInputStream(new FileInputStream(this.getFile()));
			ZipEntry e = in.getNextEntry();
			while (e != null) {

				if (!e.isDirectory()) {

					File f3 = new File(f2.getAbsolutePath() + "/" + e.getName());
					X.IO.mkdirs(f3.getParentFile());
					OutputStream out = null;

					try {
						out = new FileOutputStream(f3);
						IOUtil.copy(in, out, false);
					} finally {
						X.close(out);
					}

				}

				e = in.getNextEntry();
			}
		} finally {
			X.close(in);
		}

		return f2;

	}

	public static DFile get(String id, String name) throws IOException {

		String filename = _path(id, name);
		DFile f1 = Disk.seek(filename);
		if (f1 != null) {
			f1.limit("/temp/");
		}
		return f1;

	}

	public boolean isRoot(File f) {
		return X.isSame(X.getCanonicalPath(f.getAbsolutePath()), X.getCanonicalPath(ROOT));
	}

	public boolean isRoot(DFile f) {
		return X.isSame(X.getCanonicalPath(f.getFilename()), X.getCanonicalPath(ROOT));
	}

	@Comment(text = "临时文件长度")
	public long length() throws IOException {
		return this.getFile().length();
	}

	@Comment(text = "获取文件名")
	public String getName() {
		return name;
	}

	transient File file;

	@Comment(hide = true)
	public File getFile() throws IOException {

		if (X.callBy("nashorn", "jython", "groovy")) {
			throw new IOException("access deny!");
		}

		if (file == null) {
			// write bb to file
			file = new File(path(id, name));
		}

		if (memory) {
			if (bb != null && newdata) {
				if (file.exists()) {
					file.delete();
				} else {
					X.IO.mkdirs(file.getParentFile());
				}

				OutputStream out = null;
				try {
					out = new FileOutputStream(file);
					out.write(bb);
				} finally {
					X.close(out);
				}
			}
		}

		return file;
	}

	@Comment(text = "获取文件仓库临时文件")
	public DFile getDFile() throws IOException {
		return get(id, name);
	}

	public static String path(String path, String name) {
		long id = Math.abs(UID.hash(path));
		char p1 = (char) (id % 23 + 'a');
		char p2 = (char) (id % 19 + 'A');
		char p3 = (char) (id % 17 + 'a');
		char p4 = (char) (id % 13 + 'A');

		StringBuilder sb = new StringBuilder(ROOT);

		sb.append("/").append(p1).append("/").append(p2).append("/").append(p3).append("/").append(p4).append("/")
				.append(id);

		if (name != null)
			sb.append("/").append(name);

		return sb.toString();
	}

	private static String _path(String path, String name) {

		long id = Math.abs(UID.hash(path));
		char p1 = (char) (id % 23 + 'a');
		char p2 = (char) (id % 19 + 'A');
		char p3 = (char) (id % 17 + 'a');
		char p4 = (char) (id % 13 + 'A');

		StringBuilder sb = new StringBuilder("/temp");

		sb.append("/").append(p1).append("/").append(p2).append("/").append(p3).append("/").append(p4).append("/")
				.append(id);

		if (name != null) {
			sb.append("/").append(name);
		}

		return sb.toString();
	}

	@Comment(text = "保存输入流到临时文件")
	public void save(InputStream in) throws Exception {
		X.IO.copy(in, this.getOutputStream());
	}

	@Override
	public String toString() {
		return "Temp [name=" + name + ", size=" + size() + "]";
	}

	@Comment(text = "删除临时文件")
	public void delete() {
		try {
			if (memory) {
				bb = null;
			} else if (this.getFile() != null) {
				Task.schedule(t -> {
					try {
						File f = this.getFile();
						while (f.exists() && !isRoot(f)) {
							File f1 = f;
							f = f1.getParentFile();
							if (f1.delete()) {
								break;
							}
						}
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}

					try {
						DFile f = this.getDFile();
						if (f != null) {
							while (f.exists() && !isRoot(f)) {
								DFile f1 = f;
								f = f1.getParentFile();
								if (f1.delete()) {
									break;
								}
							}
						}
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}

				});
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public static int cleanup(long age) {

		log.warn("cleanup temp [age=" + age + "] ...");

		int count = 0;
		{
			File f1 = new File(ROOT);
			File[] ff = f1.listFiles();
			if (ff != null) {
				for (File f2 : ff) {
					if (!X.isIn(f2.getName(), "_cache")) {
						try {
							count += IOUtil.delete(f2, age, null);
						} catch (Exception e) {
							log.error(e.getMessage(), e);
						}
					}
				}
			}
		}

		{

			Lock door = Global.getLock("temp.cleanup");
			if (door.tryLock()) {
				try {
					for (String r : new String[] { "/temp", "/temp1", "/temp2" }) {
						Collection<DFile> ff = Disk.list(r);
						if (ff != null) {
							for (DFile f2 : ff) {
								try {
									count += IOUtil.delete(f2, age);
								} catch (Exception e) {
									log.error(e.getMessage(), e);
								}
							}
						}
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				} finally {
					door.unlock();
				}
			}
		}

		List<?> temp = Config.getConf().getList("cleanup.path", null);
		if (temp != null) {
			for (Object o : temp) {
				String s1 = o.toString();

				if (s1.contains("*")) {
					int i = s1.lastIndexOf("/");
					File f2 = new File(s1.substring(0, i));
					String name = s1.substring(i + 1);
					try {
						count += IOUtil.delete2(f2, age, name);
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				} else {
					File f2 = new File(s1);
					try {
						count += IOUtil.delete(f2, age, null);
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				}
			}
		}

		return count;
	}

	public static Temp get(String filename) {
		Temp t = new Temp();
		filename = X.getCanonicalPath(filename);
		if (!filename.startsWith(ROOT)) {
			filename = X.getCanonicalPath(ROOT + "/" + filename);
		}

		t.file = new File(filename);
		t.memory = false;
		return t;
	}

	@Comment(text = "改名")
	public void rename(String name) {
		try {
			// 防止name包含"/"
			name = new File(name).getName();

			File f1 = this.getFile();
			File f2 = new File(f1.getParentFile() + "/" + name);
			f1.renameTo(f2);
			this.name = name;
			file = f2;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

}
