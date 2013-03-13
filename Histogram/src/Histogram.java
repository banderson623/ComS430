import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;

/**
 * Creates a histogram of ASCII character counts for text files.
 */
public class Histogram
{
    /**
     * The histogram.
     */
    private int[] results = new int[128];

    public static void main(String[] args)
    {

        System.out.println("Generating some data...");

        // YMMV, adjust this number as you see fit
//        char[] text = generate(1000000000);
        char[] text = generate(10000000);

        Histogram h = new Histogram();

        System.gc();
        System.out.println("Starting...");
        long start = System.currentTimeMillis();
        h.process(text);
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("time: " + elapsed);

        System.out.println("-------------------------------------------");
        h.reset();
        System.gc();
        System.out.println("Starting...");
        start = System.currentTimeMillis();
        h.process(text);
        elapsed = System.currentTimeMillis() - start;
        System.out.println("time: " + elapsed);

        //h.display();
    }

    /**
     * Creates a bunch of fake data.
     *
     * @param size
     * @return
     */
    public static char[] generate(int size)
    {
        Random rand = new Random();
        char[] text = new char[size];
        for (int i = 0; i < size; ++i)
        {
            text[i] = (char) rand.nextInt(128);
        }
        return text;
    }

    /**
     * Displays histogram contents.
     */
    public void display()
    {
        final int maxWidth = 100;
        // find normalizing value
        int maxResult = -1;
        int minResult = Integer.MAX_VALUE;
        int sum = 0;

        for (int i = 0; i < results.length; ++i)
        {
            if (results[i] > maxResult)
                maxResult = results[i];

            if (results[i] < minResult)
                minResult = results[i];

            sum += results[i];
        }
        // oops
        if(minResult == 0) minResult++;

        int simpleDeviation = maxResult - minResult;
        System.out.println("Max: " + maxResult + ", \nMin: " + minResult + ",\ndiff:" + simpleDeviation );
        //int mean = (int)(sum / results.length);

        for (int i = 0; i < results.length; ++i)
        {
            char toPrint = ' ';
            if (i > 32){
                toPrint = (char)i;
            }
            int normalizedAggressive = (int)((results[i] - minResult + (simpleDeviation/minResult)) * ((float)maxWidth/(float)maxResult));

            System.out.print("\n" + toPrint  + ": " + results[i] + ":\t");
            for(int j = 0; j < normalizedAggressive; j++) System.out.print('*');

        }
    }

    /**
     * Resets histogram to all zeros.
     */
    public void reset()
    {
        for (int i = 0; i < results.length; ++i)
        {
            results[i] = 0;
        }
    }

    /**
     * Processes an array of text and records character counts.
     * (Non-ASCII characters are all recorded as 127.)
     *
     * @param text
     */
    public void process(final char[] text)
    {

        // One Thread per processor seems about right since this is purely CPU bound,
        // and there is no reason to make more threads than processors/cores.
        // --------------------------------------------------------------------
        // (note: there would be a case if this was bound by some other factor
        //        such as I/O or something with a waiting time not determined
        //        by calculation/cpu.)
        int numberOfThreadsToUse = Runtime.getRuntime().availableProcessors();
        numberOfThreadsToUse=1;
        System.out.println("Using " + numberOfThreadsToUse + " thread(s).");

        // Initialize the thread pool that we are going to use
        ExecutorService pool = Executors.newFixedThreadPool(numberOfThreadsToUse);

                                           // I don't know the java default rounding rules
                                           // So I'll force it my way.
        final int charactersPerPartition = (int) Math.floor(text.length / numberOfThreadsToUse);

        // Yeah I know, typedef's would be amazing here. I won't rant about this though..
        //  anyway, here is what we have in java instead... :)
        List<Callable<HashMap<Integer, Integer>>> tasks = new ArrayList<Callable<HashMap<Integer, Integer>>>();

        try
        {
            // I have all my partitions set up, and will make Worker objects out of them
            for(int partitionSegment = 0; partitionSegment < numberOfThreadsToUse; partitionSegment++)
            {
                final int partitionStartsAt = partitionSegment * charactersPerPartition;
                char[] myLittlePartition = new char[charactersPerPartition];
                //This is neat, IntelliJ just made my for loop into some cool arraycopy call
                System.arraycopy(text, 0 + partitionStartsAt, myLittlePartition, 0, charactersPerPartition);
                tasks.add(new Worker(myLittlePartition));

            }
            try
            {
                // Now I am going to add them all to my thread pool.
                List<Future<HashMap<Integer, Integer>>> results = pool.invokeAll(tasks);

                // Now I should block to wait for them all to count the partitions
                for(Future<HashMap<Integer, Integer>> partition : results)
                {
                    // Block, to wait for results
                    HashMap<Integer, Integer> bag = partition.get();
                    // Merge the bag into the results
                    for(int i = 0; i < this.results.length; ++i){
                        this.results[i] = bag.get(i);
                    }
                }

                System.out.println("All done!");

            } catch (InterruptedException ignored) {
                System.out.println("Something went wrong: " + ignored);
                ignored.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (ExecutionException ignored)
            {
                ignored.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            pool.shutdown();

        }
        finally
        {
            // Need to close out all of our threads
        }
    }

    // It will return an array list of integers
    private class Worker implements Callable<HashMap<Integer, Integer>>
    {
        private char[] myTextToWorkOn;

        public Worker(char[] myTextToWorkOn)
        {
            this.myTextToWorkOn = myTextToWorkOn;
        }

        @Override
        public HashMap<Integer, Integer> call()
        {
            HashMap<Integer, Integer> myResultsBaggie = new HashMap<Integer, Integer>(myTextToWorkOn.length);
            for (int i = 0; i < 127; ++i)
            {
                myResultsBaggie.put(i,0); // Initialize the hash
            }

            int sumOfCharactersOver126 = 0;
            for (int i = 0; i < myTextToWorkOn.length; ++i)
            {
                int ch = myTextToWorkOn[i];
                if (ch >= 127){
                    // No need to do get/set on the hashmap, just a set once for all of these
                    sumOfCharactersOver126++;
                }
                else
                {
                    myResultsBaggie.put(ch, myResultsBaggie.get(ch) + 1);
                }
            }
            // clever, right?
            myResultsBaggie.put(127, sumOfCharactersOver126);
            return myResultsBaggie;
        }
    }
}


