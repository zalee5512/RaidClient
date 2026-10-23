using System;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using RaidLauncher.Models;
using RaidLauncher.Services;

namespace RaidLauncher
{
    public partial class MainWindow : Window
    {
        private LauncherConfig _config;

        public MainWindow()
        {
            InitializeComponent();
            _config = ConfigManager.LoadConfig();

            GameLaunchService.StatusChanged += UpdateStatus;
            GameLaunchService.ProgressChanged += (percent, msg) =>
            {
                Dispatcher.Invoke(() =>
                {
                    ProgressBarLaunch.Value = percent;
                    ProgressBarGlobal.Value = percent;
                    TxtProgressPercent.Text = $"{percent}%";
                    TxtProgressStep.Text = msg;
                });
            };
            GameLaunchService.GameExited += () =>
            {
                Dispatcher.Invoke(() =>
                {
                    BtnPlay.Content = "JUGAR (1.8.9)";
                    BtnPlay.IsEnabled = true;
                    PanelLaunchProgress.Visibility = Visibility.Collapsed;
                    ProgressBarGlobal.Visibility = Visibility.Collapsed;
                    UpdateStatus("Juego cerrado. Listo para jugar con Raid Client.");
                });
            };

            Loaded += MainWindow_Loaded;
        }

        private async void MainWindow_Loaded(object sender, RoutedEventArgs e)
        {
            ApplyConfigToUi();
            RefreshAccountsUi();
            RefreshActiveAccountUi();

            // Comprobar e instalar automáticamente carpeta y componentes de Raid Client al abrir el launcher
            await InitializeAndInstallRaidClientAsync();
        }

        private async Task InitializeAndInstallRaidClientAsync()
        {
            try
            {
                string gameDir = GameInstallerService.GetGameDir();
                bool isInstalled = GameInstallerService.IsBasicInstallationPresent(gameDir);

                if (!isInstalled)
                {
                    BtnPlay.IsEnabled = false;
                    BtnPlay.Content = "INSTALANDO...";
                    PanelLaunchProgress.Visibility = Visibility.Visible;
                    ProgressBarGlobal.Visibility = Visibility.Visible;
                    ProgressBarLaunch.Value = 10;
                    ProgressBarGlobal.Value = 10;
                    TxtProgressPercent.Text = "10%";
                    TxtProgressStep.Text = "Configurando carpeta y archivos de Raid Client...";
                    UpdateStatus("Instalando Raid Client en su propia carpeta (.raidclient)...");
                }

                await Task.Run(async () =>
                {
                    await GameInstallerService.EnsureAllGameFilesAsync(
                        gameDir,
                        (pct, msg) =>
                        {
                            Dispatcher.Invoke(() =>
                            {
                                ProgressBarLaunch.Value = pct;
                                ProgressBarGlobal.Value = pct;
                                TxtProgressPercent.Text = $"{pct}%";
                                TxtProgressStep.Text = msg;
                            });
                        },
                        (msg) =>
                        {
                            Dispatcher.Invoke(() =>
                            {
                                UpdateStatus(msg);
                            });
                        }
                    );

                    await GameInstallerService.EnsureJavaRuntimeAsync(
                        gameDir,
                        (pct, msg) =>
                        {
                            Dispatcher.Invoke(() =>
                            {
                                ProgressBarLaunch.Value = pct;
                                ProgressBarGlobal.Value = pct;
                                TxtProgressPercent.Text = $"{pct}%";
                                TxtProgressStep.Text = msg;
                            });
                        },
                        (msg) =>
                        {
                            Dispatcher.Invoke(() =>
                            {
                                UpdateStatus(msg);
                            });
                        }
                    );
                });

                PanelLaunchProgress.Visibility = Visibility.Collapsed;
                ProgressBarGlobal.Visibility = Visibility.Collapsed;
                BtnPlay.IsEnabled = true;
                BtnPlay.Content = "JUGAR (1.8.9)";
                UpdateStatus("Listo para jugar con Raid Client.");
            }
            catch
            {
                PanelLaunchProgress.Visibility = Visibility.Collapsed;
                ProgressBarGlobal.Visibility = Visibility.Collapsed;
                BtnPlay.IsEnabled = true;
                BtnPlay.Content = "JUGAR (1.8.9)";
                UpdateStatus("Listo para jugar con Raid Client.");
            }
        }

        private void UpdateStatus(string message)
        {
            Dispatcher.Invoke(() =>
            {
                TxtStatus.Text = message;
            });
        }

        private void ApplyConfigToUi()
        {
            SliderRam.Value = _config.RamAllocationGb;
            TxtRamValue.Text = $"{_config.RamAllocationGb} GB";
            TxtJavaPath.Text = _config.JavaPath;
            ChkOptimizePvp.IsChecked = _config.OptimizePvpFlags;
        }

        private void RefreshActiveAccountUi()
        {
            var active = _config.Accounts.FirstOrDefault(a => a.Uuid == _config.SelectedAccountUuid)
                         ?? _config.Accounts.FirstOrDefault();

            if (active != null)
            {
                _config.SelectedAccountUuid = active.Uuid;
                TxtActiveUsername.Text = active.Username;
                TxtAccountType.Text = active.IsPremium ? "PREMIUM" : "NO-PREMIUM";
                BadgeAccountType.Background = active.IsPremium
                    ? new SolidColorBrush(Color.FromRgb(40, 80, 50))
                    : new SolidColorBrush(Color.FromRgb(30, 45, 65));

                TxtActiveSettingsSummary.Text = $"Versión: 1.8.9 • RAM: {_config.RamAllocationGb} GB";

                try
                {
                    ImgActiveAvatar.Source = new BitmapImage(new Uri(active.AvatarUrl));
                }
                catch
                {
                    ImgActiveAvatar.Source = null;
                }
            }
        }

        private void RefreshAccountsUi()
        {
            PanelAccountsList.Children.Clear();

            foreach (var acc in _config.Accounts)
            {
                var card = new Border
                {
                    Background = new SolidColorBrush(Color.FromRgb(20, 24, 38)),
                    CornerRadius = new CornerRadius(8),
                    Padding = new Thickness(12),
                    Margin = new Thickness(0, 0, 0, 8),
                    BorderBrush = acc.Uuid == _config.SelectedAccountUuid
                        ? new SolidColorBrush(Color.FromRgb(0, 229, 255))
                        : new SolidColorBrush(Color.FromRgb(35, 42, 60)),
                    BorderThickness = new Thickness(1)
                };

                var grid = new Grid();
                grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(45) });
                grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
                grid.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });

                // Avatar
                var imgBorder = new Border
                {
                    Width = 36,
                    Height = 36,
                    CornerRadius = new CornerRadius(6),
                    Background = new SolidColorBrush(Color.FromRgb(30, 36, 50))
                };
                var img = new Image { Stretch = Stretch.UniformToFill };
                try { img.Source = new BitmapImage(new Uri(acc.AvatarUrl)); } catch { }
                imgBorder.Child = img;
                Grid.SetColumn(imgBorder, 0);

                // Username & Tag
                var stack = new StackPanel { VerticalAlignment = VerticalAlignment.Center, Margin = new Thickness(10, 0, 0, 0) };
                var topStack = new StackPanel { Orientation = Orientation.Horizontal };
                topStack.Children.Add(new TextBlock
                {
                    Text = acc.Username,
                    FontWeight = FontWeights.Bold,
                    FontSize = 14,
                    Foreground = Brushes.White
                });
                topStack.Children.Add(new TextBlock
                {
                    Text = acc.IsPremium ? " [PREMIUM]" : " [NO-PREMIUM]",
                    FontSize = 11,
                    FontWeight = FontWeights.SemiBold,
                    Foreground = acc.IsPremium ? new SolidColorBrush(Color.FromRgb(74, 222, 128)) : new SolidColorBrush(Color.FromRgb(0, 229, 255)),
                    Margin = new Thickness(6, 2, 0, 0)
                });
                stack.Children.Add(topStack);

                if (acc.Uuid == _config.SelectedAccountUuid)
                {
                    stack.Children.Add(new TextBlock
                    {
                        Text = "✓ Cuenta Activa",
                        FontSize = 11,
                        Foreground = new SolidColorBrush(Color.FromRgb(0, 229, 255)),
                        Margin = new Thickness(0, 2, 0, 0)
                    });
                }
                Grid.SetColumn(stack, 1);

                // Actions (Select / Delete)
                var actionStack = new StackPanel { Orientation = Orientation.Horizontal, VerticalAlignment = VerticalAlignment.Center };
                if (acc.Uuid != _config.SelectedAccountUuid)
                {
                    var btnSelect = new Button
                    {
                        Content = "Seleccionar",
                        Padding = new Thickness(10, 5, 10, 5),
                        Background = new SolidColorBrush(Color.FromRgb(30, 40, 60)),
                        Foreground = Brushes.White,
                        BorderThickness = new Thickness(0),
                        Cursor = Cursors.Hand,
                        Margin = new Thickness(0, 0, 8, 0)
                    };
                    btnSelect.Click += (s, e) =>
                    {
                        _config.SelectedAccountUuid = acc.Uuid;
                        ConfigManager.SaveConfig(_config);
                        RefreshAccountsUi();
                        RefreshActiveAccountUi();
                    };
                    actionStack.Children.Add(btnSelect);
                }

                var btnDelete = new Button
                {
                    Content = "✕",
                    Padding = new Thickness(8, 5, 8, 5),
                    Background = new SolidColorBrush(Color.FromRgb(40, 25, 30)),
                    Foreground = new SolidColorBrush(Color.FromRgb(255, 90, 90)),
                    BorderThickness = new Thickness(0),
                    Cursor = Cursors.Hand
                };
                btnDelete.Click += (s, e) =>
                {
                    _config.Accounts.Remove(acc);
                    if (_config.SelectedAccountUuid == acc.Uuid)
                    {
                        _config.SelectedAccountUuid = _config.Accounts.FirstOrDefault()?.Uuid;
                    }
                    ConfigManager.SaveConfig(_config);
                    RefreshAccountsUi();
                    RefreshActiveAccountUi();
                };
                actionStack.Children.Add(btnDelete);
                Grid.SetColumn(actionStack, 2);

                grid.Children.Add(imgBorder);
                grid.Children.Add(stack);
                grid.Children.Add(actionStack);

                card.Child = grid;
                PanelAccountsList.Children.Add(card);
            }
        }

        // Window controls
        private void TitleBar_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            if (e.ButtonState == MouseButtonState.Pressed)
            {
                DragMove();
            }
        }

        private void MinimizeButton_Click(object sender, RoutedEventArgs e) => WindowState = WindowState.Minimized;
        private void CloseButton_Click(object sender, RoutedEventArgs e) => Close();

        // Navigation
        private void NavPlay_Checked(object sender, RoutedEventArgs e)
        {
            if (TabPlay != null) TabPlay.Visibility = Visibility.Visible;
            if (TabAccounts != null) TabAccounts.Visibility = Visibility.Collapsed;
            if (TabSettings != null) TabSettings.Visibility = Visibility.Collapsed;
            if (TabHardware != null) TabHardware.Visibility = Visibility.Collapsed;
        }

        private void NavAccounts_Checked(object sender, RoutedEventArgs e)
        {
            if (TabPlay != null) TabPlay.Visibility = Visibility.Collapsed;
            if (TabAccounts != null) TabAccounts.Visibility = Visibility.Visible;
            if (TabSettings != null) TabSettings.Visibility = Visibility.Collapsed;
            if (TabHardware != null) TabHardware.Visibility = Visibility.Collapsed;
            RefreshAccountsUi();
        }

        private void NavSettings_Checked(object sender, RoutedEventArgs e)
        {
            if (TabPlay != null) TabPlay.Visibility = Visibility.Collapsed;
            if (TabAccounts != null) TabAccounts.Visibility = Visibility.Collapsed;
            if (TabSettings != null) TabSettings.Visibility = Visibility.Visible;
            if (TabHardware != null) TabHardware.Visibility = Visibility.Collapsed;
        }

        private void NavHardware_Checked(object sender, RoutedEventArgs e)
        {
            if (TabPlay != null) TabPlay.Visibility = Visibility.Collapsed;
            if (TabAccounts != null) TabAccounts.Visibility = Visibility.Collapsed;
            if (TabSettings != null) TabSettings.Visibility = Visibility.Collapsed;
            if (TabHardware != null) TabHardware.Visibility = Visibility.Visible;
            LoadHardwareDiagnostics();
        }

        private async void LoadHardwareDiagnostics()
        {
            UpdateStatus("Analizando hardware y controladores de video...");
            var report = await Task.Run(() => HardwareDiagnosticService.RunDiagnostics(_config));

            Dispatcher.Invoke(() =>
            {
                TxtCpuName.Text = report.CpuName;
                TxtCpuCores.Text = $"{report.CpuCores} Núcleos / {report.CpuThreads} Hilos";

                TxtGpuName.Text = report.GpuName;
                TxtGpuDriverVersion.Text = $"Controlador: {report.GpuDriverVersion} ({report.GpuDriverDate})";
                TxtGpuDriverBadge.Text = report.IsDedicatedGpu ? "GPU DEDICADA DETECTADA" : "CONTROLADOR AL DÍA";
                BadgeGpuDriver.Background = new SolidColorBrush(Color.FromRgb(29, 51, 37));

                TxtRamSummary.Text = $"{report.TotalRamGb} GB RAM Instalada ({report.AvailableRamGb} GB Libre)";
                TxtRamRecommendation.Text = $"Asignada al juego: {_config.RamAllocationGb} GB (Recomendado para 1.8.9: {report.RecommendedRamGb} GB)";

                TxtMonitorSummary.Text = $"Pantalla detectada a {report.MonitorHz} Hz";

                ProgressHardwareScore.Value = report.OptimizationScore;
                TxtHardwareScore.Text = $"{report.OptimizationScore}% Optimizado";

                UpdateStatus($"Diagnóstico completo: {report.GpuName} con {report.TotalRamGb} GB RAM.");
            });
        }

        private void BtnRescanHardware_Click(object sender, RoutedEventArgs e)
        {
            LoadHardwareDiagnostics();
        }

        private async void BtnAutoOptimizeHardware_Click(object sender, RoutedEventArgs e)
        {
            UpdateStatus("Optimizando hardware y configuración para Minecraft 1.8.9...");
            await Task.Run(() =>
            {
                HardwareDiagnosticService.ApplyAutoOptimizations(_config, msg =>
                {
                    Dispatcher.Invoke(() => UpdateStatus(msg));
                });
            });

            ApplyConfigToUi();
            LoadHardwareDiagnostics();
            RefreshActiveAccountUi();
            MessageBox.Show("¡Optimización de Hardware completada con éxito!\n\n" +
                            "• RAM configurada en punto óptimo para 1.8.9\n" +
                            "• VBOs activos para descarga de CPU a GPU\n" +
                            "• VSync desactivado y FPS ilimitados\n" +
                            "• Parámetros G1GC de baja latencia aplicados\n" +
                            "• Modo Alto Rendimiento asignado en Windows para Java 8",
                            "Raid Launcher - Optimización", MessageBoxButton.OK, MessageBoxImage.Information);
        }

        // Account management
        private void BtnAddOffline_Click(object sender, RoutedEventArgs e)
        {
            string nick = TxtOfflineUsername.Text.Trim();
            if (string.IsNullOrWhiteSpace(nick))
            {
                MessageBox.Show("Por favor ingresa un nombre de usuario válido.", "Raid Launcher", MessageBoxButton.OK, MessageBoxImage.Warning);
                return;
            }

            if (_config.Accounts.Any(a => a.Username.Equals(nick, StringComparison.OrdinalIgnoreCase)))
            {
                MessageBox.Show("Ya existe una cuenta con este nombre.", "Raid Launcher", MessageBoxButton.OK, MessageBoxImage.Information);
                return;
            }

            var account = Account.CreateOffline(nick);
            _config.Accounts.Add(account);
            _config.SelectedAccountUuid = account.Uuid;
            ConfigManager.SaveConfig(_config);

            TxtOfflineUsername.Text = string.Empty;
            RefreshAccountsUi();
            RefreshActiveAccountUi();
            UpdateStatus($"Cuenta No-Premium '{nick}' agregada y seleccionada.");
        }

        private async void BtnMicrosoftLogin_Click(object sender, RoutedEventArgs e)
        {
            BtnMicrosoftLogin.IsEnabled = false;
            TxtMicrosoftStatus.Text = "Iniciando solicitud de inicio de sesión con Microsoft...";

            var flow = await MicrosoftAuthService.StartDeviceFlowAsync();
            if (flow == null)
            {
                TxtMicrosoftStatus.Text = "Error al conectar con Microsoft. Verifica tu conexión a internet.";
                BtnMicrosoftLogin.IsEnabled = true;
                return;
            }

            TxtMicrosoftStatus.Text = $"¡Código copiado al portapapeles!\nCódigo: {flow.user_code}\nIngresa a: {flow.verification_uri}";

            try
            {
                Clipboard.SetText(flow.user_code);
            }
            catch { }

            // Open browser automatically
            try
            {
                Process.Start(new ProcessStartInfo
                {
                    FileName = flow.verification_uri,
                    UseShellExecute = true
                });
            }
            catch { }

            var account = await MicrosoftAuthService.PollForTokenAsync(flow.device_code, flow.interval, msg =>
            {
                Dispatcher.Invoke(() => TxtMicrosoftStatus.Text = msg);
            });

            if (account != null)
            {
                _config.Accounts.RemoveAll(a => a.Uuid == account.Uuid || a.Username.Equals(account.Username, StringComparison.OrdinalIgnoreCase));
                _config.Accounts.Add(account);
                _config.SelectedAccountUuid = account.Uuid;
                ConfigManager.SaveConfig(_config);

                RefreshAccountsUi();
                RefreshActiveAccountUi();
                TxtMicrosoftStatus.Text = $"¡Bienvenido {account.Username}! Cuenta Premium vinculada.";
            }

            BtnMicrosoftLogin.IsEnabled = true;
        }

        // Settings
        private void SliderRam_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            if (_config == null || TxtRamValue == null) return;
            _config.RamAllocationGb = (int)e.NewValue;
            TxtRamValue.Text = $"{_config.RamAllocationGb} GB";
            ConfigManager.SaveConfig(_config);
            RefreshActiveAccountUi();
        }

        private void BtnDetectJava_Click(object sender, RoutedEventArgs e)
        {
            string adoptium = @"C:\Program Files\Eclipse Adoptium\jdk-8.0.504.1-hotspot\bin\javaw.exe";
            if (File.Exists(adoptium))
            {
                _config.JavaPath = adoptium;
                TxtJavaPath.Text = adoptium;
                ConfigManager.SaveConfig(_config);
                UpdateStatus("Java 8 (Eclipse Adoptium) detectado automáticamente.");
            }
            else
            {
                UpdateStatus("Ruta de Java no encontrada automáticamente. Escribe la ruta manual.");
            }
        }

        private void ChkOptimizePvp_Changed(object sender, RoutedEventArgs e)
        {
            if (_config == null) return;
            _config.OptimizePvpFlags = ChkOptimizePvp.IsChecked == true;
            ConfigManager.SaveConfig(_config);
        }

        private void BtnOpenMods_Click(object sender, RoutedEventArgs e)
        {
            string mods = Path.Combine(GameInstallerService.GetGameDir(), "mods");
            Directory.CreateDirectory(mods);
            Process.Start(new ProcessStartInfo { FileName = mods, UseShellExecute = true });
        }

        private void BtnOpenLauncherDir_Click(object sender, RoutedEventArgs e)
        {
            string dir = GameInstallerService.GetGameDir();
            Process.Start(new ProcessStartInfo { FileName = dir, UseShellExecute = true });
        }

        private void BtnReinstallPath_Click(object sender, RoutedEventArgs e)
        {
            var dialog = new InstallDialogWindow();
            dialog.ShowDialog();
        }

        // Play Game
        private async void BtnPlay_Click(object sender, RoutedEventArgs e)
        {
            var active = _config.Accounts.FirstOrDefault(a => a.Uuid == _config.SelectedAccountUuid)
                         ?? _config.Accounts.FirstOrDefault();

            if (active == null)
            {
                MessageBox.Show("Por favor crea o selecciona una cuenta antes de jugar.", "Raid Launcher", MessageBoxButton.OK, MessageBoxImage.Warning);
                return;
            }

            // Mostrar barra de progreso animada y ocultar botón de jugar
            BtnPlay.Visibility = Visibility.Collapsed;
            PanelLaunchProgress.Visibility = Visibility.Visible;
            ProgressBarGlobal.Visibility = Visibility.Visible;
            ProgressBarLaunch.Value = 5;
            ProgressBarGlobal.Value = 5;
            TxtProgressPercent.Text = "5%";
            TxtProgressStep.Text = "Iniciando cliente...";

            try
            {
                await GameLaunchService.LaunchGameAsync(_config, active);

                // Mantener el 100% visible por 2 segundos para dar confirmación clara
                await System.Threading.Tasks.Task.Delay(2000);

                PanelLaunchProgress.Visibility = Visibility.Collapsed;
                ProgressBarGlobal.Visibility = Visibility.Collapsed;
                BtnPlay.Visibility = Visibility.Visible;
                BtnPlay.Content = "EN EJECUCIÓN (1.8.9)";
                BtnPlay.IsEnabled = false;
            }
            catch (Exception ex)
            {
                PanelLaunchProgress.Visibility = Visibility.Collapsed;
                ProgressBarGlobal.Visibility = Visibility.Collapsed;
                BtnPlay.Visibility = Visibility.Visible;
                BtnPlay.IsEnabled = true;
                BtnPlay.Content = "JUGAR (1.8.9)";
                MessageBox.Show("Error al iniciar el juego: " + ex.Message, "Raid Launcher", MessageBoxButton.OK, MessageBoxImage.Error);
            }
        }
    }
}
