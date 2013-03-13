package async;
import java.util.concurrent.Future;

/**
 * A simple asynchronous interface for the calculator.
 * Callers can either receive a future or can provide
 * a callback object.
 */
public interface IAsyncCalculator
{
  /**
   * Returns a Future for the result of incrementing
   * the parameter i.
   * @param i
   *   the number to be incremented
   * @return
   *   a Future to hold the eventual return value
   */
  public Future<Integer> increment(int i);
  
  /**
   * Initiates the increment operation for the parameter
   * i and invokes the appropriate callback operation 
   * when complete.
   * @param i
   *   the number to be incremented
   * @param callback
   *   the callback to be invoked when the task is complete
   */
  public void incrementAsync(int i, ICallback callback);
}
