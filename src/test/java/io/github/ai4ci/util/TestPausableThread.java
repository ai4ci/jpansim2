package io.github.ai4ci.util;
import org.junit.jupiter.api.Test;

class TestPausableThread {

	
	static void doSleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static class TestClass extends PauseableThread {
		
		long startTime;
		int i=0;
		
		public TestClass() {
			super("test",1);
			startTime = System.currentTimeMillis();
		}

		@Override
		public void setup() {
			System.out.println("setup");
		}

		@Override
		public void doLoop() {
			i++;
			System.out.println("tick: "+i+": "+(System.currentTimeMillis()-startTime));
			doSleep(200);
		}

		@Override
		public boolean isComplete() {
			return System.currentTimeMillis() - startTime > 2000;
		}

		@Override
		public void shutdown(boolean completedNormally) {
			System.out.println(completedNormally ? "OK" : "error");
		}
		
	}
	
	@Test
	void testBasic() throws InterruptedException {
		TestClass test = new TestClass();
		test.start();
		System.out.println(test.status());
		doSleep(500);
		test.pause();
		System.out.println(test.status());
		doSleep(1000);
		test.unpause();
		System.out.println(test.status());
		test.join();
		System.out.println(test.status());
	}
	
	@Test
	void testHaltPaused() throws InterruptedException {
		TestClass test = new TestClass();
		test.start();
		System.out.println(test.status());
		doSleep(500);
		test.pause();
		System.out.println(test.status());
		doSleep(1000);
		test.halt();
		System.out.println(test.status());
		test.join();
		System.out.println(test.status());
	}
}
