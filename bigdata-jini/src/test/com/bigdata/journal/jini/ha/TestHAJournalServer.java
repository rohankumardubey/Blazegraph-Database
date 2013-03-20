/**

Copyright (C) SYSTAP, LLC 2006-2007.  All rights reserved.

Contact:
     SYSTAP, LLC
     4501 Tower Road
     Greensboro, NC 27410
     licenses@bigdata.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
/*
 * Created on Oct 31, 2012
 */
package com.bigdata.journal.jini.ha;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.jini.core.lookup.ServiceID;

import com.bigdata.ha.HAGlue;
import com.bigdata.ha.msg.HADigestRequest;
import com.bigdata.quorum.Quorum;
import com.bigdata.rdf.sail.webapp.client.HAStatusEnum;

/**
 * Life cycle and related tests for a single remote {@link HAJournalServer} out
 * of a quorum of 3. The quorum will not meet for these unit tests.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class TestHAJournalServer extends AbstractHA3JournalServerTestCase {

    public TestHAJournalServer() {
    }

    public TestHAJournalServer(final String name) {
        super(name);
    }

    /**
     * One service starts, quorum does not meet (replication factor is 3). This
     * also serves to verify the <code>HAJournal-A.config</code> file.
     */
    public void testStartA() throws Exception {
        
        final HAGlue serverA = startA();
        
        try {

            quorum.awaitQuorum(awaitQuorumTimeout, TimeUnit.MILLISECONDS);
            
            fail("Not expecting quorum meet");

        } catch (TimeoutException ex) {
        
            // ignore.
            
        }

        // Service is not met in any role around a quorum.
        try {
            serverA.awaitHAReady(awaitQuorumTimeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            // Ignore expected exception.
        }

        // Verify can access the REST API "status" page.
        doNSSStatusRequest(serverA);

        // Verify that service self-reports as NotReady via the REST API.
        assertEquals(HAStatusEnum.NotReady, getNSSHAStatus(serverA));
        
        assertTrue(getHAJournalFileA().exists());
        assertTrue(getHALogDirA().exists());
        assertTrue(getSnapshotDirA().exists());
        
//        serverA.enterErrorState().get();
//        
//        Thread.sleep(10000/*ms*/);
        
    }

    /**
     * Verify Server B will start - this helps to proof the configuration
     * files.
     */
    public void testStartB() throws Exception {
        
        final HAGlue serverB = startB();
        
        try {

            quorum.awaitQuorum(awaitQuorumTimeout, TimeUnit.MILLISECONDS);
            
            fail("Not expecting quorum meet");

        } catch (TimeoutException ex) {
        
            // ignore.
            
        }
        
        doNSSStatusRequest(serverB);

        assertTrue(getHAJournalFileB().exists());
        assertTrue(getHALogDirB().exists());
        assertTrue(getSnapshotDirB().exists());
        
    }

    /**
     * Verify Server C will start - this helps to proof the configuration
     * files.
     */
    public void testStartC() throws Exception {
        
        final HAGlue serverC = startC();
        
        try {

            quorum.awaitQuorum(awaitQuorumTimeout, TimeUnit.MILLISECONDS);
            
            fail("Not expecting quorum meet");

        } catch (TimeoutException ex) {
        
            // ignore.
            
        }
        
        doNSSStatusRequest(serverC);

        assertTrue(getHAJournalFileC().exists());
        assertTrue(getHALogDirC().exists());
        assertTrue(getSnapshotDirC().exists());
        
    }

    /**
     * Verify that the various bits of state are removed from the file system
     * when the service is destroyed.
     * 
     * @throws Exception
     */
    public void testStartA_Destroy() throws Exception {
        
        final HAGlue serverA = startA();
        
        try {

            quorum.awaitQuorum(awaitQuorumTimeout, TimeUnit.MILLISECONDS);
            
            fail("Not expecting quorum meet");

        } catch (TimeoutException ex) {
        
            // ignore.
            
        }
        
        doNSSStatusRequest(serverA);

        assertTrue(getHAJournalFileA().exists());
        assertTrue(getHALogDirA().exists());
        assertTrue(getSnapshotDirA().exists());

        destroyA();

        assertFalse(getHAJournalFileA().exists());
        assertFalse(getHALogDirA().exists());
        assertFalse(getSnapshotDirA().exists());

    }
    
    /**
     * One service starts, quorum does not meet (replication factor is 3).
     * Shutdown and restart the service. Verify that it comes back up with the
     * same {@link ServiceID}. Again, quorum does not meet.
     * <p>
     * 
     * Note: I have observed an improper shutdown for this test. The stack
     * traces below show that the shutdown code was blocked awaiting the return
     * from clearToken(), which was triggered by serviceLeave() and
     * conditionalWithdrawVote(). This does not always result in a hung service.
     * The problem would appear to be a failure to correctly identify when the
     * quorum will break. If the code gets this wrong, then it can sping in
     * QuorumActorBase.conditionalClearToken(). That seems pretty dangerous.
     * 
     * <pre>
     * "shutdownThread" daemon prio=5 tid=7fa36f243800 nid=0x115c1c000 waiting on condition [115c1b000]
     *    java.lang.Thread.State: WAITING (parking)
     *     at sun.misc.Unsafe.park(Native Method)
     *     - parking to wait for  <7c021a100> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)
     *     at java.util.concurrent.locks.LockSupport.park(LockSupport.java:156)
     *     at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:1987)
     *     at com.bigdata.quorum.AbstractQuorum$QuorumActorBase.conditionalWithdrawVoteImpl(AbstractQuorum.java:1579)
     *     at com.bigdata.quorum.AbstractQuorum$QuorumActorBase.serviceLeave(AbstractQuorum.java:1500)
     *     at com.bigdata.journal.jini.ha.HAJournalServer.beforeShutdownHook(HAJournalServer.java:524)
     *     at com.bigdata.journal.jini.ha.AbstractServer.shutdownNow(AbstractServer.java:1457)
     *     at com.bigdata.journal.jini.ha.AbstractServer$ShutdownThread.run(AbstractServer.java:1892)     *
     *     
     * "WatcherActionService1" daemon prio=5 tid=7fa36dc63800 nid=0x114f50000 waiting on condition [114f4f000]
     *    java.lang.Thread.State: WAITING (parking)
     *     at sun.misc.Unsafe.park(Native Method)
     *     - parking to wait for  <7c021a0b8> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)
     *     at java.util.concurrent.locks.LockSupport.park(LockSupport.java:156)
     *     at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:1987)
     *     at com.bigdata.quorum.AbstractQuorum$QuorumActorBase.conditionalClearToken(AbstractQuorum.java:1690)
     *     at com.bigdata.quorum.AbstractQuorum$QuorumActorBase.clearToken(AbstractQuorum.java:1437)
     *     at com.bigdata.quorum.AbstractQuorum$QuorumWatcherBase$8.run(AbstractQuorum.java:2959)
     *     at com.bigdata.quorum.AbstractQuorum$QuorumWatcherBase$1.run(AbstractQuorum.java:2048)
     *     at java.util.concurrent.ThreadPoolExecutor$Worker.runTask(ThreadPoolExecutor.java:886)
     *     at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:908)
     *     at java.lang.Thread.run(Thread.java:680)
     * </pre>
     */
    public void testRestartA() throws Exception {
        
        final UUID serviceUUID;
        final byte[] digestA;
        {

            final HAGlue serverA = startA();

            serviceUUID = serverA.getServiceUUID();
            
            try {

                quorum.awaitQuorum(awaitQuorumTimeout, TimeUnit.MILLISECONDS);

                fail("Not expecting quorum meet");

            } catch (TimeoutException ex) {

                // ignore.

            }

            doNSSStatusRequest(serverA);

            digestA = serverA.computeDigest(
                    new HADigestRequest(null/* storeId */)).getDigest();

        }

        /*
         * Restart the service.
         */
        {

            // Restart.
            final HAGlue serverA = restartA();

            // Make sure that the service came back up with the same UUID.
            assertEquals(serviceUUID, serverA.getServiceUUID());

            // Verify quorum does not meet.
            try {

                quorum.awaitQuorum(1000, TimeUnit.MILLISECONDS);

                fail("Not expecting quorum meet");

            } catch (TimeoutException ex) {

                // ignore.

            }

            // Verify NSS is running.
            doNSSStatusRequest(serverA);

            /*
             * Verify no changes in digest on restart?
             */
            final byte[] digestA1 = serverA.computeDigest(
                    new HADigestRequest(null/* storeId */)).getDigest();

            assertEquals(digestA, digestA1);

        }

    }

}
