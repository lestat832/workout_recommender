# Fix Wolf Icon - Step by Step Guide

## Quick Fix Using Android Studio's Image Asset Tool

### Step 1: Save Your DALL-E Wolf Logo
1. Save the geometric wolf head image you showed me earlier (the blue wolf on dark background)
2. Name it something like `wolf_logo_original.png`
3. Remember where you saved it

### Step 2: Open Image Asset Tool
1. In Android Studio, right-click on the `app/src/main/res` folder
2. Select **New → Image Asset**

### Step 3: Configure the Icon
1. **Icon Type**: Select "Launcher Icons (Adaptive and Legacy)"
2. **Name**: Keep it as `ic_launcher`
3. **Foreground Layer**:
   - Click on "Path" and browse to your `wolf_logo_original.png`
   - **Scaling**: Adjust the slider to make the wolf fit nicely (usually around 60-75%)
   - **Trim**: Yes (this removes extra transparent space)
4. **Background Layer**:
   - Select "Color"
   - Click the color box and enter: `#2B2D42` (wolf_charcoal)

### Step 4: Preview and Adjust
1. Check the preview on the right side
2. Make sure the wolf is centered and not cut off
3. The round icon preview should show the wolf nicely centered

### Step 5: Generate
1. Click **Next**
2. Android Studio will show you all the files it will create/overwrite
3. Click **Finish**

### Step 6: Clean and Rebuild
1. **Build → Clean Project**
2. **Build → Rebuild Project**
3. Run the app

## What This Does
- Creates all required icon sizes (mdpi through xxxhdpi)
- Generates adaptive icons for Android 8.0+
- Replaces the terrible placeholder with your actual wolf logo
- Handles both square and round icon shapes

## If You Have Issues
- Make sure your original image is high resolution (at least 512x512px)
- Try adjusting the scaling if the wolf appears too large or small
- The background color should be the dark charcoal (#2B2D42)

## Result
Your app icon will now show the proper geometric wolf head logo you created in DALL-E, not the abstract shape I made as a placeholder!