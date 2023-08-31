package ca.alexgirard.harpoonIJ

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.EditorTextField
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.VimStateMachine
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.JComponent


//import com.maddyhome.idea.vim.VimPlugin;
class HarpoonDialog(inputText: String) : DialogWrapper(true) {
    lateinit var editorTextField: EditorTextField;
    private val text: String
    private var insertModeForced = false;
    private val placeholder = "Nothing set yet :(";

    init {
        val settings: AppSettingsState = AppSettingsState.instance
        setSize(settings.dialogWidth, settings.dialogHeight)
        title = "Harpoon"
        text = inputText
        init()
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return editorTextField
    }

    override fun createCenterPanel(): JComponent? {
        val appSettings: AppSettingsState = AppSettingsState.instance
        editorTextField = EditorTextField(text)
        editorTextField.setOneLineMode(false)
        editorTextField.addSettingsProvider { editor: EditorEx ->
            editor.setFontSize(appSettings.dialogFontSize)
            val settings = editor.settings
            settings.isLineNumbersShown = true
        }

//        editorTextField.addKeyListener(object : KeyListener{
//            override fun keyTyped(e: KeyEvent?) {
//                TODO("Not yet implemented")
//            }
//
//            override fun keyPressed(e: KeyEvent?) {
//                TODO("Not yet implemented")
//            }
//
//            override fun keyReleased(e: KeyEvent?) {
//                TODO("Not yet implemented")
//            }
//
//        } );
        editorTextField.addFocusListener(object : FocusListener {
            
            override fun focusGained(e: FocusEvent?) {
                
                if (insertModeForced) return;
                for (item: VimEditor in VimPlugin.getEditor().localEditors()) {
//                    if (item.getVirtualFile() == null && item.mode == VimStateMachine.Mode.INSERT) {
//                        insertModeForced = true;
//                        item.exitInsertMode(
//                            injector.executionContextManager.onEditor(item),
//                            OperatorArguments(isOperatorPending = false, count0 = 1, mode = VimStateMachine.Mode.VISUAL, subMode = VimStateMachine.SubMode.VISUAL_BLOCK)
//                        );
                        item.exitInsertMode(
                            injector.executionContextManager.onEditor(item),
                            OperatorArguments(isOperatorPending = false, count0 = 1, mode = VimStateMachine.Mode.COMMAND, subMode = VimStateMachine.SubMode.NONE)
                        );
                        insertModeForced = false;
//                    }
                    
                    
                }
            }

            //
            override fun focusLost(e: FocusEvent?) {
//
//                for (item: VimEditor in VimPlugin.getEditor().localEditors()) {
//                    if (item.insertMode && item.getVirtualFile() == null && item.mode == VimStateMachine.Mode.INSERT) {
//                        insertModeForced = true;
//                        item.exitInsertMode(
//                            injector.executionContextManager.onEditor(item),
//                            OperatorArguments(
//                                isOperatorPending = false,
//                                count0 = 1,
//                                mode = VimStateMachine.Mode.INSERT,
//                                subMode = VimStateMachine.SubMode.NONE
//                            )
//                        );
//                        item.exitInsertMode(
//                            injector.executionContextManager.onEditor(item),
//                            OperatorArguments(
//                                isOperatorPending = false,
//                                count0 = 1,
//                                mode = VimStateMachine.Mode.COMMAND,
//                                subMode = VimStateMachine.SubMode.NONE
//                            )
//                        );
//                    }
                }
//                for (item: VimEditor in VimPlugin.getEditor().localEditors()) {
////                    item.insertMode = false;
//                    editorTextField.text = editorTextField.text + item.getPath();
//                }
//            }
        })
//            override fun focusGained(e: FocusEvent) {
//
//                val editor = (VimEditrVimPlugin.getEditor();
//                editorTextField.text+="aa";
//                for (item: VimEditor in editor.localEditors()) {
//                    item.insertMode = false;
//                    editorTextField.text+=item.getPath()
//                }
//            }
//
//            override fun focusLost(e: FocusEvent) {
////                val editor = VimPlugin.getEditor();
////                for (item: VimEditor1 in editor.localEditors()) {
////                    item.insertMode = false;
////                    editorTextField.text+=item.getPath()
////                }
//            }
//        })
        //        if (logger.isDebugEnabled())
//            logger.debug("woweee");
//        for (var editor : VimPlugin.().localEditors()) {
//            string.append(editor.getPath());
//            string.append("\n");
//        }
//        editorTextField.setText(string.toString());
        return editorTextField
    }

    override fun createSouthPanel(): JComponent? {
        return null
    }

    companion object {
        private val logger = Logger.getInstance(HarpoonDialog::class.java.getName())
    }
}
