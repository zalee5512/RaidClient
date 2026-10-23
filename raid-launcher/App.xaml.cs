using System;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Windows;
using RaidLauncher.Services;

namespace RaidLauncher
{
    public partial class App : Application
    {
        protected override void OnStartup(StartupEventArgs e)
        {
            base.OnStartup(e);

            // Si se pasa --no-install o ya estamos en la carpeta de instalación (%LOCALAPPDATA%\Programs\RaidClient)
            bool skipInstall = e.Args.Any(a => a.Equals("--no-install", StringComparison.OrdinalIgnoreCase));

            if (!skipInstall && !AppInstallerService.IsRunningFromInstallDirectory())
            {
                var dialog = new InstallDialogWindow();
                dialog.ShowDialog();

                // Si se instaló y lanzó el ejecutable nuevo, el proceso actual se cerrará por Shutdown().
                // Si el usuario eligió "Solo ejecutar por ahora", continuamos para abrir MainWindow.
                if (!dialog.ProceedWithRun)
                {
                    return;
                }
            }

            var mainWindow = new MainWindow();
            mainWindow.Show();
        }
    }
}
