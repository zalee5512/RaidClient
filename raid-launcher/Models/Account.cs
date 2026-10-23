using System;
using System.Security.Cryptography;
using System.Text;

namespace RaidLauncher.Models
{
    public class Account
    {
        public string Username { get; set; } = string.Empty;
        public string Uuid { get; set; } = string.Empty;
        public string AccessToken { get; set; } = "0";
        public bool IsPremium { get; set; } = false;
        public string AvatarUrl => $"https://minotar.net/avatar/{Username}/64.png";
        public DateTime AddedAt { get; set; } = DateTime.UtcNow;

        public static Account CreateOffline(string username)
        {
            // Standard Minecraft Offline UUID derivation: MD5("OfflinePlayer:" + username)
            byte[] hash = MD5.HashData(Encoding.UTF8.GetBytes("OfflinePlayer:" + username));
            hash[6] = (byte)((hash[6] & 0x0f) | 0x30); // version 3
            hash[8] = (byte)((hash[8] & 0x3f) | 0x80); // variant IETF
            var guid = new Guid(hash);

            return new Account
            {
                Username = username.Trim(),
                Uuid = guid.ToString("N"),
                AccessToken = "0",
                IsPremium = false
            };
        }
    }
}
