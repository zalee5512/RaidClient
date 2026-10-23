using System;
using System.Diagnostics;
using System.IO;
using System.Threading.Tasks;
using RaidLauncher.Models;

namespace RaidLauncher.Services
{
    public class GameLaunchService
    {
        public static event Action<string>? StatusChanged;
        public static event Action<Process>? GameStarted;
        public static event Action<int, string>? ProgressChanged;
        public static event Action? GameExited;

        // Versión de Forge 1.8.9 predeterminada
        private const string DefaultForgeVersion = "1.8.9-11.15.1.2318-1.8.9";
        private const string McVersion = "1.8.9";

        public static async Task LaunchGameAsync(LauncherConfig config, Account account)
        {
            await Task.Run(async () =>
            {
                try
                {
                    ProgressChanged?.Invoke(15, "Verificando archivos de Raid Client...");
                    StatusChanged?.Invoke("Verificando archivos de Raid Client...");
                    await Task.Delay(250);

                    string mcDir = GameInstallerService.GetGameDir();
                    string clientDir = @"C:\Programacion\raid-client";

                    // ─── 1. Asegurar y descargar automáticamente todos los archivos de Forge, Minecraft y librerías ───────
                    bool filesReady = await GameInstallerService.EnsureAllGameFilesAsync(
                        mcDir,
                        (pct, msg) => ProgressChanged?.Invoke(pct, msg),
                        (msg) => StatusChanged?.Invoke(msg)
                    );

                    if (!filesReady)
                    {
                        StatusChanged?.Invoke("Error al verificar archivos. Comprueba tu conexión a internet.");
                        ProgressChanged?.Invoke(0, "Error de red.");
                        return;
                    }

                    // En entorno de desarrollo, sincronizar con la compilación más reciente si existe
                    if (Directory.Exists(clientDir))
                    {
                        string compiledJar = Path.Combine(clientDir, "build", "libs", "RaidClient-1.0.jar");
                        string targetJar = Path.Combine(mcDir, "mods", "RaidClient-1.0.jar");
                        if (File.Exists(compiledJar))
                        {
                            try { File.Copy(compiledJar, targetJar, true); } catch { }
                        }
                    }

                    // ─── 2. Determinar cuenta ────────────────────────────────────────────────
                    string username = account.Username;
                    string accessToken = account.IsPremium ? account.AccessToken : "0";
                    string uuid = account.IsPremium ? account.Uuid : GuidFromName(username);

                    // ─── 3. Buscar o descargar Java 8 automáticamente ─────────────────────────
                    ProgressChanged?.Invoke(92, "Configurando motor de Java 8...");
                    await Task.Delay(150);

                    string? javaExe = null;
                    if (!string.IsNullOrWhiteSpace(config.JavaPath) && File.Exists(config.JavaPath))
                    {
                        javaExe = config.JavaPath;
                    }
                    else
                    {
                        javaExe = await GameInstallerService.EnsureJavaRuntimeAsync(
                            mcDir,
                            (pct, msg) => ProgressChanged?.Invoke(pct, msg),
                            (msg) => StatusChanged?.Invoke(msg)
                        );
                    }

                    if (string.IsNullOrEmpty(javaExe) || !File.Exists(javaExe))
                    {
                        StatusChanged?.Invoke("ERROR: No se encontró Java. Instala Java 8 o configúralo en Ajustes.");
                        ProgressChanged?.Invoke(0, "Error: Java no encontrado.");
                        return;
                    }

                    // ─── 4. Construir argumentos y arrancar Minecraft ────────────────────────
                    string activeForge = GameInstallerService.ForgeVersionId;
                    StatusChanged?.Invoke($"Iniciando Minecraft como {username}...");
                    ProgressChanged?.Invoke(97, $"Iniciando motor de Minecraft para {username}...");
                    await Task.Delay(150);

                    string args = BuildJavaArgs(config, mcDir, username, accessToken, uuid, activeForge);

                    string appDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".raidlauncher");
                    string logFile = Path.Combine(appDir, "minecraft_output.log");
                    StreamWriter? logWriter = null;
                    try { logWriter = new StreamWriter(logFile, false) { AutoFlush = true }; } catch { }

                    ProcessStartInfo psi = new ProcessStartInfo
                    {
                        FileName = javaExe,
                        Arguments = args,
                        WorkingDirectory = mcDir,
                        UseShellExecute = false,
                        CreateNoWindow = true,
                        RedirectStandardOutput = true,
                        RedirectStandardError = true,
                        WindowStyle = ProcessWindowStyle.Hidden
                    };

                    ProgressChanged?.Invoke(90, "Cargando Forge y módulos de Raid Client...");
                    var proc = Process.Start(psi);
                    if (proc != null)
                    {
                        proc.OutputDataReceived += (s, e) => { if (e.Data != null) logWriter?.WriteLine(e.Data); };
                        proc.ErrorDataReceived += (s, e) => { if (e.Data != null) logWriter?.WriteLine(e.Data); };
                        proc.BeginOutputReadLine();
                        proc.BeginErrorReadLine();

                        proc.EnableRaisingEvents = true;
                        proc.Exited += (s, e) =>
                        {
                            try { logWriter?.Flush(); logWriter?.Close(); } catch { }
                            GameExited?.Invoke();
                        };

                        GameStarted?.Invoke(proc);
                        ProgressChanged?.Invoke(100, $"¡Raid Client iniciado como {username}!");
                        StatusChanged?.Invoke($"¡Raid Client iniciado como {username}! Presiona SHIFT DERECHO en el juego.");
                    }
                    else
                    {
                        StatusChanged?.Invoke("No se pudo iniciar el proceso de Minecraft.");
                        ProgressChanged?.Invoke(0, "Error al iniciar proceso.");
                    }
                }
                catch (Exception ex)
                {
                    StatusChanged?.Invoke("Error al iniciar: " + ex.Message);
                    ProgressChanged?.Invoke(0, "Error: " + ex.Message);
                }
            });
        }

        // ─── Helpers ─────────────────────────────────────────────────────────────────

        private static string? FindForgeVersion(string versionsDir)
        {
            if (!Directory.Exists(versionsDir)) return null;

            if (Directory.Exists(Path.Combine(versionsDir, DefaultForgeVersion)))
                return DefaultForgeVersion;

            foreach (var dir in Directory.GetDirectories(versionsDir))
            {
                string name = Path.GetFileName(dir);
                if (name.IndexOf("1.8.9", StringComparison.OrdinalIgnoreCase) >= 0 &&
                    name.IndexOf("forge", StringComparison.OrdinalIgnoreCase) >= 0)
                {
                    return name;
                }
            }

            return null;
        }

        private static async Task<bool> InstallForgeAsync(string javaExe, string mcDir)
        {
            try
            {
                string installerUrl = "https://maven.minecraftforge.net/net/minecraftforge/forge/1.8.9-11.15.1.2318-1.8.9/forge-1.8.9-11.15.1.2318-1.8.9-installer.jar";
                string tempDir = Path.GetTempPath();
                string tempInstaller = Path.Combine(tempDir, "forge-1.8.9-installer.jar");

                if (!File.Exists(tempInstaller))
                {
                    using var http = new System.Net.Http.HttpClient();
                    var bytes = await http.GetByteArrayAsync(installerUrl);
                    await File.WriteAllBytesAsync(tempInstaller, bytes);
                }

                // Extraer clase auxiliar de instalación silenciosa
                string targetClass = Path.Combine(tempDir, "SilentForgeInstall.class");
                try
                {
                    var assembly = System.Reflection.Assembly.GetExecutingAssembly();
                    using var stream = assembly.GetManifestResourceStream("SilentForgeInstall.class");
                    if (stream != null)
                    {
                        using var fs = new FileStream(targetClass, FileMode.Create, FileAccess.Write);
                        stream.CopyTo(fs);
                    }
                }
                catch { }

                // Ejecutar instalación silenciosa con SilentForgeInstall
                if (File.Exists(targetClass))
                {
                    string cp = $"{tempDir};{tempInstaller}";
                    var psi = new ProcessStartInfo
                    {
                        FileName = javaExe,
                        Arguments = $"-cp \"{cp}\" SilentForgeInstall \"{mcDir}\"",
                        WorkingDirectory = mcDir,
                        UseShellExecute = false,
                        CreateNoWindow = true
                    };

                    var proc = Process.Start(psi);
                    if (proc != null)
                    {
                        await proc.WaitForExitAsync();
                        if (proc.ExitCode == 0) return true;
                    }
                }

                // Fallback visual en caso de requerir confirmación
                var visualPsi = new ProcessStartInfo
                {
                    FileName = javaExe,
                    Arguments = $"-jar \"{tempInstaller}\"",
                    WorkingDirectory = mcDir,
                    UseShellExecute = false,
                    CreateNoWindow = false
                };
                var visualProc = Process.Start(visualPsi);
                if (visualProc != null)
                {
                    await visualProc.WaitForExitAsync();
                    return visualProc.ExitCode == 0;
                }
            }
            catch { }
            return false;
        }

        private static string FindJava(LauncherConfig config)
        {
            // 1. Config personalizada
            if (!string.IsNullOrWhiteSpace(config.JavaPath) && File.Exists(config.JavaPath))
                return config.JavaPath;

            // 2. Eclipse Adoptium JDK 8
            string adoptium = @"C:\Program Files\Eclipse Adoptium\jdk-8.0.504.1-hotspot\bin\javaw.exe";
            if (File.Exists(adoptium)) return adoptium;

            // 3. Oracle / OpenJDK en Program Files
            string javaDir = @"C:\Program Files\Java";
            if (Directory.Exists(javaDir))
            {
                foreach (var jre in Directory.GetDirectories(javaDir, "*1.8*"))
                {
                    string p = Path.Combine(jre, "bin", "javaw.exe");
                    if (File.Exists(p)) return p;
                }
            }

            string javaDir32 = @"C:\Program Files (x86)\Java";
            if (Directory.Exists(javaDir32))
            {
                foreach (var jre in Directory.GetDirectories(javaDir32, "*1.8*"))
                {
                    string p = Path.Combine(jre, "bin", "javaw.exe");
                    if (File.Exists(p)) return p;
                }
            }

            // 4. Runtime de Minecraft Launcher oficial
            string appData = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);
            string mcRuntime = Path.Combine(appData, ".minecraft", "runtime", "jre-x64", "bin", "javaw.exe");
            if (File.Exists(mcRuntime)) return mcRuntime;

            string mcLauncherRuntime = @"C:\Program Files (x86)\Minecraft Launcher\runtime\jre-x64\bin\javaw.exe";
            if (File.Exists(mcLauncherRuntime)) return mcLauncherRuntime;

            return "javaw";
        }

        private static string BuildJavaArgs(LauncherConfig config, string mcDir, string username, string accessToken, string uuid, string forgeVersion)
        {
            string versionsDir = Path.Combine(mcDir, "versions");
            string nativesDir = Path.Combine(versionsDir, "1.8.9", "natives");
            if (!Directory.Exists(nativesDir))
            {
                nativesDir = Path.Combine(versionsDir, "1.8.9", "1.8.9-natives");
            }
            if (!Directory.Exists(nativesDir))
            {
                nativesDir = Path.Combine(versionsDir, forgeVersion, $"{forgeVersion}-natives");
            }
            if (!Directory.Exists(nativesDir))
            {
                nativesDir = Path.Combine(versionsDir, forgeVersion);
            }

            string gameDir = mcDir;
            string assetsDir = Path.Combine(mcDir, "assets");
            string vanillaAssets = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft", "assets");
            if (!Directory.Exists(Path.Combine(assetsDir, "objects")) && Directory.Exists(Path.Combine(vanillaAssets, "objects")))
            {
                assetsDir = vanillaAssets;
            }
            int ram = Math.Max(1, config.RamAllocationGb);

            string cp = BuildClasspath(mcDir, forgeVersion);

            string jvmArgs = $"-Xmx{ram}G " +
                             $"-Djava.library.path=\"{nativesDir}\" " +
                             $"-Dfml.ignoreInvalidMinecraftCertificates=true " +
                             $"-Dfml.ignorePatchDiscrepancies=true ";

            if (config.OptimizePvpFlags)
            {
                jvmArgs += (string.IsNullOrWhiteSpace(config.CustomJvmArgs) ? "" : config.CustomJvmArgs + " ");
            }

            string gameArgs = $"--username \"{username}\" " +
                              $"--version \"{forgeVersion}\" " +
                              $"--gameDir \"{gameDir}\" " +
                              $"--assetsDir \"{assetsDir}\" " +
                              $"--assetIndex 1.8 " +
                              $"--uuid \"{uuid}\" " +
                              $"--accessToken \"{accessToken}\" " +
                              $"--userType {(accessToken == "0" ? "legacy" : "mojang")} " +
                              $"--tweakClass net.minecraftforge.fml.common.launcher.FMLTweaker " +
                              $"--versionType release";

            string mainClass = "net.minecraft.launchwrapper.Launch";

            return $"{jvmArgs} -cp \"{cp}\" {mainClass} {gameArgs}";
        }

        private static string BuildClasspath(string mcDir, string forgeVersion)
        {
            var cpList = new System.Collections.Generic.List<string>();

            // 1. Forge JAR
            string forgeLibJar = Path.Combine(mcDir, "libraries", "net", "minecraftforge", "forge", "1.8.9-11.15.1.2318-1.8.9", "forge-1.8.9-11.15.1.2318-1.8.9.jar");
            if (File.Exists(forgeLibJar))
            {
                cpList.Add(forgeLibJar);
            }

            // 2. Librerías específicas requeridas por Forge 1.8.9 y Minecraft 1.8.9
            string forgeJson = Path.Combine(mcDir, "versions", forgeVersion, $"{forgeVersion}.json");
            string mcJson = Path.Combine(mcDir, "versions", "1.8.9", "1.8.9.json");

            void AddLibsFromJson(string jsonPath)
            {
                if (!File.Exists(jsonPath)) return;
                try
                {
                    using var doc = System.Text.Json.JsonDocument.Parse(File.ReadAllText(jsonPath));
                    if (doc.RootElement.TryGetProperty("libraries", out var libs) && libs.ValueKind == System.Text.Json.JsonValueKind.Array)
                    {
                        foreach (var lib in libs.EnumerateArray())
                        {
                            if (lib.TryGetProperty("name", out var nameProp))
                            {
                                string name = nameProp.GetString() ?? "";
                                var parts = name.Split(':');
                                if (parts.Length >= 3)
                                {
                                    string group = parts[0].Replace('.', Path.DirectorySeparatorChar);
                                    string artifact = parts[1];
                                    string version = parts[2];
                                    string jarFile = $"{artifact}-{version}.jar";
                                    string p = Path.Combine(mcDir, "libraries", group, artifact, version, jarFile);
                                    if (File.Exists(p) && !cpList.Contains(p))
                                    {
                                        cpList.Add(p);
                                    }
                                }
                            }
                        }
                    }
                }
                catch { }
            }

            AddLibsFromJson(forgeJson);
            AddLibsFromJson(mcJson);

            // 3. Vanilla 1.8.9 JAR
            string vanillaJar = Path.Combine(mcDir, "versions", "1.8.9", "1.8.9.jar");
            if (File.Exists(vanillaJar))
            {
                cpList.Add(vanillaJar);
            }

            return string.Join(";", cpList);
        }

        /// <summary>Genera un UUID v3 determinista a partir del nombre (para cuentas offline).</summary>
        private static string GuidFromName(string name)
        {
            // UUID offline estilo Minecraft: "OfflinePlayer:" + name
            byte[] bytes = System.Text.Encoding.UTF8.GetBytes("OfflinePlayer:" + name);
            using var md5 = System.Security.Cryptography.MD5.Create();
            byte[] hash = md5.ComputeHash(bytes);
            // UUID v3
            hash[6] = (byte)((hash[6] & 0x0f) | 0x30);
            hash[8] = (byte)((hash[8] & 0x3f) | 0x80);
            return new Guid(hash).ToString("N"); // sin guiones
        }
    }
}
