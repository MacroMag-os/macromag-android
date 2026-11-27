package org.macromag.os

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : Activity() {

    private lateinit var terminalOutput: TextView
    private lateinit var terminalScroll: ScrollView
    private lateinit var commandInput: EditText
    private lateinit var rootfsDir: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D1117"))
            setPadding(24, 24, 24, 24)
        }

        val headerText = TextView(this).apply {
            text = "MacroMag OS — ARM64 VOS Terminal"
            setTextColor(Color.parseColor("#58A6FF"))
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }
        rootLayout.addView(headerText)

        terminalOutput = TextView(this).apply {
            setTextColor(Color.parseColor("#3FB950"))
            textSize = 13f
            typeface = Typeface.MONOSPACE
        }

        terminalScroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#010409"))
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            )
            addView(terminalOutput)
        }
        rootLayout.addView(terminalScroll)

        val shortcutBar = HorizontalScrollView(this).apply {
            setPadding(0, 12, 0, 12)
        }
        val shortcutRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val shortcuts = listOf("sys.cpu()", "disk.list()", "mur sync", "mur list", "ls -la")
        for (sc in shortcuts) {
            val btn = Button(this).apply {
                text = sc
                textSize = 11f
                isAllCaps = false
                setOnClickListener { executeCommand(sc) }
            }
            shortcutRow.addView(btn)
        }
        shortcutBar.addView(shortcutRow)
        rootLayout.addView(shortcutBar)

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        commandInput = EditText(this).apply {
            hint = "Enter VOS or Linux command..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#8B949E"))
            typeface = Typeface.MONOSPACE
            isSingleLine = true
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
            )
        }

        val runButton = Button(this).apply {
            text = "RUN"
            setOnClickListener {
                val cmd = commandInput.text.toString().trim()
                if (cmd.isNotEmpty()) {
                    commandInput.text.clear()
                    executeCommand(cmd)
                }
            }
        }

        inputRow.addView(commandInput)
        inputRow.addView(runButton)
        rootLayout.addView(inputRow)

        setContentView(rootLayout)

        rootfsDir = File(filesDir, "rootfs")
        appendLine("Initializing MacroMag OS rootfs...")
        Thread {
            setupRootfs()
        }.start()
    }

    private fun setupRootfs() {
        val marker = File(rootfsDir, ".initialized")
        if (!marker.exists()) {
            appendLine("Extracting MacroMag OS rootfs (arm64)...")
            try {
                rootfsDir.mkdirs()
                val assetList = assets.list("") ?: emptyArray()
                val tarName = assetList.firstOrNull { it.endsWith(".tar") || it.endsWith(".tar.gz") }
                    ?: throw Exception("No rootfs archive found in assets: ${assetList.joinToString()}")

                val rawStream: InputStream = assets.open(tarName)
                extractTar(BufferedInputStream(rawStream), rootfsDir)
                marker.createNewFile()
                appendLine("MacroMag OS (ARM64) ready! Type a command below.\n")
            } catch (e: Exception) {
                appendLine("Rootfs extraction error: ${e.message}\n")
            }
        } else {
            appendLine("MacroMag OS (ARM64) loaded from cache.\n")
        }
    }

    private fun extractTar(input: InputStream, destDir: File) {
        val header = ByteArray(512)
        while (true) {
            var readTotal = 0
            while (readTotal < 512) {
                val r = input.read(header, readTotal, 512 - readTotal)
                if (r <= 0) return
                readTotal += r
            }
            if (header.all { it == 0.toByte() }) break

            val name = String(header, 0, 100).trim { it <= ' ' || it == '\u0000' }
            val sizeStr = String(header, 124, 12).trim { it <= ' ' || it == '\u0000' }
            val size = if (sizeStr.isEmpty()) 0L else sizeStr.toLongOrNull(8) ?: 0L
            val typeByte = header.get(156).toInt() and 0xFF

            val cleanName = name.removePrefix("./")
            if (cleanName.isNotEmpty() && !cleanName.contains("..")) {
                val outFile = File(destDir, cleanName)
                if (typeByte == 53 || name.endsWith("/")) {
                    outFile.mkdirs()
                } else if (typeByte == 48 || typeByte == 0) {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        var remaining = size
                        val buf = ByteArray(8192)
                        while (remaining > 0) {
                            val toRead = minOf(buf.size.toLong(), remaining).toInt()
                            val n = input.read(buf, 0, toRead)
                            if (n <= 0) break
                            fos.write(buf, 0, n)
                            remaining -= n
                        }
                    }
                    outFile.setExecutable(true, false)
                } else {
                    var remaining = size
                    while (remaining > 0) {
                        val skipped = input.skip(remaining)
                        if (skipped <= 0) break
                        remaining -= skipped
                    }
                }
            }

            val padding = ((512 - (size % 512)) % 512)
            var padRemaining = padding
            while (padRemaining > 0) {
                val skipped = input.skip(padRemaining)
                if (skipped <= 0) break
                padRemaining -= skipped
            }
        }
    }

    private fun executeCommand(cmd: String) {
        appendLine("vos> $cmd")
        Thread {
            try {
                val busybox = File(rootfsDir, "bin/busybox")
                val vosBin = File(rootfsDir, "bin/vos")
                val shBin = if (busybox.exists()) busybox.absolutePath else "/system/bin/sh"

                val env = arrayOf(
                    "PATH=${rootfsDir.absolutePath}/bin:${rootfsDir.absolutePath}/usr/bin:/system/bin",
                    "HOME=${rootfsDir.absolutePath}/root",
                    "ROOTFS=${rootfsDir.absolutePath}"
                )

                val fullCmd = if (vosBin.exists() && (cmd.startsWith("sys.") || cmd.startsWith("disk.") || cmd.startsWith("mur"))) {
                    "${vosBin.absolutePath} -c \"$cmd\""
                } else {
                    "$shBin sh -c \"cd ${rootfsDir.absolutePath} && $cmd\""
                }

                val process = Runtime.getRuntime().exec(arrayOf("/system/bin/sh", "-c", fullCmd), env, rootfsDir)
                val output = process.inputStream.bufferedReader().readText()
                val error = process.errorStream.bufferedReader().readText()
                process.waitFor()

                if (output.isNotEmpty()) appendLine(output.trimEnd())
                if (error.isNotEmpty()) appendLine(error.trimEnd())
            } catch (e: Exception) {
                appendLine("Error: ${e.message}")
            }
        }.start()
    }

    private fun appendLine(text: String) {
        runOnUiThread {
            terminalOutput.append("$text\n")
            terminalScroll.post {
                terminalScroll.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }
}
