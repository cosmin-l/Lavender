Lavender - a multi platform text editor

![alt text](https://raw.githubusercontent.com/cosmin-l/Lavender/refs/heads/main/lavender.png)

# Lavender

A lightweight, cross-platform text editor built with Java Swing.

## Features

### Multi-Tab Editing
- Open multiple files simultaneously in a tabbed interface
- Each tab has independent undo/redo history
- Tab headers show filename with unsaved changes indicator (`*`)
- Close tabs with confirmation prompt when changes are unsaved
- Double-click empty tab bar to create a new tab

### File Management
- Open, save, and save-as via menu or keyboard shortcuts
- File browser sidebar with tree view
- Lazy-loaded subdirectories for performance
- Files sorted: directories first, then alphabetically
- Double-click files in the sidebar to open them
- Open any local folder in the sidebar
- Binary file detection with read-only hex dump viewer (offset + hex + ASCII, up to 512 KB)
- Automatic `.txt` extension if none is provided on save
- UTF-8 encoding with binary file detection

### Session Persistence
- Reopens all files from the previous session on launch
- Restores the last active tab
- Remembers sidebar folder, width, and visibility
- Persists theme, font size, and all toggle preferences

### Find & Replace
- Find bar appears at the bottom of the editor (`Cmd+F` / `Ctrl+F`)
- Live match highlighting as you type
- Match counter ("N of M")
- Navigate matches with `Enter` / `Shift+Enter` or the arrow buttons
- Find & Replace mode (`Cmd+H` / `Ctrl+H`) with Replace and Replace All
- Case-insensitive search
- Red highlight on the search field when no results are found

### Editor Enhancements
- **Line numbers** — right-aligned gutter that scales with font size
- **Minimap** — right-side document overview; click or drag to scroll
- **Status bar** — real-time line and column display (`Ln X, Col Y`)
- **Line wrap** — toggle word wrapping per tab
- **Tab size** — 4 spaces

### Themes
10 built-in dark themes, selectable from the View menu:

| Theme | Style |
|---|---|
| Midnight | Cool blue-gray |
| Dracula | Classic dark |
| Monokai | Warm code editor |
| Nord | Nordic cool palette |
| Gruvbox | Earthy warm tones |
| One Dark | Atom-inspired |
| Tokyo Night | Modern blue |
| Solarized Dark | Color-blind friendly |
| Abyss | Deep blue |
| Obsidian | Green accent |

Theme selection is applied live and persisted across sessions.

### Font Size
Choose from 10, 12, 14, 16, 18, 20, or 24 pt via View > Font Size. Applied immediately and persisted.

### Typewriter Sounds
Optional keystroke audio feedback (View > Typewriter Sounds). Uses a pool of 20 audio clips to prevent stutter at high typing speeds, with subtle random volume variation for a natural feel. Skips modifier keys and shortcuts.

### Clipboard & Editing
- Cut, Copy, Paste, Select All via menu and right-click context menu
- Undo (`Cmd+Z`) and Redo (`Cmd+Shift+Z`) per tab

### Platform Support
- **macOS** — native menu bar, `Cmd`-based shortcuts, dark appearance, About dialog
- **Windows / Linux** — `Ctrl`-based shortcuts, standard window chrome
- Packaged as a fat JAR (all dependencies included) or macOS DMG installer

## Keyboard Shortcuts

| Action | macOS | Windows / Linux |
|---|---|---|
| New Tab | `Cmd+T` | `Ctrl+T` |
| Open File | `Cmd+O` | `Ctrl+O` |
| Open Folder | `Cmd+Shift+O` | `Ctrl+Shift+O` |
| Save | `Cmd+S` | `Ctrl+S` |
| Save As | `Cmd+Shift+S` | `Ctrl+Shift+S` |
| Close Tab | `Cmd+W` | `Ctrl+W` |
| Exit | `Cmd+Q` | `Ctrl+Q` |
| Undo | `Cmd+Z` | `Ctrl+Z` |
| Redo | `Cmd+Shift+Z` | `Ctrl+Shift+Z` |
| Cut | `Cmd+X` | `Ctrl+X` |
| Copy | `Cmd+C` | `Ctrl+C` |
| Paste | `Cmd+V` | `Ctrl+V` |
| Select All | `Cmd+A` | `Ctrl+A` |
| Find | `Cmd+F` | `Ctrl+F` |
| Find & Replace | `Cmd+H` | `Ctrl+H` |
| Toggle Sidebar | `Cmd+B` | `Ctrl+B` |
| Toggle Line Numbers | `Cmd+Shift+L` | `Ctrl+Shift+L` |
| Toggle Minimap | `Cmd+Shift+M` | `Ctrl+Shift+M` |
| Toggle Line Wrap | `Cmd+Shift+W` | `Ctrl+Shift+W` |

## Building

```bash
./gradlew build       # Compile and run all checks
./gradlew test        # Run unit tests
./gradlew jar         # Build executable fat JAR
./gradlew dmg         # Build macOS DMG installer (macOS only)
./gradlew clean       # Clean build artifacts
```

Run the fat JAR directly:

```bash
java -jar build/libs/Lavender.jar
```

## Requirements

- Java 11+
- Gradle 9.2.0 (included via wrapper)
