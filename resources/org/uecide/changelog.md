Changelog
=========

0.10.3
------

* Fixed headless LAF crash
* Fixed js plugin preferences tree bug
* Cleaned up embedded repsitory creation
* Update internal repository
* Adjustment to refresh strategy
* Major improvements to javascript plugin system
* Removed obsolete copy for forum plugin
* Fixed local package install


0.10.2
------

* Fixed bug compiling cpp file from open tab
* Removed _DOWN_ from masks
* Fixed other invalid function


0.10.1
------

* Removed usage of extended menu shortcut function
* Cleaned up all deprecation warnings
* Forced splash to float in AwesomeWM
* Moved JTattoo into core and upgraded to 1.6.11
* Reinstated updated web links in help menu
* Switched data folder open to open not browse
* Added editor margin theme settings
* Optimized windows startup time


0.10.0
------

* Release candidate 3
* Moved initial serial port probing into separate thread
* Added missing Javascript plugin icons
* Abstracted HTTP requests to set proper user agent
* Ignored file not found error on HTTP get
* Updated repos master package
* Fixed uninstall of local install files
* Merge branch 'master' of github.com:UECIDE/UECIDE
* Added missing true return on port set/clear
* Update issue templates
* Cleaned up image conversion settings dialog and added threshold setting
* Added XBM target image conversion format for u8glib et al
* Added automatic release generation
* New changelog formatter using Markdown
* Cleanup of function bookmark parsing
* Tidy up of splash screen
* Add --reset-preferences CLI option
* Protect against divide-by-zero with zero sized font
* Fixed unable to select no conversion for images
* Made font scale temporary and local to each editor instance
* Removed duplication of image object
* Added convolution matrix
* Added create new png/jpg/gif file
* Fixed overflow of rubberband and added tooltips to tools
* Broken tools out into individual classes
* Implemented crop
* Basic gfx editor, and binary file conversion system
* Rework markdown editor
* Tightly integrated ardublocks
* Reworked editor selection code
* Fixed anti-aliasing on markdown panels
* Fixed prototype insertion location bug that crept in with new function prototype scanning
* Cleaned up old deb packaging target
* Assign CTRL-SHIFT-T to Serial Terminal
* Advanced token parser allows more context-aware options in popup menu
* Shift+Ctrl+C now toggles single-line comments for the current line or selection
* Fix divider not changing after minimal mode
* Improve markdown display and example browser
* Cleanup of syntax code
* Added token-aware context menu. Added manual page links. Improved token parsing. Added variable and class bookmarks
* Improved icon handling and made animated icons
* Added minimalist mode
* Auto conversion of old float split sizes
* Improved tree cell rendering
* Fixed split layout prefs and added split lock option
* Fixed split positions
* Fixed tree background colour
* Fix antialiasing problem and console incorrect font scaling
* Fixed serial terminal input line color with dark theme
* Removed serial terminal from internal repo
* Re-write of the theme system
* Plugins now in the core
* Add port pragma support


0.9.6
-----

* Updated families database
* Fixed bug with extracting control file that isn't specifically gzipped
* Improved OS X bundling
* Updated bundled packages


0.9.5
-----

* Improved pragma handling
* Updated source format


0.9.4
-----

* Updated version
* Fixed prototype return type formatting


0.9.3
-----

* Improvements to plugin manager tree system
* Updated bundled ctags
* Refined ctags regex
* Better USB attribute handling
* Ignore class members in tags parsing
* Ordered core file compilation


0.9.2
-----

* Added hostname support to data location preference
* Improved busy spinner
* Solved random null pointer when compiling
* Hidden tool execution spam
* Package updates
* Added tool-based context menu entries
* :Merge branch 'master' of github.com:UECIDE/UECIDE
* Overhauled mDNS board discovery
* Added missing lang3 dependency
* Made tree split as proportion of sub-window not entire window
* Improved missing port error handling
* Accelerate boot more and quieter startup
* New split location saving system
* Solve double cancel of compilation. Clean up network discovery. Remove extraneous exceptions
* Fixed deprecated and unchecked warnings
* Switch to commons-text instead of commons-lang3
* Switch custom file copy to NIO and pass exceptions up the call chain properly
* Switch to ant-dep
* Imported jfontchooser into the core source
* Added apple stubs to maven list
* Removed apt saving in boot
* Switched maven deps to maven-ant
* Upgrade to jline3
* Build time download of many deps
* Overhauled hex file handling
* Optimizing string catenations
* Cleaned up with FindBugs scanning
* Removed redundant function list functions
* Overhauled function bookmarks
* Scrapped redundant FunctionPrototype and switched to FunctionBookmark
* Fixed tab in function prototype issue
* Removed save-before-compile requirement (it broke examples). Cleaned up parsing and build file copying
* Bundled ctags and overhauled boot installation of packages
* Cleanup excess debugging
* Added Tool UObject. Switched to ctags for parsing. Accelerate boot with file cache.
* Updated serial terminal
* Added port disconnect support
* Added concurrent compilation of libraries
* Accelerated boot sequence with multithreading


0.9.1
-----

* Prevent last sketch opening if either headless or a sketch given on command line
* Fixed local libraries and implemented delete delay/retry code for locked files


0.9.0
-----

* Adopted new sensible numbering schema
* Removed unneeded import
* Added extra baud rates and improved Ecma execution
* Added "portable" folder support
* Fixed excepion on remove not found
* Improved error reporting in apt
* Added realtime SHA checking of downloads


0.8.9
-----

* Removed superfluous output and updated serial terminal
* Rotated markdown editor
* Cleaned up apt
* Linked mouse wheel on output console to scroll
* Removed unneeded jsch
* Implemented Gritty terminal for output and deb installation progress
* Deprecated outdated java tests
* Allow manual override of port name on POSIX systems from command line
* Improved missing library detection
* Improved library recursion for non-preproc compilers
* Replaced JAVA stub for OSX
* Fixed problem of null pointer on missing res: repo file
* Updated plist to Java 1.8
* Only add extra ports that actually exist (posix only)
* Added console error reporting for failed downloads of packages and repos
* Updated bundled varcmd package to 1.0.3
* First iteration of example browser
* Reinstated adding of missing port at sketch load
* Added displaying full path of library when compiling
* Fix no save as on close of unsaved sketch
* Possible fox for font corruption on update
* Trapped failed extract of file
* Null Pointer when replacing non-existant package
* Temporarily removed SHA25 integrity check
* Temporarily removed SHA25 integrity check
* Added experimental JavaScript plugin support
* Only send raw output to output tab when being otherwise preprocessed
* Added package validation system
* Added raw output to output tab
* Updated JSch to 0.1.54
* Switched to dual launch4j launchers in Windows
* Only erase lines from console if the character following a CR is
* not
* an NL
* Upgraded windows FULL bundles to JRE 8
* Updated varcmd package
* Added CR for console line clearing
* Added download file integrity check
* Fixed package file link dereferencing without full path
* Fixed null pointer with no default option in sketch.cfg
* Added recursive uninstall of packages and updated varcmd package
* Added NOP logger
* Added slf4j api
* Updated jmDNS
* Updated RSyntaxTextArea
* Added changelog window
* Added changelog generation
* Added some files to gitignore for tidiness
* Added updated VS files
* Display locally installed packages in tree
* Improved board no-core event debugging
* Added manual package installation option
* Converted varcmds to javascript scriptlets
* Added missing sub-sub-group nodes to plugin manager tree
* Fixed multiple-source overwrite bug
* Updated families package
* Removed redundant uecide-repositories package from internal repo
* Added multi-repo file system
* Updated serial terminal
* Included master repo package
* Reinstated basic Translate for existing plugins
* Added more I18N entries
* Fixed ant-contrib inclusion
* Added internationalization support
* Improved console buffering strategy
* Updated editor
* Enhanced port command. Added hex file merging
* Bumped version
* Added script-based tools menu entry support
* Bumped version
* Added themes to PiM
* Couple of cleanups
* Added bullet3, Usb HID device support, kill support for builtin commands
* Added programmer objects and associated control logic and commands
* Added spinner icon graphics
* Added selected board/core/compiler names to top level hardware menu
* Added MCU list (Most Commonly Used) in Files menu
* Reverted draggable tabs
* Fixed Replaces: handling in GUI
* Updated preferences entries
* Added load/save dialog location remembering option and mark-all enable/disable option
* Overhaul of sketch properties system and updated RSyntaxTextArea
* Added version info file
* Draggable tabs and ultiple panes
* Added missing widgets
* Added support for editor data capture
* Updated internal repository
* Added plugin popup menu support and console copy menu
* Added console background tileable images
* Added font scaling
* Added theming to other components in the system
* Complete overhaul of theme system
* Properly handled escaped characters in comment stripping
* Properly handled escaped characters in comment stripping
* Fixed preference mismatch in autosave facility


0.8.8
-----

* Armoured integer parsing in IOKit
* Fixed linux packaging error
* Fixed broken menu shortcuts
* Cleaned up parsed errors to console
* Added ANSI codes to parsed console errors and warnings
* Fixed broken shortcut keys
* Releasing new beta
* Overhauled debian building scripts
* Added configurable reset delays
* Fixed plugin preferences default loading
* Added border to preferences window
* Re-added debug files used by some plugins
* Updated and added missing licenses
* Removed redundant files
* Added library list and port list widgets for preferences
* Added parsable message strings
* Fixed error and warning message flagging and todo list clearing
* Fixed version conflict
* Added some new builtins
* Fixed splash screen to middle of default screen in multiheaded environment
* Overhaul of communications systems
* Added environment handling, and fixed double-context creation in sketches
* Gradually cleaning up the prefs system
* Bumped version
* Added embedded binary support
* Fixed broken redirection for object editor
* New Context system for messaging and execution
* Overhaul of scripting and command systems
* Better javascript integration
* Some more builtin commands for file writing and jar loading
* New preferences tree system
* Fixed bug with varcmd in baud rate setting
* Fixed ordering of option menu
* Added missing launch4j files
* Added launch4j config
* Bumped version
* Fixed crash at startup with .ino file parameter
* Added support for adublock URI
* Fixed splash screen flickering
* Bumped version
* Improved crash reporter
* Improved manual handling and project search facility
* Moved repo list into BorderLayout panel
* Added missing archive files
* Moved APT into core and added repository handling
* Updated version
* Added build status
* Added script tag
* Fixed ant deps error
* Made console unbuffered
* Made command input stream unbuffered at last
* Fixed unknown version in debian testing
* Added GPIO control, delay, and linux serial lock files
* Fixed some headless cli bugs with new install
* Added interactive CLI system
* Added online/offline option to file menu
* Added offline mode
* Numerous bug fixes. Setting to recreate makefile each compile
* Forced apt update before library installation
* Added version flag
* Added . as valid sketch name for mkmf
* Added Makefile generation and some extra force flags
* Fixed arm apt name
* Bumped Version
* Added command line options to manipulate APT repositories and packages
* Separated library and core build files from sketch build files
* Added apt
* Locked editor based services from running during compilation
* Bumped version
* Moved plugin manager into core Added missing library identification and installation Created markdown editor with realtime preview
* Added missing markdown processor jar
* Working on new repo system
* Rolled back custom parser for property files
* Bumped version
* Fixed escape bug in property file. Updated jgit. Added cloning to GitLink
* Added Arduino config file parsing vc
* Bumped version
* Added library options system
* Implemented icon sets
* Bumped version
* Added random number VC and added UUID to UECIDE VC.
* Complete overhaul of icon system. Added purge pragma parameter
* Fixed return value on headless compile or upload failure
* Bumped version
* Added per-sketch icon support
* Fixed UTF-8 saving in OS X
* Added full support for recursive .git finding
* Added recursive .git finding
* Hardened some functions with limits
* Added toolbar icon and line drawing support
* Rewrote protocol to flexible stack based system
* Added commands to set size and colour of LCD screen
* Made the window and widget resizable
* Added virtual LCD plugin
* Made extra ports override real ports when the extra is a symlink
* Bumped version
* Added pre- and post-commands to compile and upload
* Bumped version
* Added grapher to repo
* Added grapher to repo
* Added descriptions to plugins
* Added parsing to more strings in Sketch
* Added Variable Command for getting selected options data
* Incided jgit into the core
* Improved status line
* Updated stub to find all jre8 releases
* Fixed windows exe packaging
* Added menu entries and keyboard shortcuts for compile and upload


0.8.7
-----

* Fixed crash reports
* Updated version
* Fixed XP launch, and some cosmetics
* Removed dependency version restrictions for launchpad
* Added ant as build depend
* Debianized package
* Added windows and osx version probing
* Fixed prefs open in MacOSX
* Updated version
* Added option to disable crash reporter
* Fixed some null pointer errors
* Added OS version data and moved varfuncs to base
* Fixed compilation when main header is manually included
* Fixing concurrent modifications
* Fix compilation for newer JNA
* Fixed library naming problem
* Build enhancements to support more compilers
* Rebuilt console without HTML
* Added source for windows stub
* Bundled new plugin manager;
* Updated jna, added native java support on windows
* Fixed exe loader stub at last!
* Switched to using a batch file to load windows version for now until a better stub can be crafted
* Fixed windows and mac boot stubs
* Remove autoformat bundle
* Added new bundles of plugins
* Added keyword.txt handling
* Big refactoring uecide.app -> org.uecide. New plugins loading methodology
* Fixed a subtle bug in lib include selection
* Fixed preferences repainting on theme change and cleaned up toolbar button borders
* Fixed OS X application file permissions
* Bumped version
* Finally fixed wandering tree entries
* Fixed windows slowness
* Added code page handling
* Added gutter flags
* Stopped the function list from bouncing around so much
* Added note/todo/fixme parsing and task lists
* Tidy up of the build process
* Bumped version
* Fixed sketch libraries
* Fixed null pointer in getTabCount. Added snippets plugin. Added experimental background compiler service
* Upgrade RSTA
* Bumped version
* Added new required libraries
* Added services and migrated some threads and timers to the service infrastructure
* Fixed MRU initialize bug
* Made all plugin commands non-static and added javascript engine
* Improved command line handling
* Improved crash reporting system
* Sorted function list alphabetically
* Fixed tree update collision bug
* Added crash reporter
* Added manual browser
* Caught possible NullPointer exception in sketch loading
* Added context menu to console
* Fixed typo in console CSS
* Bumped version
* Changed console to HTML
* Added theme importer plugin
* Added third party library list and made About box HTML
* Backed out property file loader
* Reverted propertyfile loader update
* Fixed release
* Added USB javax source
* Added USB support, property file includes, etc
* Added USB HID device detection
* Big cleanup, plus better handling for 1.5.x libraries
* Made ssh and scp forget the password on authentication failure
* Added preferred programmer selection to network discovery service
* Fixed serial terminal method crash in OS X java 1.6_15
* Added goto/set/fail/end special functions to script
* Rewritten command execution routines
* Fixed null pointer in non-sketch upload
* Improvements to script system
* Updated version
* Fixed too many open files bug
* Added jmdns library
* Added Jsch library
* Cleaned up error output
* Added network ssh uploading
* Fixed plugin not-installed version bug
* Improved full screen with menu
* Merge branch 'master' of github.com:UECIDE/UECIDE
* Removed border painting control from toolbar buttons
* Added full screen mode
* Updated version
* Added office laf jar
* Added tinylaf jar
* Added extra LAFs and some fixes to themes
* Added selectable editor themes
* Added experimental tree merge routine
* Updated version
* Added auto-refresh of function list. Added documentation and markdown section
* Fixed new repo problem
* Added revert to last commit on files
* Updated version
* Added git ignore
* Added new stub for icon overlay
* Updated launch4j for trusty
* Embedded executables
* Removed old system plugins folder code
* Added ASM labels with corresponding .ent sections to tree function list
* Added template plugin
* Fixed missing asm files in tree
* Fixed sketch properties saving
* Added function list in project tree
* Fixed nested comment stripping
* Added bracket adding options
* Added Artistic Style plugin
* Fixed Save As path breaking
* Forced opened files to be java.io.File class
* Fixed library install bug
* Properly fixed windows compile #line problem
* Added elf inspector
* Added error flagging
* Added project search. Fixed windows file path #line issue
* Updated version
* Added plugin callbacks to context menus
* Fixed debian package lint problem
* Updated version
* Improved facilties for debugging sketches
* Added improved about box
* Added open data folder debug entry
* Updated bundled serial terminal
* Fix bug in serial terminal for width > 80 characters
* Rememberd last opened folder in import file. Added https and ftp to windows URL open handling
* Rememberd last opened folder in import file. Added https and ftp to windows URL open handling
* Merge pull request #39 from TCWORLD/master
* Corrected "Open In OS"
* Changed version url for new site layout
* Updated version
* Ported copy for forum plugin
* Bundled autoformat
* Ported AutoFormat plugin
* Updated plugin manager
* Made plugin manager modal
* Updated download URL for new site layout
* Updated patch to r
* Fixed stellaris compilation bugs
* Added sketch properties system
* Added sketch properties system
* Saved console split position as offset from top instead of bottom
* Fixed lib-local header file issue in library recursion
* Fixed Open in OS in Windows
* Fixed library compilation crashing in windows due to rogue :
* Updated version
* Added tools for opening files and folders externally
* Merge pull request #36 from UECIDE/complete_rebuild
* New support files URL
* Fixed old plugins crashing new version
* Fixed NullPointerException when no core installed
* Updated version
* Fixed disappearing output messages problem
* Moved library storage to Library class
* Added option to disable version check. Limited version checking to 3 hourly at most
* Added updated version checking
* Added libraries-within-libraries
* Updated version
* Fixed auto opening of plugin manager
* Fixed bad plugin manager tree refresh with no board selected in editor
* Updated version
* Fixed PropertyFile file locking issue
* Forced editor menu rebuild on board install/deinstall. Forced complete rebuild on plugin manager close.
* Added tooltips to tree
* Added multiple install/uninstall to pligin manager
* Added multiple install/uninstall to pligin manager
* Updated version
* Brought main plugins into main tree
* Added export local library option
* Added export local library option
* Added library template generator
* Made complete rebuild a separate thread
* Made find loop and added found failed background change
* Updated version
* Updated version
* Added import SAR file
* Added export as SAR file
* Added export as SAR file
* Added compile / upload abort button
* Added example sketch and localization protection
* Fixed editor font bug. Added USB device ident for Linux.
* Fixed serial lock problem
* Possible fix to locked port
* Updated version
* Fixed Save As main file bug, and attempted to fix bluetooth problems
* Added missing saveTo() function stub
* Updated version
* Added option to hide secondary editor toolbar
* Fixed saveAs empty file bug
* Fixed rescan all ports on tab change
* Various bug fixes
* Added prefs and theme getters in config file variables
* Added simple bitmap viewer
* Added simple bitmap viewer
* Fixed file save "modified outside" bug
* Updated version
* Made library utility recursion optional in library.txt file
* Allow headless compilation/upload
* Fixed DTR reset bug in Windows
* Added file watcher timer to tabs to monitor external file changes
* Switched most hashes to TreeHash.  Fixed some null pointer bugs.  Open new sketch in existing editor if untitled and not modified
* Fixed plugin install problems
* Improved library category adding
* Rewritten library installer. Ditched sketchbook libraries folder (made it part of categories)
* Added file tree, drag and drop, and lots more improvements
* Fixed saving of preferences on application exit
* Added build number generation, contributor list generation and sorting of user preferences file
* Cleaned up dir structure, implemented about menu entry
* Fixed #pragma parameter string space splitting
* Fixed virgin installation issues
* Added "unable to find port" error alert to serial system
* Added new plugin manager bundle
* Merge branch 'complete_rebuild' of github.com:UECIDE/UECIDE into complete_rebuild
* Plugin system work
* Added option to keep find & replace open
* More plugin work and added basic upload support
* Added libraries, and re-worked plugin system
* Library compilation meter, plus other cleaning up and progress towards completion
* Added code to indent / outdent buttons
* More progress towards the best IDE ever
* Added new plugin toolbar icon system
* Added editor toolbar
* More work re-writing all the functionality
* Hidden RSyntaxTextArea build folder
* Removed build tree and core tree and just created the tree
* Massive cleanup of the file structure.  All third party code removed from the main source tree and either placed into auto-updating third party area or placed in lib folder as .jar files.
* update version
* Embarked on a daring resign


0.8.5
-----

* Fixed headless LAF crash
* Fixed js plugin preferences tree bug
* Cleaned up embedded repsitory creation
* Update internal repository
* Adjustment to refresh strategy
* Major improvements to javascript plugin system
* Removed obsolete copy for forum plugin
* Fixed local package install
* Fixed bug compiling cpp file from open tab
* Removed _DOWN_ from masks
* Fixed other invalid function
* Removed usage of extended menu shortcut function
* Cleaned up all deprecation warnings
* Forced splash to float in AwesomeWM
* Moved JTattoo into core and upgraded to 1.6.11
* Reinstated updated web links in help menu
* Switched data folder open to open not browse
* Added editor margin theme settings
* Optimized windows startup time
* Release candidate 3
* Moved initial serial port probing into separate thread
* Added missing Javascript plugin icons
* Abstracted HTTP requests to set proper user agent
* Ignored file not found error on HTTP get
* Updated repos master package
* Fixed uninstall of local install files
* Merge branch 'master' of github.com:UECIDE/UECIDE
* Added missing true return on port set/clear
* Update issue templates
* Cleaned up image conversion settings dialog and added threshold setting
* Added XBM target image conversion format for u8glib et al
* Added automatic release generation
* New changelog formatter using Markdown
* Cleanup of function bookmark parsing
* Tidy up of splash screen
* Add --reset-preferences CLI option
* Protect against divide-by-zero with zero sized font
* Fixed unable to select no conversion for images
* Made font scale temporary and local to each editor instance
* Removed duplication of image object
* Added convolution matrix
* Added create new png/jpg/gif file
* Fixed overflow of rubberband and added tooltips to tools
* Broken tools out into individual classes
* Implemented crop
* Basic gfx editor, and binary file conversion system
* Rework markdown editor
* Tightly integrated ardublocks
* Reworked editor selection code
* Fixed anti-aliasing on markdown panels
* Fixed prototype insertion location bug that crept in with new function prototype scanning
* Cleaned up old deb packaging target
* Assign CTRL-SHIFT-T to Serial Terminal
* Advanced token parser allows more context-aware options in popup menu
* Shift+Ctrl+C now toggles single-line comments for the current line or selection
* Fix divider not changing after minimal mode
* Improve markdown display and example browser
* Cleanup of syntax code
* Added token-aware context menu. Added manual page links. Improved token parsing. Added variable and class bookmarks
* Improved icon handling and made animated icons
* Added minimalist mode
* Auto conversion of old float split sizes
* Improved tree cell rendering
* Fixed split layout prefs and added split lock option
* Fixed split positions
* Fixed tree background colour
* Fix antialiasing problem and console incorrect font scaling
* Fixed serial terminal input line color with dark theme
* Removed serial terminal from internal repo
* Re-write of the theme system
* Plugins now in the core
* Add port pragma support
* Updated families database
* Fixed bug with extracting control file that isn't specifically gzipped
* Improved OS X bundling
* Updated bundled packages
* Improved pragma handling
* Updated source format
* Updated version
* Fixed prototype return type formatting
* Improvements to plugin manager tree system
* Updated bundled ctags
* Refined ctags regex
* Better USB attribute handling
* Ignore class members in tags parsing
* Ordered core file compilation
* Added hostname support to data location preference
* Improved busy spinner
* Solved random null pointer when compiling
* Hidden tool execution spam
* Package updates
* Added tool-based context menu entries
* :Merge branch 'master' of github.com:UECIDE/UECIDE
* Overhauled mDNS board discovery
* Added missing lang3 dependency
* Made tree split as proportion of sub-window not entire window
* Improved missing port error handling
* Accelerate boot more and quieter startup
* New split location saving system
* Solve double cancel of compilation. Clean up network discovery. Remove extraneous exceptions
* Fixed deprecated and unchecked warnings
* Switch to commons-text instead of commons-lang3
* Switch custom file copy to NIO and pass exceptions up the call chain properly
* Switch to ant-dep
* Imported jfontchooser into the core source
* Added apple stubs to maven list
* Removed apt saving in boot
* Switched maven deps to maven-ant
* Upgrade to jline3
* Build time download of many deps
* Overhauled hex file handling
* Optimizing string catenations
* Cleaned up with FindBugs scanning
* Removed redundant function list functions
* Overhauled function bookmarks
* Scrapped redundant FunctionPrototype and switched to FunctionBookmark
* Fixed tab in function prototype issue
* Removed save-before-compile requirement (it broke examples). Cleaned up parsing and build file copying
* Bundled ctags and overhauled boot installation of packages
* Cleanup excess debugging
* Added Tool UObject. Switched to ctags for parsing. Accelerate boot with file cache.
* Updated serial terminal
* Added port disconnect support
* Added concurrent compilation of libraries
* Accelerated boot sequence with multithreading
* Prevent last sketch opening if either headless or a sketch given on command line
* Fixed local libraries and implemented delete delay/retry code for locked files
* Adopted new sensible numbering schema
* Removed unneeded import
* Added extra baud rates and improved Ecma execution
* Added "portable" folder support
* Fixed excepion on remove not found
* Improved error reporting in apt
* Added realtime SHA checking of downloads
* Removed superfluous output and updated serial terminal
* Rotated markdown editor
* Cleaned up apt
* Linked mouse wheel on output console to scroll
* Removed unneeded jsch
* Implemented Gritty terminal for output and deb installation progress
* Deprecated outdated java tests
* Allow manual override of port name on POSIX systems from command line
* Improved missing library detection
* Improved library recursion for non-preproc compilers
* Replaced JAVA stub for OSX
* Fixed problem of null pointer on missing res: repo file
* Updated plist to Java 1.8
* Only add extra ports that actually exist (posix only)
* Added console error reporting for failed downloads of packages and repos
* Updated bundled varcmd package to 1.0.3
* First iteration of example browser
* Reinstated adding of missing port at sketch load
* Added displaying full path of library when compiling
* Fix no save as on close of unsaved sketch
* Possible fox for font corruption on update
* Trapped failed extract of file
* Null Pointer when replacing non-existant package
* Temporarily removed SHA25 integrity check
* Temporarily removed SHA25 integrity check
* Added experimental JavaScript plugin support
* Only send raw output to output tab when being otherwise preprocessed
* Added package validation system
* Added raw output to output tab
* Updated JSch to 0.1.54
* Switched to dual launch4j launchers in Windows
* Only erase lines from console if the character following a CR is
* not
* an NL
* Upgraded windows FULL bundles to JRE 8
* Updated varcmd package
* Added CR for console line clearing
* Added download file integrity check
* Fixed package file link dereferencing without full path
* Fixed null pointer with no default option in sketch.cfg
* Added recursive uninstall of packages and updated varcmd package
* Added NOP logger
* Added slf4j api
* Updated jmDNS
* Updated RSyntaxTextArea
* Added changelog window
* Added changelog generation
* Added some files to gitignore for tidiness
* Added updated VS files
* Display locally installed packages in tree
* Improved board no-core event debugging
* Added manual package installation option
* Converted varcmds to javascript scriptlets
* Added missing sub-sub-group nodes to plugin manager tree
* Fixed multiple-source overwrite bug
* Updated families package
* Removed redundant uecide-repositories package from internal repo
* Added multi-repo file system
* Updated serial terminal
* Included master repo package
* Reinstated basic Translate for existing plugins
* Added more I18N entries
* Fixed ant-contrib inclusion
* Added internationalization support
* Improved console buffering strategy
* Updated editor
* Enhanced port command. Added hex file merging
* Bumped version
* Added script-based tools menu entry support
* Bumped version
* Added themes to PiM
* Couple of cleanups
* Added bullet3, Usb HID device support, kill support for builtin commands
* Added programmer objects and associated control logic and commands
* Added spinner icon graphics
* Added selected board/core/compiler names to top level hardware menu
* Added MCU list (Most Commonly Used) in Files menu
* Reverted draggable tabs
* Fixed Replaces: handling in GUI
* Updated preferences entries
* Added load/save dialog location remembering option and mark-all enable/disable option
* Overhaul of sketch properties system and updated RSyntaxTextArea
* Added version info file
* Draggable tabs and ultiple panes
* Added missing widgets
* Added support for editor data capture
* Updated internal repository
* Added plugin popup menu support and console copy menu
* Added console background tileable images
* Added font scaling
* Added theming to other components in the system
* Complete overhaul of theme system
* Properly handled escaped characters in comment stripping
* Properly handled escaped characters in comment stripping
* Fixed preference mismatch in autosave facility
* Armoured integer parsing in IOKit
* Fixed linux packaging error
* Fixed broken menu shortcuts
* Cleaned up parsed errors to console
* Added ANSI codes to parsed console errors and warnings
* Fixed broken shortcut keys
* Releasing new beta
* Overhauled debian building scripts
* Added configurable reset delays
* Fixed plugin preferences default loading
* Added border to preferences window
* Re-added debug files used by some plugins
* Updated and added missing licenses
* Removed redundant files
* Added library list and port list widgets for preferences
* Added parsable message strings
* Fixed error and warning message flagging and todo list clearing
* Fixed version conflict
* Added some new builtins
* Fixed splash screen to middle of default screen in multiheaded environment
* Overhaul of communications systems
* Added environment handling, and fixed double-context creation in sketches
* Gradually cleaning up the prefs system
* Bumped version
* Added embedded binary support
* Fixed broken redirection for object editor
* New Context system for messaging and execution
* Overhaul of scripting and command systems
* Better javascript integration
* Some more builtin commands for file writing and jar loading
* New preferences tree system
* Fixed bug with varcmd in baud rate setting
* Fixed ordering of option menu
* Added missing launch4j files
* Added launch4j config
* Bumped version
* Fixed crash at startup with .ino file parameter
* Added support for adublock URI
* Fixed splash screen flickering
* Bumped version
* Improved crash reporter
* Improved manual handling and project search facility
* Moved repo list into BorderLayout panel
* Added missing archive files
* Moved APT into core and added repository handling
* Updated version
* Added build status
* Added script tag
* Fixed ant deps error
* Made console unbuffered
* Made command input stream unbuffered at last
* Fixed unknown version in debian testing
* Added GPIO control, delay, and linux serial lock files
* Fixed some headless cli bugs with new install
* Added interactive CLI system
* Added online/offline option to file menu
* Added offline mode
* Numerous bug fixes. Setting to recreate makefile each compile
* Forced apt update before library installation
* Added version flag
* Added . as valid sketch name for mkmf
* Added Makefile generation and some extra force flags
* Fixed arm apt name
* Bumped Version
* Added command line options to manipulate APT repositories and packages
* Separated library and core build files from sketch build files
* Added apt
* Locked editor based services from running during compilation
* Bumped version
* Moved plugin manager into core Added missing library identification and installation Created markdown editor with realtime preview
* Added missing markdown processor jar
* Working on new repo system
* Rolled back custom parser for property files
* Bumped version
* Fixed escape bug in property file. Updated jgit. Added cloning to GitLink
* Added Arduino config file parsing vc
* Bumped version
* Added library options system
* Implemented icon sets
* Bumped version
* Added random number VC and added UUID to UECIDE VC.
* Complete overhaul of icon system. Added purge pragma parameter
* Fixed return value on headless compile or upload failure
* Bumped version
* Added per-sketch icon support
* Fixed UTF-8 saving in OS X
* Added full support for recursive .git finding
* Added recursive .git finding
* Hardened some functions with limits
* Added toolbar icon and line drawing support
* Rewrote protocol to flexible stack based system
* Added commands to set size and colour of LCD screen
* Made the window and widget resizable
* Added virtual LCD plugin
* Made extra ports override real ports when the extra is a symlink
* Bumped version
* Added pre- and post-commands to compile and upload
* Bumped version
* Added grapher to repo
* Added grapher to repo
* Added descriptions to plugins
* Added parsing to more strings in Sketch
* Added Variable Command for getting selected options data
* Incided jgit into the core
* Improved status line
* Updated stub to find all jre8 releases
* Fixed windows exe packaging
* Added menu entries and keyboard shortcuts for compile and upload
* Fixed crash reports
* Updated version
* Fixed XP launch, and some cosmetics
* Removed dependency version restrictions for launchpad
* Added ant as build depend
* Debianized package
* Added windows and osx version probing
* Fixed prefs open in MacOSX
* Updated version
* Added option to disable crash reporter
* Fixed some null pointer errors
* Added OS version data and moved varfuncs to base
* Fixed compilation when main header is manually included
* Fixing concurrent modifications
* Fix compilation for newer JNA
* Fixed library naming problem
* Build enhancements to support more compilers
* Rebuilt console without HTML
* Added source for windows stub
* Bundled new plugin manager;
* Updated jna, added native java support on windows
* Fixed exe loader stub at last!
* Switched to using a batch file to load windows version for now until a better stub can be crafted
* Fixed windows and mac boot stubs
* Remove autoformat bundle
* Added new bundles of plugins
* Added keyword.txt handling
* Big refactoring uecide.app -> org.uecide. New plugins loading methodology
* Fixed a subtle bug in lib include selection
* Fixed preferences repainting on theme change and cleaned up toolbar button borders
* Fixed OS X application file permissions
* Bumped version
* Finally fixed wandering tree entries
* Fixed windows slowness
* Added code page handling
* Added gutter flags
* Stopped the function list from bouncing around so much
* Added note/todo/fixme parsing and task lists
* Tidy up of the build process
* Bumped version
* Fixed sketch libraries
* Fixed null pointer in getTabCount. Added snippets plugin. Added experimental background compiler service
* Upgrade RSTA
* Bumped version
* Added new required libraries
* Added services and migrated some threads and timers to the service infrastructure
* Fixed MRU initialize bug
* Made all plugin commands non-static and added javascript engine
* Improved command line handling
* Improved crash reporting system
* Sorted function list alphabetically
* Fixed tree update collision bug
* Added crash reporter
* Added manual browser
* Caught possible NullPointer exception in sketch loading
* Added context menu to console
* Fixed typo in console CSS
* Bumped version
* Changed console to HTML
* Added theme importer plugin
* Added third party library list and made About box HTML
* Backed out property file loader
* Reverted propertyfile loader update
* Fixed release
* Added USB javax source
* Added USB support, property file includes, etc
* Added USB HID device detection
* Big cleanup, plus better handling for 1.5.x libraries
* Made ssh and scp forget the password on authentication failure
* Added preferred programmer selection to network discovery service
* Fixed serial terminal method crash in OS X java 1.6_15
* Added goto/set/fail/end special functions to script
* Rewritten command execution routines
* Fixed null pointer in non-sketch upload
* Improvements to script system
* Updated version
* Fixed too many open files bug
* Added jmdns library
* Added Jsch library
* Cleaned up error output
* Added network ssh uploading
* Fixed plugin not-installed version bug
* Improved full screen with menu
* Merge branch 'master' of github.com:UECIDE/UECIDE
* Removed border painting control from toolbar buttons
* Added full screen mode
* Updated version
* Added office laf jar
* Added tinylaf jar
* Added extra LAFs and some fixes to themes
* Added selectable editor themes
* Added experimental tree merge routine
* Updated version
* Added auto-refresh of function list. Added documentation and markdown section
* Fixed new repo problem
* Added revert to last commit on files
* Updated version
* Added git ignore
* Added new stub for icon overlay
* Updated launch4j for trusty
* Embedded executables
* Removed old system plugins folder code
* Added ASM labels with corresponding .ent sections to tree function list
* Added template plugin
* Fixed missing asm files in tree
* Fixed sketch properties saving
* Added function list in project tree
* Fixed nested comment stripping
* Added bracket adding options
* Added Artistic Style plugin
* Fixed Save As path breaking
* Forced opened files to be java.io.File class
* Fixed library install bug
* Properly fixed windows compile #line problem
* Added elf inspector
* Added error flagging
* Added project search. Fixed windows file path #line issue
* Updated version
* Added plugin callbacks to context menus
* Fixed debian package lint problem
* Updated version
* Improved facilties for debugging sketches
* Added improved about box
* Added open data folder debug entry
* Updated bundled serial terminal
* Fix bug in serial terminal for width > 80 characters
* Rememberd last opened folder in import file. Added https and ftp to windows URL open handling
* Rememberd last opened folder in import file. Added https and ftp to windows URL open handling
* Merge pull request #39 from TCWORLD/master
* Corrected "Open In OS"
* Changed version url for new site layout
* Updated version
* Ported copy for forum plugin
* Bundled autoformat
* Ported AutoFormat plugin
* Updated plugin manager
* Made plugin manager modal
* Updated download URL for new site layout
* Updated patch to r
* Fixed stellaris compilation bugs
* Added sketch properties system
* Added sketch properties system
* Saved console split position as offset from top instead of bottom
* Fixed lib-local header file issue in library recursion
* Fixed Open in OS in Windows
* Fixed library compilation crashing in windows due to rogue :
* Updated version
* Added tools for opening files and folders externally
* Merge pull request #36 from UECIDE/complete_rebuild
* New support files URL
* Fixed old plugins crashing new version
* Fixed NullPointerException when no core installed
* Updated version
* Fixed disappearing output messages problem
* Moved library storage to Library class
* Added option to disable version check. Limited version checking to 3 hourly at most
* Added updated version checking
* Added libraries-within-libraries
* Updated version
* Fixed auto opening of plugin manager
* Fixed bad plugin manager tree refresh with no board selected in editor
* Updated version
* Fixed PropertyFile file locking issue
* Forced editor menu rebuild on board install/deinstall. Forced complete rebuild on plugin manager close.
* Added tooltips to tree
* Added multiple install/uninstall to pligin manager
* Added multiple install/uninstall to pligin manager
* Updated version
* Brought main plugins into main tree
* Added export local library option
* Added export local library option
* Added library template generator
* Made complete rebuild a separate thread
* Made find loop and added found failed background change
* Updated version
* Updated version
* Added import SAR file
* Added export as SAR file
* Added export as SAR file
* Added compile / upload abort button
* Added example sketch and localization protection
* Fixed editor font bug. Added USB device ident for Linux.
* Fixed serial lock problem
* Possible fix to locked port
* Updated version
* Fixed Save As main file bug, and attempted to fix bluetooth problems
* Added missing saveTo() function stub
* Updated version
* Added option to hide secondary editor toolbar
* Fixed saveAs empty file bug
* Fixed rescan all ports on tab change
* Various bug fixes
* Added prefs and theme getters in config file variables
* Added simple bitmap viewer
* Added simple bitmap viewer
* Fixed file save "modified outside" bug
* Updated version
* Made library utility recursion optional in library.txt file
* Allow headless compilation/upload
* Fixed DTR reset bug in Windows
* Added file watcher timer to tabs to monitor external file changes
* Switched most hashes to TreeHash.  Fixed some null pointer bugs.  Open new sketch in existing editor if untitled and not modified
* Fixed plugin install problems
* Improved library category adding
* Rewritten library installer. Ditched sketchbook libraries folder (made it part of categories)
* Added file tree, drag and drop, and lots more improvements
* Fixed saving of preferences on application exit
* Added build number generation, contributor list generation and sorting of user preferences file
* Cleaned up dir structure, implemented about menu entry
* Fixed #pragma parameter string space splitting
* Fixed virgin installation issues
* Added "unable to find port" error alert to serial system
* Added new plugin manager bundle
* Merge branch 'complete_rebuild' of github.com:UECIDE/UECIDE into complete_rebuild
* Plugin system work
* Added option to keep find & replace open
* More plugin work and added basic upload support
* Added libraries, and re-worked plugin system
* Library compilation meter, plus other cleaning up and progress towards completion
* Added code to indent / outdent buttons
* More progress towards the best IDE ever
* Added new plugin toolbar icon system
* Added editor toolbar
* More work re-writing all the functionality
* Hidden RSyntaxTextArea build folder
* Removed build tree and core tree and just created the tree
* Massive cleanup of the file structure.  All third party code removed from the main source tree and either placed into auto-updating third party area or placed in lib folder as .jar files.
* update version
* Embarked on a daring resign


