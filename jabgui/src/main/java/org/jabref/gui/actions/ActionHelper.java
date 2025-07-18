package org.jabref.gui.actions;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.collections.ObservableList;
import javafx.scene.control.TabPane;

import org.jabref.gui.StateManager;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.EasyBinding;

public class ActionHelper {

    public static BooleanExpression needsDatabase(StateManager stateManager) {
        return stateManager.activeDatabaseProperty().isPresent();
    }

    public static BooleanExpression needsSavedLocalDatabase(StateManager stateManager) {
        EasyBinding<Boolean> binding = EasyBind.map(stateManager.activeDatabaseProperty(), context -> context.filter(c -> c.getLocation() == DatabaseLocation.LOCAL && c.getDatabasePath().isPresent()).isPresent());
        return BooleanExpression.booleanExpression(binding);
    }

    public static BooleanExpression needsSharedDatabase(StateManager stateManager) {
        EasyBinding<Boolean> binding = EasyBind.map(stateManager.activeDatabaseProperty(), context -> context.filter(c -> c.getLocation() == DatabaseLocation.SHARED).isPresent());
        return BooleanExpression.booleanExpression(binding);
    }

    public static BooleanExpression needsMultipleDatabases(TabPane tabbedPane) {
        return Bindings.size(tabbedPane.getTabs()).greaterThan(1);
    }

    public static BooleanExpression needsStudyDatabase(StateManager stateManager) {
        EasyBinding<Boolean> binding = EasyBind.map(stateManager.activeDatabaseProperty(), context -> context.filter(BibDatabaseContext::isStudy).isPresent());
        return BooleanExpression.booleanExpression(binding);
    }

    public static BooleanExpression needsEntriesSelected(StateManager stateManager) {
        return Bindings.isNotEmpty(stateManager.getSelectedEntries());
    }

    public static BooleanExpression needsEntriesSelected(int numberOfEntries, StateManager stateManager) {
        return Bindings.createBooleanBinding(() -> stateManager.getSelectedEntries().size() == numberOfEntries,
                stateManager.getSelectedEntries());
    }

    public static BooleanExpression isFieldSetForSelectedEntry(Field field, StateManager stateManager) {
        return isAnyFieldSetForSelectedEntry(List.of(field), stateManager);
    }

    public static BooleanExpression isAnyFieldSetForSelectedEntry(List<Field> fields, StateManager stateManager) {
        ObservableList<BibEntry> selectedEntries = stateManager.getSelectedEntries();
        Binding<Boolean> fieldsAreSet = EasyBind.valueAt(selectedEntries, 0)
                                                .mapObservable(entry -> Bindings.createBooleanBinding(
                                                        () -> entry.getFields().stream().anyMatch(fields::contains),
                                                        entry.getFieldsObservable()))
                                                .orElseOpt(false);
        return BooleanExpression.booleanExpression(fieldsAreSet);
    }

    public static BooleanExpression isFilePresentForSelectedEntry(StateManager stateManager, CliPreferences preferences) {
        ObservableList<BibEntry> selectedEntries = stateManager.getSelectedEntries();
        Binding<Boolean> fileIsPresent = EasyBind.valueAt(selectedEntries, 0).mapOpt(entry -> {
            List<LinkedFile> files = entry.getFiles();

            if ((!entry.getFiles().isEmpty()) && stateManager.getActiveDatabase().isPresent()) {
                if (files.getFirst().isOnlineLink()) {
                    return true;
                }

                Optional<Path> filename = FileUtil.find(
                        stateManager.getActiveDatabase().get(),
                        files.getFirst().getLink(),
                        preferences.getFilePreferences());
                return filename.isPresent();
            } else {
                return false;
            }
        }).orElseOpt(false);

        return BooleanExpression.booleanExpression(fileIsPresent);
    }

    /**
     * Check if at least one of the selected entries has linked files
     * <br>
     * Used in {@link org.jabref.gui.maintable.OpenSelectedEntriesFilesAction} when multiple entries selected
     *
     * @param stateManager manager for the state of the GUI
     * @return a boolean binding
     */
    public static BooleanExpression hasLinkedFileForSelectedEntries(StateManager stateManager) {
        return BooleanExpression.booleanExpression(EasyBind.reduce(stateManager.getSelectedEntries(),
                entries -> entries.anyMatch(entry -> !entry.getFiles().isEmpty())));
    }
}
