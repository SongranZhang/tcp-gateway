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

package com.linkedkeeper.tcp.notify;

import java.util.concurrent.TimeUnit;

public class NotifyFuture<V> implements java.util.concurrent.Future<V> {

    private final int timeout;

    private volatile Object result;

    private short waiters;

    /**
     * Future生成时间
     */
    private final long genTime = System.currentTimeMillis();
    /**
     * Future已发送时间
     */
    private volatile long sentTime;

    public NotifyFuture(int timeout) {
        this.timeout = timeout;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        Object result = this.result;
        if (isDone0(result)) {
            return false;
        }
        synchronized (this) {
            // Allow only once
            result = this.result;
            if (isDone0(result)) {
                return false;
            }
            if (hasWaiters()) {
                notifyAll();
            }
        }
        return true;
    }

    public boolean isCancelled() {
        return false;
    }

    public boolean isDone() {
        return isDone0(result);
    }

    public boolean isDone0(Object result) {
        return result != null && (Boolean) result;
    }

    public V get() throws InterruptedException {
        return get(timeout, TimeUnit.MILLISECONDS);
    }

    public V get(long timeout, TimeUnit unit) throws InterruptedException {
        timeout = unit.toMillis(timeout); // Turn to milliseconds
        long remainTime = timeout - (sentTime - genTime); // Residual time
        if (remainTime <= 0) { // There is no time to wait
            if (isDone()) { // Directly to see if it has been returned
                return getNow();
            }
        } else { // Waiting for the rest of time
            if (await(remainTime, TimeUnit.MILLISECONDS)) {
                return getNow();
            }
        }
        throw new InterruptedException("wait reply timeout.");
    }

    public V getNow() {
        Object result = this.result;
        return (V) result;
    }

    public boolean isSuccess() {
        Object result = this.result;
        if (result == null) {
            return false;
        }
        return (Boolean) result;
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return await0(unit.toNanos(timeout), true);
    }

    private boolean await0(long timeoutNanos, boolean interruptAble) throws InterruptedException {
        if (isDone()) {
            return true;
        }
        if (timeoutNanos <= 0) {
            return isDone();
        }
        if (interruptAble && Thread.interrupted()) {
            throw new InterruptedException(toString());
        }

        long startTime = System.nanoTime();
        long waitTime = timeoutNanos;
        boolean interrupted = false;

        try {
            synchronized (this) {
                if (isDone()) {
                    return true;
                }
                if (waitTime <= 0) {
                    return isDone();
                }

                incWaiters();
                try {
                    for (; ; ) {
                        try {
                            wait(waitTime / 1000000, (int) (waitTime % 1000000));
                        } catch (InterruptedException e) {
                            if (interruptAble) {
                                throw e;
                            } else {
                                interrupted = true;
                            }
                        }
                        if (isDone()) {
                            return true;
                        } else {
                            waitTime = timeoutNanos - (System.nanoTime() - startTime);
                            if (waitTime <= 0) {
                                return isDone();
                            }
                        }
                    }
                } finally {
                    decWaiters();
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public NotifyFuture<V> setSuccess(V result) {
        if (setSuccess0(result)) {
            return this;
        }
        throw new IllegalStateException("complete already: " + this);
    }

    private boolean setSuccess0(V result) {
        if (isDone()) {
            return false;
        }
        synchronized (this) {
            // Allow only once
            if (isDone()) {
                return false;
            }
            if (this.result == null) {
                this.result = result;
            }
            if (hasWaiters()) {
                notifyAll();
            }
        }
        return true;
    }

    public NotifyFuture<V> setFailure(Throwable cause) {
        if (setFailure0(cause)) {
            return this;
        }
        throw new IllegalStateException("complete already: " + this, cause);
    }

    private boolean setFailure0(Throwable cause) {
        if (isDone()) {
            return false;
        }
        synchronized (this) {
            // Allow only once
            if (isDone()) {
                return false;
            }
            result = false;
            if (hasWaiters()) {
                notifyAll();
            }
        }
        return true;
    }

    private boolean hasWaiters() {
        return waiters > 0;
    }

    private void incWaiters() {
        if (waiters == Short.MAX_VALUE) {
            throw new IllegalStateException("too many waiters:" + this);
        }
        waiters++;
    }

    private void decWaiters() {
        waiters--;
    }

    public int getTimeout() {
        return timeout;
    }

    /**
     * 设置已发送时间
     *
     * @param sentTime 已发送时间
     */
    public void setSentTime(long sentTime) {
        this.sentTime = sentTime;
    }
}
