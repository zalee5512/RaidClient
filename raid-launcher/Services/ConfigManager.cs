using System;
using System.IO;
using System.Text.Json;
using RaidLauncher.Models;

namespace RaidLauncher.Services
{
    public class ConfigManager
    {
        private static readonly string AppDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".raidlauncher");
        private static readonly string ConfigFile = Path.Combine(AppDir, "config.json");

        public static string GetAppDir() => AppDir;

        public static LauncherConfig LoadConfig()
        {
            try
            {
                if (!Directory.Exists(AppDir))
                {
                    Directory.CreateDirectory(AppDir);
                }

                if (File.Exists(ConfigFile))
                {
                    string json = File.ReadAllText(ConfigFile);
                    var cfg = JsonSerializer.Deserialize<LauncherConfig>(json);
                    if (cfg != null)
                    {
                        ValidateConfig(cfg);
                        return cfg;
                    }
                }
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine("Error reading config: " + ex.Message);
            }

            var defaultConfig = new LauncherConfig();
            ValidateConfig(defaultConfig);
            SaveConfig(defaultConfig);
            return defaultConfig;
        }

        public static void SaveConfig(LauncherConfig config)
        {
            try
            {
                if (!Directory.Exists(AppDir))
                {
                    Directory.CreateDirectory(AppDir);
                }

                string json = JsonSerializer.Serialize(config, new JsonSerializerOptions { WriteIndented = true });
                File.WriteAllText(ConfigFile, json);
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine("Error saving config: " + ex.Message);
            }
        }

        private static void ValidateConfig(LauncherConfig config)
        {
            // Auto detect Adoptium or standard Java if not present
            if (!File.Exists(config.JavaPath))
            {
                string[] candidates = new[]
                {
                    @"C:\Program Files\Eclipse Adoptium\jdk-8.0.504.1-hotspot\bin\javaw.exe",
                    @"C:\Program Files\Java\jre-1.8\bin\javaw.exe",
                    @"C:\Program Files\Java\jre1.8.0_401\bin\javaw.exe"
                };

                foreach (var path in candidates)
                {
                    if (File.Exists(path))
                    {
                        config.JavaPath = path;
                        break;
                    }
                }
            }

            // Create default offline account if list is empty
            if (config.Accounts.Count == 0)
            {
                var defaultAcc = Account.CreateOffline("RaidPlayer");
                config.Accounts.Add(defaultAcc);
                config.SelectedAccountUuid = defaultAcc.Uuid;
            }
            else if (string.IsNullOrEmpty(config.SelectedAccountUuid))
            {
                config.SelectedAccountUuid = config.Accounts[0].Uuid;
            }
        }
    }
}
