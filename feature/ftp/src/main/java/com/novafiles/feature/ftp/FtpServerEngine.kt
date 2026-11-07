package com.novafiles.feature.ftp

import android.os.Environment
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
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FtpServerConfig(
    val port: Int = 2121,
    val username: String = "novafiles",
    val password: String = generateSecurePassword(),
    val isAnonymousAllowed: Boolean = false,
    val rootPath: String = "/storage/emulated/0",
    val isReadOnly: Boolean = true
) {
    companion object {
        fun generateSecurePassword(): String {
            val chars = "abcdefghjkmnpqrstuvwxyz23456789"
            val random = SecureRandom()
            return (1..8).map { chars[random.nextInt(chars.length)] }.joinToString("")
        }
    }
}

data class FtpServerState(
    val isRunning: Boolean = false,
    val ipAddress: String = "0.0.0.0",
    val port: Int = 2121,
    val activeClientsCount: Int = 0,
    val logs: List<String> = emptyList()
)

object FtpServerEngine {

    private val _serverState = MutableStateFlow(FtpServerState())
    val serverState: StateFlow<FtpServerState> = _serverState.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val activeClients = mutableListOf<Socket>()

    private const val MAX_ACTIVE_CLIENTS = 5
    private const val CLIENT_TIMEOUT_MS = 60_000

    fun isWithinSandbox(target: File, sandboxRoot: File): Boolean {
        return try {
            val canonicalTarget = target.canonicalFile
            val canonicalRoot = sandboxRoot.canonicalFile
            canonicalTarget == canonicalRoot || canonicalTarget.path.startsWith(canonicalRoot.path + File.separator)
        } catch (_: Exception) {
            false
        }
    }

    fun start(config: FtpServerConfig, scope: CoroutineScope) {
        if (_serverState.value.isRunning) return

        val ip = getLocalIpAddress()
        serverJob = scope.launch(Dispatchers.IO) {
            try {
                val server = ServerSocket(config.port)
                serverSocket = server
                addLog("FTP Server started at ftp://$ip:${config.port}")
                addLog("Root Directory: ${config.rootPath}")
                if (config.isReadOnly) {
                    addLog("Security policy: Server is in READ-ONLY mode.")
                }
                if (!config.isAnonymousAllowed) {
                    addLog("Authentication: User '${config.username}'. Anonymous login disabled.")
                }

                _serverState.value = _serverState.value.copy(
                    isRunning = true,
                    ipAddress = ip,
                    port = config.port
                )

                while (isActive && !server.isClosed) {
                    try {
                        val clientSocket = server.accept()
                        clientSocket.soTimeout = CLIENT_TIMEOUT_MS

                        synchronized(activeClients) {
                            if (activeClients.size >= MAX_ACTIVE_CLIENTS) {
                                clientSocket.close()
                                addLog("Rejected connection: max active clients reached ($MAX_ACTIVE_CLIENTS)")
                                return@synchronized
                            }
                            activeClients.add(clientSocket)
                        }

                        _serverState.value = _serverState.value.copy(activeClientsCount = activeClients.size)
                        addLog("Client connected: ${clientSocket.inetAddress.hostAddress}")

                        scope.launch(Dispatchers.IO) {
                            try {
                                handleClient(clientSocket, config)
                            } finally {
                                runCatching { clientSocket.close() }
                                synchronized(activeClients) { activeClients.remove(clientSocket) }
                                _serverState.value = _serverState.value.copy(activeClientsCount = activeClients.size)
                            }
                        }
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                addLog("Error starting FTP server: ${e.message}")
                stop()
            }
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
            serverSocket = null
            synchronized(activeClients) {
                activeClients.forEach { runCatching { it.close() } }
                activeClients.clear()
            }
            serverJob?.cancel()
            serverJob = null
            addLog("FTP Server stopped.")
            _serverState.value = _serverState.value.copy(
                isRunning = false,
                activeClientsCount = 0
            )
        } catch (_: Exception) {}
    }

    private fun addLog(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = "[$time] $msg"
        val updated = (_serverState.value.logs + entry).takeLast(60)
        _serverState.value = _serverState.value.copy(logs = updated)
    }

    private fun handleClient(clientSocket: Socket, config: FtpServerConfig) {
        val sandboxRoot = File(config.rootPath).canonicalFile
        var currentDir = sandboxRoot
        var isAuthenticated = false
        var currentUsername = ""
        var passiveDataServer: ServerSocket? = null

        try {
            val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
            val writer = PrintWriter(OutputStreamWriter(clientSocket.getOutputStream()), true)

            writer.println("220 NovaFiles 2.0 Secure FTP Server Ready")

            while (!clientSocket.isClosed) {
                val line = reader.readLine() ?: break
                val parts = line.trim().split(" ", limit = 2)
                val cmd = parts[0].uppercase(Locale.ROOT)
                val arg = if (parts.size > 1) parts[1].trim() else ""

                when (cmd) {
                    "USER" -> {
                        currentUsername = arg
                        if (config.isAnonymousAllowed && arg.equals("anonymous", ignoreCase = true)) {
                            isAuthenticated = true
                            writer.println("230 Anonymous user logged in")
                        } else {
                            writer.println("331 Password required for $arg")
                        }
                    }
                    "PASS" -> {
                        if (currentUsername == config.username && arg == config.password) {
                            isAuthenticated = true
                            writer.println("230 User logged in successfully")
                            addLog("User '$currentUsername' authenticated successfully")
                        } else if (config.isAnonymousAllowed && currentUsername.equals("anonymous", ignoreCase = true)) {
                            isAuthenticated = true
                            writer.println("230 Anonymous user logged in")
                        } else {
                            isAuthenticated = false
                            writer.println("530 Login incorrect")
                            addLog("Failed login attempt for user '$currentUsername'")
                        }
                    }
                    "QUIT" -> {
                        writer.println("221 Goodbye")
                        break
                    }
                    "NOOP" -> writer.println("200 OK")
                    "SYST" -> writer.println("215 UNIX Type: L8")
                    "FEAT" -> {
                        writer.println("211-Features:")
                        writer.println(" UTF8")
                        writer.println(" SIZE")
                        writer.println(" EPSV")
                        writer.println(" PASV")
                        writer.println("211 End")
                    }
                    else -> {
                        if (!isAuthenticated) {
                            writer.println("530 Please login with USER and PASS")
                            continue
                        }

                        when (cmd) {
                            "PWD" -> {
                                val relPath = if (currentDir == sandboxRoot) "/" else currentDir.relativeTo(sandboxRoot).path
                                writer.println("257 \"/$relPath\" is current directory")
                            }
                            "TYPE" -> writer.println("200 Type set to $arg")
                            "PASV" -> {
                                passiveDataServer?.close()
                                passiveDataServer = ServerSocket(0)
                                val port = passiveDataServer.localPort
                                val ipParts = getLocalIpAddress().split(".")
                                if (ipParts.size == 4) {
                                    val p1 = port / 256
                                    val p2 = port % 256
                                    val pasvIp = ipParts.joinToString(",")
                                    writer.println("227 Entering Passive Mode ($pasvIp,$p1,$p2)")
                                } else {
                                    writer.println("425 Can't open passive connection")
                                }
                            }
                            "EPSV" -> {
                                passiveDataServer?.close()
                                passiveDataServer = ServerSocket(0)
                                val port = passiveDataServer.localPort
                                writer.println("229 Entering Extended Passive Mode (|||$port|)")
                            }
                            "CWD" -> {
                                val target = if (arg.startsWith("/")) {
                                    File(sandboxRoot, arg.trimStart('/'))
                                } else {
                                    File(currentDir, arg)
                                }

                                if (isWithinSandbox(target, sandboxRoot) && target.exists() && target.isDirectory) {
                                    currentDir = target.canonicalFile
                                    writer.println("250 Directory changed successfully")
                                } else {
                                    writer.println("550 Access denied or directory does not exist")
                                }
                            }
                            "CDUP" -> {
                                if (currentDir == sandboxRoot) {
                                    writer.println("550 Already at root directory")
                                } else {
                                    val parent = currentDir.parentFile
                                    if (parent != null && isWithinSandbox(parent, sandboxRoot) && parent.exists()) {
                                        currentDir = parent.canonicalFile
                                        writer.println("200 Directory changed successfully")
                                    } else {
                                        writer.println("550 Cannot move above root")
                                    }
                                }
                            }
                            "LIST" -> {
                                writer.println("150 Opening ASCII mode data connection")
                                val dataSocket = passiveDataServer?.accept()
                                dataSocket?.use { ds ->
                                    val dataOut = PrintWriter(OutputStreamWriter(ds.getOutputStream()), true)
                                    val files = currentDir.listFiles() ?: emptyArray()
                                    val sdf = SimpleDateFormat("MMM dd HH:mm", Locale.US)
                                    for (f in files) {
                                        val perms = if (f.isDirectory) "drwxr-xr-x" else "-rw-r--r--"
                                        val size = if (f.isDirectory) 4096 else f.length()
                                        val dateStr = sdf.format(Date(f.lastModified()))
                                        dataOut.println("$perms 1 owner group $size $dateStr ${f.name}")
                                    }
                                }
                                passiveDataServer?.close()
                                passiveDataServer = null
                                writer.println("226 Transfer complete")
                            }
                            "SIZE" -> {
                                val target = File(currentDir, arg)
                                if (isWithinSandbox(target, sandboxRoot) && target.exists() && target.isFile) {
                                    writer.println("213 ${target.length()}")
                                } else {
                                    writer.println("550 File not found or access denied")
                                }
                            }
                            "RETR" -> {
                                val target = File(currentDir, arg)
                                if (isWithinSandbox(target, sandboxRoot) && target.exists() && target.isFile) {
                                    writer.println("150 Opening binary mode data connection")
                                    val dataSocket = passiveDataServer?.accept()
                                    dataSocket?.use { ds ->
                                        FileInputStream(target).use { fis ->
                                            fis.copyTo(ds.getOutputStream(), bufferSize = 16 * 1024)
                                        }
                                    }
                                    passiveDataServer?.close()
                                    passiveDataServer = null
                                    writer.println("226 Transfer complete")
                                } else {
                                    writer.println("550 File not found or access denied")
                                }
                            }
                            "STOR" -> {
                                if (config.isReadOnly) {
                                    writer.println("550 Permission denied: Server is configured as Read-Only")
                                    passiveDataServer?.close()
                                    passiveDataServer = null
                                } else {
                                    val target = File(currentDir, arg)
                                    if (!isWithinSandbox(target, sandboxRoot)) {
                                        writer.println("550 Path traversal prohibited")
                                        passiveDataServer?.close()
                                        passiveDataServer = null
                                    } else {
                                        writer.println("150 Opening binary mode data connection for upload")
                                        val dataSocket = passiveDataServer?.accept()
                                        dataSocket?.use { ds ->
                                            FileOutputStream(target).use { fos ->
                                                ds.getInputStream().copyTo(fos, bufferSize = 16 * 1024)
                                            }
                                        }
                                        passiveDataServer?.close()
                                        passiveDataServer = null
                                        writer.println("226 File stored successfully")
                                    }
                                }
                            }
                            "DELE" -> {
                                if (config.isReadOnly) {
                                    writer.println("550 Permission denied: Server is Read-Only")
                                } else {
                                    val target = File(currentDir, arg)
                                    if (isWithinSandbox(target, sandboxRoot) && target.exists() && target.isFile) {
                                        if (target.delete()) {
                                            writer.println("250 File deleted successfully")
                                        } else {
                                            writer.println("450 Failed to delete file")
                                        }
                                    } else {
                                        writer.println("550 File not found or access denied")
                                    }
                                }
                            }
                            else -> writer.println("502 Command not implemented")
                        }
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            passiveDataServer?.close()
        }
    }

    fun getLocalIpAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address.hostAddress ?: "127.0.0.1"
                    }
                }
            }
            "127.0.0.1"
        } catch (_: Exception) {
            "127.0.0.1"
        }
    }
}
