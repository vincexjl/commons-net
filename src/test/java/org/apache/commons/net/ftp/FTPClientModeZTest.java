/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.net.ftp;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Files.write;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.net.ftp.FTP.COMPRESSED_MODE_Z_TRANSFER_MODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class FTPClientModeZTest {

    private static final String DEFAULT_HOME = "ftp_root/";
    private final boolean useLocalPassiveFTP;

    @Parameters(name = "useLocalPassiveFTP={0}")
    public static Boolean[] testParameters() {
        return new Boolean[] { true, false };
    }

    public FTPClientModeZTest(boolean useLocalPassiveFTP) {
        this.useLocalPassiveFTP = useLocalPassiveFTP;
    }

    @Test
    public void testRetrievingFiles() throws Exception {

        new File(DEFAULT_HOME).mkdirs();
        String filename = "test_download.txt";
        String fileContent = "Created at " + Instant.now();
        write(Paths.get(DEFAULT_HOME).resolve(filename), fileContent.getBytes(UTF_8));

        runWithFTPserver((port, user, password) -> {
            FTPClient client = new FTPClient();
            try {
                client.connect("localhost", port);
                client.login(user, password);
                if (useLocalPassiveFTP) {
                    client.enterLocalPassiveMode();
                }
                assertTrue("Mode Z successfully activated",
                        client.setFileTransferMode(COMPRESSED_MODE_Z_TRANSFER_MODE));

                FTPFile[] files = client.listFiles();
                assertEquals("Only single file in home directory", 1, files.length);

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                assertTrue("File successfully transferred", client.retrieveFile(files[0].getName(), bos));
                assertEquals("File content is not corrupted", fileContent, new String(bos.toByteArray(), UTF_8));
            } finally {
                client.logout();
            }
        });
    }

    @Test
    public void testStoringFiles() throws Exception {

        runWithFTPserver((port, user, password) -> {
            FTPClient client = new FTPClient();
            try {
                client.connect("localhost", port);
                client.login(user, password);
                if (useLocalPassiveFTP) {
                    client.enterLocalPassiveMode();
                }
                assertTrue("Mode Z successfully activated",
                        client.setFileTransferMode(COMPRESSED_MODE_Z_TRANSFER_MODE));

                FTPFile[] filesBeforeUpload = client.listFiles();
                assertEquals("No files in home directory before upload", 0, filesBeforeUpload.length);

                String filename = "test_upload.txt";
                String fileContent = "Created at " + Instant.now();
                assertTrue("File successfully transferred",
                        client.storeFile(filename, new ByteArrayInputStream(fileContent.getBytes(UTF_8))));

                FTPFile[] filesAfterUpload = client.listFiles();
                assertEquals("Single file in home directory after upload", 1, filesAfterUpload.length);

                Path p = Paths.get(DEFAULT_HOME, filename);
                assertEquals("File content is not corrupted", fileContent, new String(readAllBytes(p), UTF_8));
            } finally {
                client.logout();
            }
        });
    }

    @Before
    @After
    public void cleanup() throws IOException {
        deleteDirectory(new File(DEFAULT_HOME));
    }

    private static final class FtpServerAndPort {
        private final int port;
        private final FtpServer ftpServer;

        FtpServerAndPort(FtpServer ftpServer, int port) {
            this.port = port;
            this.ftpServer = ftpServer;
        }
    }

    @FunctionalInterface
    interface Runner {
        void run(int port, String user, String password) throws Exception;
    }

    private static void runWithFTPserver(Runner runner) throws Exception {

        String username = "test";
        String password = "test";
        FtpServerAndPort ftpServerAndPort = setupPlainFTPserver(username, password);
        try {
            runner.run(ftpServerAndPort.port, username, password);
        } finally {
            ftpServerAndPort.ftpServer.stop();
        }
    }

    private static FtpServerAndPort setupPlainFTPserver(final String username, final String password)
            throws FtpException {

        final FtpServerFactory serverFactory = new FtpServerFactory();

        // Init user
        serverFactory.setUserManager(initUserManager(username, password));

        final ListenerFactory factory = new ListenerFactory();
        // Automatically assign port.
        factory.setPort(0);

        // replace the default listener
        Listener listener = factory.createListener();
        serverFactory.addListener("default", listener);

        // start the server
        FtpServer server = serverFactory.createServer();
        server.start();

        return new FtpServerAndPort(server, listener.getPort());
    }

    private static UserManager initUserManager(final String username, final String password) throws FtpException {

        final PropertiesUserManagerFactory propertiesUserManagerFactory = new PropertiesUserManagerFactory();
        final UserManager userManager = propertiesUserManagerFactory.createUserManager();
        BaseUser user = new BaseUser();
        user.setName(username);
        user.setPassword(password);

        List<Authority> authorities = new ArrayList<Authority>();
        authorities.add(new WritePermission());
        user.setAuthorities(authorities);

        new File(DEFAULT_HOME).mkdirs();
        user.setHomeDirectory(DEFAULT_HOME);
        userManager.save(user);
        return userManager;
    }

}
