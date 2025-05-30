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
package org.giiwa.task;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.cache.Cache;
import org.giiwa.conf.Global;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;

/**
 * used to monitor a Task
 * 
 * @author wujun
 *
 */
public class Monitor {

	static Log log = LogFactory.getLog(Monitor.class);

	/**
	 * Start.
	 *
	 * @param t  the t
	 * @param ms the ms
	 * @return the long
	 * @throws Exception
	 */
	public static long start(Task t, long ms) throws Exception {
		return start(t, ms, X.EMPTY);
	}

	public static long start(Task t) throws Exception {
		return start(t, 0, X.EMPTY);
	}

	public static long start(Task t, String access) throws Exception {
		return start(t, 0, access);
	}

	/**
	 * start a task in monitor
	 * 
	 * @param t      the task
	 * @param
	 * @param access
	 * @return
	 * @throws Exception
	 */
	public static long start(Task t, long ms, String access) throws Exception {

		long tid = UID.next("monitor.id");
		t.attach("tid", tid);
		t.attach("access", access);
		flush(t);

		t.schedule(ms);
		return tid;
	}

	private static String _name(long tid) {
		return "task/monitor/" + tid;
	}

	public static void flush(Task t) {

		long tid = X.toLong(t.attach("tid"));
		String name = _name(tid);
		long created = Global.now();
		long get = Global.now();

		Lock door = Global.getLock("monitor//" + name);
		door.lock();
		try {
			JSON jo = Cache.get(name);
			if (jo != null) {
				created = jo.getLong("_created");
				get = jo.getLong("_get");
				if (Global.now() - created > X.AMINUTE && Global.now() - get > X.AMINUTE) {
					// 1分钟没有get， kill掉 t
					t.stop(true);
					return;
				}
			}

			Field[] fs = t.getClass().getDeclaredFields();
			if (fs != null) {
				jo = JSON.create();
				jo.put("_access", t.attach("access"));
				jo.put("_name", t.getName());
				jo.put("_created", created);
				jo.put("_get", get);

				for (Field f : fs) {
					int p = f.getModifiers();
					if ((p & Modifier.TRANSIENT) != 0 || (p & Modifier.STATIC) != 0 || (p & Modifier.FINAL) != 0)
						continue;

					try {
						if (log.isDebugEnabled())
							log.debug(f.getName() + "=" + f.getType());
						f.setAccessible(true);
						jo.put(f.getName(), f.get(t));
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				}

				Cache.set(name, jo, X.AHOUR);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			door.unlock();
		}

	}

	/**
	 * Gets the.
	 *
	 * @param tid the tid
	 * @return the json
	 */
	public static JSON get(long tid) {
		return get(tid, X.EMPTY);
	}

	/**
	 * get the state with the access
	 * 
	 * @param tid
	 * @param access
	 * @return
	 */
	public static JSON get(long tid, String access) {

		String name = _name(tid);

		Lock door = Global.getLock("monitor//" + name);
		door.lock();
		try {
			Object t = Cache.get(name);
			if (t != null) {
				if (t instanceof JSON && X.isSame(access, ((JSON) t).get("_access"))) {
					JSON e = (JSON) t;
					JSON j1 = e.copy();
					j1.remove("_access", "_created", "_get", "_name");
					e.put("_get", Global.now());
					Cache.set(name, e);
					return j1;
				} else {
					return JSON.create().append("state", 201).append("error", "bad access");
				}
			} else {
				return JSON.create().append("state", 201).append("error", "not found [" + tid + "]");
			}
		} finally {
			door.unlock();
		}

	}

}
