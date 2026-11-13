package com.novafiles.feature.ftp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder

object HttpFileShareServer {

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _port = MutableStateFlow(2122)
    val port: StateFlow<Int> = _port.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    fun start(rootPath: String, httpPort: Int = 2122, scope: CoroutineScope) {
        if (_isRunning.value) return
        val sandboxRoot = File(rootPath).canonicalFile
        serverJob = scope.launch(Dispatchers.IO) {
            try {
                val server = ServerSocket(httpPort)
                serverSocket = server
                _port.value = httpPort
                _isRunning.value = true

                while (isActive && !server.isClosed) {
                    try {
                        val client = server.accept()
                        client.soTimeout = 30_000
                        scope.launch(Dispatchers.IO) {
                            handleHttpClient(client, sandboxRoot)
                        }
                    } catch (_: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (_: Exception) {
                stop()
            }
        }
    }

    fun stop() {
        runCatching { serverSocket?.close() }
        serverSocket = null
        serverJob?.cancel()
        serverJob = null
        _isRunning.value = false
    }

    private fun handleHttpClient(socket: Socket, sandboxRoot: File) {
        try {
            socket.use { s ->
                val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                val out = s.getOutputStream()
                val requestLine = reader.readLine() ?: return
                val parts = requestLine.split(" ")
                if (parts.size < 2 || parts[0] != "GET") {
                    writeTextResponse(out, "405 Method Not Allowed", "text/plain", "Method Not Allowed")
                    return
                }

                val rawPath = parts[1].substringBefore("?")
                val decoded = URLDecoder.decode(rawPath, "UTF-8").trimStart('/')
                val target = if (decoded.isEmpty()) sandboxRoot else File(sandboxRoot, decoded)

                if (!FtpServerEngine.isWithinSandbox(target, sandboxRoot) || !target.exists()) {
                    writeTextResponse(out, "404 Not Found", "text/plain", "Not Found")
                    return
                }

                if (target.isDirectory) {
                    val html = buildDirectoryHtml(target, sandboxRoot, rawPath)
                    writeTextResponse(out, "200 OK", "text/html; charset=utf-8", html)
                } else {
                    val header = buildString {
                        append("HTTP/1.1 200 OK\r\n")
                        append("Content-Type: application/octet-stream\r\n")
                        append("Content-Disposition: attachment; filename=\"${target.name.replace("\"", "")}\"\r\n")
                        append("Content-Length: ${target.length()}\r\n")
                        append("Connection: close\r\n\r\n")
                    }
                    out.write(header.toByteArray(Charsets.UTF_8))
                    FileInputStream(target).use { fis ->
                        fis.copyTo(out, bufferSize = 32 * 1024)
                    }
                    out.flush()
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun writeTextResponse(out: OutputStream, status: String, contentType: String, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $status\r\nContent-Type: $contentType\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    private fun buildDirectoryHtml(dir: File, root: File, currentUri: String): String {
        val rel = if (dir == root) "/" else "/${dir.relativeTo(root).path.replace(File.separatorChar, '/')}"
        val children = dir.listFiles()?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() }) ?: emptyList()
        val rows = buildString {
            if (dir != root) {
                val parentRel = dir.parentFile?.let {
                    if (it == root) "/" else "/${it.relativeTo(root).path.replace(File.separatorChar, '/')}"
                } ?: "/"
                append("<li><a href=\"$parentRel\">.. (Parent Directory)</a></li>")
            }
            for (f in children) {
                val childRel = f.relativeTo(root).path.replace(File.separatorChar, '/')
                val encoded = childRel.split('/').joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
                val icon = if (f.isDirectory) "📁" else "📄"
                val size = if (f.isDirectory) "Folder" else "${f.length() / 1024} KB"
                append("<li><a href=\"/$encoded\">$icon ${escapeHtml(f.name)}</a><span>$size</span></li>")
            }
        }
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1" />
              <title>NovaFiles Browser Share — ${escapeHtml(rel)}</title>
              <style>
                body { font-family: system-ui, sans-serif; background: #0B1226; color: #F8FAFC; margin: 0; padding: 24px; }
                h2 { color: #5B6EF5; margin-bottom: 4px; }
                p { color: #94A3B8; margin-top: 0; }
                ul { list-style: none; padding: 0; max-width: 780px; }
                li { display: flex; justify-content: space-between; padding: 12px 14px; margin-bottom: 8px; background: #111B3A; border: 1px solid #2A3A6A; border-radius: 10px; }
                a { color: #E2E8F0; text-decoration: none; font-weight: 500; word-break: break-all; }
                a:hover { color: #2DD4BF; }
                span { color: #94A3B8; font-size: 13px; margin-left: 12px; white-space: nowrap; }
              </style>
            </head>
            <body>
              <h2>NovaFiles Browser Share</h2>
              <p>Path: ${escapeHtml(rel)}</p>
              <ul>$rows</ul>
            </body>
            </html>
        """.trimIndent()
    }

    private fun escapeHtml(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
