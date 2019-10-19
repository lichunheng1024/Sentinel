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
package com.alibaba.csp.sentinel.transport.init;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy;
import java.util.concurrent.TimeUnit;

import com.alibaba.csp.sentinel.concurrent.NamedThreadFactory;
import com.alibaba.csp.sentinel.config.SentinelConfig;
import com.alibaba.csp.sentinel.heartbeat.HeartbeatSenderProvider;
import com.alibaba.csp.sentinel.init.InitFunc;
import com.alibaba.csp.sentinel.init.InitOrder;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.transport.HeartbeatSender;
import com.alibaba.csp.sentinel.transport.config.TransportConfig;

/**
 * Global init function for heartbeat sender.
 *
 * @author Eric Zhao
 */
@InitOrder(-1)
public class HeartbeatSenderInitFunc implements InitFunc {

    private ScheduledExecutorService pool = null;

    /**
     *  初始化创建一个调度线程池
     *      核心线程数为2，
     *      守护线程
     *      拒绝最先待处理的请求
     *      DelayedWorkQueue-（BlockingQueue的一种）
     */
    private void initSchedulerIfNeeded() {
        if (pool == null) {
            pool = new ScheduledThreadPoolExecutor(2,
                    new NamedThreadFactory("sentinel-heartbeat-send-task", true),
                    new DiscardOldestPolicy());
        }
    }

    @Override
    public void init() {
        //基于spi机制查询 com.alibaba.csp.sentinel.transport.HeartbeatSender
        HeartbeatSender sender = HeartbeatSenderProvider.getHeartbeatSender();
        if (sender == null) {
            RecordLog.warn("[HeartbeatSenderInitFunc] WARN: No HeartbeatSender loaded");
            return;
        }
        //初始化创建一个调度线程池对象
        initSchedulerIfNeeded();
        //获取执行调度任务时的间隔时间
        long interval = retrieveInterval(sender);
        //将获取的interval值设置到 csp.sentinel.heartbeat.interval.ms
        setIntervalIfNotExists(interval);
        //执行调度任务
        scheduleHeartbeatTask(sender, interval);
    }

    private boolean isValidHeartbeatInterval(Long interval) {
        return interval != null && interval > 0;
    }

    private void setIntervalIfNotExists(long interval) {
        SentinelConfig.setConfig(TransportConfig.HEARTBEAT_INTERVAL_MS, String.valueOf(interval));
    }
    /**
     * 获取间隔时间
     * 默认间隔时间是10秒，参考SimpleHttpHeartbeatSender.DEFAULT_INTERVAL
     */
    long retrieveInterval(/*@NonNull*/ HeartbeatSender sender) {
        Long intervalInConfig = TransportConfig.getHeartbeatIntervalMs();
        //验证是否合法
        if (isValidHeartbeatInterval(intervalInConfig)) {
            RecordLog.info("[HeartbeatSenderInitFunc] Using heartbeat interval "
                    + "in Sentinel config property: " + intervalInConfig);
            return intervalInConfig;
        } else {
            // 此处获取的是sentinel-transport-simple-http中的参考SimpleHttpHeartbeatSender 10毫秒
            // 特别说明，通过源码可知，在netty中是5秒
            long senderInterval = sender.intervalMs();
            RecordLog.info("[HeartbeatSenderInit] Heartbeat interval not configured in "
                    + "config property or invalid, using sender default: " + senderInterval);
            return senderInterval;
        }
    }

    /**
     * 创建并执行在给定的初始延迟之后，随后以给定的时间段首先启用的周期性动作;
     * 那就是执行将在initialDelay之后开始，然后是initialDelay+period ，然后是initialDelay + 2 * period ，等等。
     *
     * @param sender
     * @param interval
     */
    private void scheduleHeartbeatTask(/*@NonNull*/ final HeartbeatSender sender, /*@Valid*/ long interval) {
        pool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    sender.sendHeartbeat();
                } catch (Throwable e) {
                    RecordLog.warn("[HeartbeatSender] Send heartbeat error", e);
                }
            }
        }, 5000, interval, TimeUnit.MILLISECONDS);
        RecordLog.info("[HeartbeatSenderInit] HeartbeatSender started: "
                + sender.getClass().getCanonicalName());
    }
}
