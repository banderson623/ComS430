package common;

public class RemoteException extends Exception
{
  public RemoteException()
  {
    super();
  }
  
  public RemoteException(String message)
  {
    super(message);
  }

  public RemoteException(Exception cause)
  {
    super(cause);
  }
  
  public RemoteException(String message, Exception cause)
  {
    super(message, cause);
  }

}
