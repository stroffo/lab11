package it.unibo.oop.reactivegui02;

import java.awt.event.ActionListener;
import java.io.Serial;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.function.Consumer;

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

    private static Agent agent;
    private final JLabel display = new JLabel();

    public ConcurrentGUI() {
        super();
        JFrameUtil.dimensionJFrame(this);
        final JPanel panel = new JPanel();
        panel.add(display);

        agent = new Agent();
        new Thread(agent).start();

        panel.add(Button.UP.get());
        panel.add(Button.DOWN.get());
        panel.add(Button.STOP.get());

        this.getContentPane().add(panel);
        this.setVisible(true);
    }

    private void doForAllButtons(final Consumer<JButton> action) {
        Arrays.stream(Button.values()).forEach(v -> {
            action.accept(v.get());
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

        /**
         * External command to change counting direction.
         * 
         * @param newDirection the new direction.
         */
        public void changeDirection(final Direction newDirection) {
            this.currentDirection = newDirection;
        }
    }

    private enum Button {
        UP("Up", e -> agent.changeDirection(Direction.UP)),
        DOWN("Down", e -> agent.changeDirection(Direction.DOWN)),
        STOP("Stop", e -> agent.stopCounting());

        private final String label;
        private final ActionListener action;

        Button(final String label, final java.awt.event.ActionListener action) {
            this.label = label;
            this.action = action;
        }

        public JButton get() {
            final JButton btn = new JButton(this.label);
            btn.addActionListener(action);
            return btn;
        }
    }

    private enum Direction {
        UP(1),
        DOWN(-1);

        private final int step;

        Direction(final int i) {
            this.step = i;
        }

        public int getStep() {
            return step;
        }
    }
}
