package io.github.hds.pemu.app;

import io.github.hds.pemu.compiler.Compiler;
import io.github.hds.pemu.processor.Processor;
import io.github.hds.pemu.processor.ProcessorConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

public class Application extends JFrame implements KeyListener {

    private static Application INSTANCE;
    private static final String APP_TITLE = "PEMU";

    private final ProcessorConfig PROCESSOR_CONFIG;
    private @Nullable File currentProgram = null;
    private @Nullable Processor currentProcessor = null;

    private final JFileChooser FILE_DIALOG;

    private Application(@NotNull ProcessorConfig initialConfig) throws HeadlessException {
        super();
        PROCESSOR_CONFIG = initialConfig;
        updateTitle();

        ImageIcon icon = new ImageIcon(System.class.getResource("/icon.png"));
        setIconImage(icon.getImage());

        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        // FILE MENU
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        menuBar.add(fileMenu);

        JMenuItem fileOpenProgram = new JMenuItem("Open Program", 'O');
        fileOpenProgram.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        fileOpenProgram.addActionListener(this::openProgram);
        fileMenu.add(fileOpenProgram);

        JMenuItem fileClose = new JMenuItem("Quit", 'Q');
        fileClose.addActionListener(e -> { INSTANCE.stopProcessor(null); INSTANCE.dispose(); });
        fileMenu.add(fileClose);

        FILE_DIALOG = new JFileChooser();
        FILE_DIALOG.setCurrentDirectory(new File("./"));
        FILE_DIALOG.setMultiSelectionEnabled(false);
        FILE_DIALOG.setFileFilter(new FileNameExtensionFilter("PEMU program", "pemu"));

        // PROCESSOR MENU
        JMenu processorMenu = new JMenu("Processor");
        processorMenu.setMnemonic('P');
        menuBar.add(processorMenu);

        JMenuItem pRunProcessor = new JMenuItem("Run", 'R');
        pRunProcessor.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        pRunProcessor.addActionListener(this::runProcessor);
        processorMenu.add(pRunProcessor);

        JMenuItem pStopProcessor = new JMenuItem("Stop", 'S');
        pStopProcessor.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        pStopProcessor.addActionListener(this::stopProcessor);
        processorMenu.add(pStopProcessor);

        JMenuItem pConfigProcessor = new JMenuItem("Configure", 'C');
        pConfigProcessor.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        pConfigProcessor.addActionListener(this::configureProcessor);
        processorMenu.add(pConfigProcessor);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(Console.POutput.ELEMENT), new JScrollPane(Console.Debug.ELEMENT));
        splitPane.setResizeWeight(0.5d);
        splitPane.resetToPreferredSizes();
        add(splitPane);

        Console.POutput.ELEMENT.addKeyListener(this);
    }

    public void updateTitle() {
        if (currentProgram == null)
            setTitle(APP_TITLE + " (No Program Selected)");
        else
            setTitle(APP_TITLE + " (" + currentProgram.getAbsolutePath() + ")");
    }

    public void setCurrentProgram(@NotNull File program) {
        if (program.exists() && program.canRead())
            currentProgram = program;
        else currentProgram = null;
        updateTitle();
    }

    public static @NotNull Application getInstance(ProcessorConfig initialConfig) {
        if (INSTANCE == null)
            INSTANCE = new Application(initialConfig);
        return INSTANCE;
    }

    public static @NotNull Application getInstance() {
        if (INSTANCE == null)
            INSTANCE = new Application(new ProcessorConfig());
        return INSTANCE;
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (currentProcessor == null) return;

        char typed = e.getKeyChar();
        if (typed == KeyEvent.CHAR_UNDEFINED)
            currentProcessor.pressedChar = '\0';
        else
            currentProcessor.pressedChar = typed;
    }

    @Override
    public void keyPressed(KeyEvent e) { }

    @Override
    public void keyReleased(KeyEvent e) {
        if (currentProcessor == null) return;

        char released = e.getKeyChar();
        if (released != KeyEvent.CHAR_UNDEFINED && Character.toLowerCase(released) == Character.toLowerCase(currentProcessor.pressedChar))
            currentProcessor.pressedChar = '\0';
    }

    public void openProgram(ActionEvent e) {
        if (FILE_DIALOG.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            setCurrentProgram(FILE_DIALOG.getSelectedFile());
    }

    public void configureProcessor(ActionEvent e) {
        ProcessorConfigPanel panel = new ProcessorConfigPanel(PROCESSOR_CONFIG);
        int result = JOptionPane.showConfirmDialog(this, panel, "Configure Processor", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION)
            panel.apply();
    }

    public void runProcessor(ActionEvent e) {
        // Make sure that the last thread is dead
        if (currentProcessor != null && currentProcessor.isRunning()) {
            Console.Debug.println("Processor is already running!");
            return;
        }

        // Clear Debug console and check if a program is specified
        Console.Debug.clear();
        if (currentProgram == null) {
            Console.Debug.println("No program specified!");
            return;
        }

        // Create a new Processor with the specified values
        try {
            currentProcessor = new Processor(PROCESSOR_CONFIG);
        } catch (Exception err) {
            Console.Debug.println("Couldn't create processor.");
            Console.Debug.printStackTrace(err);
            return;
        }

        // Compile the selected program
        int[] compiledProgram;
        try {
            compiledProgram = Compiler.compileFile(currentProgram, currentProcessor);
        } catch (Exception err) {
            Console.Debug.println("Compilation error. (for file @'" + currentProgram.getAbsolutePath() + "')");
            Console.Debug.printStackTrace(err);
            return;
        }

        Console.Debug.println(
                "Compiled file (" + currentProgram.getName() + ") occupies "
              + compiledProgram.length * currentProcessor.MEMORY.WORD.BYTES + " / "
              + currentProcessor.MEMORY.getSize() * currentProcessor.MEMORY.WORD.BYTES + " Bytes"
        );

        // Load compiled program into memory
        try {
            currentProcessor.MEMORY.setValuesAt(0, compiledProgram);
        } catch (Exception err) {
            Console.Debug.println("Error while loading program into memory!");
            Console.Debug.printStackTrace(err);
            return;
        }

        // Run the processor
        try {
            Console.Debug.println("Running Processor:\n" + currentProcessor.getInfo());
            Console.POutput.clear();

            // We want to make sure that if the Processor fails, details about the error show on the Console
            Thread processorThread = new Thread(currentProcessor) {
                @Override
                public void run() {
                    try {
                        super.run();
                    } catch (Exception err) {
                        Console.Debug.println("Error while running program!");
                        Console.Debug.printStackTrace(err);
                    }
                    Console.Debug.println("Processor stopped!");
                }
            };
            processorThread.start();
        } catch (Exception err) {
            Console.Debug.println("Error while starting processor's thread!");
            Console.Debug.printStackTrace(err);
        }
    }

    public void stopProcessor(ActionEvent e) {
        if (currentProcessor == null || !currentProcessor.isRunning()) {
            Console.Debug.println("Couldn't stop processor because it isn't currently running!");
            return;
        }
        currentProcessor.stop();
    }
}
