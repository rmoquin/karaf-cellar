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
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.hazelcast.nio.serialization.ByteArraySerializer;
import java.io.IOException;

/**
 *
 * @author rmoquin
 */
public class CellarGenericSerializer implements ByteArraySerializer<Object> {
    private final ObjectMapper mapper = new ObjectMapper(new SmileFactory());
        
    protected int typeId;

    public CellarGenericSerializer() {
        mapper.enableDefaultTyping();
    }

    @Override
    public int getTypeId() {
        return 100;
    }
    
    @Override
    public void destroy() {
    }

    @Override
    public byte[] write(Object object) throws IOException {
        return mapper.writeValueAsBytes(object);
    }

    @Override
    public Object read(byte[] buffer) throws IOException {
        return mapper.readValue(buffer, Object.class);
    }
}
