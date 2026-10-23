using System;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using System.Windows;

namespace RaidLauncher.Services
{
    public class AppInstallerService
    {
        [DllImport("user32.dll", SetLastError = true, CharSet = CharSet.Auto)]
        private static extern IntPtr SendMessageTimeout(IntPtr hWnd, uint Msg, UIntPtr wParam, string lParam, uint fuFlags, uint uTimeout, out UIntPtr lpdwResult);

        private const uint HWND_BROADCAST = 0xFFFF;
        private const uint WM_SETTINGCHANGE = 0x001A;
        private const uint SMTO_ABORTIFHUNG = 0x0002;

        public static string GetInstallDirectory()
        {
            string localApp = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            return Path.Combine(localApp, "Programs", "RaidClient");
        }

        public static string GetInstalledExePath()
        {
            return Path.Combine(GetInstallDirectory(), "RaidLauncher.exe");
        }

        public static bool IsRunningFromInstallDirectory()
        {
            try
            {
                string currentExe = Environment.ProcessPath ?? Process.GetCurrentProcess().MainModule?.FileName ?? "";
                if (string.IsNullOrWhiteSpace(currentExe)) return true;

                string installDir = GetInstallDirectory().TrimEnd(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar);
                string exeDir = Path.GetDirectoryName(currentExe)?.TrimEnd(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar) ?? "";

                return string.Equals(installDir, exeDir, StringComparison.OrdinalIgnoreCase);
            }
            catch
            {
                return true;
            }
        }

        public static bool IsInUserPath()
        {
            try
            {
                string installDir = GetInstallDirectory();
                string? userPath = Environment.GetEnvironmentVariable("Path", EnvironmentVariableTarget.User);
                if (string.IsNullOrEmpty(userPath)) return false;

                var entries = userPath.Split(';', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);
                return entries.Any(e => string.Equals(e.TrimEnd(Path.DirectorySeparatorChar), installDir.TrimEnd(Path.DirectorySeparatorChar), StringComparison.OrdinalIgnoreCase));
            }
            catch
            {
                return false;
            }
        }

        public static void AddToUserPath(string directoryToAdd)
        {
            try
            {
                directoryToAdd = directoryToAdd.TrimEnd(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar);
                string? userPath = Environment.GetEnvironmentVariable("Path", EnvironmentVariableTarget.User) ?? "";
                var entries = userPath.Split(';', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries).ToList();

                if (!entries.Any(e => string.Equals(e.TrimEnd(Path.DirectorySeparatorChar), directoryToAdd, StringComparison.OrdinalIgnoreCase)))
                {
                    entries.Add(directoryToAdd);
                    string updatedPath = string.Join(";", entries);
                    Environment.SetEnvironmentVariable("Path", updatedPath, EnvironmentVariableTarget.User);

                    // Notificar a Windows del cambio de variables de entorno
                    try
                    {
                        SendMessageTimeout((IntPtr)HWND_BROADCAST, WM_SETTINGCHANGE, UIntPtr.Zero, "Environment", SMTO_ABORTIFHUNG, 2000, out _);
                    }
                    catch { }
                }
            }
            catch (Exception ex)
            {
                Debug.WriteLine("Error adding to user PATH: " + ex.Message);
            }
        }

        public static bool InstallApplication(bool addToPath, bool createDesktopShortcut, bool createStartMenuShortcut)
        {
            try
            {
                string currentExe = Environment.ProcessPath ?? Process.GetCurrentProcess().MainModule?.FileName ?? "";
                if (string.IsNullOrWhiteSpace(currentExe) || !File.Exists(currentExe))
                    return false;

                string installDir = GetInstallDirectory();
                Directory.CreateDirectory(installDir);

                string installedExe = GetInstalledExePath();

                // Si no estamos ejecutando ya desde el destino, copiamos el ejecutable
                if (!IsRunningFromInstallDirectory())
                {
                    File.Copy(currentExe, installedExe, true);
                }

                // Crear ejecutables auxiliares para la consola y el comando 'raid' o 'raidclient'
                CreateCliWrappers(installDir, installedExe);

                // Agregar al PATH de usuario
                if (addToPath)
                {
                    AddToUserPath(installDir);
                }

                // Crear acceso directo en el Escritorio
                if (createDesktopShortcut)
                {
                    string desktop = Environment.GetFolderPath(Environment.SpecialFolder.Desktop);
                    string desktopLink = Path.Combine(desktop, "Raid Client.lnk");
                    CreateWindowsShortcut(desktopLink, installedExe, "Lanzador oficial de Raid Client para Minecraft 1.8.9 PvP");
                }

                // Crear acceso directo en el Menú Inicio
                if (createStartMenuShortcut)
                {
                    string startMenu = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.StartMenu), "Programs");
                    Directory.CreateDirectory(startMenu);
                    string startMenuLink = Path.Combine(startMenu, "Raid Client.lnk");
                    CreateWindowsShortcut(startMenuLink, installedExe, "Lanzador oficial de Raid Client para Minecraft 1.8.9 PvP");
                }

                return true;
            }
            catch (Exception ex)
            {
                MessageBox.Show("Aviso durante la instalación: " + ex.Message, "Raid Launcher", MessageBoxButton.OK, MessageBoxImage.Warning);
                return false;
            }
        }

        private static void CreateCliWrappers(string installDir, string targetExe)
        {
            try
            {
                string raidCmd = Path.Combine(installDir, "raid.cmd");
                string raidClientCmd = Path.Combine(installDir, "raidclient.cmd");

                string content = "@echo off\r\nstart \"\" \"" + targetExe + "\" %*\r\n";

                File.WriteAllText(raidCmd, content);
                File.WriteAllText(raidClientCmd, content);
            }
            catch { }
        }

        public static void CreateWindowsShortcut(string shortcutPath, string targetExe, string description)
        {
            try
            {
                Type? shellType = Type.GetTypeFromProgID("WScript.Shell");
                if (shellType != null)
                {
                    dynamic? shell = Activator.CreateInstance(shellType);
                    if (shell != null)
                    {
                        dynamic shortcut = shell.CreateShortcut(shortcutPath);
                        shortcut.TargetPath = targetExe;
                        shortcut.WorkingDirectory = Path.GetDirectoryName(targetExe);
                        shortcut.Description = description;
                        shortcut.IconLocation = targetExe + ",0";
                        shortcut.Save();
                    }
                }
            }
            catch (Exception ex)
            {
                Debug.WriteLine("Error creating shortcut: " + ex.Message);
            }
        }

        public static void LaunchInstalledAndExit()
        {
            try
            {
                string installedExe = GetInstalledExePath();
                if (File.Exists(installedExe))
                {
                    Process.Start(new ProcessStartInfo
                    {
                        FileName = installedExe,
                        WorkingDirectory = Path.GetDirectoryName(installedExe),
                        UseShellExecute = true
                    });

                    Application.Current.Dispatcher.Invoke(() =>
                    {
                        Application.Current.Shutdown();
                    });
                }
            }
            catch { }
        }
    }
}
