package com.sunsky.server;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import redis.clients.jedis.*;

/**
 * Unit test for RedisClient
 */
public class RedisClientTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public RedisClientTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( RedisClientTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testGetJedis()
    {
	    assertTrue( true );
    }
}
