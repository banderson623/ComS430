import async.IAsyncCalculator;
import async.ICallback;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
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
  public void incrementAsync(int i, ICallback callback)
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
  public Future<Integer> increment(int n)
  {
    // TODO
    return null;
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

}


  