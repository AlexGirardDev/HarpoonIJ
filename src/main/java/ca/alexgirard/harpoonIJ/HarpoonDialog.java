package ca.alexgirard.harpoonIJ;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorTextField;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.nio.file.Files;

public class HarpoonDialog extends DialogWrapper {

    private static final Logger LOG = Logger.getInstance(HarpoonDialog.class);

    public EditorTextField editorTextField;
    private final String text;
    public int SelectedIndex = -1;
    private boolean normalModeForcedAlready = false;

    /**
     * The file the popup's text lives in while the dialog is open, or {@code null} if one could not
     * be created. See {@link #createPopupEditor()} for why the popup needs a real file at all.
     */
    private VirtualFile listFile;

    /** The popup's text as of {@link #dispose()}, captured before the editor is released. */
    private String resultText;

    public void Next() {

        var editor = editorTextField.getEditor();
        if (editor != null) {
            editor.getCaretModel().moveCaretRelatively(0, 1, false, false, false);
        }
    }

    public void Previous() {

        var editor = editorTextField.getEditor();
        if (editor != null) {
            editor.getCaretModel().moveCaretRelatively(0, -1, false, false, false);
        }
    }

    public void Ok() {

        var editor = editorTextField.getEditor();
        if (editor != null) {
            SelectedIndex = editor.getCaretModel().getLogicalPosition().line;
        }
        doOKAction();
    }

    protected HarpoonDialog(String inputText) {
        super(true);
        AppSettingsState settings = AppSettingsState.getInstance();
        setSize(settings.dialogWidth, settings.dialogHeight);
        setTitle("Harpoon");
        if (settings.enterRemap && IdeaVimIntegration.isAvailable()) {
            IdeaVimIntegration.remapEnterToSelectHarpoonItem();
        }
        text = inputText;
        init();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return editorTextField;
    }

    /** The popup's text. Safe to call after the dialog has been disposed. */
    public String getListText() {
        if (resultText != null) return resultText;
        return editorTextField == null ? text : editorTextField.getText();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        AppSettingsState appSettings = AppSettingsState.getInstance();
        editorTextField = createPopupEditor();
        editorTextField.setOneLineMode(false);
        editorTextField.addSettingsProvider(editor -> {
            editor.setFontSize(appSettings.dialogFontSize);
            editor.setInsertMode(true);
            editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(0, 0));
            var settings = editor.getSettings();
            settings.setLineNumbersShown(true);
        });


        editorTextField.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                if (normalModeForcedAlready) return;
                var editor = editorTextField.getEditor();
                if (editor == null) return;
                if (!IdeaVimIntegration.forceNormalMode(editor)) return;
                normalModeForcedAlready = true;
                // Forcing normal mode leaves the caret on the line it was already on, so there is
                // nothing to move it back from; the popup opens on the first entry.
            }

            public void focusLost(FocusEvent e) {
            }
        });
        return editorTextField;
    }

    /**
     * Builds the popup's editor over a real file on disk.
     * <p>
     * This is what makes the popup a Vim buffer. IdeaVim only installs its key handling in editors
     * whose document is backed by a file ({@code EditorHelper.isFileEditor}); over a plain
     * in-memory document it attaches nothing at all, and then the popup behaves as if it were in
     * insert mode and Escape reaches this dialog's cancel action instead of Vim. A
     * {@code LightVirtualFile} does not count - IdeaVim rejects those too - so this has to be a
     * real file. It lives in the OS temp directory rather than in the project, so it stays out of
     * VCS, indexing and Local History, and it is deleted again in {@link #dispose()}.
     * <p>
     * If the file cannot be created the popup still opens, over an in-memory document, exactly as
     * it did before. It is then not Vim-driven, which {@link IdeaVimIntegration#forceNormalMode}
     * reports.
     */
    private EditorTextField createPopupEditor() {
        Document document = createListDocument();
        if (document == null) return new EditorTextField(text);
        return new EditorTextField(document, null, PlainTextFileType.INSTANCE, false, false);
    }

    private @Nullable Document createListDocument() {
        try {
            // Files.createTempFile, not FileUtil.createTempFile: this one randomises the name and
            // creates the file owner-only. The list is the user's file paths, and a fixed name in a
            // shared temp directory is both a collision between two open projects and something
            // another local user could pre-create as a symlink.
            File file = Files.createTempFile("harpoon-list", ".txt").toFile();
            file.deleteOnExit();
            return WriteAction.compute(() -> {
                VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
                if (virtualFile == null) return null;
                Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
                if (document == null) return null;
                document.setText(text);
                // Leave the document and the file agreeing, so nothing is left unsaved and the IDE
                // never asks about the file having changed on disk.
                FileDocumentManager.getInstance().saveDocument(document);
                listFile = virtualFile;
                return document;
            });
        } catch (Exception e) {
            LOG.warn("Could not back the Harpoon popup with a file; it will not be Vim-driven", e);
            return null;
        }
    }

    @Override
    protected void dispose() {
        if (editorTextField != null) resultText = editorTextField.getText();
        try {
            super.dispose();
        } finally {
            deleteListFile();
        }
    }

    private void deleteListFile() {
        VirtualFile file = listFile;
        listFile = null;
        if (file == null) return;
        try {
            WriteAction.run(() -> {
                Document document = FileDocumentManager.getInstance().getCachedDocument(file);
                // Drop the edits rather than writing them out: the list has already been read from
                // the editor and the file is about to go away.
                if (document != null) FileDocumentManager.getInstance().reloadFromDisk(document);
                if (file.isValid()) file.delete(this);
            });
        } catch (Exception e) {
            LOG.warn("Could not delete the Harpoon popup's scratch file " + file.getPath(), e);
        }
    }

    @Override
    protected JComponent createSouthPanel() {
        return null;
    }
}
