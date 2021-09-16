package io.github.hds.pemu.app;

import io.github.hds.pemu.config.ConfigEvent;
import io.github.hds.pemu.config.ConfigManager;
import io.github.hds.pemu.config.IConfigurable;
import io.github.hds.pemu.instructions.InstructionHistory;
import io.github.hds.pemu.localization.ITranslatable;
import io.github.hds.pemu.localization.Translation;
import io.github.hds.pemu.localization.TranslationManager;
import io.github.hds.pemu.memory.IMemory;
import io.github.hds.pemu.memory.flags.IFlag;
import io.github.hds.pemu.memory.registers.IRegister;
import io.github.hds.pemu.processor.IProcessor;
import io.github.hds.pemu.utils.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.Collections;
import java.util.HashMap;

public final class MemoryView extends JFrame implements ITranslatable, IConfigurable {

    private static final String UNKNOWN_PROCESSOR = "?";

    private final Application app;

    private final Timer UPDATE_TIMER;
    private final MemoryTable MEMORY_TABLE;

    private final JLabel COLS_LABEL;
    private final JLabel UPDATE_INTERVAL_LABEL;

    private final JSpinner COLS_SPINNER;
    private final JSpinner UPDATE_INTERVAL_SPINNER;
    private final JCheckBox SHOW_SELECTED_CELL_POINTER;
    private final JComboBox<ShowMode> SHOW_AS;
    private final JCheckBox SHOW_HISTORY;
    private final JCheckBox SHOW_POINTERS;
    private final JLabel RF_VALUES;

    private enum ShowMode {
        DECIMAL, BINARY, OCTAL, HEX, CHAR;
        public static final ShowMode[] ALL = new ShowMode[] {
                DECIMAL, BINARY, OCTAL, HEX, CHAR
        };

        public static HashMap<ShowMode, String> translations = new HashMap<>(ALL.length);

        public @NotNull String getName() {
            return super.toString();
        }

        @Override
        public @NotNull String toString() {
            return translations.get(this);
        }
    }

    private static final int BINARY_BYTE_DIGITS = 8;
    private static final int OCTAL_BYTE_DIGITS  = 3;
    private static final int HEX_BYTE_DIGITS    = 2;

    private static @NotNull String toShowMode(int number, int minBytes, @Nullable ShowMode base) {
        if (base == null) base = ShowMode.DECIMAL;
        switch (base) {
            case BINARY: {
                String result = Integer.toBinaryString(number);

                if (minBytes > 0) {
                    int minimumBits = minBytes * BINARY_BYTE_DIGITS;
                    int missingBits = result.length() > minimumBits ? 0 : minimumBits - result.length();
                    String padding = String.join("", Collections.nCopies(missingBits, "0"));
                    result = padding + result;
                }

                return "0b" + result;
            }
            case OCTAL: {
                String result = Integer.toOctalString(number);

                if (minBytes > 0) {
                    int minimumDigits = minBytes * OCTAL_BYTE_DIGITS;
                    int missingDigits = result.length() > minimumDigits ? 0 : minimumDigits - result.length();
                    String padding = String.join("", Collections.nCopies(missingDigits, "0"));
                    result = padding + result;
                }

                return "0o" + result;
            }
            case HEX: {
                String result = Integer.toHexString(number);

                if (minBytes > 0) {
                    int minimumDigits = minBytes * HEX_BYTE_DIGITS;
                    int missingDigits = result.length() > minimumDigits ? 0 : minimumDigits - result.length();
                    String padding = String.join("", Collections.nCopies(missingDigits, "0"));
                    result = padding + result;
                }

                return "0x" + result;
            }
            case CHAR: {
                // If the character can't be typed
                if (Character.isISOControl(number)) {
                    // If it's a special character convert it
                    if (StringUtils.SpecialCharacters.isSpecialCharacter(number))
                        return StringUtils.SpecialCharacters.toString((char) number);
                        // Else put it as a number with a backslash in front of it (That's done to differentiate between '0' and 0)
                    else return StringUtils.SpecialCharacters.ESCAPE_CHARACTER + number;
                    // Else if the character can be typed then show it
                } else return String.valueOf((char) number);
            }
            default:
                return Integer.toString(number);
        }
    }

    protected MemoryView(@NotNull Application parentApp) {
        super();
        app = parentApp;

        setIconImage(IconUtils.importIcon("/assets/memory_view.png", Application.FRAME_ICON_SIZE).getImage());

        setSize(Application.FRAME_WIDTH, Application.FRAME_HEIGHT);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        ConfigManager.addConfigListener(this);
        TranslationManager.addTranslationListener(this);

        setLayout(new GridBagLayout());

        // Adding Options
        COLS_LABEL = new JLabel("", JLabel.RIGHT);
        addComponent(COLS_LABEL, 0, 0);
        SpinnerNumberModel colsModel = new SpinnerNumberModel(Byte.SIZE, Byte.SIZE, Byte.SIZE * Byte.SIZE, Byte.SIZE);
        COLS_SPINNER = new JSpinner(colsModel);
        addComponent(COLS_SPINNER, 1, 0);

        UPDATE_INTERVAL_LABEL = new JLabel("", JLabel.RIGHT);
        addComponent(UPDATE_INTERVAL_LABEL, 2, 0);
        SpinnerNumberModel updateDelayModel = new SpinnerNumberModel(1.0f, 0.01f, 5.0f, 0.01f);
        UPDATE_INTERVAL_SPINNER = new JSpinner(updateDelayModel);
        addComponent(UPDATE_INTERVAL_SPINNER, 3, 0);

        SHOW_AS = new JComboBox<>(ShowMode.ALL);
        JLabel showAsRenderer = (JLabel) SHOW_AS.getRenderer();
        showAsRenderer.setHorizontalAlignment(JLabel.CENTER);
        addComponent(SHOW_AS, 0, 1);

        SHOW_HISTORY = new JCheckBox();
        addComponent(SHOW_HISTORY, 1, 1);

        SHOW_POINTERS = new JCheckBox();
        addComponent(SHOW_POINTERS, 2, 1);

        SHOW_SELECTED_CELL_POINTER = new JCheckBox();
        addComponent(SHOW_SELECTED_CELL_POINTER, 1, 2);

        RF_VALUES = new JLabel();
        addComponent(RF_VALUES, 3, 1, 1,  2);

        MEMORY_TABLE = new MemoryTable();

        // Adding table to Frame
        addComponent(new JScrollPane(MEMORY_TABLE), 0, 3, 4, 1, 1.0f, 1.0f);

        UPDATE_TIMER = new Timer(0, this::updateFrame);
        updateFrame(null);
        UPDATE_TIMER.start();
    }

    @Override
    public void updateTranslations(@NotNull Translation translation) {
        translation.translateFrame("memoryView", this);
        translation.translateComponent("memoryView.colsLabel", COLS_LABEL);
        translation.translateComponent("memoryView.updateIntervalLabel", UPDATE_INTERVAL_LABEL);

        for (ShowMode mode : ShowMode.ALL) {
            ShowMode.translations.put(
                    mode, translation.getOrDefault("memoryView.showAs." + mode.getName().toLowerCase())
            );
        }

        translation.translateComponent("memoryView.showHistory", SHOW_HISTORY);
        translation.translateComponent("memoryView.showSelectedCellPointer", SHOW_SELECTED_CELL_POINTER);
        SHOW_POINTERS.setText(StringUtils.format(translation.getOrDefault("memoryView.showPointers"), "{", "}", "[", "]"));
    }

    public void addComponent(@NotNull Component component, int x, int y) {
        addComponent(component, x, y, 1, 1);
    }

    public void addComponent(@NotNull Component component, int x, int y, int w, int h) {
        addComponent(component, x, y, w, h, 1.0f, 0.0f);
    }

    public void addComponent(@NotNull Component component, int x, int y, int w, int h, float wX, float wY) {
        addComponent(component, x, y, w, h, wX, wY, 5);
    }

    public void addComponent(@NotNull Component component, int x, int y, int w, int h, float wX, float wY, int margin) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(margin, margin, margin, margin);
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.gridwidth = w;
        constraints.gridheight = h;
        constraints.weightx = wX;
        constraints.weighty = wY;
        add(component, constraints);
    }

    public void updateFrame(ActionEvent e) {
        // We update the update timer
        UPDATE_TIMER.setDelay((int) ((double) UPDATE_INTERVAL_SPINNER.getValue() * 1000.0f));
        // If this frame isn't visible we don't bother updating it
        if (!isVisible()) return;

        // Getting the processor that is currently attached to the app
        IProcessor processor = app.currentProcessor;
        DefaultTableModel model = (DefaultTableModel) MEMORY_TABLE.getModel();
        // If no processor was found then we remove the table and set registers to unknown values
        if (processor == null) {
            model.setRowCount(0);
            model.setColumnCount(0);

            RF_VALUES.setText(UNKNOWN_PROCESSOR);
            return;
        }

        // Getting the history of executed processor instructions
        InstructionHistory history = processor.getInstructionHistory();

        // These will be populated when looking through registers
        Integer IPValue = null;
        Integer SPValue = null;

        // Getting all registers and flags
        IRegister[] processorRegisters = processor.getRegisters();
        IFlag[] processorFlags = processor.getFlags();

        // Create a new table
        HTMLTableBuilder registersTable = new HTMLTableBuilder(
                (int) Math.sqrt(processorRegisters.length + processorFlags.length)
        );

        // Appending all registers to the table
        for (IRegister register : processorRegisters) {
            String shortName = register.getShortName();
            int value = register.getValue();

            // If the name is either IP or SP,
            //  populate the above defined variables
            if (IPValue == null && shortName.equals("IP"))
                IPValue = value;
            else if (SPValue == null && shortName.equals("SP"))
                SPValue = value;

            registersTable.putElement(shortName + "=" + value);
        }

        // Appending all flags to the table
        for (IFlag flag : processorFlags)
            registersTable.putElement(flag.getShortName() + "=" + flag.getValue());

        RF_VALUES.setText(registersTable.toString(true));

        IMemory memory = processor.getMemory();
        int wordBytes = memory.getWord().TOTAL_BYTES;

        // Making the table large enough to fit all the processor's memory
        int memSize = memory.getSize();
        int cols = (int) COLS_SPINNER.getValue();
        int rows = (int) Math.ceil(memSize / (float) cols);

        if (model.getColumnCount() != cols)
            model.setColumnCount(cols);

        if (model.getRowCount() != rows)
            model.setRowCount(rows);

        // Getting the currently selected column and row
        int selectedRow = MEMORY_TABLE.getSelectedRow();
        int selectedCol = MEMORY_TABLE.getSelectedColumn();

        // If the cell pointed by the currently selected one needs to be
        //  highlighted we do that
        boolean enablePointedCellFeature = SHOW_SELECTED_CELL_POINTER.isSelected();
        MEMORY_TABLE.setPointedCellEnabled(enablePointedCellFeature);
        if (enablePointedCellFeature)
            MEMORY_TABLE.clearPointedCell();

        // For each memory address
        for (int i = 0; i < memSize; i++) {
            // We get its position on the table
            int x = i % cols;
            int y = i / cols;

            // We get the currently stored value at that address
            int valueAtCurrentIndex = memory.getValueAt(i);
            // Highlighting pointed cell if necessary
            if (enablePointedCellFeature && y == selectedRow && x == selectedCol)
                MEMORY_TABLE.setPointedCell(valueAtCurrentIndex / cols, valueAtCurrentIndex % cols);

            // Getting the value to show to the user
            String value;
            if (SHOW_HISTORY.isSelected() && history != null && history.containsKey(i))
                // If the current value is an executed instruction use its name
                value = history.get(i);
            else {
                ShowMode base = (ShowMode) SHOW_AS.getSelectedItem();
                value = toShowMode(valueAtCurrentIndex, wordBytes, base);
            }

            // If the current cell is pointed by either IP or SP put the corresponding brackets
            if (SHOW_POINTERS.isSelected()) {
                if (IPValue != null && IPValue == i) value = "{ " + value + " }";
                else if (SPValue != null && SPValue == i) value = "[ " + value + " ]";
            }

            // Setting the value to be shown on the table
            model.setValueAt(value, y, x);
        }
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void loadConfig(@NotNull ConfigEvent e) {
        COLS_SPINNER.setValue(e.config.get(Integer.class, "memoryView.columns"));
        UPDATE_INTERVAL_SPINNER.setValue(e.config.get(Double.class, "memoryView.updateInterval"));

        int showAsSelIndex = e.config.get(Integer.class, "memoryView.showAs");
        if (showAsSelIndex >= 0 && showAsSelIndex < SHOW_AS.getItemCount())
            SHOW_AS.setSelectedIndex(showAsSelIndex);

        SHOW_HISTORY.setSelected(e.config.get(Boolean.class, "memoryView.showHistory"));
        SHOW_POINTERS.setSelected(e.config.get(Boolean.class, "memoryView.showPointers"));
        SHOW_SELECTED_CELL_POINTER.setSelected(e.config.get(Boolean.class, "memoryView.showSelectedCellPointer"));
    }

    @Override
    public void saveConfig(@NotNull ConfigEvent e) {
        e.config.put("memoryView.columns", COLS_SPINNER.getValue());
        e.config.put("memoryView.updateInterval", UPDATE_INTERVAL_SPINNER.getValue());
        e.config.put("memoryView.showAs", SHOW_AS.getSelectedIndex());
        e.config.put("memoryView.showHistory", SHOW_HISTORY.isSelected());
        e.config.put("memoryView.showPointers", SHOW_POINTERS.isSelected());
        e.config.put("memoryView.showSelectedCellPointer", SHOW_SELECTED_CELL_POINTER.isSelected());
    }

    @Override
    public void setDefaults(@NotNull ConfigEvent e) {
        e.config.put("memoryView.columns", 8);
        e.config.put("memoryView.updateInterval", 1.0f);
        e.config.put("memoryView.showAs", 0);
        e.config.put("memoryView.showHistory", false);
        e.config.put("memoryView.showPointers", false);
        e.config.put("memoryView.showSelectedCellPointer", false);
    }
}
