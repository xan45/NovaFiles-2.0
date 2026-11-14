# Support & Troubleshooting

Welcome to the NovaFiles support center. If you encounter difficulties, follow the troubleshooting guidance below or reach out via our GitHub community resources.

---

## Where to Get Help

- **Bug Reports**: If you find unexpected behavior, crashes, or incorrect file operations, submit a report using the [Bug Report Template](https://github.com/xan45/NovaFiles-2.0/issues/new?template=bug_report.md).
- **Feature Requests**: If you have ideas for new features or improvements, submit a proposal using the [Feature Request Template](https://github.com/xan45/NovaFiles-2.0/issues/new?template=feature_request.md).
- **General Questions**: Open a discussion under [GitHub Discussions](https://github.com/xan45/NovaFiles-2.0/discussions) or browse existing issues.

---

## Common Troubleshooting

### 1. "Permission Denied" or Cannot See Files
NovaFiles requires Android's **All files access** (`MANAGE_EXTERNAL_STORAGE`) permission on Android 11+ to read and modify storage.
- Go to device **Settings** -> **Apps** -> **Special app access** -> **All files access**.
- Locate **NovaFiles** and toggle the switch **ON**.

### 2. Cannot Install APK ("App Not Installed" or Security Block)
If Android prevents installing the downloaded APK:
- Ensure your device is running **Android 8.0 (API Level 26)** or newer.
- Enable **Install unknown apps** for the browser or download manager you used to fetch the APK.
- If Google Play Protect displays a warning for a freshly compiled alpha APK, tap **More details** -> **Install anyway**.

### 3. Background Audio Stops Playing
On Android 12+, system battery optimization may kill background services:
- Go to device **Settings** -> **Apps** -> **NovaFiles** -> **Battery**.
- Select **Unrestricted** to allow continuous background audio playback and FTP server transfers.

### 4. FTP Server Cannot Be Reached
- Verify that your Android device and PC/client are connected to the **same Wi-Fi network**.
- Ensure client uses passive FTP transfer mode (`PASV`).
- Check that the port displayed in NovaFiles matches the port configured in your FTP client.
