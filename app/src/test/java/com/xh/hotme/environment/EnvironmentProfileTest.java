package com.xh.hotme.environment;
import org.junit.Test;
import static org.junit.Assert.*;

public class EnvironmentProfileTest {
    @Test public void customAddressesKeepPortsAndApiPrefix() {
        EnvironmentProfile profile = new EnvironmentProfile("custom", "192.168.1.20:61003", "https://api.example.com:8443/tenant/hotme-api/");
        assertEquals("amqp://192.168.1.20:61003/rewoo", profile.rabbitmq);
        assertEquals("https://api.example.com:8443/tenant/hotme-api", profile.api);
        assertEquals("https://api.example.com:8443/tenant", profile.apiOrigin());
        assertEquals("http://localhost:61004/hotme-api", EnvironmentProfile.normalizeApi("http://localhost:61004"));
        assertEquals("amqp://[::1]:5672/rewoo", EnvironmentProfile.normalizeRabbitmq("[::1]"));
    }
    @Test public void rejectsIncompleteAndAmbiguousProfiles() {
        for (String url : new String[]{"", "https://", "ftp://example.com", "https://user:pass@example.com", "https://example.com?other=1", "https://example.com:65536", "https://example.com:0"}) {
            try { EnvironmentProfile.normalizeApi(url); fail("Accepted " + url); } catch (IllegalArgumentException expected) {}
        }
        for (String url : new String[]{"", "http://example.com", "amqps://example.com", "amqp://user:pass@example.com", "amqp://example.com:0", "amqp://example.com/a/b"}) {
            try { EnvironmentProfile.normalizeRabbitmq(url); fail("Accepted " + url); } catch (IllegalArgumentException expected) {}
        }
        try { new EnvironmentProfile("invalid", "", ""); fail(); } catch (IllegalArgumentException expected) {}
    }
}
