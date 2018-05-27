/**
 *  Copyright [2018] [Juraj Borza]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.jborza.camel.component.smbj

import org.apache.camel.builder.RouteBuilder
import org.apache.camel.main.Main
import org.apache.commons.io.FileUtils
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.nio.file.Paths

class FileToSmbSpecIT extends Specification {
    static final HOST = "localhost"
    static final PORT = "4445"
    static final USER = "user"
    static final PASS = "pass"
    static final SHARE = "share"

    def getSmbUri() {
        return "smb2://${HOST}:${PORT}/${SHARE}?username=${USER}&password=${PASS}"
    }

    def getTempDir() {
        if (isWindows())
            return "c:\\temp\\camel-smbj"
        else
            return "/tmp/camel-smbj"
    }

    def isWindows() {
        return (System.properties['os.name'].toLowerCase().contains('windows'))
    }

    def setup() {
        //clear samba target directory
        File directory = new File(getTempDir())
        FileUtils.cleanDirectory(directory)
        //prepare file to copy
        File subDir = new File("to-smb");
        if (!subDir.exists())
            subDir.mkdir();
        File target = new File(Paths.get("to-smb", "file-to-smb.txt").toString());
        FileUtils.writeStringToFile(target, "Hello, camel-smbj!", StandardCharsets.UTF_8);
    }

    def "one file from file to smb"() {
        when:
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("file://to-smb")
                        .to("smb2://localhost:4445/share/output/?username=user&password=pass")
            }
        })
        camelContext.start();
        Thread.sleep(10000);
        camelContext.stop();

        then:
        File target = new File(Paths.get(getTempDir(), "output", "file-to-smb.txt").toString());
        target.exists() == true
        String content = FileUtils.readFileToString(target, StandardCharsets.UTF_8)
        content == "Hello, camel-smbj!"
    }
}