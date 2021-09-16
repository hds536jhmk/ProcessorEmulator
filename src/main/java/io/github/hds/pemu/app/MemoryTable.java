package io.github.hds.pemu.app;

import io.github.hds.pemu.utils.Fonts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class MemoryTable extends JTable {

    private boolean pointedCellEnabled = false;
    private Integer[] pointedCell = null;
    private Color pointedCellForeground = Color.BLACK;
    private Color pointedCellBackground = Color.GREEN;

    protected MemoryTable() {
        super();

        // We want to clear the selection and the pointed cell if another component is focused
        //  Or if the Frame loses focus which makes the table lose focus
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                clearSelection();
                clearPointedCell();
            }
        });

        // And we also want to clear the selection and the currently pointed cell if we click outside the table
        //  In an empty spot of the Frame, I'm not sure if this is the best way of doing it
        final boolean[] isOutsideTable = { false };
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isOutsideTable[0] = false;
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isOutsideTable[0] = true;
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isOutsideTable[0]) {
                    clearSelection();
                    clearPointedCell();
                }
            }
        });

        // Removing table header and adding auto-resize
        setTableHeader(null);
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        setFont(new Font(
                Fonts.JetBrainsMono, Font.PLAIN,
                getFont().getSize())
        );
        // Setting renderer for Objects
        MemoryCellRenderer cellRenderer = new MemoryCellRenderer();
        cellRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        setDefaultRenderer(Object.class, cellRenderer);
        // Setting selection mode
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setCellSelectionEnabled(true);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    public void setPointedCellForeground(Color color) {
        pointedCellForeground = color;
    }

    public @NotNull Color getPointedCellForeground() {
        return pointedCellForeground;
    }

    public void setPointedCellBackground(Color color) {
        pointedCellBackground = color;
    }

    public @NotNull Color getPointedCellBackground() {
        return pointedCellBackground;
    }

    public void setPointedCell(int row, int column) {
        pointedCell = new Integer[] { row, column };
    }

    public void clearPointedCell() {
        pointedCell = null;
    }

    public @Nullable Integer[] getPointedCell() {
        return pointedCellEnabled ? pointedCell : null;
    }

    public void setPointedCellEnabled(boolean value) {
        pointedCellEnabled = value;
    }

    public boolean isPointedCellEnabled() {
        return pointedCellEnabled;
    }
}
