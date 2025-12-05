package it.unibo.oop.reactivegui02;

import java.io.Serial;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unibo.oop.JFrameUtil;

/**
 * Second example of reactive GUI.
 */
public final class ConcurrentGUI extends JFrame {

    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentGUI.class);

    private final JLabel display = new JLabel();
    private final Map<String, JButton> buttons = Map.of(
        "up",   new JButton("up"),
        "down", new JButton("down"),
        "stop", new JButton("stop")
    );

    public ConcurrentGUI() {
        super();
        JFrameUtil.dimensionJFrame(this);
        final JPanel panel = new JPanel();
        panel.add(display);
        panel.add(getButton("up"));
        panel.add(getButton("down"));
        panel.add(getButton("stop"));
        
        this.getContentPane().add(panel);
        this.setVisible(true);

        final Agent agent = new Agent();
        new Thread(agent).start();
        /*
         * Register a listener that stops it
         */
        getButton("up").addActionListener(e -> agent.changeDirection(Direction.UP));
        getButton("down").addActionListener(e -> agent.changeDirection(Direction.DOWN));
        getButton("stop").addActionListener(e -> agent.stopCounting());
    }

    private JButton getButton(final String btnLabel) {
        return buttons.get(btnLabel);
    }

    private void doForAllButtons(Consumer<JButton> action) {
        buttons.forEach((k, v) -> {
            action.accept(v);
        });
    }

    private final class Agent implements Runnable {
        /*
         * Stop is volatile to ensure visibility. Look at:
         *
         * http://archive.is/9PU5N - Sections 17.3 and 17.4
         *
         * For more details on how to use volatile:
         *
         * http://archive.is/4lsKW
         *
         */
        private volatile boolean stop;
        private volatile Direction currentDirection = Direction.UP;
        private int counter;

        @Override
        public void run() {
            while (!this.stop) {
                try {
                    // The EDT doesn't access `counter` anymore, it doesn't need to be volatile
                    final var nextText = Integer.toString(this.counter);
                    SwingUtilities.invokeAndWait(() -> ConcurrentGUI.this.display.setText(nextText));
                    this.counter += currentDirection.getStep();
                    Thread.sleep(100);
                } catch (InvocationTargetException | InterruptedException ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }

            doForAllButtons(btn -> btn.setEnabled(false));
        }

        /**
         * External command to stop counting.
         */
        public void stopCounting() {
            this.stop = true;
        }

        public void changeDirection(final Direction newDirection) {
            this.currentDirection = newDirection;
        }
    }

    private enum Direction {
        UP(1),
        DOWN(-1);

        private final int step;

        Direction(int i) {
            this.step = i;
        }
        
        public int getStep() {
            return step;
        }
    }
}
