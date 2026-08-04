package ca.alexgirard.harpoonIJ;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Shared fixture for the Harpoon tests.
 * <p>
 * Harpoon resolves its entries through {@link LocalFileSystem}, so the tests work with real files
 * in a scratch directory rather than the light fixture's in-memory file system.
 * <p>
 * {@link HarpoonState} caches its lists in static state keyed by project name, and the light
 * fixture reuses one project for the whole run, so both the cache and the persisted value have to
 * be cleared around every test.
 */
public abstract class HarpoonTestCase extends BasePlatformTestCase {

    /**
     * Key {@link HarpoonState} persists under. Hard-coded here on purpose: it is part of the
     * on-disk contract, and changing it would silently drop every user's existing list.
     */
    static final String PERSISTENCE_KEY = "HarpoonJumpList";

    private Path scratchDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        scratchDir = Files.createTempDirectory("harpoon-test");
        clearHarpoonList();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            closeAllEditors();
            clearHarpoonList();
            deleteScratchDir();
        } finally {
            super.tearDown();
        }
    }

    protected void clearHarpoonList() {
        HarpoonState.forgetCachedFiles();
        // The light fixture reuses one project for the whole run, so the stored list outlives an
        // individual test. setList(null) is what actually drops a list-valued property.
        PropertiesComponent.getInstance(getProject()).setList(PERSISTENCE_KEY, null);
        PropertiesComponent.getInstance(getProject()).unsetValue(PERSISTENCE_KEY);
    }

    /** Creates a real file on disk and returns it as a refreshed {@link VirtualFile}. */
    protected VirtualFile createFile(String name) throws IOException {
        Path path = scratchDir.resolve(name);
        Files.writeString(path, "contents of " + name);
        VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
        assertNotNull("could not create scratch file " + name, file);
        return file;
    }

    /** Deletes a file created by {@link #createFile} and lets the VFS notice it is gone. */
    protected void deleteFile(VirtualFile file) throws IOException {
        Path path = Path.of(file.getPath());
        Files.deleteIfExists(path);
        file.refresh(false, false);
        LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
    }

    /** Drops the in-memory cache so the next read comes back from persisted storage. */
    protected void reload() {
        HarpoonState.forgetCachedFiles();
    }

    protected void closeAllEditors() {
        var manager = FileEditorManagerEx.getInstanceEx(getProject());
        for (VirtualFile open : manager.getOpenFiles()) {
            manager.closeFile(open);
        }
    }

    /** The files currently open in editors, by name. */
    protected List<String> openFileNames() {
        var names = new ArrayList<String>();
        for (VirtualFile file : FileEditorManagerEx.getInstanceEx(getProject()).getOpenFiles()) {
            names.add(file.getName());
        }
        return names;
    }

    /** Renders a Harpoon list as file names, with {@code -} for an empty slot, for readable asserts. */
    protected List<String> slotNames(List<VirtualFile> files) {
        var names = new ArrayList<String>();
        for (VirtualFile file : files) {
            names.add(file == null ? "-" : file.getName());
        }
        return names;
    }

    protected List<String> currentSlotNames() {
        return slotNames(HarpoonState.GetFiles(getProject()));
    }

    protected List<String> persistedPaths() {
        List<String> stored = PropertiesComponent.getInstance(getProject()).getList(PERSISTENCE_KEY);
        return stored == null ? List.of() : stored;
    }

    private void deleteScratchDir() throws IOException {
        if (scratchDir == null || !Files.exists(scratchDir)) return;
        try (Stream<Path> paths = Files.walk(scratchDir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // best effort: the scratch directory is in the OS temp dir
                }
            });
        }
    }
}
