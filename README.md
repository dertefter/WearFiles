<p align="center">
  <img src="art/banner.png" alt="Banner">
</p>

<a href="https://play.google.com/store/apps/details?id=com.dertefter.wearfiles">
  <img src="https://github.com/dertefter/some_stuff_for_me/blob/master/com.dertefter.wearfiles_downloads.svg" alt="Google Play" height="60">
</a>
<a href="https://play.google.com/store/apps/details?id=com.dertefter.wearfiles">
  <img src="https://github.com/dertefter/some_stuff_for_me/blob/master/com.dertefter.wearfiles_rating.svg" alt="Google Play" height="60">
</a>
<a href="README_RU.md">
  <img src="https://img.shields.io/badge/Russian-blue?style=for-the-badge" alt="Русский язык" height="60">
</a>

# WearFiles

A simple open-source file manager designed for Wear OS ⌚

### App Features:

- File transfer from phone to watch ⌚➡️📱
- View, open, delete files on Wear OS device 📂
- Clipboard: cut / copy / paste 📋
- Pin files and folders to the main screen 📌
- Built-in image viewer 🖼️
- Built-in PDF viewer 📄

### Screenshots
<p align="center">  
  <img src="art/screenshot_1.png" width="160" alt="Screenshot 1">  
  <img src="art/screenshot_2.png" width="160" alt="Screenshot 2">  
  <img src="art/screenshot_3.png" width="160" alt="Screenshot 3">  
  <img src="art/screenshot_4.png" width="160" alt="Screenshot 4">  
  <img src="art/screenshot_5.png" width="160" alt="Screenshot 5">  
</p>  

### File transfer from phone to watch:

You can transfer files from your smartphone to your watch. To do this, install the app on both devices and make sure the connection with the watch is established.

<p align="center">
  <img src="art/example_file_transfer.gif" width="320" alt="example_file_transfer">
</p>

### ⚠️ File Access Permission ⚠️

To use the app on your watch as a file manager, you need to grant file access permission: `MANAGE_EXTERNAL_STORAGE`. Due to Wear OS platform limitations, the app cannot request this permission on its own.
However, you can use the app without it in a limited mode. You probably **don't need full file access if you just want to transfer a file from your phone and open it on your watch**.

#### What works without `MANAGE_EXTERNAL_STORAGE` permission:

- "Photos": view the list of images stored on the device
- "Videos": videos stored on the device
- "Music": list of audio files on the device
- "Received": files received from the phone
- Opening the above files (if software to open these file types is available)

#### How to grant permission manually:
If you decide that you need full access to files, you can grant permission using ADB:

1. Connect the watch to the computer via ADB
2. Run the command:  
   ``adb shell appops set --uid com.dertefter.wearfiles MANAGE_EXTERNAL_STORAGE allow``
3. Restart the app

### 💎 Support me:
[https://www.donationalerts.com/r/dertefter](https://www.donationalerts.com/r/dertefter)

## Star History

<a href="https://star-history.dera.page/#dertefter/WearFiles">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://star-history.dera.page/svg?repos=dertefter/WearFiles&theme=dark" />
   <source media="(prefers-color-scheme: light)" srcset="https://star-history.dera.page/svg?repos=dertefter/WearFiles" />
   <img alt="Star History Chart" src="https://star-history.dera.page/svg?repos=dertefter/WearFiles" />
 </picture>
</a>