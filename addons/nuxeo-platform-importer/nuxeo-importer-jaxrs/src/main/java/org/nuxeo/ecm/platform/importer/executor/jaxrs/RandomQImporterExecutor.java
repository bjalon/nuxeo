/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     bdelbosc
 */
package org.nuxeo.ecm.platform.importer.executor.jaxrs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.importer.base.ImporterRunner;
import org.nuxeo.ecm.platform.importer.queue.QueueImporter;
import org.nuxeo.ecm.platform.importer.queue.consumer.ConsumerFactory;
import org.nuxeo.ecm.platform.importer.queue.consumer.ImmutableNodeConsumerFactory;
import org.nuxeo.ecm.platform.importer.queue.manager.BQManager;
import org.nuxeo.ecm.platform.importer.queue.manager.CQManager;
import org.nuxeo.ecm.platform.importer.queue.manager.KQManager;
import org.nuxeo.ecm.platform.importer.queue.manager.QueuesManager;
import org.nuxeo.ecm.platform.importer.queue.producer.Producer;
import org.nuxeo.ecm.platform.importer.queue.producer.RandomNodeProducer;
import org.nuxeo.ecm.platform.importer.service.DefaultImporterService;
import org.nuxeo.ecm.platform.importer.source.ImmutableNode;
import org.nuxeo.runtime.api.Framework;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Path("randomQImporter")
public class RandomQImporterExecutor extends AbstractJaxRSImporterExecutor {

    private static final Log log = LogFactory.getLog(RandomQImporterExecutor.class);

    @Override
    protected Log getJavaLogger() {
        return log;
    }

    @GET
    @Path("run")
    @Produces("text/plain; charset=UTF-8")
    public String run(@QueryParam("targetPath") String targetPath,
                      @QueryParam("batchSize") Integer batchSize, @QueryParam("nbThreads") Integer nbThreads,
                      @QueryParam("nbNodes") Integer nbNodes,
                      @QueryParam("fileSizeKB") Integer fileSizeKB, @QueryParam("onlyText") Boolean onlyText,
                      @QueryParam("transactionTimeout") Integer transactionTimeout,
                      @QueryParam("queueType") String queueType,
                      @QueryParam("lang") String lang,
                      @QueryParam("countFolderAsDocument") Boolean countFolderAsDocument) {
        if (onlyText == null) {
            onlyText = true;
        }
        if (queueType == null) {
            queueType = "CQ";
        }
        if (countFolderAsDocument == null) {
            countFolderAsDocument = true;
        }
        log.info("Init Random Queue importer Executor");
        QueueImporter<ImmutableNode> importer = new QueueImporter<>(getLogger());
        QueuesManager<ImmutableNode> qm;
        switch (queueType) {
            case "KQ":
                log.info("Using Off heap KafkaQueue");
                qm = new KQManager<>(getLogger(), nbThreads);
                break;
            case "BQ":
                log.info("Using in memory BlockingQueue");
                qm = new BQManager<>(getLogger(), nbThreads, 10000);
                break;
            case "CQ":
                log.info("Using Off heap ChronicleQueue");
                // there are assert in chronicle queue code that should be disabled for performance and memory reasons
                ClassLoader loader = ClassLoader.getSystemClassLoader();
                loader.setDefaultAssertionStatus(false);
                qm = new CQManager<>(getLogger(), nbThreads);
                break;
            default:
                String msg = "Invalid queue type: " + queueType;
                log.error(msg);
                return "KO " + msg;

        }
        Producer<ImmutableNode> producer = new RandomNodeProducer(getLogger(), nbNodes, nbThreads)
                .withBlob(fileSizeKB, onlyText)
                .setLang(lang)
                .countFolderAsDocument(countFolderAsDocument);
        ConsumerFactory<ImmutableNode> consumerFactory = new ImmutableNodeConsumerFactory();
        if (transactionTimeout != null) {
            Framework.getService(DefaultImporterService.class).setTransactionTimeout(transactionTimeout);
        }
        long startTime = System.currentTimeMillis();
        log.warn(String.format("Running import of %d documents (%d KB) into %s with %d consumers, commit batch size: %d",
                nbNodes, fileSizeKB, targetPath, nbThreads, batchSize));
        importer.importDocuments(producer, qm, targetPath, "default", batchSize, consumerFactory);
        long elapsed = System.currentTimeMillis() - startTime;
        long created = importer.getCreatedDocsCounter();
        String msg = "Import terminated, created:" + created + " throughput: " + created / (elapsed / 1000) + " docs/s";
        log.warn(msg);
        return "OK " + msg;
    }

    @Override
    public String run(ImporterRunner runner, Boolean interactive) {
        return doRun(runner, interactive);
    }

}