using System.Windows;
using System.Windows.Input;
using RaidLauncher.Services;

namespace RaidLauncher
{
    public partial class InstallDialogWindow : Window
    {
        public bool ProceedWithRun { get; private set; } = false;

        public InstallDialogWindow()
        {
            InitializeComponent();
            TxtInstallPath.Text = AppInstallerService.GetInstallDirectory();
        }

        private void TitleBar_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
        {
            if (e.ButtonState == MouseButtonState.Pressed)
            {
                DragMove();
            }
        }

        private void CloseButton_Click(object sender, RoutedEventArgs e)
        {
            ProceedWithRun = true;
            Close();
        }

        private void BtnSkip_Click(object sender, RoutedEventArgs e)
        {
            ProceedWithRun = true;
            Close();
        }

        private void BtnInstall_Click(object sender, RoutedEventArgs e)
        {
            bool addToPath = ChkAddToPath.IsChecked == true;
            bool desktop = ChkDesktopShortcut.IsChecked == true;
            bool startMenu = ChkStartMenuShortcut.IsChecked == true;

            bool success = AppInstallerService.InstallApplication(addToPath, desktop, startMenu);
            if (success)
            {
                // Iniciar la versión instalada y cerrar el ejecutable actual de Downloads
                AppInstallerService.LaunchInstalledAndExit();
            }
            else
            {
                ProceedWithRun = true;
                Close();
            }
        }
    }
}
