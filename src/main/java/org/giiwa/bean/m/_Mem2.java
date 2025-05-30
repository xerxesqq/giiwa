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
package org.giiwa.bean.m;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.Bean;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.Column;
import org.giiwa.dao.Table;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;

/**
 * JVM内存使用
 * 
 * @author joe
 *
 */
@Table(name = "gi_m_mem2", memo = "GI-内存使用")
public class _Mem2 extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(_Mem2.class);

	public static BeanDAO<String, _Mem2> dao = BeanDAO.create(_Mem2.class);

	@Column(memo = "主键", unique = true, size = 50)
	String id;

	@Column(memo = "节点", size = 50)
	String node;

	@Column(memo = "总内存")
	public long total;

	@Column(memo = "已使用")
	public long used;

	public synchronized static void update(String node, long total, long used) {
		// insert or update
		try {
			V v = V.create();

			v.append("total", total);
			v.append("used", used);

			String id = UID.id(node);
			if (dao.exists2(id)) {
				dao.update(id, v.copy().force("node", node));
			} else {
				// insert
				dao.insert(v.copy().force(X.ID, id).force("node", node));
			}

			Record.dao.insert(v.force(X.ID, UID.id(node, Global.now())).force("node", node));

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

	@Table(name = "gi_m_mem2_record", memo = "GI-内存使用历史")
	public static class Record extends _Mem2 {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public static BeanDAO<String, Record> dao = BeanDAO.create(Record.class);

		public void cleanup() {
			dao.delete(W.create().and("created", Global.now() - X.AWEEK, W.OP.lt));
		}

	}

	public static void check() {
		Runtime t = Runtime.getRuntime();
		_Mem2.update(Local.id(), t.totalMemory(), t.totalMemory() - t.freeMemory());
	}

}
