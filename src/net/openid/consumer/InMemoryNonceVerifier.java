/*
 * Copyright 2006-2007 Sxip Identity Corporation
 */

package net.openid.consumer;

import org.apache.log4j.Logger;

import java.util.*;
import java.text.ParseException;

/**
 * @author Marius Scurtescu, Johnny Bufu
 */
public class InMemoryNonceVerifier extends AbstractNonceVerifier
{
    private static Logger _log = Logger.getLogger(InMemoryNonceVerifier.class);
    private static final boolean DEBUG = _log.isDebugEnabled();

    private Map _idpMap = new HashMap();

    public InMemoryNonceVerifier(int maxAge)
    {
        super(maxAge);
    }

    protected synchronized int seen(Date now, String idpUrl, String nonce)
    {
        removeAged(now);

        Set seenSet = (Set) _idpMap.get(idpUrl);

        if (seenSet == null)
        {
            seenSet = new HashSet();

            _idpMap.put(idpUrl, seenSet);
        }

        if (seenSet.contains(nonce))
        {
            _log.error("Possible replay attack! Already seen nonce: " + nonce);
            return SEEN;
        }

        seenSet.add(nonce);

        if (DEBUG) _log.debug("Nonce verified: " + nonce);

        return OK;
    }

    private synchronized void removeAged(Date now)
    {
        Set idpToRemove = new HashSet();
        Iterator idpUrls = _idpMap.keySet().iterator();
        while (idpUrls.hasNext())
        {
            String idpUrl = (String) idpUrls.next();

            Set seenSet = (Set) _idpMap.get(idpUrl);
            Set nonceToRemove = new HashSet();

            Iterator nonces = seenSet.iterator();
            while (nonces.hasNext())
            {
                String nonce = (String) nonces.next();

                try
                {
                    Date nonceDate = _dateFormat.parse(nonce);

                    if (isTooOld(now, nonceDate))
                    {
                        nonceToRemove.add(nonce);
                    }
                }
                catch (ParseException e)
                {
                    nonceToRemove.add(nonce);
                }
            }

            nonces = nonceToRemove.iterator();
            while (nonces.hasNext())
            {
                String nonce = (String) nonces.next();

                if (DEBUG)
                    _log.debug("Removing nonce: " + nonce +
                               " from OP: " + idpUrl);
                seenSet.remove(nonce);
            }

            if (seenSet.size() == 0)
                idpToRemove.add(idpUrl);
        }

        idpUrls = idpToRemove.iterator();
        while (idpUrls.hasNext())
        {
            String idpUrl = (String) idpUrls.next();

            if (DEBUG) _log.debug("Removed all nonces from OP: " + idpUrl);

            _idpMap.remove(idpUrl);
        }
    }

    protected synchronized int size()
    {
        int total = 0;

        Iterator idpUrls = _idpMap.keySet().iterator();
        while (idpUrls.hasNext())
        {
            String idpUrl = (String) idpUrls.next();

            Set seenSet = (Set) _idpMap.get(idpUrl);

            total += seenSet.size();
        }

        return total;
    }
}
