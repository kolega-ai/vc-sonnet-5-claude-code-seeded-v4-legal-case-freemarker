package com.lawfirm.lcm.service;

import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * Produces a cache-busting query suffix for static asset URLs (e.g. /css/app.css?v=1234).
 * java.util.Random is used deliberately here: this is a non-cryptographic use, cache-busting
 * only - it never guards a security decision, so SecureRandom is unnecessary overhead.
 */
@Service
public class AssetVersionService {

    private final int version = new Random().nextInt(1_000_000);

    public int getVersion() {
        return version;
    }
}
