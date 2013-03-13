import common.ICalculator;
import common.SlowCalculator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple example of a server that performs a simple
 * web service. This server accepts
 * connections on port 2222, reads an integer, and
 * writes out the successor of that integer.
 * <p>
 * Try this with FirstSwingExampleRemote
 */
public class CalculationServer
{
  private static int nextId;    
  private ICalculator calc;
 
  /**
   * @param args
   */
  public static void main(String[] args)
  {
    new CalculationServer().runServer(2222);
  }

  public CalculationServer()
  {
    calc = new SlowCalculator();
  }
  
  private static int getNextId()
  {
    synchronized(CalculationServer.class)
    {
      return nextId++;
    }
  }
  
    
  
  /**
   * Basic server loop. 
   * @param port
   *   the port number on which to listen
   */
  public void runServer(int port)
  {
    
    ServerSocket ss = null;
    try
    {
      ss = new ServerSocket(port);
      while (true)
      {
        System.out.println("Server listening on " + port);
        
        // blocks here until a client attempts to connect
        Socket s = ss.accept();
        new Thread(new MessageReader(s)).start();
      }      
    }
    catch (IOException e)
    {
      System.out.println("I/O error: " + e);
    }
    finally
    {
      if (ss != null)
      {
        try
        {
          ss.close();
        }
        catch (IOException e) 
        {
          // error trying to close the socket, not much we can do
          System.out.println("Error closing socket " + e);
        }
      }
    }
       
  }
  
  /**
   * Task will set up a dedicated connection to client
   * for two-way messaging.
   */
  private class MessageReader implements Runnable
  {
    private Socket s;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    
    // our own thread pool for handling messages
    private ExecutorService pool = Executors.newFixedThreadPool(2);
    
    public MessageReader(Socket s)
    {
      this.s = s;
    }
    /**
     * Helper method for handling a client connection.
     */
    public void run()
    {
      try
      {
        ois = new ObjectInputStream(s.getInputStream());
        oos = new ObjectOutputStream(s.getOutputStream());
        
        while (true)
        {
          try
          {
            final Message m = (Message) ois.readObject();
            Runnable r = new Runnable()
            {
              public void run()
              {
                handleRequest(m);
              }
            };
            pool.execute(r);
          }
          catch (ClassNotFoundException e)
          {
            System.out.println("Class not found: " + e);
          }
        } 
      }
      catch (IOException e)
      {
        System.out.println("Exception in handler thread: " + e);
      }
      finally
      {
        // close the connection and shut down the
        // thread pool
        pool.shutdown();
        
        try
        {
          s.close();
        }
        catch (IOException ignore){}
      }
    }

    private void handleRequest(Message m)
    {
      int id = m.getId();
      String payload = m.getPayload();
      String[] args = payload.split("\\s+");
      String result;
      if (args[0].equals("increment"))
      {
        result = processIncrementRequest(args);
      }
      else
      {
        result = "Undefined operation";
      }

      Message reply = new Message();
      reply.setId(getNextId());
      reply.setCorrelationId(id);
      reply.setPayload(result);

      try
      {
        synchronized(this)
        {
          oos.writeObject(reply);
        }
      }
      catch (IOException e)
      {
        System.out.println("Exception in handler thread: " + e);
      }
    }
    
    private String processIncrementRequest(String[] args)
    {
      try
      {
        int value = Integer.parseInt(args[1]);
        int answer = calc.increment(value);
        return "" + answer;
      }
      catch (Exception e)
      {
        return e.toString();
      }
    }
  }
  


}
