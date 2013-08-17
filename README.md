[Sensordrone Android/Java Library](http://developer.sensordrone.com)
====================================================================

Pre-compiled versions can be found on our Developer [Downloads](http://developer.sensordrone.com/downloads) page.
* [Android API](http://developer.sensordrone.com/android/api/)
* [Java API](http://developer.sensordrone.com/java/api/)

The current imagining of this library is that we will build/tag a
particular revision as a "stable" version and host if on our Developer
site, as mentioned above. In-between, this git repository will sort
of be a rolling-release, where it is updated as we go.

We tried to Javadoc comment everything to mind numbing detail, but only
made it about half-way through. The code is fairly well commented
otherwise, but we will Javadoc more stuff as we go.

The Java library uses the BlueCove library, so it is more
developer/experimental! It works on Windows/OSX/Linux, but sometimes you
need to download BlueCove and build/install a system driver.

## Compiling the Library

Currently implemented build systems:
* Gradle (build.gradle)

### Gradle quick start

gradle clean : Cleans a previously compiled build

gradle buildSDAndroidLib : Build the Android Library

gradle buildSDJavaLib : Build the Java Library

For more/other tasks, run 'gradle tasks' or check out the build.gradle
file directly.

### Dependencies
Currently, dependency management is taken care of via Gradle.

#### Android
We build the Android library against Android 4.3.3 (Gingerbread).

#### Java
We build the Java library using the BlueCove library to implement
Bluetooth support. BlueCove is an older library, but has worked well for
us on Windows/Linux/OSX. Currently, you will likely need to download
their code and build a driver to use on your specific platform. The next
step for this library would be to build/bundle the drivers.

## Source Structure

This project contains both the Java and Android code for working with
the Sensordrone. Most of the code is re-usable for either platform, so
it would make sense to keep a common implementation-independent package
that can be maintained for both packages at once, while having extra
packages for Android/Java that are implementation specific.

### com.sensorcon.sensordrone

All of the source files in this package can be used by either a Java build
or an Android build. This is shared code amongst the two versions to
allow quick updates along both builds. Implementation specific methods,
such as handling connections, are done in the Android/Java specific
packages. Most of the source code here is for 'behind-the-scenes' work,
and an app developer using the compiled library would mainly use the
classes in the Android/Java package which extend/implement these.

### com.sensorcon.sensordrone.android

The main Drone class that app developers will use is here. It implements
the connecting/disconnecting of a Sensordrone to Android via Bluetooth.

#### com.sensorcon.sensordrone.android.tools

This is a package of Android specific tools to help app developers get
up and running a little quicker. This is meant to replace the original
'Android Helper Library' in order to keep the number of extra libraries
needed to a minimum.


### com.sensorcon.sensordrone.java

The main Drone class that app developers will use is here. It implements
the connecting/disconnecting of a Sensordrone to Android via Bluetooth.

### com.sensorcon.sensordrone.dev

This package is intended for developers. There is a dev.android and a
dev.java sub package. It is mainly meant for implementing developer
methods methods that you might not to have accessible is a user library.
If you are just tweaking the library for yourself, you could just as
easily modify the files in the main java/android package instead of
extending them here.
