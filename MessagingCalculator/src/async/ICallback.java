package async;
/**
 * General interface for reporting the result of 
 * an asynchronous operation.
 */
public interface ICallback
{
  /**
   * Method to be invoked when the result of the
   * operation is available.  This method executes in the 
   * context of the execution thread for the operation.
   * @param result
   *   result of the operation
   */
  void asyncResult(int result);
  
  /**
   * Method to be invoked if the execution of the operation
   * results in an exception.  This method executes in the 
   * context of the execution thread for the operation.
   * @param e
   *   exception that occurred
   */
  void asyncException(Exception e);
}
