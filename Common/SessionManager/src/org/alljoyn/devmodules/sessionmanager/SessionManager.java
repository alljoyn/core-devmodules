/******************************************************************************
* Copyright (c) 2013, AllSeen Alliance. All rights reserved.
*
*    Permission to use, copy, modify, and/or distribute this software for any
*    purpose with or without fee is hereby granted, provided that the above
*    copyright notice and this permission notice appear in all copies.
*
*    THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
*    WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
*    MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
*    ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
*    WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
*    ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
*    OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
******************************************************************************/
package org.alljoyn.devmodules.sessionmanager;

import java.util.ArrayList;
import java.util.HashMap;

import org.alljoyn.bus.AuthListener;
import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.Status;

/**
 * The Session Manager abstracts much of the tedious boilerplate involved with 
 * setting up and destroying a session in AllJoyn. That way, developers can 
 * dive right into adding AllJoyn into their projects faster without needing 
 * to worry too much about setup and cleanup.  Along with making AllJoyn usage 
 * simpler, the Session Manager also provides a lot of useful session related 
 * features. 
 * 
 */
public class SessionManager implements SessionManagerInterface {
	/* Load the native alljoyn_java library. */
    static {
        System.loadLibrary("alljoyn_java");
    }
    
    private BusAttachment bus;
    private SessionManagerBusObject smBusObject;
    private SMBusListener sessionBusListener;
    private boolean busConnected = false;
    private boolean isAutoDisconnect = false;
    
    // Sessions currently Hosted
    private ArrayList<String> hostedSessions = new ArrayList<String>();
    // Sessions currently joined
    private ArrayList<String> joinedSessions = new ArrayList<String>();
    // Aliases being Advertised
    private ArrayList<String> aliases = new ArrayList<String>();
    // Stores the available sessions with the interested well known name prefix
    private ArrayList<String> availableSessions = new ArrayList<String>(); 
    // Stores the banned participants
    private ArrayList<String> bannedParticipants = new ArrayList<String>();
    // Stores the bus objects registered on the bus
    private ArrayList<BusObject> registeredBusObjects = new ArrayList<BusObject>();
    // Stores the session manager listeners
    private ArrayList<SessionManagerListener> sessionManagerListeners = new ArrayList<SessionManagerListener>();
    // Stores classes which contain signal handlers to be registered
    private ArrayList<Object> classesWithSignalHandlers = new ArrayList<Object>();
    
    // Map session Ids to participants
    private HashMap<Integer,ArrayList<String>> sessionIdToParticipants = new HashMap<Integer,ArrayList<String>>();
    // Map session names to session Ids
    private HashMap<String,Integer> sessionNameToId = new HashMap<String,Integer>();
    
    // HashMaps to map Hosted Session Names to Session Ports
    private HashMap<String,Short> sessionNameToPort = new HashMap<String,Short>();
    // HashMaps to map Hosted Session Ports to Session Names
    private HashMap<Short,String> sessionPortToName = new HashMap<Short,String>();
    
    // Map Aliases to their Master Sessions
    private HashMap<String,String> aliasToMasterSesssion = new HashMap<String,String>();
    // Map Master Sessions to their Aliases
    private HashMap<String,ArrayList<String>> masterSessionToAliases= new HashMap<String,ArrayList<String>>();
    
    
    // Flag for turning on or off debug messages
    private boolean debugOn = false;
    // Holds the service name (name prefix) for the session manager object
    private final String serviceName;
    // Object path of the session manager bus object
    private static final String SM_OBJECT_PATH = "/SessionManagerBusObject";
    
    /*------------------------------------------------------------------------*
     * Constructors
     *------------------------------------------------------------------------*/

    /**
     * Constructs a SessionManager.
     * 
     * @param namePrefix   the well known name prefix that will be used for 
     *                     advertisement and discovery
     */
    public SessionManager (String namePrefix) {
        this(namePrefix, new ArrayList<BusObjectData>(), new BusListener(), false);
    }

	/**
     * Constructs a SessionManager.
     * 
     * @param namePrefix   the well known name prefix that will be used for 
     *                     advertisement and discovery
     * @param localCommunicationOnly this only allows for local on device communication
     */
    public SessionManager (String namePrefix, boolean localCommunicationOnly) {
        this(namePrefix, new ArrayList<BusObjectData>(), new BusListener(), localCommunicationOnly);
    }
     
	/**
     * Construct a SessionManager and register the provided BusListener.
     * 
     * @param namePrefix   the well known name prefix that will be used for 
     *                     advertisement and discovery
     * @param busListener  the BusListener to register
     */
    public SessionManager (String namePrefix, BusListener busListener) {
        this(namePrefix, new ArrayList<BusObjectData>(), busListener, false);
    }
    
	/**
     * Construct a SessionManager and register the provided BusListener.
     * 
     * @param namePrefix   the well known name prefix that will be used for 
     *                     advertisement and discovery
     * @param busListener  the BusListener to register
     * @param localCommunicationOnly this only allows for local on device communication
     */
    public SessionManager (String namePrefix, BusListener busListener, boolean localCommunicationOnly) {
        this(namePrefix, new ArrayList<BusObjectData>(), busListener, localCommunicationOnly);
    }
    
    /**
     * Construct a SessionManager and register the provided bus objects.
     * 
     * @param namePrefix   the well known name prefix that will be used for 
     *                     advertisement and discovery
     * @param busObjects   the bus objects to register
     */
    public SessionManager (String namePrefix, ArrayList<BusObjectData> busObjects) {
        this(namePrefix, busObjects, new BusListener(), false);
    }
    
	/**
     * Construct a SessionManager and register the provided bus objects.
     * 
     * @param namePrefix   the well known name prefix that will be used for 
     *                     advertisement and discovery
     * @param busObjects   the bus objects to register
     * @param localCommunicationOnly this only allows for local on device communication
     */
    public SessionManager (String namePrefix, ArrayList<BusObjectData> busObjects, boolean localCommunicationOnly) {
        this(namePrefix, busObjects, new BusListener(), localCommunicationOnly);
    }
    
    /**
     * Construct a SessionManager and register the provided BusListener and
     * bus objects.
     * 
     * @param namePrefix   the well known name prefix that will be used for 
     *                     advertisement and discovery
     * @param busObjects   the bus objects to register
     * @param busListener  the BusListener to register
     */
    public SessionManager (String namePrefix, ArrayList<BusObjectData> busObjects, BusListener busListener) {
    	this(namePrefix, busObjects, busListener, false);
    }
    
	/**
     * Construct a SessionManager and register the provided BusListener and
     * bus objects.
     * 
     * @param namePrefix   the well known name prefix that will be used for 
     *                     advertisement and discovery
     * @param busObjects   the bus objects to register
     * @param busListener  the BusListener to register
	 * @param localCommunicationOnly this only allows for local on device communication
     */
    public SessionManager (String namePrefix, ArrayList<BusObjectData> busObjects, BusListener busListener, boolean localCommunicationOnly) {
        bus = new BusAttachment(namePrefix, localCommunicationOnly ? BusAttachment.RemoteMessage.Ignore : BusAttachment.RemoteMessage.Receive);
        serviceName = namePrefix;
        smBusObject = new SessionManagerBusObject();
        sessionBusListener = new SMBusListener(busListener);
        classesWithSignalHandlers.add(this);
        registerBusObjects(busObjects);
    }
    
    
    /*------------------------------------------------------------------------*
     * API Methods
     *------------------------------------------------------------------------*/
    /**
     * connectBus
     * registers the bus listener, connects the bus attachment to the bus, 
     * starts discovery, and registers signal handlers. 
     * Note: This method is automatically called after when the session manager
     * registers its first bus object.
     * 
     * @return  OK if successful
     */
    public Status connectBus() {
        logInfo("SessionManager.connectBus()");
        if(busConnected) {
            logInfo("SessionManager.connectBus(): Bus is already connected");
            return Status.OK;
        }
        Status status;
        
        // Register the bus listener
        bus.registerBusListener(sessionBusListener);
        logInfo("SessionManager.connectBus(): Registering Bus Listener");
        
        // Register the session manager bus object
        status = bus.registerBusObject(smBusObject, SM_OBJECT_PATH);
        logInfo("SessionManager.connectBus(): Registering SM Bus Object - " + status.toString());
        if(status != Status.OK) {
            return status;
        }
        
        // Connect the bus
        status = bus.connect();
        logInfo("SessionManager.connectBus(): Connecting Bus Attachment - " + status.toString());
        if(status != Status.OK) {
            return status;
        }
        busConnected = true;
        
        // Start Discovery
        status = bus.findAdvertisedName(serviceName);
        logInfo("SessionManager.connectBus(): FindAdvertisedName(" + serviceName + ") - " + status.toString());
        if(status != Status.OK) {
            return status;
        }
        // Pause the thread for 20 milliseconds to let the foundAdvertiseName() signals come through
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Register the signal handlers in the specified classes
        for(Object classObj : classesWithSignalHandlers) {
            status = registerSignalHandlers(classObj);
            if(status != Status.OK) {
                return status;
            }
        }
        logInfo("SessionManager.connectBus(): Registering Signal Handlers - " + status.toString());
        return status; 
    }
    
    /**
     * disconnectBus 
     * leaves all currently joined sessions, destroys all currently hosted 
     * sessions, removes all currently advertised aliases, unregisters all bus
     * objects, unregisters the BusListener, unregisters all signal handlers, 
     * and finally disconnects the bus attachment from the bus 
     * Note: This method is automatically called when the session manager 
     * unregisters its last bus object, or when calling 
     * unregisterAllBusObjects().
     */
    public void disconnectBus() {
        logInfo("SessionManager.disconnectBus()");
        if(!busConnected) {
            logInfo("SessionManager.disconnectBus(): Bus is already disconnected or is being disconnected");
            return;
        }
        
        // Leave all joined sessions
        logInfo("SessionManager.disconnectBus(): Leaving all joined sessions");
        String sessionName;
        ArrayList<String> joinedSessions = listJoinedSessions();
        if (!joinedSessions.isEmpty()) {
            for (String session : joinedSessions) {
                if(serviceName.equals("")) {
                    leaveSession(session);
                }
                else {
                    sessionName = getSessionName(session);
                    leaveSession(sessionName);
                }
            }
        }
        
        // Destroy all hosted sessions and their aliases
        logInfo("SessionManager.disconnectBus(): Destroying all hosted sessions");
        ArrayList<String> hostedSessions = listHostedSessions();
        if(!hostedSessions.isEmpty()) {
            for (String session : hostedSessions) {
                if(serviceName.equals("")) {
                    destroySession(session);
                }
                else {
                    sessionName = getSessionName(session);
                    destroySession(sessionName);
                }
            }
        }
        
        busConnected = false;
        /* If we automatically disconnected from unregistering the last bus
         * object, then don't call unregister all bus objects
         */
        if(isAutoDisconnect) {
            isAutoDisconnect = false;
        }
        else {
            // Unregister the all app defined bus objects
            unregisterAllBusObjects();
            logInfo("SessionManager.disconnectBus(): Unregistering All Bus Objects");
        }
        
        // Unregister the session manager bus object
        bus.unregisterBusObject(smBusObject);
        logInfo("SessionManager.disconnectBus(): Unregistering SM Bus Object");
        
        // Unregister the bus listener
        bus.unregisterBusListener(sessionBusListener);
        logInfo("SessionManager.disconnectBus(): Unregistering Bus Listener");
        
        // Unregister all signal handlers
        for(Object classObj : classesWithSignalHandlers) {
            bus.unregisterSignalHandlers(classObj);
        }
        logInfo("SessionManager.disconnectBus(): Unregistering Signal Handlers");
        
        // Disconnect the bus attachment
        bus.disconnect();
        logInfo("SessionManager.disconnectBus(): Disconnecting Bus Attachment");
    }
    
    /**
     * createSession
     * binds a session and advertises it on the bus for other users to join 
     * with a default SessionOpts.
     * 
     * @param sessionName  the session name (suffix) of the well known name of 
     *                     the session to join
     * @param sessionPort  the contact port to bind for the session
     * @param sessionPortListener  port listener that lets the user get the 
     *                             session id and specify the acceptance 
     *                             policy for joining the session 
     * @return  OK if successful
     */
    @Override
    public Status createSession(String sessionName, short sessionPort, SessionPortListener sessionPortListener) {
        // Create the default SessionOpts and call createSession() with it
        SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY); 
        return createSession(sessionName, sessionPort, sessionPortListener, sessionOpts);
    }

    /**
     * createSession
     * binds a session and advertises it on the bus for other users to join.
     * 
     * @param sessionName  the session name (suffix) of the well known name 
     *                     of the session to create
     * @param sessionPort  the contact port to bind for the session
     * @param sessionPortListener  port listener that lets the user get the 
     *                             session id and specify the acceptance 
     *                             policy for joining the session 
     * @param sessionOpts  the session options to be used for the session
     * @return  OK if successful
     */
    @Override
    public Status createSession(String sessionName, short sessionPort, SessionPortListener sessionPortListener, 
            SessionOpts sessionOpts) {      
    	bus.enableConcurrentCallbacks();
        String wellKnownName = getWellKnownName(sessionName);
        SMSessionPortListener smSessionPortListener = new SMSessionPortListener(sessionPortListener); 
        Mutable.ShortValue port = new Mutable.ShortValue(sessionPort);
        
        // Bind the session Port
        Status status = bus.bindSessionPort(port, sessionOpts, smSessionPortListener);
        logInfo("SessionManager.createSession(): Binding Session Port " + sessionPort + " - " + status.toString());
        if(status != Status.OK) {
            return status;
        }
        
        // Make sure the session isn't already taken
        if(listSessions().contains(wellKnownName)) {
            logInfo("SessionManager.createSession(): " + wellKnownName + " is already being advertised");
            status = Status.FAIL;
        }
        else {
            logInfo("SessionManager.createSession(): " + wellKnownName + " is available");
            // Request the Well Known Name
            int flag = BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE;
            status = bus.requestName(wellKnownName, flag);
            logInfo("SessionManager.createSession(): Requesting name " + wellKnownName + " - " + status.toString());
            if(status == Status.OK) {
                // Advertise the Well Known Name
                status = bus.advertiseName(wellKnownName, sessionOpts.transports);
                logInfo("SessionManager.createSession(): Advertising name " + wellKnownName + " - " + status.toString());
                if(status == Status.OK) {
                    // Map the newly created session to the session port
                    addSessionNameToPort(sessionName, sessionPort);
                    // Add the new session to the list of hosted sessions
                    addHostedSession(wellKnownName);
                    // Successful return here
                    return status;
                }
                // Fall through and cleanup on failure
                bus.releaseName(wellKnownName);
            }
        }
        bus.unbindSessionPort(sessionPort);
        return status;
    }

    /**
     * destroySession
     * stops the advertisement of a session and unbinds its port.
     * 
     * @param sessionName  the session name (suffix) of the well known name of
     *                     the session to destroy
     */
    @Override
    public void destroySession(String sessionName) {
        String wellKnownName = getWellKnownName(sessionName);
        if(sessionNameToPort.containsKey(sessionName)) {
            short contactPort = sessionNameToPort.get(sessionName);
            if(sessionNameToId.containsKey(sessionName)) {
                int sessionId = sessionNameToId.get(sessionName);
                // Clear the list of participants for this session
                clearParticipants(sessionId);
                removeSessionNameToId(sessionName);
            }
            
            // Unbind the session port
            bus.unbindSessionPort(contactPort);
            logInfo("SessionManager.destroySession(): Unbinding Session Port " + contactPort);
            // Stop advertising the session
            bus.cancelAdvertiseName(wellKnownName, SessionOpts.TRANSPORT_ANY);
            logInfo("SessionManager.destroySession(): Canceling Advertised Name " + wellKnownName);
            // Release the well known name
            bus.releaseName(wellKnownName);
            logInfo("SessionManager.destroySession(): Releasing Name " + wellKnownName);
            
            // Remove the session from the list of hosted sessions
            removeHostedSession(wellKnownName);   
            // Remove the session from the session name to port mapping
            removeSessionNameToPort(sessionName);
            // Clear all aliases for this session
            removeAllAliasesOfMasterSession(sessionName);
        }
        
        // Remove the alias if a user tried to call destroy() on an alias
        if(listAliases().contains(wellKnownName)) {
            removeAlias(sessionName);
        }
    }

    /**
     * joinSession
     * joins an existing session.
     * 
     * @param sessionName  the session name (suffix) of the well known name of 
     *                     the session to join
     * @param sessionPort  the session port of the session to join
     * @param sessionId    set to the unique id for the session
     * @param sessionOpts  the session options of the joined session
     * @param sessionListener  listener for asynchronous session events
     * @return  OK if successful
     */
    @Override
    public Status joinSession (String sessionName, short sessionPort, Mutable.IntegerValue sessionId,
            SessionOpts sessionOpts, SessionListener sessionListener) {
    	bus.enableConcurrentCallbacks();
        String wellKnownName = getWellKnownName(sessionName);
        SMSessionListener smSessionListener = new SMSessionListener(sessionListener);
        
        // Join the session
        Status status = bus.joinSession(wellKnownName, sessionPort, sessionId, sessionOpts, smSessionListener);
        logInfo("SessionManager.joinSession(" + wellKnownName + ", " + sessionPort + ")" + " - " + status.toString());
        if(status == Status.OK) {
            /* 
             * Add yourself to your list of participants for the session because sessionMemberAdded wont be triggered
             * for yourself
             */
            addParticipant(sessionId.value, bus.getUniqueName());
            // Add the session to the list of joined sessions
            addJoinedSession(wellKnownName);
            addSessionNameToId(sessionName, sessionId.value);
            
        }
        return status;
    }

    /**
     * leaveSession
     * leaves the specified session.
     * 
     * @param sessionName  the session name (suffix) of the well known name of 
     *                     the session to leave
     * @return  OK if successful
     */
    @Override
    public Status leaveSession (String sessionName) {
        String wellKnownName = getWellKnownName(sessionName);
        // Get the sessionId of the session
        int sessionId = getSessionId(sessionName);
        // Leave the session
        Status status = bus.leaveSession(sessionId);
        if(status == Status.OK) {
            removeSessionNameToId(sessionName);
            clearParticipants(sessionId);
            removeJoinedSession(wellKnownName);
        }
        logInfo("SessionManager.leaveSession(" + wellKnownName + ") - " + status.toString());
        return status;
    }

    /**
     * joinOrCreateSession
     * joins the specified session if it exists or creates it if it doesn't.
     * 
     * @param sessionName  the session name (suffix) of the well known name of 
     *                     the session to join
     * @param sessionPort  the session port of the session to join
     * @param sessionId    set to the unique id for the session
     * @param sessionOpts  the session options of the joined session
     * @param sessionListener  listener for asynchronous session events
     * @param sessionPortListener  port listener that lets the user get the 
     *                             session id and specify the acceptance 
     *                             policy for joining the session 
     * @return  JoinOrCreateReturn  structure containing a flag to tell whether
     *                              the method tried to join or create 
     *                              a session and the AllJoyn return status of 
     *                              the operation
     */
    @Override
    public JoinOrCreateReturn joinOrCreateSession(String sessionName, short sessionPort, Mutable.IntegerValue sessionId,
            SessionOpts sessionOpts, SessionListener sessionListener, SessionPortListener sessionPortListener) {
        Status status;
        boolean joined;
        String wellKnownName = getWellKnownName(sessionName);
        logInfo("SessionManager.joinOrCreateSession(): Available Sessions - " + listSessions().toString());
        // If the desired session is available, then join it
        if(listSessions().contains(wellKnownName)) {
            status = joinSession(sessionName, sessionPort, sessionId, sessionOpts, sessionListener);
            joined = true;
            logInfo("SessionManager.joinOrCreateSession(): Joining Session - " + status.toString());
        }
        else {
            // The desired session is not available so create it
            status = createSession(sessionName, sessionPort, sessionPortListener, sessionOpts);
            joined = false;
            logInfo("SessionManager.joinOrCreateSession(): Creating Session - " + status.toString());
        }
        JoinOrCreateReturn ret = new JoinOrCreateReturn(status, joined);
        return ret;
    }

    /** 
     * getParticipants
     * gets the unique ids of all participants in the specified session.
     * 
     * @param sessionName  the session name (suffix) of the well known name of
     *                     the interested session
     * @return  a list of unique Ids of all participants in the session
     */
    @Override
    public synchronized ArrayList<String> getParticipants(String sessionName) {
        int sessionId = getSessionId(sessionName);
        ArrayList<String> clone = new ArrayList<String>();
        String wkn = getWellKnownName(sessionName);
        
        /* 
         * If we are hosting the session, but there is no session Id yet then we are
         * the only ones in the session
         */
        logInfo("SessionManager.getParticipants(): SessionName: " + sessionName + "  SessionID: " + sessionId);
        if((listHostedSessions().contains(wkn) || listAliases().contains(wkn)) && sessionId == -1) {
            clone.add(bus.getUniqueName());
            return clone;
        }
        // Get the list of participants for the session
        if(sessionIdToParticipants.containsKey(sessionId)) {
            ArrayList<String> participantList = sessionIdToParticipants.get(sessionId);
            if(participantList != null) {
                // Create and return a deep copy of the participant list for the specified session
                for (String participant : participantList) {
                    clone.add(new String(participant));
                }
            }
        }
        logInfo("SessionManager.getParticipants(): " + clone.toString());
        return clone;
    }
    
    /** 
     * getNumParticipants
     * gets the number of participants in the specified session.
     * 
     * @param sessionName  the session name (suffix) of the well known name of 
     *                     the interested session
     * @return  the number of participants in the session
     */
    @Override
    public synchronized int getNumParticipants(String sessionName) {
        int size = -1;
        // Get the ID of the session
        int sessionId = getSessionId(sessionName);
        if(sessionIdToParticipants.containsKey(sessionId)) {
            size = sessionIdToParticipants.get(sessionId).size();
        }
        logInfo("SessionManager.getNumParticipants(" + sessionId + "): " + size);
        return size;
    }

    /** 
     * listSessions
     * lists the well known names of all of the available sessions that have
     * the same well known name prefix as the one specified when instantiating 
     * the session manager object.
     * 
     * @return  a list of well known names of all interested available sessions
     */
    @Override
    public synchronized ArrayList<String> listSessions() {
        // Create and return a deep copy of the session list
        ArrayList<String> clone = new ArrayList<String>(availableSessions.size());
        for (String session : availableSessions) {
            clone.add(new String(session));
        }
        logInfo("SessionManager.listSessions(): " + clone.toString());
        return clone;
    }
    
    /**
     * listHostedSessions
     * lists the well known names of all sessions currently hosted by the 
     * session manager object.
     * 
     * @return  a list of the well known names of all sessions currently 
     *          hosted by the session manager object
     */
    @Override
    public synchronized ArrayList<String> listHostedSessions() {
        // Create and return a deep copy of the hosted sessions list
        ArrayList<String> clone = new ArrayList<String>(hostedSessions.size());
        for (String session : hostedSessions) {
            clone.add(new String(session));
        }
        logInfo("SessionManager.listHostedSessions(): " + clone.toString());
        return clone;
    }
    
    /**
     * listJoinedSessions
     * lists the well known names of all sessions currently joined by the
     * session manager object.
     * 
     * @return  a list of the well known names of all sessions currently 
     *          joined by the session manager object
     */
    @Override
    public synchronized ArrayList<String> listJoinedSessions() {
        // Create and return a deep copy of the joined sessions list
        ArrayList<String> clone = new ArrayList<String>(joinedSessions.size());
        for (String session : joinedSessions) {
            clone.add(new String(session));
        }
        logInfo("SessionManager.listJoinedSessions(): " + clone.toString());
        return clone;
    }

    /**
     * getBusAttachment
     * gets the bus attachment created by the session manager.
     * 
     * @return  the bus attachment created and used by the session manager object
     */
    @Override
    public BusAttachment getBusAttachment() {
        return bus;
    }

    /**
     * registerBusObject
     * registers a single bus object on the bus.
     * 
     * @param busObject   the bus object to register
     * @param objectPath  the object path to register the bus object at
     * @return  OK if successful
     */
    @Override
    public Status registerBusObject(BusObject busObject, String objectPath) {
        // Register a single bus object
        Status status = bus.registerBusObject(busObject, objectPath);
        logInfo("SessionManager.registerBusObject(): Registering bus object at " + objectPath 
                + " - " + status.toString());
        if(status == Status.OK) {
            registeredBusObjects.add(busObject);
            // Connect the bus after the app registers its first bus object
//            if(registeredBusObjects.size() == 1){
//                logInfo("SessionManager.registerBusObject(): Registered the 1st bus object, Attempting to connect the bus");
//                status = connectBus();
//                if(status != Status.OK) {
//                    logError("SessionManager: Failed to connect Bus - " + status);
//                    return status;
//                }
//            }
        }
        return status;
    }
    
    /**
     * registerBusObjects
     * registers a list of bus object at their object paths on the bus.
     * 
     * @param busObjects  the list of bus objects and their corresponding 
     *                    object paths to register
     * @return  OK if successful
     */
    @Override
    public Status registerBusObjects(ArrayList<BusObjectData> busObjects) {
        if(busObjects == null) {
            return Status.FAIL;
        }
        Status status = Status.OK;
            
        // Register all the bus objects in the list of given bus objects
        for (BusObjectData busObjectData : busObjects) {
            status = registerBusObject(busObjectData.getBusObject(), busObjectData.getObjectPath());
            if(status != Status.OK) {
                return status;
            }
        }
        return status;
    }
    
    /**
     * unregisterBusObject
     * unregisters a single bus object from the bus.
     * 
     * @param busObject  the bus object to unregister
     */
    @Override
    public void unregisterBusObject(BusObject busObject) {
        if(busObject == null) {
            return;
        }
        // Unregister a single bus object
        logInfo("SessionManager.unregisterBusObject()");
        bus.unregisterBusObject(busObject);
        registeredBusObjects.remove(busObject);
        if(registeredBusObjects.size() == 0) {
            // Disconnect the bus after unregistering the last bus object
            logInfo("SessionManager.unregisterBusObject(): Unregistered the last bus object, Attempting to disconnect the bus");
            isAutoDisconnect = true;
            disconnectBus();
        }
    }
    
    /**
     * unregisterBusObjects
     * unregisters a list of bus object from the bus.
     * 
     * @param busObjects  the list of bus objects to unregister
     */
    @Override
    public void unregisterBusObjects(ArrayList<BusObjectData> busObjects) {
        if(busObjects == null) {
            return;
        }
        // Unregister all the bus objects in the list of given bus objects
        for (BusObjectData busObjectData : busObjects) {
            unregisterBusObject(busObjectData.getBusObject());
        }
    }
    
    /**
     * unregisterAllBusObjects
     * unregisters all busObjects currently registered to the bus. This will 
     * result in a disconnecting of the bus attachment and therefore should 
     * be used during cleanup if not using disconnectBus().
     */
    @Override
    public void unregisterAllBusObjects() {
        // Copy the List to avoid ConcurrentModificationExceptions
        ArrayList<BusObject> copy = new ArrayList<BusObject>();
        for(BusObject busObj : registeredBusObjects) {
            copy.add(busObj);
        }
        // Unregister all bus objects that are still registered
        for(BusObject busObj : copy) {
            unregisterBusObject(busObj);
        }
    }
    
    /**
     * getSessionId
     * gets the session Id of the session with the specified session name.
     * 
     * @param sessionName  the name of the session to get the id of
     * @return  the session id of the session if currently participating in it,
     *          otherwise -1
     */
    @Override
    public int getSessionId(String sessionName) {
        if(sessionName != null) {
              if(sessionNameToId.containsKey(sessionName)) {
                  logInfo("SessionManager.getSessionId(): Name: " + sessionName + "  Id: " + sessionNameToId.get(sessionName));
                  return sessionNameToId.get(sessionName);
              }
              // Return the session id of the master session if sessionName is an Alias
              if(aliasToMasterSesssion.containsKey(sessionName)) {
                  String masterSession = aliasToMasterSesssion.get(sessionName);
                  return getSessionId(masterSession);
              }
        }
        return -1;
    }
    
    /**
     * registerSignalHandlers
     * registers all signal handlers in the specified class.
     * 
     * @param classWithSignalHandlers  the class containing the signal handlers 
     *                                 to be registered
     * @return  OK if successful 
     */
    @Override
    public Status registerSignalHandlers(Object classWithSignalHandlers) {
        // Register all the signal handlers in the given class
        Status status = bus.registerSignalHandlers(classWithSignalHandlers);
        if(status == Status.OK && !classesWithSignalHandlers.contains(classWithSignalHandlers)) {
            classesWithSignalHandlers.add(classWithSignalHandlers);
        }
        return status;
    }
    
    /**
     * addSessionManagerListener
     * adds an additional listener to detect the sessionJoined(), 
     * sessionLost(), sessionMemberAdded(), and sessionMemberRemoved() signals.
     * 
     * @param sessionManagerListener  the listener with the callback methods 
     *                                to invoke
     */
    @Override
    public synchronized void addSessionManagerListener(SessionManagerListener sessionManagerListener) {
    	if(sessionManagerListener != null) {
	        sessionManagerListeners.remove(sessionManagerListener);
	        sessionManagerListeners.add(sessionManagerListener);
    	}
    }
    
    /**
     * isBusConnected
     * tells whether or not the bus attachment is connected to the bus.
     * 
     * @return  true if the bus attachment is connected to the bus. 
     *          Otherwise false is returned.
     */
    @Override
    public boolean isBusConnected() {
        return busConnected;
    }
    
    /**
     * registerAuthListener
     * registers a user-defined authentication listener class with a specific 
     * default key store.
     * 
     * @param authMechanisms  the authentication mechanism(s) to use for 
     *                        peer-to-peer authentication
     * @param listener        the authentication listener
     * @param keyStoreFileName  the name of the default key store. Note that
     *                          the default key store implementation may be 
     *                          overriden with 
     *                          registerKeyStoreListener(KeyStoreListener).
     * @param isShared   Set to true if the default keystore will be shared 
     *                   between multiple programs. Sll programs must have 
     *                   read/write permissions to the keyStoreFileName file. 
     * @return  OK if successful
     */
    @Override
    public Status registerAuthListener(String authMechanisms, AuthListener listener,
            String keyStoreFileName, boolean isShared) {
        return bus.registerAuthListener(authMechanisms, listener, keyStoreFileName, isShared);
    }
    
    /**
     * registerAuthListener
     * registers a user-defined authentication listener class with a specific 
     * default key store.
     * 
     * @param authMechanisms  the authentication mechanism(s) to use for 
     *                        peer-to-peer authentication
     * @param listener        the authentication listener
     * @param keyStoreFileName  the name of the default key store. Note that
     *                          the default key store implementation may be 
     *                          overriden with 
     *                          registerKeyStoreListener(KeyStoreListener).
     * @return  OK if successful
     */
    @Override
    public Status registerAuthListener(String authMechanisms, AuthListener listener,
            String keyStoreFileName) {
        return bus.registerAuthListener(authMechanisms, listener, keyStoreFileName);
    }
    
    /**
     * registerAuthListener
     * registers a user-defined authentication listener class with a specific 
     * default key store.
     * 
     * @param authMechanisms  the authentication mechanism(s) to use for 
     *                        peer-to-peer authentication
     * @param listener        the authentication listener
     * @return  OK if successful
     */
    @Override
    public Status registerAuthListener(String authMechanisms, AuthListener listener) {
        return bus.registerAuthListener(authMechanisms, listener);
    }
    
    /**
     * addAlias
     * advertises a new well known name for an existing hosted session.
     * 
     * @param aliasSessionName   the session name (well known name suffix) of 
     *                           the alias to advertise
     * @param masterSessionName  the session name (well known name suffix) of 
     *                           the session that is being aliased
     * @param sessionOpts        used to specify the transport to advertise the
     *                           alias over
     * @return  OK if successful
     */
    @Override
    public Status addAlias(String aliasSessionName, String masterSessionName, SessionOpts sessionOpts) {
        Status status;
        String wellKnownName = getWellKnownName(aliasSessionName);
        String masterWkn = getWellKnownName(masterSessionName); 
        
        logInfo("SessionManager.addAlias(" + wellKnownName + ", " + masterWkn + ")");
        
        // Make sure the wellKnownName isn't already taken
        if(listSessions().contains(wellKnownName)) {
            logInfo("SessionManager.addAlias(): " + wellKnownName + " is already being advertised");
            return Status.FAIL;
        }
        logInfo("SessionManager.addAlias(): " + wellKnownName + " is available");
        
        // Make sure the Master Session is Valid
        if(!listHostedSessions().contains(masterWkn)) {
            logInfo("SessionManager.addAlias(): You are not hosting Master Session " + masterWkn);
            return Status.FAIL;
        }

        // Request the Well Known Name
        int flag = BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE;
        status = bus.requestName(wellKnownName, flag);
        logInfo("SessionManager.addAlias(): Requesting name " + wellKnownName + " - " + status.toString());
        if(status == Status.OK) {
            // Advertise the Well Known Name
            status = bus.advertiseName(wellKnownName, sessionOpts.transports);
            logInfo("SessionManager.addAlias(): Advertising name " + wellKnownName + " - " + status.toString());
            if(status == Status.OK) {
                // Add the new alias to the list of advertised aliases
                addAliasToList(wellKnownName);
                // Map the alias to its master session
                addAliasToMaster(aliasSessionName, masterSessionName);
                // Successful return here
                return status;
            }
            // Fall through and cleanup on failure
            bus.releaseName(wellKnownName);
        }
        return status;
    }
    
    /**
     * removeAlias
     * stops the advertisement of the specified alias.
     * 
     * @param aliasSessionName  the well known name suffix of the alias session
     */
    @Override
    public void removeAlias(String aliasSessionName) {
        logInfo("SessionManager.removeAlias(): Removing Alias - " + aliasSessionName);
        String wellKnownName = getWellKnownName(aliasSessionName);
        if(listAliases().contains(wellKnownName)) {
            // Stop advertising the session
            bus.cancelAdvertiseName(wellKnownName, SessionOpts.TRANSPORT_ANY);
            logInfo("SessionManager.removeAlias(): Canceling Advertised Name " + wellKnownName);
            // Release the well known name
            bus.releaseName(wellKnownName);
            logInfo("SessionManager.removeAlias(): Releasing Name " + wellKnownName);
            // Remove the alias from the list of advertised aliases
            removeAliasFromList(wellKnownName);
            // Remove the alias from the Alias to Master Session mapping
            removeAliasFromMaster(aliasSessionName);
        }
    }
    
    /**
     * listAliases
     * lists the well known names of all aliases currently advertised by the 
     * session manager object.
     * 
     * @return  a list of the well known names of all aliases currently 
     *          advertised by the session manager object
     */
    @Override
    public synchronized ArrayList<String> listAliases() {
        // Create and return a deep copy of the alias list
        ArrayList<String> clone = new ArrayList<String>(aliases.size());
        for (String alias : aliases) {
            clone.add(new String(alias));
        }
        logInfo("SessionManager.listAliases(): " + clone.toString());
        return clone;
    }
    
    
    /**
     * getSessionName
     * gets the session name (well known name suffix) of the given full well
     * known name.
     * 
     * @param wellKnownName  the full well known name
     * @return  the session name (well known name suffix) of the given well 
     *          known name
     */
    @Override
    public String getSessionName(String wellKnownName) {
        return wellKnownName.replaceFirst(serviceName + ".", "");
    }
    
//    @Override
//    public void breakOutSession(String[] ids, int waitTime) {
//        // TODO Auto-generated method stub
//        
//    }
//
//    @Override
//    public void mergeSessions(String[] sessions) {
//        // TODO Auto-generated method stub
//        
//    }
//
//    @Override
//    public void kickParticipants(String[] ids) {
//        // TODO Auto-generated method stub
//        
//    }
//
//    @Override
//    public void banParticipants(String[] ids) {
//        kickParticipants(ids);
//        for(int i=0; i < ids.length; i++) {
//            addBannedParticipant(ids[i]);
//        }
//    }
//    
//    @Override
//    public void unbanParticipants(String[] ids) {
//        for(int i=0; i < ids.length; i++) {
//            removeBannedParticipant(ids[i]);
//        }
//    }
    
    
    /*------------------------------------------------------------------------*
     * Private Listener Classes
     *------------------------------------------------------------------------*/
    private class SMBusListener extends BusListener {
        BusListener bl;
        
        SMBusListener(BusListener busListener) {
            bl = busListener;
        }
        
        /**
         * This method is called when AllJoyn discovers a remote attachment
         * that is hosting an chat channel.  We expect that since we only
         * do a findAdvertisedName looking for instances of the chat
         * well-known name prefix we will only find names that we know to
         * be interesting.  When we find a remote application that is
         * hosting a channel, we add its channel name it to the list of
         * available channels selectable by the user.
         */
        public void foundAdvertisedName(String name, short transport, String namePrefix) {
        	//bus.enableConcurrentCallbacks();
            logInfo("BusListener.foundAdvertisedName - " + name);
            addFoundSession(name);
            bl.foundAdvertisedName(name, transport, namePrefix);
            logInfo("should have called my BusListener!!!");
            logInfo("Going to iterate over the registered listeners: "+sessionManagerListeners.size());
            for(SessionManagerListener listener : sessionManagerListeners) {
            	logInfo("Going to call listener: "+listener);
                listener.foundAdvertisedName(name, transport, namePrefix);
            }
        }
        
        /**
         * This method is called when AllJoyn decides that a remote bus
         * attachment that is hosting an chat channel is no longer available.
         * When we lose a remote application that is hosting a channel, we
         * remove its name from the list of available sessions selectable
         * by the user.  
         */
        public void lostAdvertisedName(String name, short transport, String namePrefix) {
        	//bus.enableConcurrentCallbacks();
            logInfo("BusListener.lostAdvertisedName - " + name);
            removeFoundSession(name);
            bl.lostAdvertisedName(name, transport, namePrefix);
            for(SessionManagerListener listener : sessionManagerListeners) {
                listener.lostAdvertisedName(name, transport, namePrefix);
            }
        }
        
        public void nameOwnerChanged(String busName, String previousOwner, String newOwner) {
            logInfo("BusListener.nameOwnerChanged - name: " + busName + "  Prev: " + previousOwner + "  New: " + newOwner);
            bl.nameOwnerChanged(busName, previousOwner, newOwner);
            for(SessionManagerListener listener : sessionManagerListeners) {
                listener.nameOwnerChanged(busName, previousOwner, newOwner);
            }
        }
        
        public void busStopping() {
            logInfo("BusListener.busStopping()");
            bl.busStopping();
            for(SessionManagerListener listener : sessionManagerListeners) {
                listener.busStopping();
            }
        }
    }
    
    private class SMSessionPortListener extends SessionPortListener {
        // The app's session port listener
        private SessionPortListener spl;
        
        public SMSessionPortListener(SessionPortListener sessionPortListener) {
            spl = sessionPortListener;
        }
        
        /**
         * This method is called when a client tries to join the session
         * we have bound.  It asks us if we want to accept the client into
         * our session.
         */
        public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
            logInfo("SessionPortListener.acceptSessionJoiner(" + sessionPort + "," + joiner + ")");
            if(bannedParticipants.contains(joiner)) {
                return false;
            }
            return spl.acceptSessionJoiner(sessionPort, joiner, sessionOpts);
        }
        
        /**
         * If we return true in acceptSessionJoiner, we admit a new client
         * into our session.  The session does not really exist until a 
         * client joins, at which time the session is created and a session
         * ID is assigned.  This method communicates to us that this event
         * has happened, and provides the new session ID for us to use.
         */
        public void sessionJoined(short sessionPort, int id, String joiner) {
        	//bus.enableConcurrentCallbacks();
            logInfo("SessionPortListener.sessionJoined(" + sessionPort + "," + joiner + ")");
            String sessionName = sessionPortToName.get(sessionPort);
            addSessionNameToId(sessionName, id);
            
            // Add the host to the list of participants. Should only happen once
            if(!getParticipants(sessionName).contains(bus.getUniqueName())) {
                logInfo("SessionPortListener.SessionJoined(): Host is adding self to Participant list");
                addParticipant(id, bus.getUniqueName());
                logInfo("SessionPortListener.SessionJoined(): Setting Session Listener");
                SMSessionListener smSessionListener = new SMSessionListener(new SessionListener());
                bus.setSessionListener(id, smSessionListener); 
                // Explicitly trigger SessionMemberAdded for the first Joiner in the Host 
                smSessionListener.sessionMemberAdded(id, joiner);
            }
            
            spl.sessionJoined(sessionPort, id, joiner);
            
            for(SessionManagerListener listener : sessionManagerListeners) {
                listener.sessionJoined(sessionPort, id, joiner);
            }
        }             
    };
    
    private class SMSessionListener extends SessionListener {
        // The app's session listener
        private SessionListener sl;
        
        public SMSessionListener(SessionListener sessionListener) {
            sl = sessionListener;
        }
        
        public void sessionLost(int sessionId) {
            // TODO: Cleanup joined sessions here
        	//bus.enableConcurrentCallbacks();
            logInfo("SessionListener.sessionLost(" + sessionId + ")");
            //clearParticipants(sessionId);
            sl.sessionLost(sessionId);
            for(SessionManagerListener listener : sessionManagerListeners) {
                listener.sessionLost(sessionId);
            }
        }
        
        public void sessionMemberAdded(int sessionId, String uniqueName) {
            logInfo("SessionListener.sessionMemberAdded(" + sessionId + ", " + uniqueName + ")");
            //bus.enableConcurrentCallbacks();
            // Add the new session joiner to the list of participants for the session
            addParticipant(sessionId, uniqueName);
            sl.sessionMemberAdded(sessionId, uniqueName);
            for(SessionManagerListener listener : sessionManagerListeners) {
                listener.sessionMemberAdded(sessionId, uniqueName);
            }
        }
        
        public void sessionMemberRemoved(int sessionId, String uniqueName) {
            logInfo("SessionListener.sessionMemberRemoved(" + sessionId + ", " + uniqueName + ")");
            //bus.enableConcurrentCallbacks();
            // Remove the participant from the list of participants for the session
            removeParticipant(sessionId, uniqueName);
            sl.sessionMemberRemoved(sessionId, uniqueName);
            for(SessionManagerListener listener : sessionManagerListeners) {
                listener.sessionMemberRemoved(sessionId, uniqueName);
            }
        }
    }
    
    /*
     * SessionManagerBusObject 
     * This class is not being used yet
     */
    private class SessionManagerBusObject implements SessionManagerBusInterface, BusObject {

        @Override
        public void RequestBreakOut(String uniqueId/*, SessionInfo sessionInfo*/)
                throws BusException { 
        }

        @Override
        public void AcceptBreakOut() throws BusException {  
        }

        @Override
        public void RequestMerge(String str/*, SessionInfo sessionInfo*/)
                throws BusException {
        }

        @Override
        public void AcceptMerge(String str/*, SessionInfo sessionInfo*/)
                throws BusException {   
        }

        @Override
        public void MigrateSession(/*SessionInfo sessionInfo*/) throws BusException {
        }

        @Override
        public void KickFromSession() throws BusException {  
        }

        @Override
        public void CreateSignalEmitter(String uniqueId) throws BusException {  
        }
        
    }

    
    /*------------------------------------------------------------------------*
     * Helper Methods
     *------------------------------------------------------------------------*/
    /**
     * setDebug
     * enables or disables debug messages.
     * 
     * @param debug  true to turn debug message on, false otehrwise.
     */
    public void setDebug (boolean debug) {
        debugOn = debug;
    }
    
    private void logInfo(String message) {
        if(debugOn) {
            System.out.println(message);
        }
    }
    
    private void logError(String message) {
        System.err.println(message);
    }
    
    private String getWellKnownName(String sessionName) {
        if(serviceName.equals("") || serviceName.contains(" ")) {
            return sessionName;
        }
        else {
            return serviceName + "." + sessionName;
        }
    }
     
    
    // Thread Safe Methods
    // Accessing The List of Available Sessions
    private synchronized void addFoundSession(String sessionName) {
        availableSessions.remove(sessionName);
        availableSessions.add(sessionName);
    }
    
    private synchronized void removeFoundSession(String sessionName) {
        availableSessions.remove(sessionName);
    }
    
    // Accessing The List of Hosted Sessions
    private synchronized void addHostedSession(String sessionName) {
        hostedSessions.remove(sessionName);
        hostedSessions.add(sessionName);
    }
    
    private synchronized void removeHostedSession(String sessionName) {
        hostedSessions.remove(sessionName);
    }
    
    // Accessing The List of Joined Sessions
    private synchronized void addJoinedSession(String sessionName) {
        joinedSessions.remove(sessionName);
        joinedSessions.add(sessionName);
    }
    
    private synchronized void removeJoinedSession(String sessionName) {
        joinedSessions.remove(sessionName);
    }
    
    // Accessing The Lists of Participants
    private synchronized void addParticipant(int sessionId, String name) {
        logInfo("SessionManager.addParticipant(" + sessionId + ", " + name + ")");
        if(sessionIdToParticipants.containsKey(sessionId)) {
            sessionIdToParticipants.get(sessionId).remove(name);
        }
        else {
            sessionIdToParticipants.put(sessionId, new ArrayList<String>());
        }
        sessionIdToParticipants.get(sessionId).add(name);
    }
    
    private synchronized void removeParticipant(int sessionId, String name) {
        if(sessionIdToParticipants.containsKey(sessionId)) {
            sessionIdToParticipants.get(sessionId).remove(name);
        }
    }
    
    private synchronized void clearParticipants(int sessionId) {
        logInfo("SessionManager: Clearing Participants in session " + sessionId);
        if(sessionIdToParticipants.containsKey(sessionId)) {
            sessionIdToParticipants.get(sessionId).clear();
            sessionIdToParticipants.remove(sessionId);
        }
    }
    
    // Accessing The List of Banned Participants
    private synchronized void addBannedParticipant(String name) {
        bannedParticipants.remove(name);
        bannedParticipants.add(name);
    }
    
    private synchronized void removeBannedParticipant(String name) {
        bannedParticipants.remove(name);
    }
    
    private synchronized void clearBannedParticipants() {
        bannedParticipants.clear();
    }
    
    // Accessing The Maps of Session Names to Ports
    private synchronized void addSessionNameToPort(String name, short sessionPort) {
        sessionNameToPort.remove(name);
        sessionNameToPort.put(name, sessionPort);
        sessionPortToName.remove(sessionPort);
        sessionPortToName.put(sessionPort, name);
    }
    
    private synchronized void removeSessionNameToPort(String sessionName) {
        if(sessionNameToPort.containsKey(sessionName)) {
            short sessionPort = sessionNameToPort.get(sessionName);
            sessionPortToName.remove(sessionPort);
            sessionNameToPort.remove(sessionName);
        }
    }
    
    // Accessing The Maps of Session Names to Ids
    private synchronized void addSessionNameToId(String sessionName, int sessionId) {
        sessionNameToId.remove(sessionName);
        sessionNameToId.put(sessionName, sessionId);
    }
    
    private synchronized void removeSessionNameToId(String sessionName) {
        sessionNameToId.remove(sessionName);
    }
    
    // Accessing The List of Aliases
    private synchronized void addAliasToList(String aliasSessionName) {
        aliases.remove(aliasSessionName);
        aliases.add(aliasSessionName);
    }
    
    private synchronized void removeAliasFromList(String aliasSessionName) {
        aliases.remove(aliasSessionName);
    }
    
    // Accessing The Mappings of Aliases to Master Sessions
    private synchronized void addAliasToMaster(String aliasSessionName, String masterSessionName) {
        // Map the Alias to the Master Session
        aliasToMasterSesssion.remove(aliasSessionName);
        aliasToMasterSesssion.put(aliasSessionName, masterSessionName);
        
        // Add the Alias to the Master Session's list of Aliases
        if(masterSessionToAliases.containsKey(masterSessionName)) {
            masterSessionToAliases.get(masterSessionName).remove(aliasSessionName);
        }
        else {
            masterSessionToAliases.put(masterSessionName, new ArrayList<String>());
        }
        masterSessionToAliases.get(masterSessionName).add(aliasSessionName);
    }
    
    private synchronized void removeAliasFromMaster(String aliasSessionName) {
        if(aliasToMasterSesssion.containsKey(aliasSessionName)) {
            String masterSessionName = aliasToMasterSesssion.get(aliasSessionName);
            // Unmap the Alias from the Master Session
            aliasToMasterSesssion.remove(aliasSessionName);
            
            // Remove the Alias from the Master Session's list of Aliases
            if(masterSessionToAliases.containsKey(masterSessionName)) {
                masterSessionToAliases.get(masterSessionName).remove(aliasSessionName);
                if(masterSessionToAliases.get(masterSessionName).isEmpty()) {
                    masterSessionToAliases.remove(masterSessionName);
                }
            }
        }
    }
    
    /**
     * This method removes and stops advertising all the well known names of 
     * the given master session
     *  
     * @param masterSessionName  the session name of the master session of the aliases
     */
    private void removeAllAliasesOfMasterSession(String masterSessionName) {
        if(masterSessionToAliases.containsKey(masterSessionName)) {
            // Get the Aliases of the Master Session
            ArrayList<String> aliases = listAliases();
            // Stop Advertising and Remove all of the Aliases of the Master Session
            for(String alias : aliases) {
                String aliasSession = getSessionName(alias);
                removeAlias(aliasSession);
            }
        }
    }

}


