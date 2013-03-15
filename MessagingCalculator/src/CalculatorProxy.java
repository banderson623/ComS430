import async.IAsyncCalculator;
import async.ICallback;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Remote proxy implementing the IAsyncCalculator interface
 * using a persistent socket connection to send and receive
 * messages.  The main weakness of this implementation
 * is that if the connection is ever lost, the client
 * will have to create another CalculatorProxy instance.
 */
public class CalculatorProxy implements IAsyncCalculator
{
  private static AtomicInteger nextId = new AtomicInteger(0);
  private ObjectOutputStream oos;
  
  Map<Integer, ICallback> callbackCache = new HashMap<Integer, ICallback>();
  
  /**
   * Constructs
   * @param host
   * @param port
   * @throws java.io.IOException
   */
  public CalculatorProxy(String host, int port) throws IOException
  {
    Socket s = new Socket(host, port);
    oos = new ObjectOutputStream(s.getOutputStream());
    MessageReader reader = new MessageReader(s.getInputStream());
    new Thread(reader).start();
  }

  private static int getNextId()
  {
    return nextId.getAndIncrement();
  }
  
  @Override
  public synchronized void incrementAsync(int i, ICallback callback)
  {
    int id = getNextId();
    String request = "increment " + i; 
    Message message = new Message();
    message.setId(id);
    message.setPayload(request);
    try
    {
      oos.writeObject(message);
      callbackCache.put(id, callback);
    }
    catch (Exception e)
    {
      // Could be IOException or something else; 
      // just tell the client about it
      callback.asyncException(e);
    }
  }

  @Override
  public Future<Integer> increment(final int n)
  {
      ResponseCallBackHandler handlerAndFuture = new ResponseCallBackHandler();
      incrementAsync(n, handlerAndFuture);

      return handlerAndFuture;

  }
  
  /**
   * Continuously reads and dispatches incoming messages.
   */
  private class MessageReader implements Runnable
  {
    private InputStream is;
    public MessageReader(InputStream is)
    {
      this.is = is;
    }
    
    public void run()
    {
      try
      {
        ObjectInputStream ois = new ObjectInputStream(is);
        while (true)
        {
          final Message m = (Message) ois.readObject();
          CalculatorProxy.this.dispatch(m);
        } 
      }
      catch (ClassNotFoundException e)
      {
        // Should never happen, but log error...
        System.out.println("Class not found: " + e);
      }
      catch (IOException ioe)
      {
        // connection lost, just exit
      }
    }
  }
  
  /**
   * Handle an incoming message.
   * @param m
   */
  private void dispatch(Message m)
  {
    int correlationId = m.getCorrelationId();
    String result = m.getPayload();
    
    // See if message is associated with a callback or a future
    ICallback cb = callbackCache.remove(correlationId);
    if (cb != null)
    {
      try
      {
        int r = Integer.parseInt(result);
        cb.asyncResult(r);
      }
      catch (NumberFormatException e)
      {
        // if the result isn't an integer, it's some
        // kind of error
        cb.asyncException(new Exception(result));
      }
    }
    else
    {
      // TODO   
    }
  }



    private class ResponseCallBackHandler implements ICallback, Future<Integer> {
        private int valueReturned = Integer.MIN_VALUE;
        private Exception exceptionReturned;

        @Override
        public synchronized void asyncResult(int result)
        {
            valueReturned = result;
        }

        @Override
        public synchronized void asyncException(Exception e)
        {
            exceptionReturned = e;
        }

        @Override /* this does nothing */
        public synchronized boolean cancel(boolean b)
        {
            exceptionReturned = new InterruptedException("You canceled it.");
            return true;
        }

        @Override
        public boolean isCancelled()
        {
            return false;
        }

        @Override
        public synchronized boolean isDone()
        {
            return (this.valueReturned != Integer.MIN_VALUE ||
                    this.exceptionReturned != null);
        }

        @Override
        public Integer get() throws InterruptedException, ExecutionException
        {
            // block until this is returned
            while(!isDone()){
                Thread.sleep(1000); // be reasonable
            }
            if(this.exceptionReturned != null)
            {
                // There has got to be a better way to do this.
                if(exceptionReturned.getClass() == (new InterruptedException()).getClass())
                {
                    throw (InterruptedException) exceptionReturned;
                }
                else
                {
                    throw (ExecutionException) exceptionReturned;
                }
            }

            return valueReturned;
        }

        @Override
        public Integer get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }

}


  