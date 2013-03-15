import async.IAsyncCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class TestProxy
{
    public static void main(String[] args) throws Exception
    {
        List<Future<Integer>> theFutureIsHere = new ArrayList<Future<Integer>>();

        IAsyncCalculator calc = new CalculatorProxy("localhost", 2222);
        theFutureIsHere.add(calc.increment(42));
        theFutureIsHere.add(calc.increment(33));
        theFutureIsHere.add(calc.increment(1));
        //getting wild.
        theFutureIsHere.get(1).cancel(true);

        System.out.println("Calling get() on the future");
        System.out.println("This should block until proxy gets message from server...");
        for(Future<Integer> theTimeIsNow : theFutureIsHere){
            System.out.println("Processing the future...");
            try
            {
                Integer myInt = theTimeIsNow.get();
                System.out.println(myInt);
            }
            catch (Exception e)
            {
                System.out.println("Error: " + e.getMessage());
            } finally {
                System.out.println("Done!");
            }
        }
    }
}
