/**
 * 
 */
package com.acertainbookstore.client.workloads;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 * 
 * CertainWorkload class runs the workloads by different workers concurrently.
 * It configures the environment for the workers using WorkloadConfiguration
 * objects and reports the metrics
 * 
 */
public class CertainWorkload {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		int numConcurrentWorkloadThreads = 30;
		String serverAddress = "http://localhost:8081";
		boolean localTest = false;
		List<WorkerRunResult> workerRunResults = new ArrayList<WorkerRunResult>();
		List<Future<WorkerRunResult>> runResults = new ArrayList<Future<WorkerRunResult>>();

		// Initialize the RPC interfaces if its not a localTest, the variable is
		// overriden if the property is set
		String localTestProperty = System
				.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
		localTest = (localTestProperty != null) ? Boolean
				.parseBoolean(localTestProperty) : localTest;

		BookStore bookStore = null;
		StockManager stockManager = null;
		if (localTest) {
			CertainBookStore store = new CertainBookStore();
			bookStore = store;
			stockManager = store;
		} else {
			stockManager = new StockManagerHTTPProxy(serverAddress + "/stock");
			bookStore = new BookStoreHTTPProxy(serverAddress);
		}

		// Generate data in the bookstore before running the workload
		initializeBookStoreData(bookStore, stockManager);

		ExecutorService exec = Executors
				.newFixedThreadPool(numConcurrentWorkloadThreads);

		for (int i = 0; i < numConcurrentWorkloadThreads; i++) {
			WorkloadConfiguration config = new WorkloadConfiguration(bookStore,
					stockManager);
			Worker workerTask = new Worker(config);
			// Keep the futures to wait for the result from the thread
			runResults.add(exec.submit(workerTask));
		}

		// Get the results from the threads using the futures returned
		for (Future<WorkerRunResult> futureRunResult : runResults) {
			WorkerRunResult runResult = futureRunResult.get(); // blocking call
			workerRunResults.add(runResult);
		}

		exec.shutdownNow(); // shutdown the executor

		// Finished initialization, stop the clients if not localTest
		if (!localTest) {
			((BookStoreHTTPProxy) bookStore).stop();
			((StockManagerHTTPProxy) stockManager).stop();
		}

		reportMetric(workerRunResults);
	}

	/**
	 * Computes the metrics and prints them
	 * 
	 * @param workerRunResults
	 */
	public static void reportMetric(List<WorkerRunResult> workerRunResults) {
        final long RESOLUTION = TimeUnit.MILLISECONDS.toNanos(1);

        int totalCustomerInteraction = 0;
        int unsuccessfulRuns = 0;
        int totalRuns = 0;

        List<Double> allThroughput = new ArrayList<Double>();
        List<Double> allLatency = new ArrayList<Double>();

        for (WorkerRunResult result : workerRunResults) {
            totalCustomerInteraction += result.getTotalFrequentBookStoreInteractionRuns();
            totalRuns += result.getTotalRuns();
            unsuccessfulRuns += (result.getTotalRuns() - result.getSuccessfulInteractions());

            System.out.println(String.format("Successful requests: %d", result.getSuccessfulInteractions()));
            System.out.println(String.format("Elapsed Time in Nano: %d", result.getElapsedTimeInNanoSecs()));

            double latency = (result.getElapsedTimeInNanoSecs() / (double) RESOLUTION) / result.getTotalRuns();
            System.out.println("Avg. Latency: " + latency);
            allLatency.add(latency);

            double throughput = result.getSuccessfulFrequentBookStoreInteractionRuns() /
                    (result.getElapsedTimeInNanoSecs() / (double) RESOLUTION);
            System.out.println("Throughput pr. 10 millis: " + throughput);
            allThroughput.add(throughput);
        }

        System.out.println("Throughput: " + allThroughput);

		double throughputSum = 0;
		for (double i : allThroughput){
			throughputSum += i;
		}

		System.out.println("Avg. Throughput: " + throughputSum / allThroughput.size());
        System.out.println("Latency: " + allLatency);

		double latencySum = 0;
		for (double i : allLatency){
			latencySum += i;
		}

		System.out.println("Avg. Latency: " + latencySum / allLatency.size());

        System.out.println(String.format("Total requests: %d", totalRuns));
        System.out.println(String.format("Percentage unsuccessful requests: %.2f",
                unsuccessfulRuns / (float) totalRuns * 100));
        System.out.println(String.format("Percentage Customer Interaction requests: %.2f",
                totalCustomerInteraction / (float) totalRuns * 100));
    }

	/**
	 * Generate the data in bookstore before the workload interactions are run
	 * 
	 * Ignores the serverAddress if its a localTest
	 * 
	 */
	public static void initializeBookStoreData(BookStore bookStore,
			StockManager stockManager) throws Exception {
        BookSetGenerator generator = new BookSetGenerator();
        stockManager.removeAllBooks();
        stockManager.addBooks(generator.nextSetOfStockBooks(100));
	}
}
