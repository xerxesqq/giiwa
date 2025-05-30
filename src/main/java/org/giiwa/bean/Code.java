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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.conf.Global;
import org.giiwa.dao.Bean;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.Column;
import org.giiwa.dao.Table;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;

/**
 * The code bean, used to store special code linked with s1 and s2 fields
 * table="gi_code"
 * 
 * @author wujun
 *
 */
@Table(name = "gi_code", memo = "GI-验证码")
public final class Code extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(Code.class);

	public static final BeanDAO<String, Code> dao = BeanDAO.create(Code.class);

	@Column(memo = "主键", unique = true, size = 50)
	private String id;

	@Column(memo = "名称1")
	private String s1;

	@Column(memo = "名称2")
	private String s2;

	@Column(memo = "过期时间")
	private long expired;

	public long getExpired() {
		return expired;
	}

	public static int create(String s1, String s2, V v) {
		if (v == null) {
			v = V.create();

		}

		String id = UID.id(s1, s2);
		try {
			if (dao.exists(id)) {
				dao.delete(id);
			}
			return dao.insert(v.force(X.ID, id).set("s1", s1).set("s2", s2));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return -1;
	}

	public static Code load(String s1, String s2) {
		W q = W.create().and("s1", s1).and("s2", s2).and("expired", Global.now(), W.OP.gt);
		dao.optimize(q);
		return dao.load(q);
	}

	public static void delete(String s1, String s2) {
		dao.delete(W.create().and("s1", s1).and("s2", s2));
	}

	public static int cleanup() {
		log.warn("cleanup [Code] ...");

		W q = W.create().and("expired", Global.now() - X.AWEEK, W.OP.lte);
		dao.optimize(q);
		return dao.delete(q);
	}

}
