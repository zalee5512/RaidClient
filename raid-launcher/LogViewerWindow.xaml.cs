using System;
using System.Diagnostics;
using System.IO;
using System.Windows;
using System.Windows.Input;
using RaidLauncher.Services;

namespace RaidLauncher
{
    public partial class LogViewerWindow : Window
    {
        private readonly string _logFile;

        public LogViewerWindow()
        {
            InitializeComponent();

            string appDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".raidlauncher");
            string clientDir = GameInstallerService.GetGameDir();
            string logClient = Path.Combine(clientDir, "logs", "minecraft_output.log");
            string logApp = Path.Combine(appDir, "minecraft_output.log");

            _logFile = File.Exists(logClient) ? logClient : logApp;

            LoadLogs();
        }

        public void LoadLogs()
        {
            try
            {
                if (File.Exists(_logFile))
                {
                    using var fs = new FileStream(_logFile, FileMode.Open, FileAccess.Read, FileShare.ReadWrite);
                    using var reader = new StreamReader(fs);
                    string content = reader.ReadToEnd();
                    TxtLogs.Text = string.IsNullOrWhiteSpace(content) ? "El archivo de registro está vacío." : content;
                    TxtLogs.ScrollToEnd();
                    TxtStatus.Text = $"Archivo: {Path.GetFileName(_logFile)} ({new FileInfo(_logFile).Length / 1024} KB)";
                }
                else
                {
                    TxtLogs.Text = "No se ha generado ningún registro de salida todavía.\nInicia el juego para registrar la salida de la consola de Minecraft.";
                    TxtStatus.Text = "Sin archivo de log";
                }
            }
            catch (Exception ex)
            {
                TxtLogs.Text = "Error al leer el archivo de log: " + ex.Message;
            }
        }

        private void TitleBar_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            if (e.ButtonState == MouseButtonState.Pressed)
            {
                DragMove();
            }
        }

        private void CloseButton_Click(object sender, RoutedEventArgs e) => Close();

        private void BtnRefresh_Click(object sender, RoutedEventArgs e) => LoadLogs();

        private void BtnCopy_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                if (!string.IsNullOrWhiteSpace(TxtLogs.Text))
                {
                    Clipboard.SetText(TxtLogs.Text);
                    MessageBox.Show("¡Log copiado al portapapeles! Puedes pegarlo directamente para enviarlo.", "Raid Client - Logs", MessageBoxButton.OK, MessageBoxImage.Information);
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show("Error al copiar: " + ex.Message, "Raid Client", MessageBoxButton.OK, MessageBoxImage.Warning);
            }
        }

        private void BtnOpenFolder_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                string dir = Path.GetDirectoryName(_logFile) ?? GameInstallerService.GetGameDir();
                if (Directory.Exists(dir))
                {
                    Process.Start(new ProcessStartInfo
                    {
                        FileName = dir,
                        UseShellExecute = true
                    });
                }
            }
            catch { }
        }
    }
}
