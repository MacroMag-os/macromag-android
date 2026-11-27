package org.macromag.os

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.zip.GZIPInputStream

class MainActivity : AppCompatActivity() {
    private lateinit var outputView: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var inputBox: EditText
    private var shellWriter: OutputStreamWriter? = null
    private var shellProcess: Process? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D1117"))
        }

        val header = TextView(this).apply {
            text = "  MacroMag OS (ARM64 VOS Terminal)"
            setTextColor(Color.parseColor("#58A6FF"))
            setBackgroundColor(Color.parseColor("#161B22"))
            typeface = Typeface.MONOSPACE
            textSize = 14f
            setPadding(16, 24, 16, 24)
        }
        root.addView(header)

        scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            )
            setPadding(20, 16, 20, 16)
        }

        outputView = TextView(this).apply {
            setTextColor(Color.parseColor("#E6EDF3"))
            typeface = Typeface.MONOSPACE
            textSize = 13f
            setTextIsSelectable(true)
            text = "Inicjalizacja MacroMag OS rootfs...\n"
        }
        scrollView.addView(outputView)
        root.addView(scrollView)

        val shortcutScroll = HorizontalScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#161B22"))
        }
        val shortcutBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(8, 8, 8, 8)
        }

        val shortcuts = listOf(
            "sys.cpu()" to "sys.cpu()\n",
            "disk.list()" to "disk.list()\n",
            "mur sync" to "mur sync\n",
            "mur list" to "mur list\n",
            "ls -la" to "ls -la\n",
            "Ctrl+C" to "\u0003"
        )

        for ((label, cmd) in shortcuts) {
            val btn = Button(this).apply {
                text = label
                textSize = 11f
                isAllCaps = false
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#21262D"))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(6, 0, 6, 0)
                layoutParams = lp
                setOnClickListener { sendCommand(cmd) }
            }
            shortcutBar.addView(btn)
        }
        shortcutScroll.addView(shortcutBar)
        root.addView(shortcutScroll)

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#161B22"))
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 12, 16, 16)
        }

        val prompt = TextView(this).apply {
            text = "vos> "
            setTextColor(Color.parseColor("#3FB950"))
            typeface = Typeface.MONOSPACE
            textSize = 14f
        }
        inputRow.addView(prompt)

        inputBox = EditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            hint = "Wpisz komende VOS lub Linux..."
            typeface = Typeface.MONOSPACE
            textSize = 14f
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_SEND
            setBackgroundColor(Color.TRANSPARENT)
            setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    val cmd = text.toString()
                    setText("")
                    appendOutput("vos> $cmd\n")
                    sendCommand("$cmd\n")
                    true
                } else {
                    false
                }
            }
        }
        inputRow.addView(inputBox)

        val sendBtn = Button(this).apply {
            text = "Run"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#238636"))
            setOnClickListener {
                val cmd = inputBox.text.toString()
                inputBox.setText("")
                appendOutput("vos> $cmd\n")
                sendCommand("$cmd\n")
            }
        }
        inputRow.addView(sendBtn)
        root.addView(inputRow)

        setContentView(root)

        Thread { bootMacroMag() }.start()
    }

    private fun appendOutput(text: String) {
        runOnUiThread {
            outputView.append(text)
            scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun sendCommand(cmd: String) {
        Thread {
            try {
                shellWriter?.write(cmd)
                shellWriter?.flush()
            } catch (e: Exception) {
                appendOutput("[Blad wysylania]: ${e.message}\n")
            }
        }.start()
    }

    private fun bootMacroMag() {
        val rootfsDir = File(filesDir, "rootfs")
        val marker = File(rootfsDir, ".installed")

        if (!marker.exists()) {
            appendOutput("Rozpakowywanie systemu MacroMag OS (arm64)...\n")
            rootfsDir.mkdirs()
            try {
                extractTarGz("macromag-arm64-rootfs.tar.gz", rootfsDir)
                marker.createNewFile()
                appendOutput("Gotowe! Uruchamianie powloki...\n\n")
            } catch (e: Exception) {
                appendOutput("Blad rozpakowywania rootfs: ${e.message}\n")
                return
            }
        } else {
            appendOutput("System MacroMag OS gotowy.\n\n")
        }

        val nativeDir = applicationInfo.nativeLibraryDir
        val prootBin = File(nativeDir, "libproot.so")
        val busyboxBin = File(nativeDir, "libbusybox.so")
        val tmpDir = File(cacheDir, "proot_tmp").apply { mkdirs() }

        val initBin = when {
            File(rootfsDir, "init").exists() -> "/init"
            File(rootfsDir, "usr/bin/vos").exists() -> "/usr/bin/vos"
            File(rootfsDir, "bin/sh").exists() -> "/bin/sh"
            else -> "/bin/sh"
        }

        try {
            val cmd = if (prootBin.exists()) {
                listOf(
                    prootBin.absolutePath,
                    "-r", rootfsDir.absolutePath,
                    "-0",
                    "-w", "/root",
                    "-b", "/dev",
                    "-b", "/proc",
                    "-b", "/sys",
                    initBin
                )
            } else if (busyboxBin.exists()) {
                listOf(busyboxBin.absolutePath, "sh")
            } else {
                listOf("/system/bin/sh")
            }

            val pb = ProcessBuilder(cmd)
            pb.environment()["PROOT_TMP_DIR"] = tmpDir.absolutePath
            pb.environment()["HOME"] = "/root"
            pb.environment()["TERM"] = "xterm-256color"
            pb.environment()["PATH"] = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
            pb.redirectErrorStream(true)

            val proc = pb.start()
            shellProcess = proc
            shellWriter = OutputStreamWriter(proc.outputStream)

            val reader = proc.inputStream.bufferedReader()
            val buf = CharArray(1024)
            var len: Int
            while (reader.read(buf).also { len = it } != -1) {
                appendOutput(String(buf, 0, len))
            }
            val code = proc.waitFor()
            appendOutput("\n[Proces zakonczony z kodem: $code]\n")
        } catch (e: Exception) {
            appendOutput("Blad uruchamiania MacroMag OS: ${e.message}\n")
        }
    }

    private fun extractTarGz(assetName: String, destDir: File) {
        assets.open(assetName).use { rawIn ->
            GZIPInputStream(BufferedInputStream(rawIn)).use { gzIn ->
                val header = ByteArray(512)
                while (true) {
                    var readTotal = 0
                    while (readTotal < 512) {
                        val r = gzIn.read(header, readTotal, 512 - readTotal)
                        if (r <= 0) return
                        readTotal += r
                    }
                    if (header.all { it == 0.toByte() }) return

                    val name = String(header, 0, 100).trim { it <= ' ' || it == '\u0000' }
                    val sizeStr = String(header, 124, 12).trim { it <= ' ' || it == '\u0000' }
                    val size = if (sizeStr.isNotEmpty()) sizeStr.toLongOrNull(8) ?: 0L else 0L
                    val typeFlag = header[156].toInt().toChar()

                    val outFile = File(destDir, name)
                    if (typeFlag == '5' || name.endsWith("/")) {
                        outFile.mkdirs()
                    } else if (typeFlag == '0' || typeFlag == '\u0000') {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            var remaining = size
                            val buf = ByteArray(8192)
                            while (remaining > 0) {
                                val toRead = minOf(buf.size.toLong(), remaining).toInt()
                                val r = gzIn.read(buf, 0, toRead)
                                if (r <= 0) break
                                fos.write(buf, 0, r)
                                remaining -= r
                            }
                        }
                        outFile.setExecutable(true, false)
                    } else {
                        var remaining = size
                        val buf = ByteArray(8192)
                        while (remaining > 0) {
                            val toRead = minOf(buf.size.toLong(), remaining).toInt()
                            val r = gzIn.read(buf, 0, toRead)
                            if (r <= 0) break
                            remaining -= r
                        }
                    }

                    val padding = (512 - (size % 512)) % 512
                    var skipRem = padding
                    while (skipRem > 0) {
                        val s = gzIn.skip(skipRem)
                        if (s <= 0) break
                        skipRem -= s
                    }
                }
            }
        }
    }
}
