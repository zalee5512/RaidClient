using System;
using System.Collections.Generic;

namespace RaidLauncher.Models
{
    public class LauncherConfig
    {
        public int RamAllocationGb { get; set; } = 2;
        public string JavaPath { get; set; } = @"C:\Program Files\Eclipse Adoptium\jdk-8.0.504.1-hotspot\bin\javaw.exe";
        public string CustomJvmArgs { get; set; } = "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=20 -XX:+DisableExplicitGC";
        public int WindowWidth { get; set; } = 1280;
        public int WindowHeight { get; set; } = 720;
        public bool Fullscreen { get; set; } = false;
        public bool OptimizePvpFlags { get; set; } = true;
        public string? SelectedAccountUuid { get; set; }
        public List<Account> Accounts { get; set; } = new List<Account>();
    }
}
