package org.macromag.os

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import java.io.File

class MainActivity : AppCompatActivity(), TerminalSessionClient, TerminalViewClient {

    private lateinit var terminalView: TerminalView
    private var session: TerminalSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0D1117.toInt())
        }

        terminalView = TerminalView(this, null).apply {
            setTextSize(36)
            setTerminalViewClient(this@MainActivity)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }

        // Dolny pasek szybkich klawiszy MacroMag OS
        val shortcutBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF161B22.toInt())
        }

        val shortcuts = listOf("TAB" to "\t", "ESC" to "\u001b", "mur sync" to "mur sync\n", "mur list" to "mur list\n", "sys.cpu()" to "sys.cpu()\n")
        for ((label, seq) in shortcuts) {
            val btn = Button(this).apply {
                text = label
                textSize = 11f
                setTextColor(0xFF58A6FF.toInt())
                setBackgroundColor(0xFF21262D.toInt())
                setOnClickListener { session?.write(seq) }
            }
            shortcutBar.addView(btn)
        }

        rootLayout.addView(terminalView)
        rootLayout.addView(shortcutBar)
        setContentView(rootLayout)

        Thread {
            prepareRootfs()
            runOnUiThread { startVosSession() }
        }.start()
    }

    private fun prepareRootfs() {
        val rootfsDir = File(filesDir, "rootfs")
        val marker = File(rootfsDir, ".installed")
        if (marker.exists()) return

        rootfsDir.mkdirs()
        val nativeDir = applicationInfo.nativeLibraryDir
        val busybox = File(nativeDir, "libbusybox.so").absolutePath
        val tarFile = File(filesDir, "rootfs.tar.gz")

        assets.open("macromag-arm64-rootfs.tar.gz").use { input ->
            tarFile.outputStream().use { output -> input.copyTo(output) }
        }

        ProcessBuilder(busybox, "tar", "-xzf", tarFile.absolutePath, "-C", rootfsDir.absolutePath)
            .redirectErrorStream(true)
            .start()
            .waitFor()

        tarFile.delete()
        marker.createNewFile()
    }

    private fun startVosSession() {
        val nativeDir = applicationInfo.nativeLibraryDir
        val proot = File(nativeDir, "libproot.so").absolutePath
        val rootfs = File(filesDir, "rootfs").absolutePath
        val tmpDir = File(cacheDir, "proot-tmp").apply { mkdirs() }.absolutePath

        val args = arrayOf(
            "-0",
            "-r", rootfs,
            "-b", "/dev",
            "-b", "/proc",
            "-b", "/sys",
            "-w", "/root",
            "/init"
        )

        val env = arrayOf(
            "HOME=/root",
            "TERM=xterm-256color",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "PROOT_TMP_DIR=$tmpDir"
        )

        session = TerminalSession(proot, rootfs, args, env, TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS, this)
        terminalView.attachSession(session)
    }

    override fun onTextChanged(changedSession: TerminalSession) { terminalView.onScreenUpdated() }
    override fun onTitleChanged(changedSession: TerminalSession) {}
    override fun onSessionFinished(finishedSession: TerminalSession) { finish() }
    override fun onCopyTextToClipboard(session: TerminalSession, text: String) {}
    override fun onPasteTextFromClipboard(session: TerminalSession?) {}
    override fun onBell(session: TerminalSession) {}
    override fun onColorsChanged(session: TerminalSession) {}
    override fun onTerminalCursorStateChange(state: Boolean) {}
    override fun getTerminalCursorStyle(): Int = TerminalEmulator.DEFAULT_TERMINAL_CURSOR_STYLE
    override fun logError(tag: String?, message: String?) {}
    override fun logWarn(tag: String?, message: String?) {}
    override fun logInfo(tag: String?, message: String?) {}
    override fun logDebug(tag: String?, message: String?) {}
    override fun logVerbose(tag: String?, message: String?) {}
    override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {}
    override fun logStackTrace(tag: String?, e: Exception?) {}

    override fun onScale(scale: Float): Float = 1.0f
    override fun onSingleTapUp(e: MotionEvent?) { terminalView.requestFocus() }
    override fun shouldBackButtonBeMappedToEscape(): Boolean = false
    override fun shouldEnforceCharBasedInput(): Boolean = true
    override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
    override fun isTerminalViewSelected(): Boolean = true
    override fun copyModeChanged(copyMode: Boolean) {}
    override fun onKeyDown(keyCode: Int, e: KeyEvent?, session: TerminalSession?): Boolean = false
    override fun onKeyUp(keyCode: Int, e: KeyEvent?): Boolean = false
    override fun onLongPress(event: MotionEvent?): Boolean = false
    override fun readControlKey(): Boolean = false
    override fun readAltKey(): Boolean = false
    override fun readShiftKey(): Boolean = false
    override fun readFnKey(): Boolean = false
    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession?): Boolean = false
    override fun onEmulatorSet() {}
}
