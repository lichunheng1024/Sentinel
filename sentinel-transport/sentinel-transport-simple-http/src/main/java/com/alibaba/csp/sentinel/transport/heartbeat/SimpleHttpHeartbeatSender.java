/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.transport.heartbeat;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.transport.HeartbeatSender;
import com.alibaba.csp.sentinel.transport.config.TransportConfig;
import com.alibaba.csp.sentinel.transport.heartbeat.client.SimpleHttpClient;
import com.alibaba.csp.sentinel.transport.heartbeat.client.SimpleHttpRequest;
import com.alibaba.csp.sentinel.transport.heartbeat.client.SimpleHttpResponse;
import com.alibaba.csp.sentinel.util.StringUtil;

/**
 * The heartbeat sender provides basic API for sending heartbeat request to provided target.
 * This implementation is based on a trivial HTTP client.
 *
 * @author Eric Zhao
 * @author leyou
 */
public class SimpleHttpHeartbeatSender implements HeartbeatSender {
    /**
       该地址为sentinel-dashboard 暴露出来的用于机器注册的endpoint
     */
    private static final String HEARTBEAT_PATH = "/registry/machine";
    private static final int OK_STATUS = 200;
    //默认间隔时间为10秒
    private static final long DEFAULT_INTERVAL = 1000 * 10;
    /**
     * 进行sendHeartbeat时传递的数消息对象封装
     */
    private final HeartbeatMessage heartBeat = new HeartbeatMessage();
    private final SimpleHttpClient httpClient = new SimpleHttpClient();

    private final List<InetSocketAddress> addressList;

    private int currentAddressIdx = 0;

    //根据配置解析 配置的 sentinel-dashboard访问地址，主要用于上报
    public SimpleHttpHeartbeatSender() {
        // Retrieve the list of default addresses.
        List<InetSocketAddress> newAddrs = getDefaultConsoleIps();
        RecordLog.info("[SimpleHttpHeartbeatSender] Default console address list retrieved: " + newAddrs);
        this.addressList = newAddrs;
    }

    /**
     * 心跳检测具体执行逻辑
     * @return
     * @throws Exception
     */
    @Override
    public boolean sendHeartbeat() throws Exception {
        // 校验一下传输端口，这个端口就是在SimpleHttpCommandCenter中通过 new ServerSocket(port,backlog)
        // 探测创建的服务器端socket (引入sentinel-core的业务应用)监听的port
        if (TransportConfig.getRuntimePort() <= 0) {
            RecordLog.info("[SimpleHttpHeartbeatSender] Runtime port not initialized, won't send heartbeat");
            return false;
        }
        //获取dashboard 访问地址
        InetSocketAddress addr = getAvailableAddress();
        if (addr == null) {
            return false;
        }
        /**
         *  HEARTBEAT_PATH="/registry/machine"  ，这个endpoint是sentinel-dashboard 对外暴露出来的，
         *  主要用于收集请求者的机器信息，包括：
         *      1. sentinel的version
         *      2. actually timestamp
         *      3. 端口号（在解释一遍，这个端口号是 SimpleHttpCommandCenter中通过socket监听的形式对外暴露出来的一个端口号）
         *         暴露出来的端口主要为了接收外部请求(sentinel-dashboard)请求的 httpCommand
         *
         */
        SimpleHttpRequest request = new SimpleHttpRequest(addr, HEARTBEAT_PATH);
        request.setParams(heartBeat.generateCurrentMessage());
        SimpleHttpResponse response = null;
        try {
            // 目前获取到的response 的statusCode是200还是其他，并没有实际用到，此处是否可以增加一个监控告警。
            // 及response结果的输出
            //通过httpclient发起http post请求，将自己的机器信息注册到sentinel-dashboard上
            response = httpClient.post(request);
            if (response.getStatusCode() == OK_STATUS) {
                return true;
            }
        } catch (Exception e) {
            RecordLog.warn("[SimpleHttpHeartbeatSender] Failed to send heartbeat to " + addr + " : ", e);
        }
        //这一行是我自己加的一段输出日志
        RecordLog.warn("[SimpleHttpHeartbeatSender] response status is not ok."+ (response!=null ? response.getStatusCode():""));
        return false;
    }

    @Override
    public long intervalMs() {
        return DEFAULT_INTERVAL;
    }

    private InetSocketAddress getAvailableAddress() {
        if (addressList == null || addressList.isEmpty()) {
            return null;
        }
        if (currentAddressIdx < 0) {
            currentAddressIdx = 0;
        }
        int index = currentAddressIdx % addressList.size();
        return addressList.get(index);
    }

    //获取sentinel-dashboard的地址
    private List<InetSocketAddress> getDefaultConsoleIps() {
        List<InetSocketAddress> newAddrs = new ArrayList<InetSocketAddress>();
        try {
            //获取sentinel-dashboard 连接地址，
            // 使用springCloud Alibaba 时，该地址配置在application.yml上
            // spring.cloud.sentinel.transport.dashboard的值
            String ipsStr = TransportConfig.getConsoleServer();
            if (StringUtil.isEmpty(ipsStr)) {
                RecordLog.warn("[SimpleHttpHeartbeatSender] Dashboard server address not configured");
                return newAddrs;
            }

            for (String ipPortStr : ipsStr.split(",")) {
                if (ipPortStr.trim().length() == 0) {
                    continue;
                }
                if (ipPortStr.startsWith("http://")) {
                    ipPortStr = ipPortStr.trim().substring(7);
                }
                String[] ipPort = ipPortStr.trim().split(":");
                int port = 80;
                if (ipPort.length > 1) {
                    port = Integer.parseInt(ipPort[1].trim());
                }
                newAddrs.add(new InetSocketAddress(ipPort[0].trim(), port));
            }
        } catch (Exception ex) {
            RecordLog.warn("[SimpleHeartbeatSender] Parse dashboard list failed, current address list: " + newAddrs, ex);
            ex.printStackTrace();
        }
        //
        return newAddrs;
    }
}
