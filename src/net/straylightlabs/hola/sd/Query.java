/*
 * The MIT License
 *
 * Copyright (c) 2015-2018 Todd Kulesza <todd@dropline.net>
 *
 * This file is part of Hola.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.straylightlabs.hola.sd;

import net.straylightlabs.hola.dns.*;
import net.straylightlabs.hola.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class Query {
    private final Service service;
    private final Domain domain;
    private final int browsingTimeout;
    private final Lock socketLock;

    private MulticastSocket socket;
    private InetAddress mdnsGroupIPv4;
    private InetAddress mdnsGroupIPv6;
    private boolean isUsingIPv4;
    private boolean isUsingIPv6;
    private Question initialQuestion;
    private Set<Question> questions;
    private Set<Instance> instances;
    private Set<Record> records;
    private boolean listenerStarted;
    private boolean listenerFinished;

    public static final String MDNS_IP4_ADDRESS = "224.0.0.251";
    public static final String MDNS_IP6_ADDRESS = "FF02::FB";
    public static final int MDNS_PORT = 5353;
    private static final int WAIT_FOR_LISTENER_MS = 10; // Number of milliseconds to wait for the listener to start
    static final InetAddress TEST_SUITE_ADDRESS = null;

    /**
     * The browsing socket will timeout after this many milliseconds
     */
    private static final int BROWSING_TIMEOUT = 750;

    /**
     * Create a Query for the given Service and Domain.
     *
     * @param service service to search for
     * @param domain  domain to search on
     * @return a new Query object
     */
    @SuppressWarnings("unused")
    public static Query createFor(Service service, Domain domain) {
        return new Query(service, domain, BROWSING_TIMEOUT);
    }

    /**
     * Create a Query for the given Service and Domain.
     *
     * @param service service to search for
     * @param domain  domain to search on
     * @param timeout time in MS to wait for a response
     * @return a new Query object
     */
    @SuppressWarnings("unused")
    public static Query createWithTimeout(Service service, Domain domain, int timeout) {
        return new Query(service, domain, timeout);
    }

    private Query(Service service, Domain domain, int browsingTimeout) {
        this.service = service;
        this.domain = domain;
        this.browsingTimeout = browsingTimeout;
        this.questions = new HashSet<>();
        this.records = new HashSet<>();
        this.socketLock = new ReentrantLock();
    }

    /**
     * Synchronously runs the Query a single time.
     *
     * @return a list of Instances that match this Query
     * @throws IOException thrown on socket and network errors
     */
    public Set<Instance> runOnce() throws IOException {
        return runOnceOn(InetAddress.getLocalHost());
    }

    /**
     * Synchronously runs the Query a single time.
     *
     * @param localhost address of the network interface to listen on
     * @return a list of Instances that match this Query
     * @throws IOException thrown on socket and network errors
     */
    public Set<Instance> runOnceOn(InetAddress localhost) throws IOException {
        initialQuestion = new Question(service, domain);
        instances = Collections.synchronizedSet(new HashSet<>());
        try {
            Thread listener = null;
            if (localhost != TEST_SUITE_ADDRESS) {
                openSocket(localhost);
                listener = listenForResponses();
                while (!isServerIsListening()) {
                }
            }
            ask(initialQuestion);
            if (listener != null) {
                try {
                    listener.join();
                } catch (InterruptedException e) {
                }
            }
        } finally {
            closeSocket();
        }
        return instances;
    }

    private void ask(Question question) throws IOException {
        if (questions.contains(question)) {
            return;
        }

        questions.add(question);
        if (isUsingIPv4) {
            question.askOn(socket, mdnsGroupIPv4);
        }
        if (isUsingIPv6) {
            question.askOn(socket, mdnsGroupIPv6);
        }
    }

    private boolean isServerIsListening() {
        boolean retval;
        try {
            while (!socketLock.tryLock(WAIT_FOR_LISTENER_MS, TimeUnit.MILLISECONDS)) {
                socketLock.notify();
            }
            if (listenerFinished) {
                throw new RuntimeException("Listener has already finished");
            }
            retval = listenerStarted;
        } catch (InterruptedException e) {
            throw new RuntimeException("Server is not listening");
        } finally {
            socketLock.unlock();
        }
        return retval;
    }

    /**
     * Asynchronously runs the Query in a new thread.
     */
    @SuppressWarnings("unused")
    public void start() {
        throw new RuntimeException("Not implemented yet");
    }

    private void openSocket(InetAddress localhost) throws IOException {
        mdnsGroupIPv4 = InetAddress.getByName(MDNS_IP4_ADDRESS);
        mdnsGroupIPv6 = InetAddress.getByName(MDNS_IP6_ADDRESS);
        socket = new MulticastSocket(MDNS_PORT);
        socket.setInterface(localhost);
        try {
            socket.joinGroup(mdnsGroupIPv4);
            isUsingIPv4 = true;
        } catch (SocketException e) {
        }
        try {
            socket.joinGroup(mdnsGroupIPv6);
            isUsingIPv6 = true;
        } catch (SocketException e) {
        }
        if (!isUsingIPv4 && !isUsingIPv6) {
            throw new IOException("No usable network interfaces found");
        }
        socket.setTimeToLive(10);
        socket.setSoTimeout(browsingTimeout);
    }

    private Thread listenForResponses() {
        Thread listener = new Thread(this::collectResponses);
        listener.start();
        return listener;
    }

    private Set<Instance> collectResponses() {
        long startTime = System.currentTimeMillis();
        long currentTime = startTime;
        socketLock.lock();
        listenerStarted = true;
        listenerFinished = false;
        socketLock.unlock();
        for (int timeouts = 0; timeouts == 0 && currentTime - startTime < browsingTimeout; ) {
            byte[] responseBuffer = new byte[Message.MAX_LENGTH];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            try {
                if (socket == null) break;
                socket.receive(responsePacket);
                currentTime = System.currentTimeMillis();
                //Utils.dumpPacket(responsePacket, "response");
                try {
                    parseResponsePacket(responsePacket);
                } catch (IllegalArgumentException e) {
                    timeouts = 0;
                    continue;
                }
                timeouts = 0;
            } catch (SocketTimeoutException e) {
                timeouts++;
            } catch (IOException e) {
            }
        }
        socketLock.lock();
        listenerFinished = true;
        socketLock.unlock();
        buildInstancesFromRecords();
        return instances;
    }

    void parseResponsePacket(DatagramPacket packet) throws IOException {
        Response response = Response.createFrom(packet);
        if (response.answers(questions)) {
            records.addAll(response.getRecords());
            fetchMissingRecords();
        } else {
            // This response isn't related to any of the questions we asked
        }
    }

    /**
     * Verify that each PTR record has corresponding SRV, TXT, and either A or AAAA records.
     * Request any that are missing.
     */
    private void fetchMissingRecords() throws IOException {
        for (PtrRecord ptr : records.stream().filter(r -> r instanceof PtrRecord).map(r -> (PtrRecord) r).collect(Collectors.toList())) {
            fetchMissingSrvRecordsFor(ptr);
            fetchMissingTxtRecordsFor(ptr);
        }
        for (SrvRecord srv : records.stream().filter(r -> r instanceof SrvRecord).map(r -> (SrvRecord) r).collect(Collectors.toList())) {
            fetchMissingAddressRecordsFor(srv);
        }
    }

    private void fetchMissingSrvRecordsFor(PtrRecord ptr) throws IOException {
        long numRecords = records.stream().filter(r -> r instanceof SrvRecord).filter(
                r -> r.getName().equals(ptr.getPtrName())
        ).count();
        if (numRecords == 0) {
            querySrvRecordFor(ptr);
        }
    }

    private void fetchMissingTxtRecordsFor(PtrRecord ptr) throws IOException {
        long numRecords = records.stream().filter(r -> r instanceof TxtRecord).filter(
                r -> r.getName().equals(ptr.getPtrName())
        ).count();
        if (numRecords == 0) {
            queryTxtRecordFor(ptr);
        }
    }

    private void fetchMissingAddressRecordsFor(SrvRecord srv) throws IOException {
        long numRecords = records.stream().filter(r -> r instanceof ARecord || r instanceof AaaaRecord).filter(
                r -> r.getName().equals(srv.getTarget())
        ).count();
        if (numRecords == 0) {
            queryAddressesFor(srv);
        }
    }

    private void querySrvRecordFor(PtrRecord ptr) throws IOException {
        Question question = new Question(ptr.getPtrName(), Question.QType.SRV, Question.QClass.IN);
        ask(question);
    }

    private void queryTxtRecordFor(PtrRecord ptr) throws IOException {
        Question question = new Question(ptr.getPtrName(), Question.QType.TXT, Question.QClass.IN);
        ask(question);
    }

    private void queryAddressesFor(SrvRecord srv) throws IOException {
        Question question = new Question(srv.getTarget(), Question.QType.A, Question.QClass.IN);
        ask(question);
        question = new Question(srv.getTarget(), Question.QType.AAAA, Question.QClass.IN);
        ask(question);
    }

    void buildInstancesFromRecords() {
        records.stream().filter(r -> r instanceof PtrRecord && initialQuestion.answeredBy(r))
                .map(r -> (PtrRecord) r).forEach(ptr -> instances.add(Instance.createFromRecords(ptr, records)));
    }

    private void closeSocket() {
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

    /* Accessors for test suite */

    Set<Question> getQuestions() {
        return Collections.unmodifiableSet(questions);
    }

    Set<Instance> getInstances() {
        return Collections.unmodifiableSet(instances);
    }
}
