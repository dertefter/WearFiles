# FAQ

[Читать на русском](FAQ_RU.md)

Here is a collection of popular questions and answers.

## 1. I found a bug / I want to suggest an idea.
You can leave an issue, and we also have a [Telegram chat](https://t.me/wearfiles_app).

## 2. I didn't understand anything about ADB and granting permissions. Please explain what's what?

So, I'll try to describe it in simple terms. Let's start from afar. In Android, there are several ways to access files:

### [MediaStore API](https://developer.android.com/training/data-storage/shared/media)

- This is a way to access photos/videos/music
- The following permissions are used: `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`
- In this mode, we **do not get** permission to access all files (`MANAGE_EXTERNAL_STORAGE`)
- The permissions `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO` can be granted directly from the watch, there won't be any hassle with ADB

##### Example:
From the main screen, you open the "Photos" section. The watch will request the necessary permission itself: a standard permission request dialog will open.
After granting the permissions, you will be able to view the list of photos, as well as open and view them. **That's it!**

### [Full file access](https://developer.android.com/training/data-storage/manage-all-files)

- This is a way to get full access to files on the device.
- The `MANAGE_EXTERNAL_STORAGE` permission is required

##### Example:

From the main screen, you open the "Device storage" section. If the permission is granted, you will be able to:
- View files and folders on the device
- Move, delete, rename files and folders
- Create folders

To grant the permission for access to all files, you may need to connect to the device via ADB. Or maybe not :)

### Why is that?

For a long time, the permission for access to all files was broken on WearOS. Usually, on devices running **WearOS 6 and earlier**, this permission is not shown in the permission settings and cannot be granted just like that.

However, on **WearOS 7**, according to reports from many users, this permission is available again and **can be granted** directly from the watch without using ADB.

### Let's sum up

- If you just want to view photos/videos on the device. Or if you just want to transfer a file from your phone and open it on the watch:
  **You don't need to bother with ADB**.
- If you want to use the WearFiles app as a file manager on your watch: you may have to bother with ADB.

## 3. What needs to be done to transfer files from a smartphone to the watch?

- Install the app on the smartphone and on the watch.
- Make sure both are updated to the latest version.
- Make sure the watch is connected to the smartphone.
- Make sure Google services are updated and the watch is detected in the WearOS app (or in the Watch app if you have a Pixel Watch)

File transfer relies on Google services, so the stability of file transfer heavily depends on them.


## 4. My watch/smartphone doesn't have Google services / doesn't have Google Play. Will I be able to transfer files from my smartphone to the watch?

**No. File transfer is impossible if one of the devices doesn't have Google services.**


## 5. I am 100% sure that file transfer should work, but the app doesn't see my watch.

- At the moment, this problem does exist. It appears on some devices.
- I can't determine exactly what this problem is related to. File transfer heavily depends on Google services, so it's difficult to identify the source of the problem.
- Try the advice from point 2. I also recommend checking whether the WearFiles app, Google services, and the WearOS app are being killed by the system.