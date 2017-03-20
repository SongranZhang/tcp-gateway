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

package com.linkedkeeper.tcp.connector.tcp.listener;

import com.linkedkeeper.tcp.connector.Session;
import com.linkedkeeper.tcp.connector.api.listener.SessionEvent;
import com.linkedkeeper.tcp.connector.api.listener.SessionListener;
import com.linkedkeeper.tcp.connector.tcp.TcpSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class TcpHeartbeatListener implements Runnable, SessionListener {

    private final static Logger logger = LoggerFactory.getLogger(TcpHeartbeatListener.class);

    private TcpSessionManager tcpSessionManager = null;

    private ReentrantLock lock = new ReentrantLock();
    private Condition notEmpty = lock.newCondition();

    private int checkPeriod = 30 * 1000;
    private volatile boolean stop = false;

    public TcpHeartbeatListener(TcpSessionManager tcpSessionManager) {
        this.tcpSessionManager = tcpSessionManager;
    }

    public void run() {
        while (!stop) {
            if (isEmpty()) {
                awaitQueue();
            }
            logger.info("TcpHeartbeatListener/online session count : " + tcpSessionManager.getSessionCount());
            // sleep period
            try {
                Thread.sleep(checkPeriod);
            } catch (InterruptedException e) {
                logger.error("TcpHeartbeatListener run occur InterruptedException!", e);
            }
            // is stop
            if (stop) {
                break;
            }
            // 检测在线用户，多久没有发送心跳，超过规定时间的删除掉
            checkHeartBeat();
        }
    }

    public void checkHeartBeat() {
        Session[] sessions = tcpSessionManager.getSessions();
        for (Session session : sessions) {
            if (session.expire()) {
                session.close();
                logger.info("heart is expire,clear sessionId:" + session.getSessionId());
            }
        }
    }

    private boolean isEmpty() {
        return tcpSessionManager.getSessionCount() == 0;
    }

    private void awaitQueue() {
        boolean flag = lock.tryLock();
        if (flag) {
            try {
                notEmpty.await();
            } catch (InterruptedException e) {
                logger.error("TcpHeartbeatListener awaitQueue occur InterruptedException!", e);
            } catch (Exception e) {
                logger.error("await Thread Queue error!", e);
            } finally {
                lock.unlock();
            }
        }
    }

    private void signalQueue() {
        boolean flag = false;
        try {
            flag = lock.tryLock(100, TimeUnit.MILLISECONDS);
            if (flag)
                notEmpty.signalAll();
        } catch (InterruptedException e) {
            logger.error("TcpHeartbeatListener signalQueue occur InterruptedException!", e);
        } catch (Exception e) {
            logger.error("signal Thread Queue error!", e);
        } finally {
            if (flag)
                lock.unlock();
        }
    }

    public void stop() {
        this.stop = true;
    }

    public void sessionCreated(SessionEvent se) {
        signalQueue();
    }

    public void sessionDestroyed(SessionEvent se) {
    }

}
