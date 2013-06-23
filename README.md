UECIDE
======

Universal Embedded Computing IDE

First came the Arduino.  Arduino forked the Processing IDE and used it for
their own ends.  Arduino took off big time, and prople all over the globe
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
