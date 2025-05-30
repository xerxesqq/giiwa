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
package org.giiwa.web.view;

import java.util.Map;

import org.giiwa.json.JSON;
import org.giiwa.web.Controller;

import jakarta.servlet.RequestDispatcher;

public class JspView extends View {

	@Override
	protected boolean parse(Object file, Controller m, String viewname) throws Exception {
		//TODO
		String name = View.getCanonicalPath(file).substring((Controller.GIIWA_HOME + "/modules/").length());
		if (log.isDebugEnabled())
			log.debug("viewname=" + name);

		name = name.replaceAll("\\\\", "/");
		Map<String, Object> data = m.getData();
		if (data != null) {
			for (String s : data.keySet()) {
				m.req.setAttribute(s, data.get(s));
			}
		}
		m.req.setAttribute(System.getProperty("org.apache.jasper.Constants.JSP_FILE", "org.apache.catalina.jsp_file"),
				name);

		RequestDispatcher rd = m.req.getRequestDispatcher(name);
		if (rd == null) {
			log.warn("Not a valid resource path:" + name);
			return false;
		} else {
			if (log.isDebugEnabled())
				log.debug("including jsp page, name=" + name);
		}

		rd.include(m.req.req, m.resp);

		return true;
	}

	@Override
	public String parse(Object file, JSON params) {
		// TODO Auto-generated method stub
		return null;
	}

}
