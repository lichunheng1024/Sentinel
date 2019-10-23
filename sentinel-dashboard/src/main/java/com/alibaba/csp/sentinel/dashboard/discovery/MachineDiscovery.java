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
package com.alibaba.csp.sentinel.dashboard.discovery;

import java.util.List;
import java.util.Set;

/**
 * 机器发现接口类
 *  一个简易的服务注册中心
 */
public interface MachineDiscovery {

    String UNKNOWN_APP_NAME = "CLUSTER_NOT_STARTED";

    /**
     * 获取已注册的app名称
     *
     * @return
     */
    List<String> getAppNames();

    /**
     * 获取注册到sentinel-dashboard上的简洁的 客户端appInfo 列表
     */
    Set<AppInfo> getBriefApps();

    /**
     * 根据客户端应用名称(需要被保护的业务应用名称)，获取appInfo
     * @param app
     * @return
     */
    AppInfo getDetailApp(String app);

    /**
     * 根据应用名称，删除所有注册信息
     * Remove the given app from the application registry.
     *
     * @param app application name
     * @since 1.5.0
     */
    void removeApp(String app);

    /**
       新增一条机器注册信息
     */
    long addMachine(MachineInfo machineInfo);

    /**
     * 根据应用名称、客户端机器ip,客户端机器port 删除注册信息
     *
     * Remove the given machine instance from the application registry.
     *
     * @param app the application name of the machine
     * @param ip machine IP
     * @param port machine port
     * @return true if removed, otherwise false
     * @since 1.5.0
     */
    boolean removeMachine(String app, String ip, int port);
}