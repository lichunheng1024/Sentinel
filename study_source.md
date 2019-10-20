#### 可优化的点

> 1. 流控规则中不支持一次性配置多个"针对来源"。--自己简单写了个支持，后续再考虑优化

> 2. com.alibaba.csp.sentinel.transport.heartbeat.SimpleHttpHeartbeatSender.sendHeartbeat()方法在拿到response
     后如果statusCode不是200，则应该输出具体的错误信息，以便排查问题。
     
```java_holder_method_tree

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
            response = httpClient.post(request);
            if (response.getStatusCode() == OK_STATUS) {
                return true;
            }
        } catch (Exception e) {
            RecordLog.warn("[SimpleHttpHeartbeatSender] Failed to send heartbeat to " + addr + " : ", e);
        }
        RecordLog.warn("[SimpleHttpHeartbeatSender] response status is not ok."+ (response!=null ? response.getStatusCode():""));
        return false;
    }
```