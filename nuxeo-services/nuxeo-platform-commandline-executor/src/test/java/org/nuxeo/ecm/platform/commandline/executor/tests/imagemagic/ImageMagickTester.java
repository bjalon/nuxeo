/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.platform.commandline.executor.tests.imagemagic;

import java.io.File;
import java.util.List;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * Test identify imagemagick commandline.
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
@LocalDeploy("org.nuxeo.ecm.platform.commandline.executor:OSGI-INF/commandline-imagemagic-test-contrib.xml")
public class ImageMagickTester {

    @Test
    public void testIdentifyExec() throws Exception {
        CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
        assertNotNull(cles);

        CommandAvailability ca = cles.getCommandAvailability("identify");
        Assume.assumeTrue("ImageMagick is not available, skipping test", ca.isAvailable());

        File img = FileUtils.getResourceFileFromContext("test.png");

        CmdParameters params = cles.getDefaultCmdParameters();
        params.addNamedParameter("filePath", img);

        ExecResult result = cles.execCommand("identify", params);

        assertTrue(result.isSuccessful());
        assertSame(0, result.getReturnCode());

        List<String> lines = result.getOutput();

        assertEquals("PNG 48 48", lines.get(0));
    }

}