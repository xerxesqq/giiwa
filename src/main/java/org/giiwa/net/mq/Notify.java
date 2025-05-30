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
package org.giiwa.net.mq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.net.mq.MQ.Request;
import org.giiwa.task.Function;

class Notify extends IStub {

	private static Log log = LogFactory.getLog(Notify.class);

	public static String name = "notify";

	private static Map<String, List<Object[]>> waiter = new HashMap<String, List<Object[]>>();

	public Notify() {
		super(name);
	}

	@Override
	public void onRequest(long seq, Request req) {

		String name = req.from;

		Object d = null;

		try {
			d = req.get();
		} catch (Exception e) {
			log.error("seq=" + seq + ", req=" + req, e);
		}

		List<Object[]> l1 = waiter.get(name);
		if (l1 != null) {
			synchronized (l1) {
				for (Object[] a : l1) {
					synchronized (a) {
						a[0] = d;
						a.notifyAll();
					}
				}
			}
		}

	}

	@SuppressWarnings("unchecked")
	public static <T, R> R wait(String name, long timeout, Function<T, R> func) {
		Object[] a = new Object[1];
		try {
			synchronized (a) {
				List<Object[]> l1 = waiter.get(name);
				if (l1 == null) {
					l1 = new ArrayList<Object[]>();
					waiter.put(name, l1);
				}
				synchronized (l1) {
					l1.add(a);
				}

				R t1 = null;
				if (func != null) {
					t1 = func.apply(null);
				}
				if (t1 != null) {
					return t1;
				} else {
					a.wait(timeout);
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			List<Object[]> l1 = waiter.get(name);
			if (l1 != null) {
				synchronized (l1) {
					l1.remove(a);
					if (l1.isEmpty()) {
						waiter.remove(name);
					}
				}
			}
		}

		return ((R) a[0]);
	}

}
