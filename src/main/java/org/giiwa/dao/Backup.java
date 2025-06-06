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
package org.giiwa.dao;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Data;
import org.giiwa.bean.Disk;
import org.giiwa.dao.Helper.W;
import org.giiwa.dfile.DFile;
import org.giiwa.json.JSON;
import org.giiwa.misc.IOUtil;
import org.giiwa.task.BiConsumer;
import org.giiwa.task.BiFunction;
import org.giiwa.task.Consumer;

public final class Backup {

	private static Log log = LogFactory.getLog(Backup.class);

	public static long backup(String table, ZipOutputStream out) throws IOException, SQLException {
		return backup(table, null, out, null);
	}

	public static long write(String filename, byte[] bb, ZipOutputStream out) throws IOException {

		ZipEntry e = new ZipEntry(filename);
		try {
			out.putNextEntry(e);
			out.write(bb);
		} finally {
			out.closeEntry();
		}
		return bb.length;

	}

	public static long backup(String table, W q, ZipOutputStream out, BiFunction<String, Data, JSON> func)
			throws IOException, SQLException {

		if (log.isDebugEnabled())
			log.debug("backup, table=" + table);

		ZipEntry e = new ZipEntry(table + ".db");
		try {
			if (q == null) {
				q = W.create();
			}

			out.putNextEntry(e);
			int s = 0;
			Beans<Data> l1 = Helper.primary.load(table, q, s, 100, Data.class);
			while (l1 != null && !l1.isEmpty()) {
				for (Data d : l1) {
					JSON j1 = null;
					if (func != null) {
						j1 = func.apply(table, d);
					} else {
						j1 = d.json();
					}
					if (j1 != null) {
						j1.append("_table", table);
						String s1 = j1.toString();
						s1 = Base64.getEncoder().encodeToString(s1.getBytes()) + "\r\n";
						out.write(s1.getBytes());
					}
				}
				s += l1.size();
				l1 = Helper.primary.load(table, q, s, 100, Data.class);
			}
			return s;
		} finally {
			out.closeEntry();
		}

	}

	public static long backup(DFile f, ZipOutputStream out) throws IOException {

		if (f == null || !f.exists())
			return 0;

		if (f.isDirectory()) {
			long len = 0;
			DFile[] ff = f.listFiles();
			if (ff != null) {
				for (DFile f1 : ff) {
					len += backup(f1, out);
				}
			}
			return len;
		} else if (f.isFile()) {

			if (log.isDebugEnabled())
				log.debug("backup, file=" + f.getFilename());

			ZipEntry e = new ZipEntry("/.dfile/" + f.getFilename());
			out.putNextEntry(e);
			InputStream in = f.getInputStream();
			try {
				return IOUtil.copy(in, out, false);
			} finally {
				out.closeEntry();
				X.close(in);
			}
		}
		return 0;
	}

	public static void recover(String filename, ZipInputStream in) throws Exception {

		if (filename.startsWith("/.dfile/")) {
			filename = filename.replaceFirst("/.dfile/", X.EMPTY);
		}
		DFile f = Disk.seek(filename);
		if (f.exists()) {
			f.delete();
		}

		if (log.isDebugEnabled())
			log.debug("recover, file=" + f.getFilename());

		OutputStream out = f.getOutputStream();
		try {
			IOUtil.copy(in, out, false);
		} finally {
			X.close(out);
		}
	}

	public static void recover(String table, ZipInputStream in, Consumer<JSON> func) throws IOException {

		if (log.isDebugEnabled()) {
			log.debug("recover, table=" + table);
		}

		if (func == null) {
			throw new IOException("callback func is null");
		}

		BufferedReader re = new BufferedReader(new InputStreamReader(in));

		String line = re.readLine();
		while (line != null) {
			String s1 = new String(Base64.getDecoder().decode(line));
			JSON j1 = JSON.fromObject(s1);
			func.accept(j1);
//			Helper.insert(table, V.fromJSON(j1));

			line = re.readLine();
		}

	}

	public static void find(ZipInputStream in, String filename, BiConsumer<String, ZipInputStream> func)
			throws IOException {

		ZipEntry e = in.getNextEntry();
		while (e != null) {
			if (e.getName().matches(filename)) {
				func.accept(e.getName(), in);
			}
			e = in.getNextEntry();
		}

	}

	public static void recover(ZipInputStream in) throws IOException {

		find(in, ".*", (filename, in1) -> {
			try {
//				if (filename.startsWith("/.dfile/")) {
//					recover(filename, in1);
//				} else if (filename.endsWith(".db")) {
				if (filename.endsWith(".db")) {
					recover(filename.substring(0, filename.length() - 3), in, null);
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		});

	}

}
