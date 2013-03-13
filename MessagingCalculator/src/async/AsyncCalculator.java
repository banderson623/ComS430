package async;

import common.RemoteException;
import common.SlowCalculator;

import java.util.concurrent.*;


/**
 * Sample implementation of an asynchronous interface
 * that either returns a Future or uses a callback object.
 */
public class AsyncCalculator implements IAsyncCalculator
{
  private ExecutorService executor = Executors.newFixedThreadPool(1);
  
  @Override
  public Future<Integer> increment(final int i)
  {
    // Create a Callable that returns an integer.  If
    // an exception occurs executing the task, the caller
    // will get it wrapped in an ExecutionException
    // when claiming the future.
    Callable<Integer> c = new Callable<Integer>()
    {
      public Integer call() throws RemoteException
      {
        SlowCalculator calc = new SlowCalculator();
        return calc.increment(i);
      }
    };
    
    // We'll return a Future to the caller, and let
    // the executor perform the task
    FutureTask<Integer> f = new FutureTask<Integer>(c);
    executor.submit(f);
    return f;
    
    // Shorthand for the statements above...
    //return executor.submit(c);
  }

  @Override
  public void incrementAsync(final int i, final ICallback callback)
  {
    // No return value or exception needed, so we can just
    // use an ordinary Runnable
    Runnable r = new Runnable()
    {
      public void run()
      {
        SlowCalculator calc = new SlowCalculator();
        try
        {
          int ret = calc.increment(i);
          callback.asyncResult(ret);
        }
        catch (Exception e)
        {
          callback.asyncException(e);
        }
      }
    };
    
    executor.submit(r);
  }

}
