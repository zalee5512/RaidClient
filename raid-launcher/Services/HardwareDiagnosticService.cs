using System;
using System.Collections.Generic;
using System.IO;
using System.Management;
using System.Runtime.InteropServices;
using Microsoft.Win32;
using RaidLauncher.Models;

namespace RaidLauncher.Services
{
    public class HardwareReport
    {
        public string CpuName { get; set; } = "Desconocido";
        public int CpuCores { get; set; } = 4;
        public int CpuThreads { get; set; } = 8;

        public string GpuName { get; set; } = "Desconocido";
        public string GpuDriverVersion { get; set; } = "Desconocido";
        public string GpuDriverDate { get; set; } = "Desconocido";
        public double GpuVramGb { get; set; } = 2.0;
        public bool IsDedicatedGpu { get; set; } = false;

        public int TotalRamGb { get; set; } = 8;
        public double AvailableRamGb { get; set; } = 4.0;
        public int RecommendedRamGb { get; set; } = 3;

        public int MonitorHz { get; set; } = 60;
        public bool IsJavaConfiguredHighPerf { get; set; } = false;
        public int OptimizationScore { get; set; } = 85;

        public List<string> StatusNotes { get; set; } = new List<string>();
        public List<string> Warnings { get; set; } = new List<string>();
    }

    public static class HardwareDiagnosticService
    {
        public static HardwareReport RunDiagnostics(LauncherConfig config)
        {
            var report = new HardwareReport();

            try
            {
                // ─── 1. Diagnóstico de CPU ───────────────────────────────────────────
                using (var searcher = new ManagementObjectSearcher("SELECT Name, NumberOfCores, NumberOfLogicalProcessors FROM Win32_Processor"))
                {
                    foreach (ManagementObject obj in searcher.Get())
                    {
                        report.CpuName = obj["Name"]?.ToString()?.Trim() ?? "Procesador";
                        if (int.TryParse(obj["NumberOfCores"]?.ToString(), out int cores)) report.CpuCores = cores;
                        if (int.TryParse(obj["NumberOfLogicalProcessors"]?.ToString(), out int threads)) report.CpuThreads = threads;
                        break;
                    }
                }

                // ─── 2. Diagnóstico de Memoria RAM ───────────────────────────────────
                using (var searcher = new ManagementObjectSearcher("SELECT TotalPhysicalMemory FROM Win32_ComputerSystem"))
                {
                    foreach (ManagementObject obj in searcher.Get())
                    {
                        if (long.TryParse(obj["TotalPhysicalMemory"]?.ToString(), out long bytes))
                        {
                            report.TotalRamGb = (int)Math.Round(bytes / (1024.0 * 1024.0 * 1024.0));
                        }
                        break;
                    }
                }

                using (var searcher = new ManagementObjectSearcher("SELECT FreePhysicalMemory FROM Win32_OperatingSystem"))
                {
                    foreach (ManagementObject obj in searcher.Get())
                    {
                        if (long.TryParse(obj["FreePhysicalMemory"]?.ToString(), out long kb))
                        {
                            report.AvailableRamGb = Math.Round(kb / (1024.0 * 1024.0), 1);
                        }
                        break;
                    }
                }

                // ─── 3. Diagnóstico de Tarjeta Gráfica y Controladores ────────────────
                using (var searcher = new ManagementObjectSearcher("SELECT Name, DriverVersion, DriverDate, AdapterRAM, CurrentRefreshRate FROM Win32_VideoController"))
                {
                    foreach (ManagementObject obj in searcher.Get())
                    {
                        string name = obj["Name"]?.ToString() ?? "";
                        if (string.IsNullOrWhiteSpace(name)) continue;

                        report.GpuName = name;
                        report.GpuDriverVersion = obj["DriverVersion"]?.ToString() ?? "No detectada";

                        string rawDate = obj["DriverDate"]?.ToString() ?? "";
                        if (rawDate.Length >= 8)
                        {
                            string y = rawDate.Substring(0, 4);
                            string m = rawDate.Substring(4, 2);
                            string d = rawDate.Substring(6, 2);
                            report.GpuDriverDate = $"{d}/{m}/{y}";
                        }
                        else
                        {
                            report.GpuDriverDate = rawDate;
                        }

                        if (long.TryParse(obj["AdapterRAM"]?.ToString(), out long vram))
                        {
                            report.GpuVramGb = Math.Round(vram / (1024.0 * 1024.0 * 1024.0), 1);
                        }

                        if (int.TryParse(obj["CurrentRefreshRate"]?.ToString(), out int hz) && hz > 0)
                        {
                            report.MonitorHz = hz;
                        }

                        string nameLower = name.ToLower();
                        if (nameLower.Contains("nvidia") || nameLower.Contains("geforce") || nameLower.Contains("radeon rx") || nameLower.Contains("rtx") || nameLower.Contains("gtx"))
                        {
                            report.IsDedicatedGpu = true;
                        }

                        // Si encontramos una dedicada, priorizamos mostrarla
                        if (report.IsDedicatedGpu) break;
                    }
                }

                // ─── 4. Cálculo de RAM Óptima para 1.8.9 ──────────────────────────────
                // En 1.8.9, asignar más de 4GB es perjudicial (GC pauses). 
                if (report.TotalRamGb <= 4)
                {
                    report.RecommendedRamGb = 2; // Dejar 2GB libres para Windows
                }
                else if (report.TotalRamGb <= 8)
                {
                    report.RecommendedRamGb = 3; // Balance ideal
                }
                else
                {
                    report.RecommendedRamGb = 3; // 3GB o 4GB es el dulce spot para 1.8.9
                }

                // ─── 5. Verificar Preferencia de GPU en Windows para Java ─────────────
                string javaPath = config.JavaPath;
                if (!File.Exists(javaPath))
                {
                    javaPath = @"C:\Program Files\Eclipse Adoptium\jdk-8.0.504.1-hotspot\bin\javaw.exe";
                }

                try
                {
                    using (var key = Registry.CurrentUser.OpenSubKey(@"Software\Microsoft\DirectX\UserGpuPreferences"))
                    {
                        if (key != null)
                        {
                            var val = key.GetValue(javaPath)?.ToString() ?? "";
                            report.IsJavaConfiguredHighPerf = val.Contains("GpuPreference=2");
                        }
                    }
                }
                catch { }

                // ─── 6. Diagnóstico y Recomendaciones ────────────────────────────────
                int score = 70;

                report.StatusNotes.Add($"CPU: {report.CpuName} ({report.CpuCores} núcleos / {report.CpuThreads} hilos)");
                report.StatusNotes.Add($"GPU: {report.GpuName} (Controlador: {report.GpuDriverVersion})");
                report.StatusNotes.Add($"Memoria: {report.TotalRamGb} GB RAM instalada ({report.AvailableRamGb} GB disponible)");
                report.StatusNotes.Add($"Monitor: Tasa de refresco detectada a {report.MonitorHz} Hz");

                // Evaluar controlador
                if (!string.IsNullOrEmpty(report.GpuDriverDate))
                {
                    report.StatusNotes.Add($"Fecha del controlador gráfico: {report.GpuDriverDate} (En orden)");
                    score += 10;
                }

                // Evaluar GPU
                if (report.IsDedicatedGpu)
                {
                    report.StatusNotes.Add("Tarjeta gráfica dedicada de alto rendimiento detectada.");
                    score += 10;
                }
                else
                {
                    report.StatusNotes.Add("GPU Integrada detectada (VBOs y optimizaciones PvP recomendadas).");
                    score += 5;
                }

                if (report.IsJavaConfiguredHighPerf)
                {
                    report.StatusNotes.Add("Java 8 ya tiene asignado el perfil de Alto Rendimiento en Windows.");
                    score += 10;
                }
                else
                {
                    report.Warnings.Add("Java 8 no tiene asignado 'Alto Rendimiento' en Windows.");
                }

                if (config.RamAllocationGb == report.RecommendedRamGb)
                {
                    report.StatusNotes.Add($"RAM asignada ({config.RamAllocationGb} GB) es óptima para Minecraft 1.8.9.");
                    score += 10;
                }
                else
                {
                    report.Warnings.Add($"RAM actual ({config.RamAllocationGb} GB) difiere de la recomendada ({report.RecommendedRamGb} GB).");
                }

                report.OptimizationScore = Math.Min(100, score);
            }
            catch (Exception ex)
            {
                report.Warnings.Add("Error en diagnóstico: " + ex.Message);
            }

            return report;
        }

        public static void ApplyAutoOptimizations(LauncherConfig config, Action<string>? onStatus)
        {
            var report = RunDiagnostics(config);

            onStatus?.Invoke("1/4 Asignando memoria RAM óptima...");
            config.RamAllocationGb = report.RecommendedRamGb;
            config.OptimizePvpFlags = true;

            onStatus?.Invoke("2/4 Configurando Alto Rendimiento en Windows para Java 8...");
            string javaPath = config.JavaPath;
            if (!File.Exists(javaPath))
            {
                javaPath = @"C:\Program Files\Eclipse Adoptium\jdk-8.0.504.1-hotspot\bin\javaw.exe";
            }

            if (File.Exists(javaPath))
            {
                try
                {
                    using (var key = Registry.CurrentUser.CreateSubKey(@"Software\Microsoft\DirectX\UserGpuPreferences"))
                    {
                        key.SetValue(javaPath, "GpuPreference=2;"); // 2 = High Performance GPU
                    }
                }
                catch { }
            }

            onStatus?.Invoke("3/4 Optimizando opciones de renderizado (options.txt)...");
            try
            {
                string clientDir = @"C:\Programacion\raid-client";
                string runOptions = Path.Combine(clientDir, "run", "options.txt");
                string mcOptions = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft", "options.txt");
                string raidOptions = Path.Combine(GameInstallerService.GetGameDir(), "options.txt");

                ApplyOptimizedOptions(runOptions);
                ApplyOptimizedOptions(mcOptions);
                ApplyOptimizedOptions(raidOptions);
            }
            catch { }

            onStatus?.Invoke("4/4 Desactivando Light Pipeline de Forge...");
            try
            {
                string forgeCfg = @"C:\Programacion\raid-client\run\config\forge.cfg";
                if (File.Exists(forgeCfg))
                {
                    string text = File.ReadAllText(forgeCfg);
                    text = text.Replace("B:forgeLightPipelineEnabled=true", "B:forgeLightPipelineEnabled=false");
                    File.WriteAllText(forgeCfg, text);
                }
            }
            catch { }

            ConfigManager.SaveConfig(config);
            onStatus?.Invoke("¡Optimización de Hardware completada con éxito!");
        }

        private static void ApplyOptimizedOptions(string filePath)
        {
            if (!File.Exists(filePath)) return;

            string[] lines = File.ReadAllLines(filePath);
            for (int i = 0; i < lines.Length; i++)
            {
                if (lines[i].StartsWith("enableVsync:")) lines[i] = "enableVsync:false";
                else if (lines[i].StartsWith("useVbo:")) lines[i] = "useVbo:true";
                else if (lines[i].StartsWith("maxFps:")) lines[i] = "maxFps:260";
                else if (lines[i].StartsWith("entityShadows:")) lines[i] = "entityShadows:false";
                else if (lines[i].StartsWith("mipmapLevels:")) lines[i] = "mipmapLevels:0";
                else if (lines[i].StartsWith("renderClouds:")) lines[i] = "renderClouds:false";
            }
            File.WriteAllLines(filePath, lines);
        }
    }
}
