/*
 * Copyright 2013 The Apache Software Foundation.
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
package org.apache.karaf.cellar.hazelcast.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.hazelcast.nio.serialization.ByteArraySerializer;
import java.io.IOException;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.hazelcast.HazelcastNode;
import org.slf4j.Logger;

/**
 *
 * @author rmoquin
 */
public class GenericCellarSerializer<T> implements ByteArraySerializer<T> {

    private static final transient Logger LOGGER = org.slf4j.LoggerFactory.getLogger(GenericCellarSerializer.class);
    protected static final ObjectMapper mapper = new ObjectMapper();
    protected Class<T> clazz;
    private final int typeId;

    static {
        SimpleModule module = new SimpleModule();
        module.addAbstractTypeMapping(Node.class, HazelcastNode.class);
        mapper.registerModule(new AfterburnerModule().setUseValueClassLoader(false));
        mapper.registerModule(module);
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_CONCRETE_AND_ARRAYS);
    }

    public GenericCellarSerializer(int typeId, Class<T> clazz) {
        this.clazz = clazz;
        LOGGER.warn("Created JSON serializer for " + clazz);
        this.typeId = typeId;
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    @Override
    public byte[] write(T object) throws IOException {
        LOGGER.warn("Writing value object: " + object);
        return mapper.writeValueAsBytes(object);
    }

    @Override
    public T read(byte[] in) throws IOException {
        T val = mapper.readValue(in, clazz);
        LOGGER.warn("Read value object: " + val);
        return val;
    }

    @Override
    public void destroy() {
    }
}
