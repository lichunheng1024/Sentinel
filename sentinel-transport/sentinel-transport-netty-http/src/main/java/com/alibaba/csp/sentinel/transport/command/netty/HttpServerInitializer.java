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
package com.alibaba.csp.sentinel.transport.command.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

/**
 * @author Eric Zhao
 */
public class HttpServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline p = socketChannel.pipeline();

        p.addLast(new HttpRequestDecoder());
        /**
         *  当我们用POST方式请求服务器的时候，对应的参数信息是保存在message body中的,
         *  如果只是单纯的用HttpServerCodec是无法完全的解析Http POST请求的，
         *  因为HttpServerCodec只能获取uri中参数，所以需要加上HttpObjectAggregator.
         */
        p.addLast(new HttpObjectAggregator(1024 * 1024));
        p.addLast(new HttpResponseEncoder());

        p.addLast(new HttpServerHandler());
    }
}
