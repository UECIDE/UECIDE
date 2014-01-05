UECIDE
======

Universal Embedded Computing IDE

First came the Arduino.  Arduino forked the Processing IDE and used it for
their own ends.  Arduino took off big time, and people all over the globe
started using it.

Then along came new players to the market - chipKIT, Launchpad, etc.

They all took one look at Arduino, saw it was good, and copied it.

The Arduino IDE has been forked and forked again until it must feel "like
an E-chord on an old flat top guitar".  Everyone has forked the IDE and
bent it to their own ends, each one modified to meet a specific task.

So now, if you have a selection of different boards, you have a selection
of copies of the same IDE, but each with its own little tweaks and
modifications to support a specific set of boards.

None of them have addressed the basic failings in the IDE that have
existed since that first fork, but have inherited a huge amount of
cruft and fluff from the original Processing->Arduino conversion.  It's
not really surprising, after all - they have all focussed on porting it
to their specific boards without paying too much time working on the 
IDE itself.

This project is an attempt to redress the balance.  UECIDE is aiming to
be a generic IDE based on the Arduino IDE which can easilly support ALL
the available boards.  It does this by not supporting any boards directly
but providing a framework where a board developer or fanatical group can
simply drop in the definitions for the boards and the chip on the boards
without having to make any modifications to how the IDE works.

Much of the original Arduino IDE code has been ripped out and replaced
with custom code that is aimed at being 100% generic.  Gone are all the
hard-coded executables.  Gone is the old single-core structure.  Say
hello to a nice clean structure which separates the cores from the IDE,
and the boards from the core.

Also the IDE itself has had a bit of a makeover.  Re-branding the IDE to
match your corporate identity is now nice and simple - just a collection
of PNG files (gone are the old ropey GIF files) and a couple of text files
to create a completely unique branded IDE with no programming whatsoever.

Another major aim of the project is to make it simple to build the IDE for
all major platforms (Windows, Linux, Mac, etc) with just one command on a
single machine.  No need to switch to another operating system just to
create the IDE for that platform.

How to compile
==============

First you need to be running Ubuntu or something similar.  While it should
be possible to compile on any system we have only been developing on Ubuntu
so if you need any help and you're not on Ubuntu you're on your own I'm
afraid.

Then you need to install openjdk version 6.  While we'd love to use some of
the nice features of Java 7, thanks to Apple we're not able to (anything older
than about 5 minutes ago with Apple only has Java 6 available).

    $ sudo apt-get install openjdk-6-jdk

If you want to build the Windows distribution you will also need the mingw packages:

    $ sudo apt-get install gcc-mingw32

And of course you will need ant to be able to do the compiling:

    $ sudo apt-get install ant

All the building is done from the "build" directory.  In there is a configuration
file you will need to examine and edit if needed.  The main entry you will need
to check out is the bootclass.path entry which points to where the Java version 6
core files are.  On an Ubuntu system these are stored in /usr/lib/jvm somewhere.
If you are on a 32-bit system the default setting won't be right, so you will need
to change it.  To confirm what it should be you can find all the installed JDK
locations with:

    bob@computer:~/UECIDE/build$ find /usr/lib/jvm -name rt.jar
    /usr/lib/jvm/java-1.5.0-gcj-4.7/jre/lib/rt.jar
    /usr/lib/jvm/java-6-openjdk-i386/jre/lib/rt.jar
    /usr/lib/jvm/java-7-openjdk-i386/jre/lib/rt.jar

Copy and paste the openjdk 6 line into the bootclass.path setting in the 
build.settings file.

You should now be good to build:

    bob@computer:~/UECIDE/build$ ant

Assuming there are no errors, you should now have a freshly built version of
UECIDE available for testing.  You can run it with:

    bob@computer:~/UECIDE/build$ linux/work/uecide

You can turn your freshly built test version into a zip file for easy distribution
with:

    bob@computer:~/UECIDE/build$ ant dist

Or a .deb file with:

    bob@computer:~/UECIDE/build$ ant deb

Both of which will end up in the linux directory.

If you want to build for a different target system you can, with

    bob@computer:~/UECIDE/build$ ant windows-build

You can replace "windows-" with "macosx-".  You can also use the "dist" target
with the prefixes:

    bob@computer:~/UECIDE/build$ ant windows-dist


