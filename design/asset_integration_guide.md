# Asset Integration Guide for Fortis Lupus

## How to Add the DALL-E Generated Images

### 1. Save the Images
Save the three DALL-E images with these filenames:
- Wolf head logo → `ic_wolf_logo.png`
- Howling wolf splash → `wolf_howling_splash.png`
- Wolf pack feature → `wolf_pack_feature.png`

### 2. Generate App Icon Sizes
Use the wolf head logo to create launcher icons in these sizes:

**For mipmap-mdpi (48x48dp):**
- `ic_launcher.png` - 48x48px
- `ic_launcher_round.png` - 48x48px

**For mipmap-hdpi (72x72dp):**
- `ic_launcher.png` - 72x72px
- `ic_launcher_round.png` - 72x72px

**For mipmap-xhdpi (96x96dp):**
- `ic_launcher.png` - 96x96px
- `ic_launcher_round.png` - 96x96px

**For mipmap-xxhdpi (144x144dp):**
- `ic_launcher.png` - 144x144px
- `ic_launcher_round.png` - 144x144px

**For mipmap-xxxhdpi (192x192dp):**
- `ic_launcher.png` - 192x192px
- `ic_launcher_round.png` - 192x192px

### 3. Add Images to Project
1. Delete the placeholder XML files I created
2. Add the PNG images to these locations:
   - `app/src/main/res/drawable-nodpi/ic_wolf_logo.png`
   - `app/src/main/res/drawable-nodpi/wolf_howling_splash.png`
   - `app/src/main/res/drawable-nodpi/wolf_pack_feature.png`
3. Add launcher icons to respective mipmap folders

### 4. Tools for Icon Generation
You can use these tools to generate the icon sizes:
- Android Studio's Image Asset Studio (Right-click on res → New → Image Asset)
- Online tool: https://romannurik.github.io/AndroidAssetStudio/
- Command line: ImageMagick

Example ImageMagick command:
```bash
convert ic_wolf_logo.png -resize 48x48 mipmap-mdpi/ic_launcher.png
convert ic_wolf_logo.png -resize 72x72 mipmap-hdpi/ic_launcher.png
convert ic_wolf_logo.png -resize 96x96 mipmap-xhdpi/ic_launcher.png
convert ic_wolf_logo.png -resize 144x144 mipmap-xxhdpi/ic_launcher.png
convert ic_wolf_logo.png -resize 192x192 mipmap-xxxhdpi/ic_launcher.png
```

### 5. Adaptive Icon Setup
For Android 8.0+ adaptive icons, you'll need:
- Foreground layer: Wolf logo with padding
- Background layer: Solid #2B2D42 color

The existing `ic_launcher.xml` files in `mipmap-anydpi-v26` will handle this.