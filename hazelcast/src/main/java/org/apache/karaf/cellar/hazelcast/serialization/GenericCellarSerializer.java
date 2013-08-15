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
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author rmoquin
 */
public class GenericCellarSerializer<T> implements StreamSerializer<T> {
    protected static final SmileFactory f = new SmileFactory();
    protected static final ObjectMapper mapper = new ObjectMapper(f);
    protected Class<T> clazz;
    private final int typeId;

    public GenericCellarSerializer(int typeId, Class<T> clazz) {
        this.clazz = clazz;
//        mapper.registerModule(new AfterburnerModule().setUseValueClassLoader(false));
        mapper.registerModule(new CellarTypesModule());
        this.typeId = typeId;
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    @Override
    public void write(ObjectDataOutput out, T object) throws IOException {
        final OutputStream outputStream = (OutputStream) out;
        mapper.writeValue(outputStream, object);
    }

    @Override
    public T read(ObjectDataInput in) throws IOException {
        final InputStream inputStream = (InputStream) in;
        return mapper.readValue(inputStream, clazz);
    }

    @Override
    public void destroy() {
    }
}
