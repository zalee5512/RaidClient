using System;
using System.Collections.Generic;
using System.IO;
using System.IO.Compression;
using System.Net.Http;
using System.Reflection;
using System.Text.Json;
using System.Threading.Tasks;

namespace RaidLauncher.Services
{
    public class GameInstallerService
    {
        private static readonly HttpClient _http = new HttpClient { Timeout = TimeSpan.FromMinutes(3) };

        public const string ForgeVersionId = "1.8.9-forge1.8.9-11.15.1.2318-1.8.9";

        /// <summary>
        /// Carpeta propia e independiente de Raid Client (%APPDATA%\.raidclient)
        /// </summary>
        public static string GetGameDir()
        {
            string path = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".raidclient");
            if (!Directory.Exists(path))
            {
                Directory.CreateDirectory(path);
            }
            return path;
        }

        public static bool IsBasicInstallationPresent(string mcDir)
        {
            string v189Jar = Path.Combine(mcDir, "versions", "1.8.9", "1.8.9.jar");
            string forgeJar = Path.Combine(mcDir, "libraries", "net", "minecraftforge", "forge", "1.8.9-11.15.1.2318-1.8.9", "forge-1.8.9-11.15.1.2318-1.8.9.jar");
            string modJar = Path.Combine(mcDir, "mods", "RaidClient-1.0.jar");
            string lwjgl = Path.Combine(mcDir, "versions", "1.8.9", "natives", "lwjgl64.dll");

            return File.Exists(v189Jar) && File.Exists(forgeJar) && File.Exists(modJar) && File.Exists(lwjgl);
        }

        public static async Task<string?> EnsureJavaRuntimeAsync(string mcDir, Action<int, string>? onProgress, Action<string>? onStatus)
        {
            // 1. Comprobar si ya tenemos Java 8 portable descargado en .raidclient o .raidlauncher
            string[] portableDirs = new[]
            {
                Path.Combine(mcDir, "runtime", "jre-8"),
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".raidlauncher", "runtime", "jre-8")
            };

            foreach (var dir in portableDirs)
            {
                if (Directory.Exists(dir))
                {
                    var found = Directory.GetFiles(dir, "javaw.exe", SearchOption.AllDirectories);
                    if (found.Length > 0 && File.Exists(found[0]))
                    {
                        return found[0];
                    }
                }
            }

            // 2. Buscar si la PC ya tiene algún Java 8 instalado
            string? systemJava = FindSystemJava(mcDir);
            if (!string.IsNullOrEmpty(systemJava) && File.Exists(systemJava))
            {
                return systemJava;
            }

            // 3. Si la computadora no tiene Java instalado, descargar Java 8 de 64 bits automáticamente
            try
            {
                string targetDir = Path.Combine(mcDir, "runtime", "jre-8");
                onStatus?.Invoke("Descargando motor Java 8 de 64 bits para Minecraft...");
                onProgress?.Invoke(92, "Descargando motor Java 8 portable (38 MB)...");

                Directory.CreateDirectory(targetDir);
                string zipPath = Path.Combine(Path.GetTempPath(), "temurin-jre8.zip");

                string jreUrl = "https://api.adoptium.net/v3/binary/latest/8/ga/windows/x64/jre/hotspot/normal/eclipse";
                await DownloadFileAsync(jreUrl, zipPath);

                onStatus?.Invoke("Configurando Java 8 de 64 bits...");
                onProgress?.Invoke(96, "Configurando motor de Java...");

                ZipFile.ExtractToDirectory(zipPath, targetDir, true);
                try { File.Delete(zipPath); } catch { }

                var found = Directory.GetFiles(targetDir, "javaw.exe", SearchOption.AllDirectories);
                if (found.Length > 0 && File.Exists(found[0]))
                {
                    return found[0];
                }
            }
            catch (Exception ex)
            {
                onStatus?.Invoke("Aviso al descargar Java: " + ex.Message);
            }

            return null;
        }

        public static string? FindSystemJava(string mcDir)
        {
            var candidates = new List<string>
            {
                // Eclipse Adoptium / Temurin
                @"C:\Program Files\Eclipse Adoptium\jdk-8.0.504.1-hotspot\bin\javaw.exe",
                @"C:\Program Files\Eclipse Adoptium\jdk-8.0.504.1-hotspot\jre\bin\javaw.exe",

                // Runtimes oficiales de Minecraft y TLauncher
                Path.Combine(mcDir, "runtime", "jre-x64", "bin", "javaw.exe"),
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft", "runtime", "jre-x64", "bin", "javaw.exe"),
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft", "runtime", "jre-legacy", "windows-x64", "jre-legacy", "bin", "javaw.exe"),
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft", "runtime", "java-runtime-alpha", "windows-x64", "java-runtime-alpha", "bin", "javaw.exe"),
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft", "runtime", "java-runtime-gamma", "windows-x64", "java-runtime-gamma", "bin", "javaw.exe"),
                @"C:\Program Files (x86)\Minecraft Launcher\runtime\jre-x64\bin\javaw.exe",
                @"C:\Program Files (x86)\Minecraft Launcher\runtime\jre-legacy\windows-x64\jre-legacy\bin\javaw.exe",

                // Rutas habituales de Oracle Java 8
                @"C:\Program Files\Java\jre1.8.0_421\bin\javaw.exe",
                @"C:\Program Files\Java\jre1.8.0_401\bin\javaw.exe",
                @"C:\Program Files\Java\jre1.8.0_351\bin\javaw.exe",
                @"C:\Program Files\Java\jre1.8.0_51\bin\javaw.exe",
                @"C:\Program Files\Java\jre-8\bin\javaw.exe",
                @"C:\Program Files\Java\jdk1.8.0_421\bin\javaw.exe"
            };

            foreach (var p in candidates)
            {
                if (File.Exists(p)) return p;
            }

            // Escanear carpetas de Program Files
            string[] scanRoots = new[]
            {
                @"C:\Program Files\Java",
                @"C:\Program Files\Eclipse Adoptium",
                @"C:\Program Files\Semeru",
                @"C:\Program Files (x86)\Java"
            };

            foreach (var root in scanRoots)
            {
                if (Directory.Exists(root))
                {
                    try
                    {
                        var found = Directory.GetFiles(root, "javaw.exe", SearchOption.AllDirectories);
                        if (found.Length > 0 && File.Exists(found[0])) return found[0];
                    }
                    catch { }
                }
            }

            // Buscar en el PATH de Windows mediante where.exe
            try
            {
                var psi = new System.Diagnostics.ProcessStartInfo
                {
                    FileName = "where.exe",
                    Arguments = "javaw",
                    UseShellExecute = false,
                    RedirectStandardOutput = true,
                    CreateNoWindow = true
                };
                using var proc = System.Diagnostics.Process.Start(psi);
                if (proc != null)
                {
                    string output = proc.StandardOutput.ReadToEnd();
                    proc.WaitForExit();
                    var lines = output.Split(new[] { '\r', '\n' }, StringSplitOptions.RemoveEmptyEntries);
                    foreach (var line in lines)
                    {
                        if (File.Exists(line.Trim())) return line.Trim();
                    }
                }
            }
            catch { }

            return null;
        }

        public static async Task<bool> EnsureAllGameFilesAsync(string mcDir, Action<int, string>? onProgress, Action<string>? onStatus)
        {
            try
            {
                if (string.IsNullOrWhiteSpace(mcDir))
                {
                    mcDir = GetGameDir();
                }

                string versionsDir = Path.Combine(mcDir, "versions");
                string v189Dir = Path.Combine(versionsDir, "1.8.9");
                string vForgeDir = Path.Combine(versionsDir, ForgeVersionId);
                string nativesDir = Path.Combine(v189Dir, "natives");
                string librariesDir = Path.Combine(mcDir, "libraries");
                string modsDir = Path.Combine(mcDir, "mods");
                string configDir = Path.Combine(mcDir, "config");
                string assetsDir = Path.Combine(mcDir, "assets");
                string indexesDir = Path.Combine(assetsDir, "indexes");

                // Crear estructura completa de carpetas de Raid Client
                Directory.CreateDirectory(mcDir);
                Directory.CreateDirectory(v189Dir);
                Directory.CreateDirectory(vForgeDir);
                Directory.CreateDirectory(nativesDir);
                Directory.CreateDirectory(librariesDir);
                Directory.CreateDirectory(modsDir);
                Directory.CreateDirectory(configDir);
                Directory.CreateDirectory(assetsDir);
                Directory.CreateDirectory(indexesDir);

                // Ruta origen de .minecraft por si ya existen archivos en la máquina (copia instantánea)
                string vanillaDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft");

                // ─── 0. Crear configuración óptima de splash.properties (sin pantalla blanca) ───
                string splashCfg = Path.Combine(configDir, "splash.properties");
                if (!File.Exists(splashCfg))
                {
                    try
                    {
                        File.WriteAllText(splashCfg, "# Raid Client - Disable Forge loading splash to prevent mixin conflicts\nenabled=false\n");
                    }
                    catch { }
                }

                // ─── 0b. Crear options.txt predeterminado de alto rendimiento si no existe ───
                string optionsTxt = Path.Combine(mcDir, "options.txt");
                if (!File.Exists(optionsTxt))
                {
                    try
                    {
                        File.WriteAllText(optionsTxt, "invertYMouse:false\nmouseSensitivity:0.5\nfov:0.0\ngamma:1.0\nmusic:0.0\nsound:1.0\nrenderDistance:8\nguiScale:0\nparticles:1\nbobView:true\nanaglyph3d:false\nmaxFps:260\nfboEnable:true\ndifficulty:2\nfancyGraphics:false\nao:2\nclouds:false\nskin:default\nlastServer:\nlang:es_ES\nchatVisibility:0\nchatColors:true\nchatLinks:true\nchatLinksPrompt:true\nchatOpacity:1.0\nsnooperEnabled:false\nfullscreen:false\nenableVsync:false\nuseVbo:true\nhideServerAddress:false\nadvancedItemTooltips:false\npauseOnLostFocus:true\ntouchscreen:false\noverrideWidth:0\noverrideHeight:0\nheldItemTooltips:true\nchatHeightFocused:1.0\nchatHeightUnfocused:0.44366196\nchatScale:1.0\nchatWidth:1.0\nshowInventoryAchievementHint:false\nmipmapLevels:0\nstreamBytesPerPixel:0.5\nstreamMicVolume:1.0\nstreamSystemVolume:1.0\nstreamKbps:0.5\nstreamFps:0.31690142\nstreamCompression:1\nstreamSendMetadata:true\nstreamPreferredServer:\nstreamChatEnabled:0\nstreamChatUserFilter:0\nstreamMicToggleBehavior:0\nforceUnicodeFont:false\nallowBlockAlternatives:true\nreducedDebugInfo:false\nuseNativeTransport:true\nentityShadows:false\nrealmsNotifications:false\n");
                    }
                    catch { }
                }

                // ─── 1. Asegurar archivos JSON de versión ─────────────────────────────
                string mcJsonPath = Path.Combine(v189Dir, "1.8.9.json");
                string forgeJsonPath = Path.Combine(vForgeDir, $"{ForgeVersionId}.json");

                ExtractResourceIfMissing("1.8.9.json", mcJsonPath);
                ExtractResourceIfMissing("1.8.9-forge.json", forgeJsonPath);

                // ─── 2. Asegurar Minecraft 1.8.9 base JAR (8.4 MB) ───────────────────
                string mcClientJar = Path.Combine(v189Dir, "1.8.9.jar");
                if (!File.Exists(mcClientJar) || new FileInfo(mcClientJar).Length < 1000000)
                {
                    string vanilla189 = Path.Combine(vanillaDir, "versions", "1.8.9", "1.8.9.jar");
                    if (File.Exists(vanilla189) && new FileInfo(vanilla189).Length > 1000000)
                    {
                        onStatus?.Invoke("Sincronizando Minecraft 1.8.9...");
                        File.Copy(vanilla189, mcClientJar, true);
                    }
                    else
                    {
                        onStatus?.Invoke("Descargando Minecraft 1.8.9 base oficial (8.4 MB)...");
                        onProgress?.Invoke(15, "Descargando Minecraft 1.8.9 base...");
                        string mcUrl = "https://launcher.mojang.com/v1/objects/3870888a6c3d349d3771a3e9d16c9bf5e076b908/client.jar";
                        await DownloadFileAsync(mcUrl, mcClientJar);
                    }
                }

                // ─── 3. Asegurar gráficos y controles nativos de Windows ──────────────
                string lwjgl64 = Path.Combine(nativesDir, "lwjgl64.dll");
                string jinput64 = Path.Combine(nativesDir, "jinput-dx8_64.dll");

                if (!File.Exists(lwjgl64) || !File.Exists(jinput64))
                {
                    string vanillaNatives = Path.Combine(vanillaDir, "versions", "1.8.9", "natives");
                    if (Directory.Exists(vanillaNatives) && File.Exists(Path.Combine(vanillaNatives, "lwjgl64.dll")))
                    {
                        onStatus?.Invoke("Sincronizando controladores gráficos nativos...");
                        foreach (var dll in Directory.GetFiles(vanillaNatives, "*.dll"))
                        {
                            try { File.Copy(dll, Path.Combine(nativesDir, Path.GetFileName(dll)), true); } catch { }
                        }
                    }
                    else
                    {
                        onStatus?.Invoke("Descargando gráficos nativos y controles de Windows...");
                        onProgress?.Invoke(25, "Instalando aceleración gráfica (Natives)...");

                        string[] nativeUrls = new[]
                        {
                            "https://libraries.minecraft.net/org/lwjgl/lwjgl/lwjgl-platform/2.9.4-nightly-20150209/lwjgl-platform-2.9.4-nightly-20150209-natives-windows.jar",
                            "https://libraries.minecraft.net/net/java/jinput/jinput-platform/2.0.5/jinput-platform-2.0.5-natives-windows.jar",
                            "https://libraries.minecraft.net/tv/twitch/twitch-platform/6.5/twitch-platform-6.5-natives-windows-64.jar"
                        };

                        foreach (var nUrl in nativeUrls)
                        {
                            string tempJar = Path.Combine(Path.GetTempPath(), Path.GetFileName(nUrl));
                            await DownloadFileAsync(nUrl, tempJar);
                            try
                            {
                                using var archive = ZipFile.OpenRead(tempJar);
                                foreach (var entry in archive.Entries)
                                {
                                    if (entry.FullName.EndsWith(".dll", StringComparison.OrdinalIgnoreCase))
                                    {
                                        string dest = Path.Combine(nativesDir, entry.Name);
                                        entry.ExtractToFile(dest, true);
                                    }
                                }
                            }
                            catch { }
                            try { File.Delete(tempJar); } catch { }
                        }
                    }
                }

                // ─── 3b. Asegurar índice de recursos 1.8.json ─────────────────────────
                string assetIndex18 = Path.Combine(indexesDir, "1.8.json");
                if (!File.Exists(assetIndex18) || new FileInfo(assetIndex18).Length < 1000)
                {
                    string vanillaIndex = Path.Combine(vanillaDir, "assets", "indexes", "1.8.json");
                    if (File.Exists(vanillaIndex))
                    {
                        File.Copy(vanillaIndex, assetIndex18, true);
                    }
                    else
                    {
                        onStatus?.Invoke("Descargando índice de recursos de Minecraft 1.8...");
                        string assetUrl = "https://launchermeta.mojang.com/v1/packages/f6ad102bcaa53b1a58358f16e376d548d44933ec/1.8.json";
                        await DownloadFileAsync(assetUrl, assetIndex18);
                    }
                }

                // ─── 4. Asegurar motor Forge 1.8.9 Universal ──────────────────────────
                string forgeJar = Path.Combine(librariesDir, "net", "minecraftforge", "forge", "1.8.9-11.15.1.2318-1.8.9", "forge-1.8.9-11.15.1.2318-1.8.9.jar");
                if (!File.Exists(forgeJar) || new FileInfo(forgeJar).Length < 1000000)
                {
                    string vanillaForge = Path.Combine(vanillaDir, "libraries", "net", "minecraftforge", "forge", "1.8.9-11.15.1.2318-1.8.9", "forge-1.8.9-11.15.1.2318-1.8.9.jar");
                    string localDevForge = @"C:\Programacion\forge-1.8.9-11.15.1.2318-1.8.9-universal.jar";

                    Directory.CreateDirectory(Path.GetDirectoryName(forgeJar)!);

                    if (File.Exists(localDevForge) && new FileInfo(localDevForge).Length > 1000000)
                    {
                        File.Copy(localDevForge, forgeJar, true);
                    }
                    else if (File.Exists(vanillaForge) && new FileInfo(vanillaForge).Length > 1000000)
                    {
                        File.Copy(vanillaForge, forgeJar, true);
                    }
                    else
                    {
                        onStatus?.Invoke("Descargando motor oficial Forge 1.8.9...");
                        onProgress?.Invoke(40, "Descargando motor Forge 1.8.9 (3.8 MB)...");
                        string forgeUrl = "https://maven.minecraftforge.net/net/minecraftforge/forge/1.8.9-11.15.1.2318-1.8.9/forge-1.8.9-11.15.1.2318-1.8.9-universal.jar";
                        await DownloadFileAsync(forgeUrl, forgeJar);
                    }
                }

                // ─── 5. Analizar y descargar librerías faltantes ────────────────────────
                onStatus?.Invoke("Comprobando librerías de combate y audio...");
                onProgress?.Invoke(50, "Comprobando librerías de 1.8.9...");

                var missingLibs = new List<(string Name, string? BaseUrl, string LocalPath)>();

                CheckLibrariesFromJson(mcJsonPath, librariesDir, missingLibs);
                CheckLibrariesFromJson(forgeJsonPath, librariesDir, missingLibs);

                // Comprobar primero si podemos copiarlas desde .minecraft/libraries
                var stillMissing = new List<(string Name, string? BaseUrl, string LocalPath)>();
                foreach (var lib in missingLibs)
                {
                    string relPath = MavenToRelativePath(lib.Name).Replace('/', Path.DirectorySeparatorChar);
                    string vanillaLib = Path.Combine(vanillaDir, "libraries", relPath);

                    if (File.Exists(vanillaLib) && new FileInfo(vanillaLib).Length > 0)
                    {
                        Directory.CreateDirectory(Path.GetDirectoryName(lib.LocalPath)!);
                        try { File.Copy(vanillaLib, lib.LocalPath, true); } catch { }
                    }

                    if (!File.Exists(lib.LocalPath))
                    {
                        stillMissing.Add(lib);
                    }
                }

                if (stillMissing.Count > 0)
                {
                    int total = stillMissing.Count;
                    int done = 0;
                    foreach (var lib in stillMissing)
                    {
                        done++;
                        int pct = 50 + (int)((done / (float)total) * 35);
                        onProgress?.Invoke(pct, $"Descargando librerías ({done}/{total})...");

                        string relPath = MavenToRelativePath(lib.Name);
                        string url;
                        if (!string.IsNullOrWhiteSpace(lib.BaseUrl))
                        {
                            string b = lib.BaseUrl.TrimEnd('/') + "/";
                            url = b + relPath;
                        }
                        else
                        {
                            url = "https://libraries.minecraft.net/" + relPath;
                        }

                        Directory.CreateDirectory(Path.GetDirectoryName(lib.LocalPath)!);
                        await DownloadFileAsync(url, lib.LocalPath);
                    }
                }

                // ─── 5b. Asegurar SpongePowered Mixin para el cliente ──────────────────
                string mixinJarPath = Path.Combine(librariesDir, "org", "spongepowered", "mixin", "0.7.10-SNAPSHOT", "mixin-0.7.10-SNAPSHOT.jar");
                if (!File.Exists(mixinJarPath) || new FileInfo(mixinJarPath).Length < 100000)
                {
                    ExtractResourceIfMissing("mixin-0.7.10-SNAPSHOT.jar", mixinJarPath, forceOverwrite: true);
                }

                // ─── 6. Sincronizar RaidClient-1.0.jar en .raidclient/mods ──────────────
                onStatus?.Invoke("Sincronizando Raid Client...");
                onProgress?.Invoke(88, "Instalando módulos de Raid Client...");

                string targetModJar = Path.Combine(modsDir, "RaidClient-1.0.jar");
                string devModJar = @"C:\Programacion\raid-client\build\libs\RaidClient-1.0.jar";

                if (File.Exists(devModJar))
                {
                    try { File.Copy(devModJar, targetModJar, true); } catch { }
                }
                else
                {
                    ExtractResourceIfMissing("RaidClient-1.0.jar", targetModJar, forceOverwrite: true);
                }

                onProgress?.Invoke(90, "Archivos de Raid Client listos.");
                return true;
            }
            catch (Exception ex)
            {
                onStatus?.Invoke("Aviso al verificar archivos: " + ex.Message);
                return false;
            }
        }

        private static void ExtractResourceIfMissing(string resourceName, string targetPath, bool forceOverwrite = false)
        {
            if (File.Exists(targetPath) && !forceOverwrite) return;
            try
            {
                var asm = Assembly.GetExecutingAssembly();
                using var stream = asm.GetManifestResourceStream(resourceName);
                if (stream != null)
                {
                    Directory.CreateDirectory(Path.GetDirectoryName(targetPath)!);
                    using var fs = new FileStream(targetPath, FileMode.Create, FileAccess.Write, FileShare.ReadWrite);
                    stream.CopyTo(fs);
                }
            }
            catch { }
        }

        private static async Task DownloadFileAsync(string url, string destinationPath)
        {
            if (File.Exists(destinationPath)) return;
            try
            {
                var bytes = await _http.GetByteArrayAsync(url);
                string temp = destinationPath + ".tmp";
                await File.WriteAllBytesAsync(temp, bytes);
                if (File.Exists(destinationPath)) File.Delete(destinationPath);
                File.Move(temp, destinationPath);
            }
            catch { }
        }

        private static void CheckLibrariesFromJson(string jsonPath, string librariesDir, List<(string Name, string? BaseUrl, string LocalPath)> missingList)
        {
            if (!File.Exists(jsonPath)) return;
            try
            {
                using var doc = JsonDocument.Parse(File.ReadAllText(jsonPath));
                if (doc.RootElement.TryGetProperty("libraries", out var libs) && libs.ValueKind == JsonValueKind.Array)
                {
                    foreach (var lib in libs.EnumerateArray())
                    {
                        if (lib.TryGetProperty("name", out var nameProp))
                        {
                            string name = nameProp.GetString() ?? "";
                            string? baseUrl = null;
                            if (lib.TryGetProperty("url", out var urlProp))
                            {
                                baseUrl = urlProp.GetString();
                            }

                            string rel = MavenToRelativePath(name);
                            string localPath = Path.Combine(librariesDir, rel.Replace('/', Path.DirectorySeparatorChar));

                            if (!File.Exists(localPath))
                            {
                                missingList.Add((name, baseUrl, localPath));
                            }
                        }
                    }
                }
            }
            catch { }
        }

        private static string MavenToRelativePath(string mavenCoord)
        {
            var parts = mavenCoord.Split(':');
            if (parts.Length < 3) return mavenCoord;

            string group = parts[0].Replace('.', '/');
            string artifact = parts[1];
            string version = parts[2];
            return $"{group}/{artifact}/{version}/{artifact}-{version}.jar";
        }
    }
}
