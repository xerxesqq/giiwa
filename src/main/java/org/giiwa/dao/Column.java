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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * the {@code Column} Class used to annotate the Bean, define the
 * collection/table mapping with the Bean
 * 
 * 
 * @author joe
 *
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {

	/**
	 * not table field
	 * 
	 * @return boolean
	 */
	boolean no() default false;

	/**
	 * the column name.
	 *
	 * @return String
	 */
	String name() default X.EMPTY;

	/**
	 * the length of the column
	 * 
	 * @return the length of the column, default -1
	 */
	int length() default -1;

	/**
	 * is Index field.
	 *
	 * @return true, if successful
	 */
	boolean index() default false;

	/**
	 * full text index, the number is the weight
	 * 
	 * @return 1 text field
	 */
	int text() default 0;

	/**
	 * the string type, "text", "clob", "blob",
	 * 
	 * @return
	 */
	int size() default 0;

	/**
	 * is Unique column.
	 *
	 * @return true, if successful
	 */
	boolean unique() default false;

	/**
	 * the memo of the column
	 * 
	 * @return
	 */
	String memo() default X.EMPTY;

	/**
	 * the valid value of the column
	 * 
	 * @return
	 */
	String value() default X.EMPTY;

}
