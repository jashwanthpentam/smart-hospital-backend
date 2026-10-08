package com.hospital;

public class TestRunner {
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("RUNNING MANDATORY PRIORITY QUEUE LOGIC TESTS");
        System.out.println("=================================================");
        QueueServiceTest test = new QueueServiceTest();

        try {
            test.setUp();
            test.testPriorityQueueOrdering();
            System.out.println("✅ PASS: testPriorityQueueOrdering (EMERGENCY > URGENT > NORMAL)");

            test.setUp();
            test.testSamePriorityEarlierArrivalFirst();
            System.out.println("✅ PASS: testSamePriorityEarlierArrivalFirst (Tie-breaker earlier arrival first)");

            System.out.println("=================================================");
            System.out.println("ALL MANDATORY PRIORITY QUEUE TESTS PASSED!");
            System.out.println("=================================================");
        } catch (Throwable t) {
            System.err.println("❌ FAIL: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }
    }
}
