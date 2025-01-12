package io.github.marco4413.pemu.application;

import io.github.marco4413.pemu.files.FileUtils;
import io.github.marco4413.pemu.localization.ITranslatable;
import io.github.marco4413.pemu.localization.Translation;
import io.github.marco4413.pemu.localization.TranslationManager;
import io.github.marco4413.pemu.utils.IconUtils;
import io.github.marco4413.pemu.utils.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

public final class GFileDialog extends JFileChooser implements ITranslatable {

    public static final ImageIcon ICON_SAVE = IconUtils.importIcon("/assets/save.png", ApplicationGUI.MENU_ITEM_ICON_SIZE);
    public static final ImageIcon ICON_OPEN = IconUtils.importIcon("/assets/open_file.png", ApplicationGUI.MENU_ITEM_ICON_SIZE);

    private static GFileDialog INSTANCE;

    private static @NotNull String localeTextFileDesc = "";
    private static @NotNull String localePEMUFileDesc = "";
    private static @NotNull String localePEMULibFileDesc = "";

    private @NotNull String localeOpenDialogTitle = "";
    private @NotNull String localeSaveDialogTitle = "";
    private @NotNull String localeOverwritePanelTitle = "";
    private @NotNull String localeOverwritePanelMsg = "";
    private @NotNull String localeFileIsReadOnly = "";
    private @NotNull String localeFileIsLocked = "";
    private @NotNull String localeCantWritePanelMsg = "";

    private File openDialogDirectory = new File("./");
    private File saveDialogDirectory = new File("./");

    private GFileDialog() {
        super();

        TranslationManager.addTranslationListener(this);

        setMultiSelectionEnabled(false);
        setFileSelectionMode(JFileChooser.FILES_ONLY);
    }

    public static @NotNull GFileDialog getInstance() {
        if (INSTANCE == null) INSTANCE = new GFileDialog();
        return INSTANCE;
    }

    public static @NotNull FileNameExtensionFilter getTextFileFilter() {
        return new FileNameExtensionFilter(localeTextFileDesc, "txt");
    }

    public static @NotNull FileNameExtensionFilter getPEMUFileFilter() {
        return new FileNameExtensionFilter(localePEMUFileDesc, "pemu");
    }

    public static @NotNull FileNameExtensionFilter getPEMULibFileFilter() {
        return new FileNameExtensionFilter(localePEMULibFileDesc, "pemulib");
    }

    @Override
    public void updateTranslations(@NotNull Translation translation) {
        localeTextFileDesc    = translation.getOrDefault("gFileDialog.textFileDesc"   );
        localePEMUFileDesc    = translation.getOrDefault("gFileDialog.PEMUFileDesc"   );
        localePEMULibFileDesc = translation.getOrDefault("gFileDialog.PEMULibFileDesc");

        localeOpenDialogTitle = translation.getOrDefault("gFileDialog.openDialogTitle");
        localeSaveDialogTitle = translation.getOrDefault("gFileDialog.saveDialogTitle");
        localeOverwritePanelTitle = translation.getOrDefault("gFileDialog.overwritePanelTitle");
        localeOverwritePanelMsg = translation.getOrDefault("gFileDialog.overwritePanelMsg");
        localeFileIsReadOnly = translation.getOrDefault("gFileDialog.fileIsReadOnly");
        localeFileIsLocked = translation.getOrDefault("gFileDialog.fileIsLocked");
        localeCantWritePanelMsg = translation.getOrDefault("gFileDialog.cantWritePanelMsg");
    }

    public int showOpenDialog(@Nullable Component parent, @Nullable File startingDirectory,
                              @NotNull FileNameExtensionFilter filter, @NotNull FileNameExtensionFilter... choosableFilters) throws HeadlessException {
        setDialogTitle(localeOpenDialogTitle);

        resetChoosableFileFilters();
        setFileFilter(filter);
        for (FileNameExtensionFilter choosableFilter : choosableFilters)
            addChoosableFileFilter(choosableFilter);

        if (startingDirectory == null)
            setCurrentDirectory(openDialogDirectory);
        else setCurrentDirectory(startingDirectory);

        int response = super.showOpenDialog(parent);
        openDialogDirectory = getCurrentDirectory();
        return response;
    }

    @Override
    public int showOpenDialog(@Nullable Component parent) throws HeadlessException {
        setDialogTitle(localeOpenDialogTitle);

        setCurrentDirectory(openDialogDirectory);
        int response = super.showOpenDialog(parent);
        openDialogDirectory = getCurrentDirectory();
        return response;
    }

    public int showSaveDialog(@Nullable Component parent, @Nullable File startingDirectory,
                              @NotNull FileNameExtensionFilter filter, @NotNull FileNameExtensionFilter... choosableFilters) throws HeadlessException {
        setDialogTitle(localeSaveDialogTitle);

        resetChoosableFileFilters();
        setFileFilter(filter);
        for (FileNameExtensionFilter choosableFilter : choosableFilters)
            addChoosableFileFilter(choosableFilter);

        if (startingDirectory == null)
            setCurrentDirectory(saveDialogDirectory);
        else setCurrentDirectory(startingDirectory);

        int response = super.showSaveDialog(parent);
        saveDialogDirectory = getCurrentDirectory();
        return response;
    }

    @Override
    public int showSaveDialog(@Nullable Component parent) throws HeadlessException {
        setDialogTitle(localeSaveDialogTitle);

        setCurrentDirectory(saveDialogDirectory);
        int response = super.showSaveDialog(parent);
        saveDialogDirectory = getCurrentDirectory();
        return response;
    }

    @Override
    protected JDialog createDialog(@Nullable Component parent) throws HeadlessException {
        JDialog dialog = super.createDialog(parent);
        switch (getDialogType()) {
            case SAVE_DIALOG:
                dialog.setIconImage(ICON_SAVE.getImage());
                break;
            case OPEN_DIALOG:
                dialog.setIconImage(ICON_OPEN.getImage());
                break;
        }
        return dialog;
    }

    @Override
    public void approveSelection() {
        if (getDialogType() == SAVE_DIALOG) {
            File file = FileUtils.getFileWithExtension(getSelectedFile(), FileUtils.getFileExtensionFromFilter(getFileFilter()));
            setSelectedFile(file);

            if (file.exists()) {
                if (file.canWrite()) {
                    if (JOptionPane.showConfirmDialog(
                            this, StringUtils.format(localeOverwritePanelMsg, file.getName()),
                            StringUtils.format(localeOverwritePanelTitle, getDialogTitle()), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE
                        ) == JOptionPane.OK_OPTION) super.approveSelection();
                } else
                    JOptionPane.showMessageDialog(
                            this, StringUtils.format(localeCantWritePanelMsg, file.getName(), (file.canRead() ? localeFileIsReadOnly : localeFileIsLocked)),
                            getDialogTitle(), JOptionPane.WARNING_MESSAGE
                    );
            } else super.approveSelection();
        } else super.approveSelection();
    }
}
