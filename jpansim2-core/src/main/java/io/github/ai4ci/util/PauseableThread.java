package io.github.ai4ci.util;

/**
 * A pauseable daemon thread which will can be required to go into a waiting
 * state after each execution of the loop. This is basically a thread which
 * executes a while loop, with pause, resume and halt functions.
 *
 * The thread will repeatedly call doLoop() until isComplete() is true, or the
 * thread is halted. If the thread is paused, it will wait until unpaused or
 * halted. Once complete or halted, it will call shutdown() and exit.
 *
 *
 */
public abstract class PauseableThread extends Thread {

	volatile boolean halt = false;
	volatile boolean pause = false;

	volatile boolean waiting = false;
	volatile Object trigger = new Object();

//	private Thread shutdownHook;

	/**
	 * Constructor. This creates a daemon thread with the given name and
	 * priority. If the name is null, the default thread name is used. If the
	 * priority is less than or equal to 0, the default thread priority is used.
	 */
	protected PauseableThread() {
		this(null, -1);
	}

	/**
	 * Constructor. This creates a daemon thread with the given name and
	 * priority. If the name is null, the default thread name is used. If the
	 * priority is less than or equal to 0, the default thread priority is used.
	 *
	 * @param name     the name of the thread, or null to use the default thread
	 *                 name
	 * @param priority the priority of the thread, or a value less than or equal
	 *                 to 0 to use the default thread priority
	 */
	protected PauseableThread(String name, int priority) {
		super();
		this.setDaemon(true);
		if (priority > 0) { this.setPriority(priority); }
		if (name != null) {
			this.setName(name);
//		shutdownHook = new Thread() {
//			public void run() {
//				PauseableThread.this.halt();
//			}
//		};
//		Runtime.getRuntime().addShutdownHook(shutdownHook);
		}
	}

	/**
	 * Repeatedly called while the thread is not paused or halting. This can
	 * interact with resources shared with other threads but needs to be
	 * synchronised on them (or use non blocking). This operates equivalent to a
	 * `while (!isComplete()) {doLoop();}`
	 */
	public abstract void doLoop();

	/**
	 * Threads can be halted by calling this method. This will cause the thread
	 * to exit its loop and proceed to shutdown. If the thread is currently
	 * paused, it will be unpaused to allow it to exit. If the thread is already
	 * complete, this does nothing.
	 */
	public void halt() {
		this.halt = true;
		this.unpause();
	}

	/**
	 * Signify the thread loop is complete and can proceed to shutdown. This is
	 * checked even if the thread is paused.
	 *
	 * @return true if the thread loop is complete and the thread should proceed
	 *         to shutdown, false otherwise
	 */
	public abstract boolean isComplete();

	/**
	 * Check if the thread is currently waiting in a paused state. This can be
	 * used to check if the thread is paused, but can also be true if the thread
	 * is in the process of pausing (i.e. it has executed pause() but has not yet
	 * entered the waiting state). Note that this does not necessarily mean the
	 * thread is paused, as it could be in the process of resuming (i.e. it has
	 * been unpaused but has not yet exited the waiting state).
	 *
	 * @return true if the thread is currently waiting in a paused state, false
	 *         otherwise. Note that this can be true even if the thread is not
	 *         paused, if it is in the process of pausing.
	 */
	public boolean isWaiting() { return this.waiting; }

	/**
	 * Threads can pause themselves but will never unpause (unless halting). If
	 * the thread is complete this does nothing.
	 */
	public void pause() {
		if (!this.isComplete()) { this.pause = true; }
	}

	/**
	 * Run the thread. This will repeatedly call doLoop() until isComplete() is
	 * true, or the thread is halted. If the thread is paused, it will wait until
	 * unpaused or halted. Once complete or halted, it will call shutdown() and
	 * exit.
	 */
	@Override
	public void run() {
		this.setup();
		while (!this.halt && !this.isComplete()) {
			if (!this.pause) { this.doLoop(); }
			while (this.pause && !this.isComplete() && !this.halt) {
				this.waiting = true;
				synchronized (this.trigger) {
					try {
						this.trigger.wait();
					} catch (InterruptedException e) {
						this.halt = true;
					}
				}
				this.waiting = false;
			}
		}
//		Runtime.getRuntime().removeShutdownHook(shutdownHook);
//		shutdownHook = null;
		synchronized (this.trigger) {
			this.trigger.notifyAll();
		}
		this.shutdown(this.isComplete());
		return;
	}

	/**
	 * Run on thread once at the start. Can setup thread local resources.
	 * Constructor can be used to set up shared resources.
	 */
	public abstract void setup();

	/**
	 * Shutdown the thread. This must close down any resources and exit without
	 * error.
	 *
	 * @param completedNormally did the loop complete or was the thread halted?
	 */
	public abstract void shutdown(boolean completedNormally);

	/**
	 * Get the approximate current status of the thread. This can be "running",
	 * "pausing", "paused", "resuming", "halting", "halted" or "complete". Note
	 * that the thread can be in the process of pausing or resuming, so the
	 * status may not always reflect the current state of the thread.
	 *
	 * @return the current status of the thread
	 */
	public String status() {
		if (this.isComplete()) { return "complete"; }
		if (this.halt && this.getState().equals(State.TERMINATED)) {
			return "halted";
		}
		if (this.halt) { return "halting"; }
		if (this.pause && !this.waiting) { return "pausing"; }
		if (this.waiting && !this.pause) { return "resuming"; }
		if (this.waiting && this.pause) { return "paused"; }
		return "running";
	}

	/**
	 * Non blocking unpause a thread. Nothing happens if the thread is not
	 * paused. Otherwise the thread will be woken up and resume executing
	 * doLoop() repeatedly, until isComplete() is true, or told to halt.
	 */
	public void unpause() {
		this.pause = false;
		synchronized (this.trigger) {
			this.trigger.notifyAll();
		}
	}
}
