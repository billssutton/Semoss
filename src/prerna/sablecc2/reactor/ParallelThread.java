package prerna.sablecc2.reactor;

public class ParallelThread implements Runnable {

	IParallelWorker worker = null;
	
	@Override
	public void run() {
		worker.run();
	}

}
