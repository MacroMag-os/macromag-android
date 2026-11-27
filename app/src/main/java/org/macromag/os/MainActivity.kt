package org.macromag.os

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var terminalOutput: TextView
    private lateinit var terminalScroll: ScrollView
    private lateinit var commandInput: EditText
    private lateinit var rootfsDir: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        terminalOutput = findViewById(R.id.terminalOutput)
        terminalScroll = findViewById(R.id.terminalScroll)
        commandInput = findViewById(R.id.commandInput)
        val runButton: Button = findViewById(R.id.runButton)

        rootfsDir = File(filesDir, "rootfs")

        appendLine("Initializing MacroMag OS rootfs...")
        Thread {
            setupRootfs()
        }.start()

        runButton.setOnClickListener {
            val cmd = commandInput.text.toString().trim()
            if (cmd.isNotEmpty()) {
                commandInput.text.clear()
                executeCommand(cmd)
            }
        }

        setupShortcut(R.id.btnSysCpu, "sys.cpu()")
        setupShortcut(R.id.btnDiskList, "disk.list()")
        setupShortcut(R.id.btnMurSync, "mur sync")
        setupShortcut(R.id.btnMurList, "mur list")
        setupShortcut(R.id.btnLs, "ls -la")
    }

    private fun setupShortcut(buttonId: Int, command: String) {
        findViewById<Button>(buttonId).setOnClickListener {
            executeCommand(command)
        }
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
