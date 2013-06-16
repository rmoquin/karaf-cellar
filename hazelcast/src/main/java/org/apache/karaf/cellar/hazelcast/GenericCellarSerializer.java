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
package org.apache.karaf.cellar.hazelcast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.nio.serialization.TypeSerializer;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author rmoquin
 */
public class GenericCellarSerializer implements TypeSerializer {
    private final ObjectMapper mapper = new ObjectMapper();

    public GenericCellarSerializer() {
        mapper.registerModule(new AfterburnerModule());
    }

    @Override
    public int getTypeId() {
        return 5;
    }

    @Override
    public void write(ObjectDataOutput out, Object object)
            throws IOException {
        mapper.writeValue((OutputStream) out, object);
    }

    @Override
    public Object read(ObjectDataInput in) throws IOException {
        return mapper.readValue((InputStream) in,
                Object.class);
    }

    @Override
    public void destroy() {
    }
}
