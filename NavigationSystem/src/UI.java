import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class UI extends JPanel
{
    private RouteCalculator rc;
    private Position currentPosition = new Position(0);
    private Position currentDestination = new Position(0);
    private Route currentRoute;

    private JPanel mapDisplay;
    private JButton confirmDestinationButton;
    private JLabel nextTurn;
    private JTextField routeText;

    private boolean isProcessingNewRoute = false;

    private GetRouteCalculationWorker routerUIWorker;

    public static UI createAndShow(RouteCalculator rc)
    {
        // create the frame
        JFrame frame = new JFrame("Nav system");
        UI ui = new UI(rc);
        // create an instance of our JPanel subclass and 
        // add it to the frame
        frame.getContentPane().add(ui);

        // use the preferred sizes
        frame.pack();

        // we want to shut down the application if the 
        // "close" button is pressed on the frame
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // make the frame visible and start the UI machinery
        frame.setVisible(true);

        return ui;

    }

    public UI(RouteCalculator rc)
    {
        this.rc = rc;
        mapDisplay = new MapDisplay();
        mapDisplay.setPreferredSize(new Dimension(500, 200));
        confirmDestinationButton = new JButton("Set Destination");
        nextTurn = new JLabel();
        routeText = new JTextField(10);
        confirmDestinationButton.addActionListener(new DestinationButtonListener());

        // lay out the pieces vertically
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        this.add(mapDisplay);
        this.add(nextTurn);
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(routeText);
        bottomPanel.add(confirmDestinationButton);
        this.add(bottomPanel);
    }

    public void setPosition(final Position p)
    {
        // Changed to use an anonymous runnable class inside
        // SwingUtilities.invokeLater so that it is processed
        // properly by the swing thread.
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                currentPosition = p;
                repaint();
            }
        });
    }

    public void setNewRoute(final Route r)
    {
        // Created a new Runnable to be invoked
        // within the swing-friendly GUI thread.
        // because this is called by a Maneuver object
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                currentRoute = r;
                confirmDestinationButton.setEnabled(true);
                repaint();
            }
        });
    }

    public void announceNextTurn(final Instruction inst)
    {
        // Changed to use an anonymous runnable class inside
        // SwingUtilities.invokeLater so that it is processed
        // properly by the swing thread.
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                String text = inst.decode();
                if(isProcessingNewRoute)
                {
                    // Added a small section to note when
                    // a (re)calculation is happening.
                    text += " (calculating new route)";
                }
                nextTurn.setText(text);
                repaint();
            }
        });
    }

    // listener attached to confirmDestinationButton...
    private class DestinationButtonListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            // Update the UI immediately... with the following text
            String routeString = routeText.getText();
            try
            {
                if(routerUIWorker != null && !(routerUIWorker.isDone() ||
                                               routerUIWorker.isCancelled()))
                {
                    // If we are still running lets cancel this and accept
                    // a new route calculation request
                    routerUIWorker.cancel(true);
                }
                // dispatches to a friendly swing thread.
                routerUIWorker = new GetRouteCalculationWorker(routeString);
                routerUIWorker.execute();
//                rcWorker.cancel(false);
            } catch (NumberFormatException nfe)
            {
                nextTurn.setText("Error: " + routeString + " is not recognized as an integer");
                // do a little more than nothing
            }
        }
    }

    /**
     * Pretends to draw a map showing current position and route...
     */
    private class MapDisplay extends JPanel
    {
        public void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g;
            g2.setBackground(Color.WHITE);
            g2.clearRect(0, 0, getWidth(), getHeight());
            g2.drawRect(0, 0, getWidth(), getHeight());
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
            String pos = "Current position: " + currentPosition.getCoordinate();
            String dest = "Current destination: " + currentDestination.getCoordinate();
            String route = "Route: " + (currentRoute == null ? "No route selected" : currentRoute.toString());
            g2.drawString(pos, 30, 30);
            g2.drawString(dest, 30, 60);
            g2.drawString(route, 30, 90);

        }
    }

    /**
     * Added a new class that will execute the new route calculation
     * in a separate swing-friendly thread so the User Interface does not block
     * <p/>
     * Fun.
     */
    private class GetRouteCalculationWorker extends SwingWorker<String, Object>
    {
        private final String routeString;
        private boolean wasSuccessful;

        public GetRouteCalculationWorker(String route)
        {
            super();    // Call SwingWorker's parent method
            routeString = route;
            wasSuccessful = false;
        }

        @Override
        public String doInBackground()
        {
            try
            {
                int destinationAsInteger = Integer.parseInt(routeString);
//                confirmDestinationButton.setEnabled(false);
                isProcessingNewRoute = true;
                currentDestination = new Position(destinationAsInteger);
                rc.calculateRoute(currentDestination);
            }
            catch (Exception err)
            {
                System.out.println("Error: " + err.getMessage());
            } finally
            {

                return "Done doInBackground";
            }
        }

        @Override
        protected void done()
        {
            confirmDestinationButton.setEnabled(true);
            isProcessingNewRoute = false;
            repaint();

        }
    }
}