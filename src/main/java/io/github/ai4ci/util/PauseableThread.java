package io.github.ai4ci.util;

/**
 * A pauseable daemon thread which will can be required to go into a waiting
 * state after each execution of the loop. This is basically a thread which
 * executes a while loop, with pause, resume and halt functions.
 */
public abstract class PauseableThread extends Thread {
	
	volatile boolean halt = false;
	volatile boolean pause = false;
	
	volatile boolean waiting = false;
	volatile Object trigger = new Object();
	
//	private Thread shutdownHook;
	
	protected PauseableThread() {
		this(null,-1);
	}
	
	protected PauseableThread(String name, int priority) {
		super();
		this.setDaemon(true);
		if (priority > 0) this.setPriority(priority);
		if (name != null) this.setName(name);
//		shutdownHook = new Thread() {
//			public void run() {
//				PauseableThread.this.halt();
//			}
//		};
//		Runtime.getRuntime().addShutdownHook(shutdownHook);
	}
	
	public boolean isWaiting() {
		return waiting;
	}
	
	public void halt() {
		halt = true;
		unpause();
	}
	
	/**
	 * Threads can pause themselves but will never unpause (unless halting). If 
	 * the thread is complete this does nothing.
	 */
	public void pause() {
		if (!isComplete()) pause = true;
	}
	
	/**
	 * Non blocking unpause a thread. Nothing happens if the thread is not 
	 * paused. Otherwise the thread will be woken up and resume executing 
	 * doLoop() repeatedly, until isComplete() is true, or told to halt.
	 */
	public void unpause() {
		this.pause = false;
		synchronized(trigger) {this.trigger.notifyAll();}
	}
	
	public void run() {
		setup();
		while(!halt && !isComplete()) {
			if (!pause) doLoop();
			while (pause && !isComplete() && !halt) {
				this.waiting = true;
				synchronized(trigger) {try {
					trigger.wait();
				} catch (InterruptedException e) {
					this.halt = true;
				}}
				this.waiting = false;
			}
		}
//		Runtime.getRuntime().removeShutdownHook(shutdownHook);
//		shutdownHook = null;
		synchronized(trigger) {trigger.notifyAll();}
		shutdown(isComplete());
		return;
	}
	
	public String status() {
		if (isComplete()) return "complete";
		if (halt && this.getState().equals(State.TERMINATED)) return "halted";
		if (halt) return "halting";
		if (pause && !waiting) return "pausing";
		if (waiting && !pause) return "resuming";
		if (waiting && pause) return "paused";
		return "running";
	}
	
	/**
	 * Run on thread once at the start. Can setup thread local resources. 
	 * Constructor can be used to set up shared resources.
	 */
	public abstract void setup();
	/**
	 * Repeatedly called while the thread is not paused or halting. This can 
	 * interact with resources shared with other threads but needs to be 
	 * synchronised on them (or use non blocking). This operates equivalent to a 
	 * `while (!isComplete()) {doLoop();}`
	 */
	public abstract void doLoop();
	/**
	 * Signify the thread loop is complete and proceed to shutdown. This is
	 * checked even if the thread is paused.
	 */
	public abstract boolean isComplete();
	/**
	 * Shutdown the thread. This must close down any resources and exit without
	 * error.
	 * @param completedNormally did the loop complete or was the thread halted?
	 */
	public abstract void shutdown(boolean completedNormally);
}
