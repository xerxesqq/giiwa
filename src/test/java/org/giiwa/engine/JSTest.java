package org.giiwa.engine;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.task.Task;
import org.giiwa.web.Controller;
import org.junit.Test;

public class JSTest {

	@Test
	public void test() {

		try {
			TimeStamp t = TimeStamp.create();
			String code = "return x";
			JSON p = JSON.create();
			for (int i = 0; i < 1000; i++) {
				p.put("x", i);
				Object o = JS.run(code, p);
				System.out.println(o + ", cost=" + t.reset() + ", i=" + i);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void testThis() {

		Task.init(10);

		String s = "return that.a";
		try {
			Map<String, Object> p1 = new HashMap<String, Object>();
			p1.put("that", JSON.create().append("a", 10));

			System.out.println(JS.run(s, p1));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());

		}
	}

	@Test
	public void testScales() {

		_init();

		Task[] tt = new Task[10];
		for (int i = 0; i < tt.length; i++) {

			tt[i] = new Task() {

				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;
				int n = 1000;

				@Override
				public void onFinish() {
					if (n > 0) {
						this.schedule(0);
					}
				}

				@Override
				public void onExecute() {

					n--;

					TimeStamp t = TimeStamp.create();
					String code = "return (b + c) + '=' + b + ' + ' + c";
					JSON j1 = JSON.create();
					j1.append("b", n);
					j1.append("c", 2 * n);
					j1.append("a", 3);

					try {
						Object r = JS.run(code, j1);
//						System.out.println(r + ", cost=" + t.past() + ", n=" + n);
//						Object r = j1.getLong("b") + j1.getLong("c");
//						Object r = Velocity.parse("#set($a=$b+$c) $a", j1);
						System.out.println(r + ", cost=" + t.past() + ", n=" + n);

					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

			};
			tt[i].schedule(0);
		}

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testBindings1() {

		_init();

		int n = 10;

		TimeStamp t = TimeStamp.create();
//		String code = "function aaa() {return b.aaa + c.substring(1) + " + n + ";};_d1.ret=aaa();";
		String code = "return b.aaa + c.substring(1)";
//		String code = "return 1 + 2";
//		JSON d1 = JSON.create();
		JSON j1 = JSON.create();
		j1.append("b", JSON.create().append("aaa", 2222));
		j1.append("c", "abcdef");
		j1.append("a", X.inst);
//		j1.append("_d1", d1);

		try {
			Object r = JS.run(code, j1);
//			JS.run(code, j1);
//			Object r = d1.get("ret");
			System.out.println(r + ", cost=" + t.past() + ", n=" + n);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void _init() {

		Task.init(100);

//		Properties prop = new Properties();
//		prop.put("log4j.rootLogger", "WARN,Log1");
//		prop.put("log4j.appender.Log1", "org.apache.log4j.ConsoleAppender");
//		prop.put("log4j.appender.Log1.layout", "org.apache.log4j.PatternLayout");
//		prop.put("log4j.appender.G.layout.ConversionPattern", "%p [%t] [%d] - %m - [%l]%n");
//		prop.put("log4j.logger.org.giiwa", "debug");
//
//		PropertyConfigurator.configure(prop);

		Controller.GIIWA_HOME = "/Users/joe/d/";

	}

}
