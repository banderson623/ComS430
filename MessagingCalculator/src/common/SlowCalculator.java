package common;


public class SlowCalculator implements ICalculator
{
  @Override
  public int increment(int n) throws RemoteException
  {
    lookBusy(2000);
//    if (n == 1) 
//    {
//      System.out.println("Throwing exception"); 
//      throw new RemoteException("I am throwing this for no reason!");
//    }
    return n + 1;
  }
  
  private static void lookBusy(long millis)
  {
    long interval = 300;
    long stop = System.currentTimeMillis() + millis;
    
    try
    {
      while(!Thread.currentThread().isInterrupted() && 
             System.currentTimeMillis() < stop)
      {
        System.out.print(".");
        Thread.sleep(interval);      
      }
    }
    catch (InterruptedException ie)
    {}
  }

}
