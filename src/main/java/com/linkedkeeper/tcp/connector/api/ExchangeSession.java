/*
 * Copyright (c) 2016, LinkedKeeper
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * - Neither the name of LinkedKeeper nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.linkedkeeper.tcp.connector.api;

import com.linkedkeeper.tcp.connector.Connection;
import com.linkedkeeper.tcp.connector.SessionManager;
import com.linkedkeeper.tcp.connector.api.listener.SessionEvent;
import com.linkedkeeper.tcp.connector.api.listener.SessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by frank@linkedkeeper.com on 17/1/10.
 */
public class ExchangeSession extends SessionValid {

    private final static Logger logger = LoggerFactory.getLogger(ExchangeSession.class);

    /**
     * The session identifier of this Session.
     */
    private String sessionId = null;

    private transient List<SessionListener> listeners = new CopyOnWriteArrayList<SessionListener>();

    private transient Connection connection = null;

    /**
     * The Manager with which this Session is associated.
     */
    private transient SessionManager sessionManager = null;

    public void access() {
        // Check to see if access is in progress or has previously been called
        if (!isValid) {
            return;
        }
        lastAccessedTime = System.currentTimeMillis();
    }

    public void connect() {
        // Check to see if tellNew is in progress or has previously been called
        if (connecting || !isValid) {
            logger.debug("the session " + sessionId + " is connecting or isValid = false!");
            return;
        }
        connecting = true;
        connection.connect();
        addSessionEvent();

        connecting = false;
        logger.debug("the session " + sessionId + " is ready!");
    }

    private void addSessionEvent() {
        SessionEvent event = new SessionEvent(this);
        for (SessionListener listener : listeners) {
            try {
                listener.sessionCreated(event);
                logger.info("SessionListener " + listener + " .sessionCreated() is invoked successfully!");
            } catch (Exception e) {
                logger.error("addSessionEvent error.", e);
            }
        }
    }

    /**
     * Perform the internal processing required to invalidate this session,
     * without triggering an exception if the session has already expired.
     */
    public void close() {
        close(true);
    }

    /**
     * Perform the internal processing required to invalidate this session,
     * without triggering an exception if the session has already expired.
     *
     * @param notify Should we notify listeners about the demise of this session?
     */
    void close(boolean notify) {
        // Check to see if close is in progress or has previously been called
        if (closing || !isValid) {
            logger.debug("the session " + sessionId + " is closing or isValid = false!");
            return;
        }
        synchronized (this) {
            // Check again, now we are inside the sync so this code only runs
            // once
            // Double check locking - closing and isValid need to be volatile
            if (closing || !isValid) {
                logger.debug("the session " + sessionId + " is closing or isValid = false!");
                return;
            }
            // Mark this session as "being closed"
            closing = true;
            if (notify) {
                SessionEvent event = new SessionEvent(this);
                for (SessionListener listener : listeners) {
                    try {
                        listener.sessionDestroyed(event);
                        logger.debug("SessionListener " + listener + " .sessionDestroyed() is invoked successfully!");
                    } catch (Exception e) {
                        logger.error("sessionDestroyed error! " + e);
                    }
                }
            }
            setValid(false);

            connection.close();

            recycle();
            // We have completed close of this session
            closing = false;
            logger.debug("the session " + sessionId + " have been destroyed!");
        }
    }

    /**
     * Release all object references, and initialize instance variables, in
     * preparation for reuse of this object.
     */
    public void recycle() {
        logger.debug("the session " + sessionId + " is recycled!");
        // Remove this session from our manager's active sessions
        sessionManager.removeSession(this);

        // Reset the instance variables associated with this Session
        listeners.clear();
        listeners = null;
        creationTime = 0L;
        connecting = false;
        closing = false;
        sessionId = null;
        lastAccessedTime = 0L;
        maxInactiveInterval = -1;
        isValid = false;
        sessionManager = null;
    }

    public boolean expire() {
        //A negative time indicates that the session should never time out.
        if (maxInactiveInterval < 0)
            return false;

        long timeNow = System.currentTimeMillis();
        int timeIdle = (int) ((timeNow - lastAccessedTime) / 1000L);
        if (timeIdle >= maxInactiveInterval) {
            return true;
        }
        return false;
    }

    /**
     * Add a session event listener to this component.
     */
    public void addSessionListener(SessionListener listener) {
        if (null == listener) {
            throw new IllegalArgumentException("addSessionListener listener");
        }
        listeners.add(listener);
    }

    /**
     * Remove a session event listener from this component.
     */
    public void removeSessionListener(SessionListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("removeSessionListener listener");
        }
        listeners.remove(listener);
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return this.connection;
    }
}
