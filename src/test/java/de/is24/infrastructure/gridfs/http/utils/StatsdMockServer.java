package de.is24.infrastructure.gridfs.http.utils;

import static de.is24.infrastructure.gridfs.http.utils.retry.RetryUtils.execute;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.flapdoodle.embed.process.runtime.Network;
import de.is24.infrastructure.gridfs.http.utils.retry.RetryUtils;


/**
 * Mock for a statsd server using non-blocking IO according to http://www.onjava.com/pub/a/onjava/2002/09/04/nio.html?page=2
 */
public class StatsdMockServer extends ExternalResource implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(StatsdMockServer.class);

  private ServerSocketChannel server;
  private int port;
  private Selector selector;
  private Thread thread;

  @Override
  public void before() throws Throwable {
    server = ServerSocketChannel.open();
    server.configureBlocking(false);
    execute().maxTries(3).wait(2).command(new RetryUtils.Retryable<Void>() {
        public Void run() throws Throwable {
          port = Network.getFreeServerPort();
          LOG.info("StatsdMockServer starting : port={}", port);
          server.socket().bind(new InetSocketAddress(port));
          return null;
        }
      });
    selector = Selector.open();
    server.register(selector, SelectionKey.OP_ACCEPT);

    thread = new Thread(this);
    thread.start();
  }

  @Override
  public void after() {
    try {
      thread.interrupt();
    } catch (Exception e) {
    }
    try {
      selector.close();
    } catch (IOException e) {
    }
    try {
      server.close();
    } catch (Exception e) {
    }
  }

  public int getPort() {
    return port;
  }

  @Override
  public void run() {
    try {
      while (!thread.isInterrupted()) {
        selector.select();

        Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
        while (iterator.hasNext()) {
          SelectionKey key = iterator.next();
          iterator.remove();

          if (!key.isValid()) {
            continue;
          }

          if (key.isAcceptable()) {
            SocketChannel client = server.accept();
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ);
            continue;
          }

          if (key.isReadable()) {
            SocketChannel client = (SocketChannel) key.channel();
            int BUFFER_SIZE = 32;
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            try {
              client.read(buffer);
            } catch (Exception e) {
              e.printStackTrace();
            }

            continue;
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }


  }
}
