# Third-Party Notices

## Original Etched

Re-Etched is an unofficial fork of Etched 3.0.4.

- Original project: https://github.com/jacksonhardaway/etched
- Fork base: https://github.com/jacksonhardaway/etched/commit/b57a6286c6bf3109a26bbdbceed8b6a46b60d9fe

Original project copyright and license terms are reproduced in the bundled
`LICENSE` file.

The original Etched project identifies its resource directories as:

```text
All Rights Reserved
Copyright (c) 2021 Moonflower Studio
```

Those resources were later consolidated under `src/main/resources`. Re-Etched
does not claim ownership of inherited Etched models, textures, sounds,
translations, data files, or branding. This notice does not grant additional
rights to those resources.

Original credits retained from Etched:

- Moonflower Studio, original project;
- Ocelot and Jackson, development;
- Farcr, art;
- AstraZoey, sound design.

Additional translation contributions identified in the project history
include Koha for French and Ryo TAGAMI for Japanese.

## JLayer 1.0.1

Re-Etched includes JLayer 1.0.1, developed by JavaZOOM, for MP3 decoding.

- Project metadata: http://www.javazoom.net/javalayer/javalayer.html
- Maven artifact: https://repo1.maven.org/maven2/javazoom/jlayer/1.0.1/
- Original source archive: https://repo1.maven.org/maven2/javazoom/jlayer/1.0.1/jlayer-1.0.1-sources.jar

JLayer source files designate the GNU Library General Public License version 2
or, at the recipient's option, any later version. This distribution exercises
that option under version 2.1 of the GNU Lesser General Public License. A copy
of that license is provided at `META-INF/licenses/LGPL-2.1.txt`.

The Re-Etched build relocates the distributed JLayer bytecode from the
`javazoom` namespace to `gg.moonflower.etched.javazoom`. No manual changes are
made to upstream JLayer source files. The build script and exact Re-Etched
source corresponding to a release are available from that release's Git tag.
The original JLayer source archive is supplied alongside release binaries.

JLayer source preserves additional upstream notices, including:

```text
Copyright (C) 1993, 1994 Tobias Bading
Copyright (c) 1991 MPEG/audio software simulation group,
All Rights Reserved
```

## OpenJDK WaveFileReader

`gg.moonflower.etched.api.util.WaveDataReader` is derived from OpenJDK's
`com.sun.media.sound.WaveFileReader`.

- Original source: https://github.com/openjdk/jdk8u/blob/d8e9a28061080610370084422bde4356a02382dd/jdk/src/share/classes/com/sun/media/sound/WaveFileReader.java
- Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
- Original authors: Kara Kytle, Jan Borgersen, and Florian Bomers.

The derived file is licensed under the GNU General Public License version 2
only with the Classpath Exception. A copy is provided at
`META-INF/licenses/GPL-2.0-with-Classpath-exception.txt`.

The file was modified for Etched beginning on 2021-06-10. Changes include its
package, class name, API, constants, parsing implementation, and later Minecraft
compatibility updates through 2024-01-24.
