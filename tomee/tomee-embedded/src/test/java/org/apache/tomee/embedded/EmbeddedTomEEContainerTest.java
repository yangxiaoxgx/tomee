/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.tomee.embedded;

import org.apache.commons.io.FileUtils;
import org.apache.openejb.config.DeploymentsResolver;
import org.apache.openejb.loader.IO;
import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.spi.ContainerSystem;
import org.junit.Test;

import javax.ejb.embeddable.EJBContainer;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EmbeddedTomEEContainerTest {
    @Test
    public void containerTest() throws Exception {
        File war = createWar();
        Properties p = new Properties();
        p.setProperty(EJBContainer.APP_NAME, "test");
        p.setProperty(EJBContainer.PROVIDER, EmbeddedTomEEContainer.class.getName());
        p.put(EJBContainer.MODULES, war.getAbsolutePath());
        p.setProperty(EmbeddedTomEEContainer.TOMEE_EJBCONTAINER_HTTP_PORT, "-1");
        try {
            EJBContainer container = EJBContainer.createEJBContainer(p);
            assertNotNull(container);
            assertNotNull(container.getContext());
            URL url = new URL("http://127.0.0.1:" + System.getProperty(EmbeddedTomEEContainer.TOMEE_EJBCONTAINER_HTTP_PORT) + "/test/index.html");
            assertEquals("true", IO.readProperties(url).getProperty("ok"));
            container.close();
        } finally {
            try {
                FileUtils.forceDelete(war);
            } catch (IOException e) {
                FileUtils.deleteQuietly(war);
            }
        }
    }

    @Test
    public void classpath() throws Exception {
        final Properties p = new Properties();
        p.setProperty(EJBContainer.PROVIDER, EmbeddedTomEEContainer.class.getName());
        p.setProperty(DeploymentsResolver.CLASSPATH_INCLUDE, ".*tomee-embedded.*");
        p.setProperty(EmbeddedTomEEContainer.TOMEE_EJBCONTAINER_HTTP_PORT, "-1");
        final EJBContainer container = EJBContainer.createEJBContainer(p);
        assertNotNull(container);
        final ABean bean = ABean.class.cast(container.getContext().lookup("java:global/tomee-embedded/ABean"));
        assertNotNull(bean);
        assertEquals("ok", bean.embedded());
        container.close();
    }

    private File createWar() throws IOException {
        File file = new File(System.getProperty("java.io.tmpdir") + "/tomee-" + Math.random());
        if (!file.mkdirs() && !file.exists()) {
            throw new RuntimeException("can't create " + file.getAbsolutePath());
        }

        write("ok=true", new File(file, "index.html"));
        write("<beans />", new File(file, "WEB-INF/classes/META-INF/beans.xml"));
        return file;
    }

    private static void write(String content, File file) throws IOException {
        if (!file.getParentFile().mkdirs() && !file.getParentFile().exists()) {
            throw new RuntimeException("can't create " + file.getParent());
        }

        FileWriter index = new FileWriter(file);
        index.write(content);
        index.close();
    }
}
