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

package com.linkedkeeper.tcp.utils;

import com.linkedkeeper.tcp.constant.Constants;
import com.linkedkeeper.tcp.exception.InitErrorException;
import io.netty.channel.Channel;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

public class NetUtils {

    private final static Logger logger = LoggerFactory.getLogger(NetUtils.class);

    /**
     * 最小端口
     */
    private static final int MIN_PORT = 0;
    /**
     * 最大端口
     */
    private static final int MAX_PORT = 65535;

    /**
     * 判断端口是否有效 0-65535
     *
     * @param port 端口
     * @return 是否有效
     */
    public static boolean isInvalidPort(int port) {
        return port > MAX_PORT || port < MIN_PORT;
    }

    /**
     * 判断端口是否随机端口 小于0表示随机
     *
     * @param port 端口
     * @return 是否随机端口
     */
    public static boolean isRandomPort(int port) {
        return port < 0;
    }

    /**
     * 检查当前指定端口是否可用，不可用则自动+1再试（随机端口从默认端口开始检查）
     *
     * @param host 当前ip地址
     * @param port 当前指定端口
     * @return 从指定端口开始后第一个可用的端口
     */
    public static int getAvailablePort(String host, int port) {
        if (isAnyHost(host)
                || isLocalHost(host)
                || isHostInNetworkCard(host)) {
        } else {
            throw new InitErrorException("The host " + host
                    + " is not found in network cards, please check config");
        }
        if (port < MIN_PORT) {
            port = Constants.DEFAULT_SERVER_PORT;
        }
        for (int i = port; i <= MAX_PORT; i++) {
            ServerSocket ss = null;
            try {
                ss = new ServerSocket();
                ss.bind(new InetSocketAddress(host, i));
                logger.debug("ip:{} port:{" + host + "} is available" + i);
                return i;
            } catch (IOException e) {
                // continue
                logger.warn("Can't bind to address [{" + host + "}:{" + i + "}], " +
                        "Maybe 1) The port has been bound. " +
                        "2) The network card of this host is not exists or disable. " +
                        "3) The host is wrong.");
                logger.info("Begin try next port(auto +1):{" + i + 1 + "}");
            } finally {
                if (ss != null) {
                    try {
                        ss.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        throw new InitErrorException("Can't bind to ANY port of " + host + ", please check config");
    }

    /**
     * 任意地址
     */
    public static final String ANYHOST = "0.0.0.0";

    private static final Pattern LOCAL_IP_PATTERN = Pattern.compile("127(\\.\\d{1,3}){3}$");

    /**
     * IPv4地址
     */
    public static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$");

    /**
     * 是否本地地址 127.x.x.x 或者 localhost
     *
     * @param host 地址
     * @return 是否本地地址
     */
    public static boolean isLocalHost(String host) {
        return StringUtils.isNotBlank(host)
                && (LOCAL_IP_PATTERN.matcher(host).matches() || "localhost".equalsIgnoreCase(host));
    }

    /**
     * 是否默认地址 0.0.0.0
     *
     * @param host 地址
     * @return 是否默认地址
     */
    public static boolean isAnyHost(String host) {
        return ANYHOST.equals(host);
    }

    /**
     * 是否IPv4地址 0.0.0.0
     *
     * @param host 地址
     * @return 是否默认地址
     */
    public static boolean isIPv4Host(String host) {
        return StringUtils.isNotBlank(host)
                && IPV4_PATTERN.matcher(host).matches();
    }

    /**
     * 是否非法地址（本地或默认）
     *
     * @param host 地址
     * @return 是否非法地址
     */
    private static boolean isInvalidLocalHost(String host) {
        return StringUtils.isBlank(host)
                || isAnyHost(host)
                || isLocalHost(host);
    }

    /**
     * 是否合法地址（非本地，非默认的IPv4地址）
     *
     * @param address InetAddress
     * @return 是否合法
     */
    private static boolean isValidAddress(InetAddress address) {
        if (address == null || address.isLoopbackAddress())
            return false;
        String name = address.getHostAddress();
        return (name != null
                && !isAnyHost(name)
                && !isLocalHost(name)
                && isIPv4Host(name));
    }

    /**
     * 是否网卡上的地址
     *
     * @param host 地址
     * @return 是否默认地址
     */
    public static boolean isHostInNetworkCard(String host) {
        try {
            InetAddress addr = InetAddress.getByName(host);
            return NetworkInterface.getByInetAddress(addr) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 得到本机IPv4地址
     *
     * @return ip地址
     */
    public static String getLocalHost() {
        InetAddress address = getLocalAddress();
        return address == null ? null : address.getHostAddress();
    }

    /**
     * 遍历本地网卡，返回第一个合理的IP，保存到缓存中
     *
     * @return 本地网卡IP
     */
    public static InetAddress getLocalAddress() {
        InetAddress localAddress = null;
        try {
            localAddress = InetAddress.getLocalHost();
            if (isValidAddress(localAddress)) {
                return localAddress;
            }
        } catch (Throwable e) {
            logger.warn("Error when retriving ip address: " + e.getMessage(), e);
        }
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    try {
                        NetworkInterface network = interfaces.nextElement();
                        Enumeration<InetAddress> addresses = network.getInetAddresses();
                        if (addresses != null) {
                            while (addresses.hasMoreElements()) {
                                try {
                                    InetAddress address = addresses.nextElement();
                                    if (isValidAddress(address)) {
                                        return address;
                                    }
                                } catch (Throwable e) {
                                    logger.warn("Error when retriving ip address: " + e.getMessage(), e);
                                }
                            }
                        }
                    } catch (Throwable e) {
                        logger.warn("Error when retriving ip address: " + e.getMessage(), e);
                    }
                }
            }
        } catch (Throwable e) {
            logger.warn("Error when retriving ip address: " + e.getMessage(), e);
        }
        logger.error("Can't get valid host, will use 127.0.0.1 instead.");
        return localAddress;
    }

    /**
     * InetSocketAddress转 host:port 字符串
     *
     * @param address InetSocketAddress转
     * @return host:port 字符串
     */
    public static String toAddressString(InetSocketAddress address) {
        if (address == null) {
            return StringUtils.EMPTY;
        } else {
            return toIpString(address) + ":" + address.getPort();
        }
    }

    /**
     * 得到ip地址
     *
     * @param address InetSocketAddress
     * @return ip地址
     */
    public static String toIpString(InetSocketAddress address) {
        if (address == null) {
            return null;
        } else {
            InetAddress inetAddress = address.getAddress();
            return inetAddress == null ? address.getHostName() :
                    inetAddress.getHostAddress();
        }
    }

    /**
     * 本地多ip情况下、连一下注册中心地址得到本地IP地址
     *
     * @param registryIp 注册中心地址
     * @return 本地多ip情况下得到本地能连上注册中心的IP地址
     */
    public static String getLocalHostByRegistry(String registryIp) {
        String host = null;
        if (registryIp != null && registryIp.length() > 0) {
            List<InetSocketAddress> addrs = getIpListByRegistry(registryIp);
            for (int i = 0; i < addrs.size(); i++) {
                InetAddress address = getLocalHostBySocket(addrs.get(i));
                if (address != null) {
                    host = address.getHostAddress();
                    if (host != null && !NetUtils.isInvalidLocalHost(host)) {
                        return host;
                    }
                }
            }
        }
        if (NetUtils.isInvalidLocalHost(host)) {
            host = NetUtils.getLocalHost();
        }
        return host;
    }

    /**
     * 通过连接远程地址得到本机内网地址
     *
     * @param remoteAddress 远程地址
     * @return 本机内网地址
     */
    private static InetAddress getLocalHostBySocket(InetSocketAddress remoteAddress) {
        InetAddress host = null;
        try {
            // 去连一下远程地址
            Socket socket = new Socket();
            try {
                socket.connect(remoteAddress, 1000);
                // 得到本地地址
                host = socket.getLocalAddress();
            } finally {
                try {
                    socket.close();
                } catch (Throwable e) {
                    logger.warn("NetUtils getLocalHostBySocket occur Exception!", e);
                }
            }
        } catch (Exception e) {
            logger.warn("Can not connect to host {" + remoteAddress.toString() + "}, cause by :{" + e.getMessage() + "}");
        }
        return host;
    }

    /**
     * 解析注册中心地址配置为多个连接地址
     *
     * @param registryIp 注册中心地址
     * @return
     */
    public static List<InetSocketAddress> getIpListByRegistry(String registryIp) {
        List<String[]> ips = new ArrayList<String[]>();
        String defaultPort = null;

        String[] srcIps = registryIp.split(",");
        for (String add : srcIps) {
            int a = add.indexOf("://");
            if (a > -1) {
                add = add.substring(a + 3); // 去掉协议头
            }
            String[] s1 = add.split(":");
            if (s1.length > 1) {
                if (defaultPort == null && s1[1] != null && s1[1].length() > 0) {
                    defaultPort = s1[1];
                }
                ips.add(new String[]{s1[0], s1[1]}); // 得到ip和端口
            } else {
                ips.add(new String[]{s1[0], defaultPort});
            }
        }

        List<InetSocketAddress> ads = new ArrayList<InetSocketAddress>();
        for (int j = 0; j < ips.size(); j++) {
            String[] ip = ips.get(j);
            try {
                InetSocketAddress address = new InetSocketAddress(ip[0],
                        Integer.parseInt(ip[1] == null ? defaultPort : ip[1]));
                ads.add(address);
            } catch (Exception e) {
                logger.warn("NetUtils getIpListByRegistry occur Exception!", e);
            }
        }

        return ads;
    }

    /**
     * 判断当前ip是否符合白名单
     *
     * @param whitelist 白名单，可以配置为*
     * @param localIP   当前地址
     * @return
     */
    public static boolean isMatchIPByPattern(String whitelist, String localIP) {
        if (StringUtils.isNotBlank(whitelist)) {
            if ("*".equals(whitelist)) {
                return true;
            }
            for (String ips : whitelist.replace(',', ';').split(";", -1)) {
                try {
                    if (ips.contains("*")) { // 带通配符
                        String regex = ips.trim().replace(".", "\\.").replace("*", ".*");
                        Pattern pattern = Pattern.compile(regex);
                        if (pattern.matcher(localIP).find()) {
                            return true;
                        }
                    } else if (!isIPv4Host(ips)) { // 不带通配符的正则表达式
                        String regex = ips.trim().replace(".", "\\.");
                        Pattern pattern = Pattern.compile(regex);
                        if (pattern.matcher(localIP).find()) {
                            return true;
                        }
                    } else {
                        if (ips.equals(localIP)) {
                            return true;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("syntax of pattern {" + ips + "} is invalid");
                }
            }
        }
        return false;
    }

    /**
     * 连接转字符串
     *
     * @param local  本地地址
     * @param remote 远程地址
     * @return
     */
    public static String connectToString(InetSocketAddress local, InetSocketAddress remote) {
        return toAddressString(local) + " <-> " + toAddressString(remote);
    }

    /**
     * 连接转字符串
     *
     * @param local1  本地地址
     * @param remote1 远程地址
     * @return
     */
    public static String channelToString(SocketAddress local1, SocketAddress remote1) {
        try {
            InetSocketAddress local = (InetSocketAddress) local1;
            InetSocketAddress remote = (InetSocketAddress) remote1;
            return toAddressString(local) + " -> " + toAddressString(remote);
        } catch (Exception e) {
            return local1 + "->" + remote1;
        }
    }

    /**
     * 是否可以telnet
     *
     * @param ip      远程地址
     * @param port    远程端口
     * @param timeout 连接超时
     * @return 是否可连接
     */
    public static boolean canTelnet(String ip, int port, int timeout) {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), timeout);
            return socket.isConnected();
        } catch (Exception e) {
            logger.warn("NetUtils canTelnet occur Exception!", e);
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static String getTransportKey(String ip, int port) {
        return ip + "::" + port;
    }

    public static String getClientTransportKey(String protocolName, String ip, int port) {
        return protocolName + "::" + ip + "::" + port;
    }

    public static String getTransportKey(Channel channel) {
        InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
        String remoteIp = NetUtils.toIpString(address);
        int port = address.getPort();
        return getTransportKey(remoteIp, port);
    }
}