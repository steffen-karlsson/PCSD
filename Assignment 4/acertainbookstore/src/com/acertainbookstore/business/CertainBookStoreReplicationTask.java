package com.acertainbookstore.business;

import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreMessageTag;
import com.acertainbookstore.utils.BookStoreUtility;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;

import java.util.concurrent.Callable;

/**
 * CertainBookStoreReplicationTask performs replication to a slave server. It
 * returns the result of the replication on completion using ReplicationResult
 */
public class CertainBookStoreReplicationTask implements
		Callable<ReplicationResult> {

    private ReplicationRequest request;
    private String server;

	public CertainBookStoreReplicationTask(String server, ReplicationRequest request) {
        this.server = server;
		this.request = request;
	}

	@Override
	public ReplicationResult call() throws Exception {
        System.out.println("FOOBAR");
        ContentExchange exchange = new ContentExchange();
        String urlString = server + BookStoreMessageTag.REPLICATEREQUEST;
        System.out.println("URL: " + urlString);
        exchange.setMethod("POST");
        exchange.setURL(urlString);

        String requestXml = BookStoreUtility.serializeObjectToXMLString(request);
        Buffer requestContent = new ByteArrayBuffer(requestXml);
        exchange.setRequestContent(requestContent);

        try {
            HttpClient client = new HttpClient();
            client.start();
            BookStoreUtility.SendAndRecv(client, exchange);
            System.out.println("FOOBAR1");
        } catch (BookStoreException ignore) {
            System.out.println("FOOBAR2");
            return new ReplicationResult(server, false);
        } finally {
            System.out.println("FOOBAR3");
        }
        return new ReplicationResult(server, true);
    }

}
