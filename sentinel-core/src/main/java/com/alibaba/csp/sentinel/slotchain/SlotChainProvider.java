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
package com.alibaba.csp.sentinel.slotchain;

import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.slots.DefaultSlotChainBuilder;
import java.util.ServiceLoader;

/**
 * slot：槽
 *
 *  xxxProvider 一般为通过一种spi机制加载读取一系列某接口
 *  此类的spi是基于Java原生的ServiceLoader.load(xxx.class)来实现的。
 *  查找 classpath下的 META-INF/services/com.alibaba.csp.sentinel.slotchain.SlotChainBuilder 文件内的配置数据
 *
 * A provider for creating slot chains via resolved slot chain builder SPI.
 *
 * @author Eric Zhao
 * @since 0.2.0
 */
public final class SlotChainProvider {

    private static volatile SlotChainBuilder slotChainBuilder = null;

    private static final ServiceLoader<SlotChainBuilder> LOADER = ServiceLoader.load(SlotChainBuilder.class);

    /**
     * The load and pick process is not thread-safe, but it's okay since the method should be only invoked
     * via {@code lookProcessChain} in {@link com.alibaba.csp.sentinel.CtSph} under lock.
     *
     * @return new created slot chain
     */
    public static ProcessorSlotChain newSlotChain() {
        if (slotChainBuilder != null) {
            return slotChainBuilder.build();
        }

        resolveSlotChainBuilder();

        if (slotChainBuilder == null) {
            RecordLog.warn("[SlotChainProvider] Wrong state when resolving slot chain builder, using default");
            slotChainBuilder = new DefaultSlotChainBuilder();
        }
        return slotChainBuilder.build();
    }

    private static void resolveSlotChainBuilder() {
        for (SlotChainBuilder builder : LOADER) {
            //以springcloudalibaba 0.0.9来看，配置com.alibaba.csp.sentinel.slotchain.SlotChainBuilder的有两处。
            // sentinel-parameter-flow-control中配置了com.alibaba.csp.sentinel.slots.HotParamSlotChainBuilder
            //以及 sentinel-core中的com.alibaba.csp.sentinel.slots.DefaultSlotChainBuilder
            // 也可以根据自己的实际需求增加或调整校验链中slot的顺序
            // 优先使用非默认的。
            if (builder.getClass() != DefaultSlotChainBuilder.class) {
                slotChainBuilder = builder;
                break;
            }
        }
        if (slotChainBuilder == null){
            // No custom builder, using default.
            slotChainBuilder = new DefaultSlotChainBuilder();
        }

        RecordLog.info("[SlotChainProvider] Global slot chain builder resolved: "
            + slotChainBuilder.getClass().getCanonicalName());
    }

    private SlotChainProvider() {}
}
