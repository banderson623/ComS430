import javax.swing.*;
import java.util.Random;

public class NavSystem
{
  public static void main(String[] args)
  {
    final ManeuverGen manueverGenerator = new ManeuverGen();
    FakeDataGenerator dataGenerator = new FakeDataGenerator();
    final RouteCalculator rc = new RouteCalculator(manueverGenerator, dataGenerator);
    
    // create and start UI...
    Runnable r = new Runnable()
    {
      public void run()
      {
        UI ui = UI.createAndShow(rc);
        manueverGenerator.attachUI(ui);
      }
    };
    SwingUtilities.invokeLater(r);
    new GPS(manueverGenerator, dataGenerator).start();
  }
}

class ManeuverGen
{
  private Route currentRoute;
  private Position currentPosition;
  private UI ui;
  private Random rand = new Random();
  public void attachUI(UI ui)
  {
    this.ui = ui;
  }

  public void setNewRoute(Route r)
  {
    currentRoute = r;
    ui.setNewRoute(r);
  }

  public void updatePosition(Position p)
  {
    currentPosition = p;
    ui.setPosition(p);
    Instruction inst = nextManeuver();
    if (inst != null)
    {
      ui.announceNextTurn(inst);
    }
  }

  public Position getCurrentPosition()
  {
    return currentPosition;
  }

  private Instruction nextManeuver()
  {
    if (currentRoute == null)
    {
      return new Instruction("No route selected");
    }
    else if (currentRoute.getDest().equals(currentPosition))
    {
      return new Instruction("Arrived!");
    }
    else
    {
      String dir = rand.nextInt(2) == 0 ? "left" : "right";
      int dist = 100 * (rand.nextInt(4) + 1);
      return new Instruction("Turn " + dir + " in " + dist + " meters");
    }
  }
}

class RouteCalculator
{
  private ManeuverGen mg;
  private FakeDataGenerator gen;
  
  public RouteCalculator(ManeuverGen mg, FakeDataGenerator gen)
  {
    this.mg = mg;
    this.gen = gen;
  }

  public void calculateRoute(Position dst)
  {
    Route r = doCalculate(mg.getCurrentPosition(), dst);
    mg.setNewRoute(r);
    gen.setDest(dst.getCoordinate());
  }

  // This operation may take a long time (e.g. minutes)
  private Route doCalculate(Position src, Position dst)
  {
    lookBusy(10000);
    return new Route(src, dst);
  }

  private static void lookBusy(long millis)
  {
    long interval = 300;
    long stop = System.currentTimeMillis() + millis;

    try
    {
      while (!Thread.currentThread().isInterrupted()
          && System.currentTimeMillis() < stop)
      {
        System.out.print(".");
        Thread.sleep(interval);
      }
    }
    catch (InterruptedException ie)
    {
    }
  }
}

class GPS extends Thread
{
  private ManeuverGen mg;
  private FakeDataGenerator gen;

  public GPS(ManeuverGen mg, FakeDataGenerator gen)
  {
    this.mg = mg;
    this.gen = gen;
  }

  public void run()
  {
    while (true)
    {
      Position p = gen.readData();
      mg.updatePosition(p);
    }
  }

}

class Position
{
  private final int p;

  public Position(int p)
  {
    this.p = p;
  }

  public int getCoordinate()
  {
    return p;
  }
  
  @Override
  public boolean equals(Object obj)
  {
    if (obj == null || obj.getClass() != Position.class) return false;
    return (p == ((Position) obj).p);
  }
  
  @Override
  public String toString()
  {
    return "" + p;
  }
}

class Route
{
  private final Position src;
  private final Position dst;

  public Route(Position src, Position dst)
  {
    this.src = src;
    this.dst = dst;
  }

  @Override
  public String toString()
  {
    return "From " + src + " to " + dst;
  }
  
  public Position getSource()
  {
    return src;
  }
  
  public Position getDest()
  {
    return dst;
  }
}

class Instruction
{
  private final String inst;

  public Instruction(String inst)
  {
    this.inst = inst;
  }

  String decode()
  {
    return inst;
  }
}

// since we don't have a real satellite or real car
class FakeDataGenerator
{
  private int pos;
  private int dest;
  
  public synchronized void setDest(int dest)
  {
    this.dest = dest;
  }
  
  public Position readData()
  {
    try
    {
      Thread.sleep(3000);
    }
    catch (InterruptedException ie)
    {
    }
    synchronized (this)
    {
      // pretend to get closer to "destination"
      if (pos < dest) ++pos;
      else if (pos > dest) --pos;
      return new Position(pos);
    }
  }
}
