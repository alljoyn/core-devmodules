---------------------
Important Information
---------------------

THIS CODE IS DEVELOPER CODE TO PROVIDE REFERENCE TO A COMPLEX APPLICATION
THAT USES ALLJOYN IN DIFFERENT WAYS FROM LOCAL COMMUNICATION TO AN
ANDROID BACKGROUND SERVICE AND OFF DEVICE TO OTHER APPLICATIONS.

THE SOFTWARE CONTAINS ADVANCED CONCEPTS IN ANDROID AND DEALS WITH
MULTIPLE THREADS AND DOES SOME NON STANDARD CONCEPTS.

------------------
Projet information
------------------
This software contains an application call AroundMe that is used as a showcase of one
possible way that AllJoyn could be used in an application.  There are a few interesting
parts going on.  There is an API layer(AllJoynServicesAPI) that the AroundMe application
uses that exposes a set of calls into the various Module software.  The interaction between
the API layer and an Android Service is via AllJoyn (Could have been standard Android
communication, but since AllJoyn can be used for local communication and off device, why not?).

An Android Service runs that contains all the off device interactions of AllJoyn
that exists in the AllJoynServiceFramework project.  This loads each of the Modules
and keeps references to them.

Here is a list of the various projects and a brief description:
1) aroundme - This is a pure Android application that uses the AllJoynServicesAPI to showcase
                how to use this layer to build an applications. It is not commercial quality
                and minor bugs may exist. It is intended as a reference point to start.
2) AllJoynServicesAPI - Developer library that uses AllJoyn to talk to an Android service.
                Purely a wrapper around AllJoyn calls to spawn actions in the other modules.
                This is a library to allow a developer to avoid ever using AllJoyn and can
                make requests like to start/stop the AndroidService and GetProfile(user)
                and be notified through listener classes that actions have occurred.
3) AllJoynServicesFramework - This is the counterpart to the AllJoynServicesAPI and is an Android
                    service that manages each of the software modules to activate and release them.
4) Chat - Exposes the ability over AllJoyn to send a string to a group of users or a specific user.
          This is almost identical to the functionality in the current AllJoyn SDK except that we
          leverage a Group that manages the AllJoyn Sessions so that a user can be in multiple chat
          rooms at the same time.
5) Common - This is a library that contains java objects that is shared among the projects. It contains
            interfaces that are implemented, and data objects. A SessionManager project also exists
            that contains the interactions with AllJoyn using the AllJoyn API's to create and join sessions.
6) Debug - This allows for the ability to view android logcat information on a remote mobile devices.
            It defines interaction over AllJoyn to connect to a device and get then have that device
            start sending logcat data using an AllJoyn signal.
7) FileTransfer - Rudimentary file transfer that chunks a file and sends it using AllJoyn signals.
8) Groups - This wraps up AllJoyn session management to form private and public groups that can
            accept/deny access. It allows for flow control to a set of users and contains a sample
            test app along with it.
9) MediaQuery - This uses AllJoyn to interact with remote devices to execute a Android MediaQuery
            call and return the data. This, at its core, extends the Andorid MediaQuery to be performed
            on a remote device.
10) Notification - This is similar to chat except upon receipt it shows a Android Notification.
11) Profile - Creates a GUID that is saved and pulls contact and allows for remote devices to access
            this data. The data is populated via the ProfileLoader library. It also informs an
            application using this software that other Peers are nearby and can be interacted with.
12) ProfileLoader - Library that contains an activity to read the Android database to pull a
            selected contact information and save it to the filesystem for re-use.
13) RemoteControl - Allows the ability to send an Android onKeyDown event to remove devices. Also allow for sending an Android Intent to remote devices.
14) Whiteboard - Allows for penDown/Up drawing events to be transmitted to other devices.
                This passes a Line data structure up to an application like "aroundme" in which
                to render the line. Also provides control to clear the screen. uses groups in
                order to have multiple whiteboard instances at the same time.

-----------------------
Setting up the Projects
-----------------------
This software is intended to be loaded into eclipse and all the projects
should be loaded.  To do so follow the following steps:
1) Download the AllJoyn Android SDK
2) Copy the alljoyn.jar from the "java/jar" folder to Common/SessionManager/libs
3) Copy the liballjoyn_java.so from the "java/lib" folder to Common/SessionManager/bin/armeabi
4) Open eclipse and create a new workspace if you choose.
5) Select the File menu option and click "Import".
6) Choose General -> "Existing Projects into Workspace" and click the "Next" button.
7) Click the "Browse..." button and navigate to the folder where this README is located.
8) Click the "Select All" button.
9) Click the "Finish" button.
10) Wait for projects to build. There will be an error, we need to setup the android_support_v4.jar.
11) Right click on the ModuleCommon project.
12) Highlight Android Tools -> Add Support Library...
13) Hightlight "Android Support Library" and follow the prompt to install.
14) The aroundme project may now be loaded onto a device.  UnitTest projects may also be run as well.
 

