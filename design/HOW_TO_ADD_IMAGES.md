# How to Add the DALL-E Wolf Images to Your Android Project

## Important Note
The code has been updated to reference these three wolf images:
1. `ic_wolf_logo` - The geometric wolf head logo
2. `wolf_howling_splash` - The howling wolf under the moon
3. `wolf_pack_feature` - The running wolf pack

## Steps to Add the Images

### 1. Save the Images from DALL-E
Save the three images you showed me with these exact filenames:
- Wolf head logo → `ic_wolf_logo.png`
- Howling wolf → `wolf_howling_splash.png`  
- Wolf pack → `wolf_pack_feature.png`

### 2. Add Images to the Project
Place the PNG files in this directory:
```
app/src/main/res/drawable-nodpi/
```

Create the `drawable-nodpi` folder if it doesn't exist. The `-nodpi` suffix ensures Android won't scale these images.

### 3. Generate App Launcher Icons
For the app icon, you need multiple sizes. Use Android Studio's Image Asset tool:

1. Right-click on the `res` folder
2. Select `New` → `Image Asset`
3. Choose `Launcher Icons (Adaptive and Legacy)`
4. For the foreground layer: Browse and select your `ic_wolf_logo.png`
5. For the background layer: Select `Color` and use `#2B2D42` (wolf_charcoal)
6. Adjust the scaling so the wolf logo fits nicely with padding
7. Click `Next` and `Finish`

This will generate all the required mipmap sizes automatically.

### 4. Build and Run
After adding the images:
1. Sync your project (`File` → `Sync Project with Gradle Files`)
2. Clean and rebuild (`Build` → `Clean Project`, then `Build` → `Rebuild Project`)
3. Run the app on your emulator or device

## Where the Images Are Used

1. **App Icon**: Shows on device home screen and app drawer
2. **Splash Screen**: Wolf howling image appears when app launches
3. **Home Screen Header**: Small wolf logo next to "FORTIS LUPUS" 
4. **Empty State**: Faded wolf image when no workouts exist
5. **Feature Graphic**: Save for Google Play Store listing

## Troubleshooting

If images don't appear:
- Ensure filenames match exactly (case-sensitive)
- Check that files are in `drawable-nodpi` folder
- Try invalidating caches: `File` → `Invalidate Caches and Restart`
- Make sure images are in PNG format