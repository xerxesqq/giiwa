/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
*/
package org.giiwa.bean;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.app.task.CleanupTask;
import org.giiwa.conf.Global;
import org.giiwa.dao.Bean;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Column;
import org.giiwa.dao.Helper;
import org.giiwa.dao.Table;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.dao.RDSHelper;
import org.giiwa.json.JSON;
import org.giiwa.web.Language;

/**
 * The Class Stat is used to stat utility and persistence.
 * 
 * @author wujun
 *
 */
@Table(name = "gi_stat", memo = "GI-统计信息表")
public final class Stat extends Bean implements Comparable<Stat> {

	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(Stat.class);

	public static Language lang = Language.getLanguage();

	public static BeanDAO<Long, Stat> dao = BeanDAO.create(Stat.class);

	public static enum SIZE {
		min, m10, m15, m30, hour, day, week, month, season, year
	};

	public static enum TYPE {
		delta, snapshot;
	}

	@Column(memo = "主键", unique = true)
	protected long id;

	@Column(memo = "模块名称", size = 50)
	protected String module; // 统计模块

	@Column(memo = "统计日期", size = 50)
	protected String date; // 日期

	@Column(memo = "统计时间")
	protected long time; // 时间

	@Column(memo = "统计粒度", size = 50)
	protected String size;// size of the stat data

	public String getDate() {
		return date;
	}

	private static Set<String> _existsTables = new HashSet<String>();

	public static String table(String module) {
		String name = "gi_stat_"
				+ (module.replaceAll("\\.delta", "").replaceAll("\\.snapshot", "").replaceAll("[\\.-]", "_"));
		if (!_existsTables.contains(name)) {
			// create table;
			if (Helper.primary instanceof RDSHelper) {
				dao.createTable(name, "GI-统计信息-" + module, JSON.create().append("distributed", 0));
			}
			_existsTables.add(name);
		}
		return name;
	}

	public String getModule() {
		return module;
	}

	/**
	 * Insert or update.
	 *
	 * @param module the module
	 * @param date   the date
	 * @param size   the size
	 * @param q0     the query
	 * @param v      the value
	 * @param n      the n
	 * @return the int
	 */
	private static int insertOrUpdate(String module, String date, SIZE size, W q0, V v, long... n) {
		if (v == null) {
			v = V.create();
		} else {
			v = v.copy();
		}

		String table = table(module);

//		Lock door = Local.getLock("table/" + table);
//		if (door.tryLock()) {
		try {
			W q = q0.copy().and("date", date).and("size", size.toString()).and("module", module);

			// force optimize
			Helper.optimize(table, q);

			if (log.isDebugEnabled()) {
				log.debug("table=" + table + ",q=" + q);
			}

//			synchronized (Stat.class) {

			if (!Helper.primary.exists(table, q)) {

//				Helper.optimize(table, W.create().and(X.ID, id));

				v.append("date", date).append("size", size.toString()).append("module", module);
				for (int i = 0; i < n.length; i++) {
					v.set("n" + i, n[i]);
				}

				if (log.isDebugEnabled()) {
					log.debug("snapshot, v=" + v);
				}

				long id = UID.hash(module + "_" + date + "_" + size + "_" + q.toString());
				W q1 = W.create().and(X.ID, id);
				Helper.optimize(table, q1);

				if (log.isDebugEnabled()) {
					log.debug("table=" + table + ",q1=" + q1);
				}

				if (Helper.primary.exists(table, q1)) {
					// update
					return Helper.primary.updateTable(table, q1, v);
				} else {
					v.force(X.ID, id);
					return Helper.primary.insertTable(table, v);
				}

			} else {
				/**
				 * only update if count > original
				 */
				for (int i = 0; i < n.length; i++) {
					v.set("n" + i, n[i]);
				}
				return Helper.primary.updateTable(table, q, v);
			}

//			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			// cleanup(module, size);
//				door.unlock();
		}
//		}

		return -1;
	}

	public static void delete(String module, W q) {
		// delete
		String table = table(module);
		Helper.primary.delete(table, q);
	}

	public static int cleanup(String table, SIZE size) {
		// delete old data
//		String table = table(module);
		W q1 = W.create().and("size", size.toString());

		switch (size) {
		case min:
			q1.and("time", Global.now() - X.ADAY, W.OP.lt);
			break;
		case m10:
			q1.and("time", Global.now() - 2 * X.ADAY, W.OP.lt);
			break;
		case m15:
			q1.and("time", Global.now() - 2 * X.ADAY, W.OP.lt);
			break;
		case m30:
			q1.and("time", Global.now() - 2 * X.ADAY, W.OP.lt);
			break;
		case hour:
			q1.and("time", Global.now() - 7 * X.ADAY, W.OP.lt);
			break;
		case day:
			q1.and("time", Global.now() - X.AYEAR, W.OP.lt);
			break;
		case week:
			q1.and("time", Global.now() - X.AYEAR, W.OP.lt);
			break;
		case month:
			q1.and("time", Global.now() - 2 * X.AYEAR, W.OP.lt);
			break;
		case season:
			q1.and("time", Global.now() - 2 * X.AYEAR, W.OP.lt);
			break;
		case year:
			return 0;
		}

		Helper.optimize(table, q1);

		return Helper.primary.delete(table, q1);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable.compareTo(java.lang.Object)
	 */
	public int compareTo(Stat o) {
		if (this == o)
			return 0;

		int c = getDate().compareTo(o.getDate());
		return c;
	}

	/**
	 * @Deprecated
	 * @param time
	 * @param name
	 * @param sizes
	 * @param v
	 * @param n
	 */
	public static void delta(long time, String name, SIZE[] sizes, V v, long... n) {
		if (sizes != null) {
			for (SIZE size : sizes) {
				delta(time, name, size, W.create(), v, n);
			}
		}
	}

	public static long[] time(SIZE size, String date) {
		if (SIZE.min == size) {
			long t = lang.parse(date, "yyyy-MM-dd/HH");
			t = Stat.tohour(t);
			return new long[] { t, t + X.AHOUR };

		} else if (SIZE.m10 == size) {
			long t = lang.parse(date, "yyyy-MM-dd/HH");
			t = Stat.tohour(t);
			return new long[] { t, t + X.AHOUR };

		} else if (SIZE.m15 == size) {
			long t = lang.parse(date, "yyyy-MM-dd/HH");
			t = Stat.tohour(t);
			return new long[] { t, t + X.AHOUR };

		} else if (SIZE.m30 == size) {
			long t = lang.parse(date, "yyyy-MM-dd/HH");
			t = Stat.tohour(t);
			return new long[] { t, t + X.AHOUR };

		} else if (SIZE.hour == size) {
			long t = lang.parse(date, "yyyy-MM-dd");
			t = Stat.today(t);
			return new long[] { t, t + X.ADAY };
		} else if (SIZE.day == size) {
			long t = lang.parse(date, "yyyy-MM");
			t = Stat.tomonth(t);
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(t);
			c.add(Calendar.MONTH, 1);
			return new long[] { t, c.getTimeInMillis() };
		} else if (SIZE.month == size) {
			long t = lang.parse(date, "yyyy");
			t = Stat.toyear(t);
			return new long[] { t, t + X.AYEAR };
		}

		return new long[] { 0, 0 };
	}

	/**
	 * 
	 * @param time
	 * @param name
	 * @param q
	 * @param v
	 * @param n
	 */
	public static void delta(long time, String name, W q, V v, long... n) {

		if (v == null) {
			v = V.create();
		}
		if (q == null) {
			q = W.create();
		}

		long time1 = Stat.tomin(time);
		W q1 = q;
		V v1 = v;

		_delta(time1, name, SIZE.min, q1.copy(), v1.copy(), n);

		Stat s1 = Stat.load(name, TYPE.snapshot, SIZE.min, q1.copy().and("time", time1, W.OP.lte).sort("time", -1));

		if (s1 != null) {
			long[] n1 = new long[n.length];
			for (int i = 0; i < n1.length; i++) {
				n1[i] = s1.getLong("n" + i);
			}

			for (SIZE s2 : new SIZE[] { SIZE.m10, SIZE.m15, SIZE.m30, SIZE.hour, SIZE.day, SIZE.week, SIZE.month,
					SIZE.season, SIZE.year }) {
				long time2 = Stat.parse(time1, s2);
				_snapshot(time2, name, s2, q1.copy(), v1.copy(), n1);
			}
		}

	}

	/**
	 * @Deprecated
	 * @param time
	 * @param name
	 * @param size
	 * @param q
	 * @param v
	 * @param n
	 */
	public static void delta(long time, String name, SIZE size, W q, V v, long... n) {
		_delta(time, name, size, q, v, n);
	}

	private static void _delta(long time, String name, SIZE size, W q, V v, long... n) {

		String date = format(time, size);
		String table = table(name);
		if (q == null) {
			q = W.create();
		}
		if (v == null) {
			v = V.create();
		}

		W q1 = q.and("module", name + "." + Stat.TYPE.snapshot).and("size", size.toString()).and("date", date, W.OP.lt)
				.sort("time", -1);
		Helper.optimize(table, q1);

		Stat s1 = Helper.primary.load(table, q1, Stat.class);

		long[] d = new long[n.length];
		for (int i = 0; i < d.length; i++) {
			d[i] = s1 == null ? n[i] : n[i] + s1.getLong("n" + i);
		}

		v.append("time", time);
		Stat.insertOrUpdate(name + "." + Stat.TYPE.snapshot, date, size, q, v, d);
		Stat.insertOrUpdate(name + "." + Stat.TYPE.delta, date, size, q, v, n);

	}

	/**
	 * @Deprecated
	 * @param time
	 * @param name
	 * @param sizes
	 * @param q
	 * @param v
	 * @param n
	 */
	public static void snapshot(long time, String name, SIZE[] sizes, W q, V v, long... n) {
		if (sizes != null) {
			for (SIZE size : sizes) {
				snapshot(time, name, size, q, v, n);
			}
		}
	}

	public static String format(long time, SIZE size) {

		if (SIZE.min == size) {
			return lang.format(time, "yyyy-MM-dd/HH:mm");
		} else if (SIZE.m10 == size) {
			time = time / X.AMINUTE / 10 * X.AMINUTE * 10;
			return lang.format(time, "yyyy-MM-dd/HH:mm");
		} else if (SIZE.m15 == size) {
			time = time / X.AMINUTE / 15 * X.AMINUTE * 15;
			return lang.format(time, "yyyy-MM-dd/HH:mm");
		} else if (SIZE.m30 == size) {
			time = time / X.AMINUTE / 30 * X.AMINUTE * 30;
			return lang.format(time, "yyyy-MM-dd/HH:mm");
		} else if (SIZE.hour == size) {
			return lang.format(time, "yyyy-MM-dd/HH");
		} else if (SIZE.day == size) {
			return lang.format(time, "yyyy-MM-dd");
		} else if (SIZE.week == size) {
			return lang.format(time, "yyyy|ww");
		} else if (SIZE.month == size) {
			return lang.format(time, "yyyy-MM");
		} else if (SIZE.season == size) {
			int season = X.toInt(lang.format(time, "MM")) / 3 + 1;
			return lang.format(time, "yyyy") + "/0" + season;
		} else if (SIZE.year == size) {
			return lang.format(time, "yyyy");
		} else {
			return lang.format(time, "yyyy-MM-dd");
		}
	}

	public static long parse(long time, SIZE size) {

		if (SIZE.min == size) {
			return Stat.tomin(time);
		} else if (SIZE.m10 == size) {
			return Stat.tom10(time);
		} else if (SIZE.m15 == size) {
			return Stat.tom15(time);
		} else if (SIZE.m30 == size) {
			return Stat.tom30(time);
		} else if (SIZE.hour == size) {
			return Stat.tohour(time);
		} else if (SIZE.day == size) {
			return Stat.today(time);
		} else if (SIZE.week == size) {
			return Stat.toweek(time);
		} else if (SIZE.month == size) {
			return Stat.tomonth(time);
		} else if (SIZE.season == size) {
			return Stat.toseason(time);
		} else if (SIZE.year == size) {
			return Stat.toyear(time);
		}

		return time;
	}

	/**
	 * snapshot the stat
	 * 
	 * @param time
	 * @param name
	 * @param q
	 * @param v
	 * @param n
	 */
	public static void snapshot(long time, String name, W q, V v, long... n) {

		if (v == null) {
			v = V.create();
		}
		if (q == null) {
			q = W.create();
		}

		long time1 = Stat.tomin(time);
		V v1 = v;
		W q1 = q;

		_snapshot(time1, name, SIZE.min, q1.copy(), v1.copy(), n);

//		Stat s1 = Stat.load(name, TYPE.snapshot, SIZE.min, q1.copy().and("time", time1, W.OP.lte).sort("time", -1));
//
//		if (s1 != null) {

//			long[] n1 = new long[n.length];
//			for (int i = 0; i < n1.length; i++) {
//				n1[i] = s1.getLong("n" + i);
//			}

		for (SIZE s2 : new SIZE[] { SIZE.hour, SIZE.day, SIZE.week, SIZE.month, SIZE.year }) {

			time1 = Stat.parse(time, s2);
			_snapshot(time1, name, s2, q1.copy(), v1.copy(), n);
		}

//		}

	}

	/**
	 * @Deprecated
	 * @param time the long of the timestamp
	 * @param name the string of the module name
	 * @param size the SIZE
	 * @param q    the query
	 * @param v    the value
	 * @param n    the data
	 */
	public static void snapshot(long time, String name, SIZE size, W q, V v, long... n) {
		_snapshot(time, name, size, q, v, n);
	}

	private static void _snapshot(long time, String name, SIZE size, W q, V v, long... n) {

		String date = format(time, size);
		String table = table(name);

		W q1 = q.copy().and("module", name + "." + TYPE.snapshot).and("size", size.toString())
				.and("time", time, W.OP.lt).sort("time", -1);

		Helper.optimize(table, q1);

		if (log.isDebugEnabled()) {
			log.debug("table=" + table + ",q=" + q1);
		}

		Stat s1 = Helper.primary.load(table, q1, Stat.class);

		long[] d = new long[n.length];
		for (int i = 0; i < d.length; i++) {
			d[i] = (s1 == null) ? 0 : (n[i] - s1.getLong("n" + i));
		}

		v.append("time", time);

		Stat.insertOrUpdate(name + "." + Stat.TYPE.snapshot, date, size, q, v, n);
		Stat.insertOrUpdate(name + "." + Stat.TYPE.delta, date, size, q, v, d);

	}

	public static Beans<Stat> load(String name, TYPE type, SIZE size, W q, int s, int n) {
		if (q == null) {
			q = W.create();
		}
		q.and("module", name + "." + type).and("size", size.toString());

		String table = table(name);

		Helper.optimize(table, q);

		try {
			Beans<Stat> l1 = Helper.primary.load(table, q, s, n, Stat.class);
			if (l1 != null && !l1.isEmpty()) {
				Set<String> dates = new HashSet<String>();
				for (int i = l1.size() - 1; i >= 0; i--) {
					Stat s1 = l1.get(i);
					String date = s1.getDate();
					if (dates.contains(date)) {
						l1.remove(i);
					} else {
						dates.add(date);
					}
				}
			}

			return l1;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return Beans.create();
	}

	public static Stat load(String name, TYPE type, SIZE size, W q) {
		if (q == null) {
			q = W.create();
		}
		q.and("module", name + "." + type).and("size", size.toString());

		return Helper.primary.load(table(name), q, Stat.class);
	}

	public static long max(String field, String name, TYPE type, SIZE size, W q) {
		if (q == null) {
			q = W.create();
		}
		q.and("module", name + "." + type.toString()).and("size", size.toString());

		Data d = Helper.primary.load(table(name), q.copy().sort(name, -1), Data.class);
		if (d != null) {
			return X.toLong(d.get(name));
		}
		return -1;
	}

	public static long sum(String field, String name, TYPE type, SIZE size, W q) {
		if (q == null) {
			q = W.create();
		}
		q.and("module", name + "." + type.toString()).and("size", size.toString());
		return X.toLong((Object) Helper.primary.sum(table(name), q, field));
	}

	public static long avg(String field, String name, TYPE type, SIZE size, W q) throws SQLException {
		if (q == null) {
			q = W.create();
		}
		q.and("module", name + "." + type.toString()).and("size", size.toString());
		return X.toLong((Object) Helper.primary.avg(table(name), q, field));
	}

	public static long min(String field, String name, TYPE type, SIZE size, W q) {
		if (q == null) {
			q = W.create();
		}
		q.and("module", name + "." + type.toString()).and("size", size.toString());
		return X.toLong((Object) Helper.primary.min(table(name), q, field));
	}

	/**
	 * the start time of today
	 * 
	 * @param time the long of the timestamp
	 * 
	 * @return the truncated timestamp
	 */
	public static long today(long time) {
		return lang.parse(lang.format(time, "yyyy-MM-dd"), "yyyy-MM-dd");
	}

	public static long today() {
		return today(Global.now());
	}

	public static long tohour() {
		return tohour(Global.now());
	}

	public static long tohour(long time) {
		return lang.parse(lang.format(time, "yyyyMMddHH"), "yyyyMMddHH");
	}

	public static long tomin() {
		return tomin(Global.now());
	}

	public static long tomin(long time) {
		return lang.parse(lang.format(time, "yyyyMMddHHmm"), "yyyyMMddHHmm");
	}

	/**
	 * the start time of this week
	 * 
	 * @return
	 */
	public static long toweek() {
		return toweek(Global.now());
	}

	public static long toweek(long time) {
		return lang.parse(lang.format(time, "yyyy-w"), "yyyy-w");
	}

	/**
	 * the start time of this month
	 * 
	 * @return
	 */
	public static long tomonth() {
		return tomonth(Global.now());
	}

	public static long tomonth(long time) {
		return lang.parse(lang.format(time, "yyyy-MM"), "yyyy-MM");
	}

	/**
	 * the start time of this season
	 * 
	 * @return
	 */
	public static long toseason() {
		return toseason(Global.now());
	}

	public static long toseason(long time) {
		int season = X.toInt(lang.format(time, "MM")) / 3;
		return lang.parse(lang.format(time, "yyyy") + "-" + (season * 3), "yyyy-MM");
	}

	/**
	 * the start time of this year
	 * 
	 * @return
	 */
	public static long toyear() {
		return toyear(Global.now());
	}

	public static long toyear(long time) {
		return lang.parse(lang.format(time, "yyyy"), "yyyy");
	}

	public static List<Stat> merge(String module, W q, String groupby, MergeFunc func) {
		// load from stat, and group

		String table = table(module);

		List<Stat> l1 = new ArrayList<Stat>();

		try {
			List<?> l2 = Helper.primary.distinct(table, groupby,
					q.copy().and(groupby, null, W.OP.neq).and(groupby, X.EMPTY, W.OP.neq));
			if (l2 != null) {
				for (Object o : l2) {
					try {
						Beans<Stat> bs = Helper.primary.load(table, q.copy().and(groupby, o), 0, 10000, Stat.class);
						if (!bs.isEmpty()) {
							Stat s = bs.get(0);

							for (String name : s.keySet()) {
								if (name.startsWith("n")) {
									Object o1 = s.get(name);
									if (o1 instanceof Long) {
										long v = func.call(name, bs);
										s.set(name, v);
									}
								}
							}

							l1.add(s);
						}
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				}
			}
		} catch (SQLException err) {
			log.error(err.getMessage(), err);
		}

		return l1;

	}

	@FunctionalInterface
	public static interface MergeFunc extends Serializable {
		public long call(String name, List<Stat> l1);
	}

	public static List<?> distinct(String module, String field, W q) throws SQLException {
		String table = table(module);
		return Helper.primary.distinct(table, field, q);
	}

	public static long tom15() {
		return tom15(Global.now());
	}

	public static long tom15(long ms) {
		long hour = tohour(ms);

		ms = ms - hour;
		if (ms > X.AMINUTE * 45) {
			hour += X.AMINUTE * 45;
		} else if (ms > X.AMINUTE * 30) {
			hour += X.AMINUTE * 30;
		} else if (ms > X.AMINUTE * 15) {
			hour += X.AMINUTE * 15;
		}
		return hour;
	}

	public static long tom30() {
		return tom30(Global.now());
	}

	public static long tom30(long time) {
		long hour = tohour(time);

		time = time - hour;
		if (time > X.AMINUTE * 30) {
			hour += X.AMINUTE * 30;
		}
		return hour;
	}

	public static long tom10() {
		return tom10(Global.now());
	}

	public static long tom10(long ms) {
		long hour = tohour(ms);

		ms = ms - hour;
		if (ms > X.AMINUTE * 50) {
			hour += X.AMINUTE * 50;
		} else if (ms > X.AMINUTE * 40) {
			hour += X.AMINUTE * 40;
		} else if (ms > X.AMINUTE * 30) {
			hour += X.AMINUTE * 30;
		} else if (ms > X.AMINUTE * 20) {
			hour += X.AMINUTE * 20;
		} else if (ms > X.AMINUTE * 10) {
			hour += X.AMINUTE * 10;
		}
		return hour;
	}

	public synchronized static int cleanup() {

		int n = 0;
		List<JSON> l1 = Helper.primary.listTables(null, 10000);
		for (JSON j1 : l1) {

			String name = j1.getString("name");
			if (name.startsWith("gi_stat_") && CleanupTask.inCleanupTime()) {

//				task.attach("table", name);

				if (log.isInfoEnabled()) {
					log.info("cleanup [" + name + "], detail=" + j1);
				}

				// delete duty data
				try {
					long n1 = Helper.primary.delete(name, W.create().and("time", Global.now(), W.OP.gt));
					if (n1 > 0) {
						GLog.applog.info("sys", "cleanup", "table=" + name + ", removed duty=" + n1);
						n += n1;
					}
				} catch (Throwable e) {
					log.error(e.getMessage(), e);
					GLog.applog.error("sys", "cleanup", "stat cleanup duty failed", e);
				}

				for (SIZE s1 : rules) {

					try {
						long n1 = cleanup(name, s1);
						if (n1 > 0) {
							GLog.applog.info("sys", "cleanup", "table=" + name + ", removed." + s1 + "=" + n1);
							n += n1;
						}

					} catch (Throwable e) {
						log.error(e.getMessage(), e);
						GLog.applog.error("sys", "cleanup", "stat cleanup failed", e);
					}
				}
			}
		}
		return n;
	}

	private static SIZE[] rules = new SIZE[] { SIZE.year, SIZE.season, SIZE.month, SIZE.week, SIZE.day, SIZE.hour,
			SIZE.m30, SIZE.m15, SIZE.m10, SIZE.min };

}
