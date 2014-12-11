package com.acertainbookstore.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.acertainbookstore.interfaces.Replicator;

/**
 * CertainBookStoreReplicator is used to replicate updates to slaves
 * concurrently.
 */
public class CertainBookStoreReplicator implements Replicator {

    private int maxReplicatorThreads;

	public CertainBookStoreReplicator(int maxReplicatorThreads) {
		this.maxReplicatorThreads = maxReplicatorThreads;
	}

	public List<Future<ReplicationResult>> replicate(Set<String> slaveServers,
			ReplicationRequest request) {
        ExecutorService service = Executors.newFixedThreadPool(maxReplicatorThreads);
        List<Future<ReplicationResult>> requests = new ArrayList<Future<ReplicationResult>>();
        for (String slaveServer : slaveServers)
            requests.add(service.submit(new CertainBookStoreReplicationTask(slaveServer, request)));

		return requests;
	}
}
