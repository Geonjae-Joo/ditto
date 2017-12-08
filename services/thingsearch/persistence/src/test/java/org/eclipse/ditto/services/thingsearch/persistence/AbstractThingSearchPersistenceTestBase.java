/*
 * Copyright (c) 2017 Bosch Software Innovations GmbH.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/index.php
 *
 * Contributors:
 *    Bosch Software Innovations GmbH - initial contribution
 */
package org.eclipse.ditto.services.thingsearch.persistence;

import static org.eclipse.ditto.services.thingsearch.persistence.PersistenceConstants.POLICIES_BASED_SEARCH_INDEX_COLLECTION_NAME;
import static org.eclipse.ditto.services.thingsearch.persistence.PersistenceConstants.THINGS_COLLECTION_NAME;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.bson.Document;
import org.eclipse.ditto.model.things.Thing;
import org.eclipse.ditto.services.thingsearch.common.model.ResultList;
import org.eclipse.ditto.services.thingsearch.persistence.read.MongoThingsSearchPersistence;
import org.eclipse.ditto.services.thingsearch.persistence.read.query.MongoAggregationBuilderFactory;
import org.eclipse.ditto.services.thingsearch.persistence.read.query.MongoQueryBuilderFactory;
import org.eclipse.ditto.services.thingsearch.persistence.write.impl.MongoEventToPersistenceStrategyFactory;
import org.eclipse.ditto.services.thingsearch.persistence.write.impl.MongoThingsSearchUpdaterPersistence;
import org.eclipse.ditto.services.thingsearch.querymodel.criteria.CriteriaFactory;
import org.eclipse.ditto.services.thingsearch.querymodel.criteria.CriteriaFactoryImpl;
import org.eclipse.ditto.services.thingsearch.querymodel.expression.ThingsFieldExpressionFactory;
import org.eclipse.ditto.services.thingsearch.querymodel.expression.ThingsFieldExpressionFactoryImpl;
import org.eclipse.ditto.services.thingsearch.querymodel.query.AggregationBuilderFactory;
import org.eclipse.ditto.services.thingsearch.querymodel.query.PolicyRestrictedSearchAggregation;
import org.eclipse.ditto.services.thingsearch.querymodel.query.Query;
import org.eclipse.ditto.services.thingsearch.querymodel.query.QueryBuilderFactory;
import org.eclipse.ditto.services.utils.test.mongo.MongoDbResource;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.reactivestreams.Publisher;

import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.Success;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.event.LoggingAdapter;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.testkit.javadsl.TestKit;


/**
 * Abstract base class for search persistence tests.
 */
public abstract class AbstractThingSearchPersistenceTestBase {

    protected static final List<String> KNOWN_SUBJECTS = Collections.singletonList("abc:mySid");
    protected static final List<String> KNOWN_SUBJECTS_2 = Arrays.asList("iot-things:mySid", "iot-things:unknown");
    protected static final String KNOWN_ATTRIBUTE_1 = "attribute1";
    protected static final String KNOWN_NEW_VALUE = "newValue";

    private static final Config CONFIG = ConfigFactory.load("test");

    protected static final CriteriaFactory cf = new CriteriaFactoryImpl();
    protected static final ThingsFieldExpressionFactory fef = new ThingsFieldExpressionFactoryImpl();
    protected static final QueryBuilderFactory qbf = new MongoQueryBuilderFactory();
    protected static final AggregationBuilderFactory abf = new MongoAggregationBuilderFactory();
    private static MongoDbResource mongoResource;
    private static MongoClientWrapper mongoClient;
    /** */
    private MongoThingsSearchPersistence readPersistence;
    private MongoCollection<Document> thingsCollection;
    private MongoCollection<Document> policiesCollection;
    protected MongoThingsSearchUpdaterPersistence writePersistence;

    private ActorSystem actorSystem;
    private ActorMaterializer actorMaterializer;
    protected LoggingAdapter log;

    @BeforeClass
    public static void startMongoResource() {
        mongoResource = new MongoDbResource("localhost");
        mongoResource.start();
        mongoClient = provideClientWrapper();
    }

    /** */
    @Before
    public void before() {
        final Config config = ConfigFactory.load("test");
        actorSystem = ActorSystem.create("AkkaTestSystem", config);
        log = actorSystem.log();
        actorMaterializer = ActorMaterializer.create(actorSystem);
        readPersistence = provideReadPersistence();
        writePersistence = provideWritePersistence();
        thingsCollection = mongoClient.getDatabase().getCollection(THINGS_COLLECTION_NAME);
        policiesCollection = mongoClient.getDatabase().getCollection
                (POLICIES_BASED_SEARCH_INDEX_COLLECTION_NAME);

        log.info("before() method of test is done..");
    }

    private MongoThingsSearchPersistence provideReadPersistence() {
        final MongoThingsSearchPersistence mongoThingsSearchPersistence =
                new MongoThingsSearchPersistence(provideClientWrapper(), actorSystem);
        mongoThingsSearchPersistence.initIndexes();
        return mongoThingsSearchPersistence;
    }

    private MongoThingsSearchUpdaterPersistence provideWritePersistence() {
        return new MongoThingsSearchUpdaterPersistence(mongoClient, log,
                MongoEventToPersistenceStrategyFactory.getInstance());
    }

    private static MongoClientWrapper provideClientWrapper() {
        return new MongoClientWrapper(mongoResource.getBindIp(), mongoResource.getPort(), "testSearchDB", CONFIG);
    }

    /** */
    @After
    public void after() throws InterruptedException {
        if (mongoClient != null) {
            if (thingsCollection != null)
                retryWithBackoff(() -> thingsCollection.drop());
            retryWithBackoff(() -> policiesCollection.drop());
        }
        if (actorSystem != null) {
            TestKit.shutdownActorSystem(actorSystem);
            actorSystem = null;
            log = null;
            actorMaterializer = null;
        }
    }

    @AfterClass
    public static void stopMongoResource() {
        try {
            if (mongoClient != null) {
                mongoClient.getMongoClient().close();
            }
            if (mongoResource != null) {
                mongoResource.stop();
            }
        } catch (final IllegalStateException e) {
            System.err.println("IllegalStateException during shutdown of MongoDB: " + e.getMessage());
        }
    }

    protected Long count(final Query query) {
        try {
            return readPersistence.count(query) //
                    .limit(1) //
                    .runWith(Sink.seq(), actorMaterializer) //
                    .toCompletableFuture() //
                    .get() //
                    .get(0);
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    protected long aggregateCount(final PolicyRestrictedSearchAggregation policyRestrictedSearchAggregation) {
        try {
            return readPersistence.count(policyRestrictedSearchAggregation) //
                    .runWith(Sink.seq(), actorMaterializer) //
                    .toCompletableFuture() //
                    .get() //
                    .get(0);
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    protected ResultList<String> findAll(final PolicyRestrictedSearchAggregation policyRestrictedSearchAggregation) {
        return waitFor(readPersistence.findAll(policyRestrictedSearchAggregation)).get(0);
    }

    protected ResultList<String> findAll(final Query query) {
        try {
            return readPersistence.findAll(query) //
                    .limit(1) //
                    .runWith(Sink.seq(), actorMaterializer) //
                    .toCompletableFuture() //
                    .get() //
                    .get(0);
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void runBlocking(final Source<?, NotUsed> publisher) {
        final List<Source<?, NotUsed>> publishers = Collections.singletonList(publisher);

        runBlocking(publishers);
    }

    protected void runBlocking(final List<Source<?, NotUsed>> publishers) {
        publishers.stream()
                .map(p -> p.runWith(Sink.ignore(), actorMaterializer))
                .map(CompletionStage::toCompletableFuture)
                .forEach(this::finishCompletableFuture);
    }

    private void finishCompletableFuture(final CompletableFuture future) {
        try {
            future.get();
        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> T runBlockingWithReturn(final Source<T, NotUsed> publisher)
            throws ExecutionException, InterruptedException {
        final CompletionStage<T> done = publisher.runWith(Sink.last(), actorMaterializer);
        return done.toCompletableFuture().get();
    }

    protected void insertOrUpdateThing(final Thing thing, final long revision, final long policyRevision) {
        runBlocking(writePersistence.insertOrUpdate(thing, revision, policyRevision));
    }

    protected void delete(final String thingId, final long revision) {
        runBlocking(writePersistence.delete(thingId, revision));
    }

    protected MongoClientWrapper getClient() {
        return mongoClient;
    }

    private void retryWithBackoff(final Supplier<Publisher<Success>> publisher) {
        IllegalStateException lastException = null;
        for (int i = 0; i < 20; ++i) {
            try {
                waitFor(Source.fromPublisher(publisher.get()));
                return;
            } catch (final IllegalStateException e) {
                lastException = e;
                backoff();
            }
        }
        throw lastException;
    }

    private <T> List<T> waitFor(final Source<T, ?> source) {
        try {
            return source
                    .limit(1) //
                    .runWith(Sink.seq(), actorMaterializer) //
                    .toCompletableFuture() //
                    .get();

        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("squid:S2925")
    private void backoff() {
        try {
            Thread.sleep(100);
        } catch (final InterruptedException e) {
            // do nothing
        }
    }
}
