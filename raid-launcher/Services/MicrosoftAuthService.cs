using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using RaidLauncher.Models;

namespace RaidLauncher.Services
{
    public class MicrosoftAuthService
    {
        // Microsoft Nintendo Switch Client ID (widely recognized for device code auth on live.com)
        private const string ClientId = "00000000441cc96b";
        private static readonly HttpClient Http = new HttpClient();

        public class DeviceCodeResponse
        {
            public string device_code { get; set; } = string.Empty;
            public string user_code { get; set; } = string.Empty;
            public string verification_uri { get; set; } = string.Empty;
            public int expires_in { get; set; }
            public int interval { get; set; } = 5;
            public string message { get; set; } = string.Empty;
        }

        public static async Task<DeviceCodeResponse?> StartDeviceFlowAsync()
        {
            try
            {
                var content = new FormUrlEncodedContent(new[]
                {
                    new KeyValuePair<string, string>("client_id", ClientId),
                    new KeyValuePair<string, string>("scope", "service::user.auth.xboxlive.com::MBI_SSL"),
                    new KeyValuePair<string, string>("response_type", "device_code")
                });

                var response = await Http.PostAsync("https://login.live.com/oauth20_connect.srf", content);
                if (response.IsSuccessStatusCode)
                {
                    string json = await response.Content.ReadAsStringAsync();
                    return JsonSerializer.Deserialize<DeviceCodeResponse>(json);
                }
                else
                {
                    string err = await response.Content.ReadAsStringAsync();
                    System.Diagnostics.Debug.WriteLine("Live connect error: " + err);
                }
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine("Device code error: " + ex.Message);
            }
            return null;
        }

        public static async Task<Account?> PollForTokenAsync(string deviceCode, int intervalSeconds, Action<string> statusCallback)
        {
            DateTime expireTime = DateTime.UtcNow.AddMinutes(10);
            while (DateTime.UtcNow < expireTime)
            {
                await Task.Delay(intervalSeconds * 1000);

                var content = new FormUrlEncodedContent(new[]
                {
                    new KeyValuePair<string, string>("client_id", ClientId),
                    new KeyValuePair<string, string>("grant_type", "urn:ietf:params:oauth:grant-type:device_code"),
                    new KeyValuePair<string, string>("device_code", deviceCode)
                });

                var response = await Http.PostAsync("https://login.live.com/oauth20_token.srf", content);
                string json = await response.Content.ReadAsStringAsync();

                if (response.IsSuccessStatusCode)
                {
                    using var doc = JsonDocument.Parse(json);
                    string msAccessToken = doc.RootElement.GetProperty("access_token").GetString()!;
                    statusCallback("¡Código verificado! Conectando con Xbox Live...");
                    return await AuthenticateWithXboxAsync(msAccessToken, statusCallback);
                }

                if (json.Contains("authorization_pending"))
                {
                    statusCallback("Esperando que introduzcas el código en el navegador...");
                    continue;
                }

                if (json.Contains("expired_token") || json.Contains("access_denied"))
                {
                    statusCallback("La sesión expiró o fue cancelada.");
                    return null;
                }
            }
            return null;
        }

        private static async Task<Account?> AuthenticateWithXboxAsync(string msAccessToken, Action<string> status)
        {
            try
            {
                // 1. Xbox Live authentication
                status("Conectando con Xbox Live...");
                var xblReq = new
                {
                    Properties = new
                    {
                        AuthMethod = "RPS",
                        SiteName = "user.auth.xboxlive.com",
                        RpsTicket = $"t={msAccessToken}"
                    },
                    RelyingParty = "http://auth.xboxlive.com",
                    TokenType = "JWT"
                };

                var xblRes = await Http.PostAsync(
                    "https://user.auth.xboxlive.com/user/authenticate",
                    new StringContent(JsonSerializer.Serialize(xblReq), Encoding.UTF8, "application/json")
                );

                if (!xblRes.IsSuccessStatusCode)
                {
                    status("Error en Xbox Live: " + await xblRes.Content.ReadAsStringAsync());
                    return null;
                }

                string xblJson = await xblRes.Content.ReadAsStringAsync();
                using var xblDoc = JsonDocument.Parse(xblJson);
                string xblToken = xblDoc.RootElement.GetProperty("Token").GetString()!;
                string uhs = xblDoc.RootElement.GetProperty("DisplayClaims").GetProperty("xui")[0].GetProperty("uhs").GetString()!;

                // 2. XSTS Token
                status("Obteniendo credenciales XSTS...");
                var xstsReq = new
                {
                    Properties = new
                    {
                        SandboxId = "RETAIL",
                        UserTokens = new[] { xblToken }
                    },
                    RelyingParty = "rp://api.minecraftservices.com/",
                    TokenType = "JWT"
                };

                var xstsRes = await Http.PostAsync(
                    "https://xsts.auth.xboxlive.com/xsts/authorize",
                    new StringContent(JsonSerializer.Serialize(xstsReq), Encoding.UTF8, "application/json")
                );

                if (!xstsRes.IsSuccessStatusCode)
                {
                    status("Error en XSTS: " + await xstsRes.Content.ReadAsStringAsync());
                    return null;
                }

                string xstsJson = await xstsRes.Content.ReadAsStringAsync();
                using var xstsDoc = JsonDocument.Parse(xstsJson);
                string xstsToken = xstsDoc.RootElement.GetProperty("Token").GetString()!;

                // 3. Minecraft Token
                status("Iniciando sesión en Minecraft...");
                var mcReq = new { identityToken = $"XBL3.0 x={uhs};{xstsToken}" };
                var mcRes = await Http.PostAsync(
                    "https://api.minecraftservices.com/authentication/login_with_xbox",
                    new StringContent(JsonSerializer.Serialize(mcReq), Encoding.UTF8, "application/json")
                );

                if (!mcRes.IsSuccessStatusCode)
                {
                    status("Error en Minecraft Services: " + await mcRes.Content.ReadAsStringAsync());
                    return null;
                }

                string mcJson = await mcRes.Content.ReadAsStringAsync();
                using var mcDoc = JsonDocument.Parse(mcJson);
                string mcToken = mcDoc.RootElement.GetProperty("access_token").GetString()!;

                // 4. Minecraft Profile
                status("Descargando perfil de jugador...");
                var profileReq = new HttpRequestMessage(HttpMethod.Get, "https://api.minecraftservices.com/minecraft/profile");
                profileReq.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", mcToken);

                var profileRes = await Http.SendAsync(profileReq);
                if (!profileRes.IsSuccessStatusCode)
                {
                    status("Error al obtener perfil: " + await profileRes.Content.ReadAsStringAsync());
                    return null;
                }

                string profileJson = await profileRes.Content.ReadAsStringAsync();
                using var profileDoc = JsonDocument.Parse(profileJson);
                string username = profileDoc.RootElement.GetProperty("name").GetString()!;
                string uuid = profileDoc.RootElement.GetProperty("id").GetString()!;

                status("¡Cuenta Premium iniciada correctamente!");
                return new Account
                {
                    Username = username,
                    Uuid = uuid,
                    AccessToken = mcToken,
                    IsPremium = true
                };
            }
            catch (Exception ex)
            {
                status("Error: " + ex.Message);
                return null;
            }
        }
    }
}
