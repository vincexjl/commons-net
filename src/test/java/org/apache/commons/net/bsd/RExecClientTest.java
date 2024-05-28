/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.net.bsd;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link RExecClient}.
 */
public class RExecClientTest {

    private RExecClient newClient() {
        return new RExecClient();
    }

    @Test
    public void testConstructor() {
        assertDoesNotThrow(RExecClient::new);
    }

    @Test
    public void testDefaultPort() {
        assertEquals(RExecClient.DEFAULT_PORT, newClient().getDefaultPort());
    }

    @Test
    public void testGetErrorStream() throws IOException {
        try (InputStream inputStream = newClient().getErrorStream()) {
            assertNull(inputStream);
        }
    }

    @Test
    public void testGetInputStream() throws IOException {
        try (InputStream inputStream = newClient().getInputStream()) {
            assertNull(inputStream);
        }
    }

    @Test
    public void testGetOutputStream() throws IOException {
        try (OutputStream outputStream = newClient().getOutputStream()) {
            assertNull(outputStream);
        }
    }

}
