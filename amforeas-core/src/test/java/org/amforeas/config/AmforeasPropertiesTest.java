package org.amforeas.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import amforeas.config.AmforeasProperties;
import amforeas.config.SystemWrapper;

@ExtendWith(MockitoExtension.class)
@Tag("offline-tests")
public class AmforeasPropertiesTest {

    private final Properties javaProperties = new Properties();

    @Mock
    SystemWrapper system;

    @BeforeAll
    public static void setUp () {

    }

    @BeforeEach
    public void setUpEach () {
        javaProperties.clear();
        javaProperties.setProperty("amforeas.server.root", "/amforeas/*");
        javaProperties.setProperty("amforeas.server.host", "0.0.0.0");
        javaProperties.setProperty("amforeas.server.http.port", "8080");
        javaProperties.setProperty("amforeas.alias.list", "alias1");
        javaProperties.setProperty("amforeas.alias1.jdbc.driver", "H2_MEM");
        javaProperties.setProperty("amforeas.alias1.jdbc.database", "test_db");
    }

    @Test
    public void testLoad () {
        AmforeasProperties properties = AmforeasProperties.of(javaProperties);
        assertEquals(properties.get(AmforeasProperties.SERVER_ROOT), "/amforeas/*");
        assertTrue(properties.isValid());
    }

    @Test
    public void testIsNotValid () {
        Properties invalid = new Properties();
        assertFalse(AmforeasProperties.of(invalid).isValid());

        invalid.setProperty("amforeas.server.root", "x");
        assertFalse(AmforeasProperties.of(invalid).isValid());

        invalid.setProperty("amforeas.server.host", "x");
        assertFalse(AmforeasProperties.of(invalid).isValid());

        invalid.setProperty("amforeas.server.http.port", "x");
        assertFalse(AmforeasProperties.of(invalid).isValid());

        invalid.setProperty("amforeas.alias.list", "alias1");
        assertFalse(AmforeasProperties.of(invalid).isValid());

        invalid.setProperty("amforeas.alias.list", "alias1");
        invalid.setProperty("amforeas.alias1.jdbc.driver", "H2_MEM");
        assertFalse(AmforeasProperties.of(invalid).isValid());

        invalid.setProperty("amforeas.alias.list", "alias1");
        invalid.setProperty("amforeas.alias1.jdbc.driver", "H2_MEM");
        invalid.setProperty("amforeas.alias1.jdbc.database", "test_db");
        assertTrue(AmforeasProperties.of(invalid).isValid());
    }

    @Test
    public void testGet () {
        AmforeasProperties properties = AmforeasProperties.of(javaProperties);
        assertEquals(properties.get(AmforeasProperties.SERVER_ROOT), "/amforeas/*");
        assertThrows(IllegalArgumentException.class, () -> properties.get(""));
        assertThrows(IllegalArgumentException.class, () -> properties.get(null));
        assertThrows(IllegalArgumentException.class, () -> properties.get("invalid"));
    }

    @Test
    public void testDefaultValue () {
        assertEquals(AmforeasProperties.of(javaProperties).get(AmforeasProperties.SERVER_THREADS_MIN), "5");

        javaProperties.setProperty("amforeas.server.threads.min", "5555555");
        assertEquals(AmforeasProperties.of(javaProperties).get(AmforeasProperties.SERVER_THREADS_MIN), "5555555");
    }

    @Test
    public void testOptionalValue () {
        assertNull(AmforeasProperties.of(javaProperties).get(AmforeasProperties.SERVER_SECURE_PORT));

        javaProperties.setProperty("amforeas.server.https.port", "8443");
        assertEquals(AmforeasProperties.of(javaProperties).get(AmforeasProperties.SERVER_SECURE_PORT), "8443");
    }

    @Test
    public void testGetAlias () {
        AmforeasProperties properties = AmforeasProperties.of(javaProperties);
        assertEquals(properties.get(AmforeasProperties.DB_DRIVER, "alias1"), "H2_MEM");
        assertThrows(IllegalArgumentException.class, () -> properties.get(AmforeasProperties.DB_DRIVER, ""));
        assertThrows(IllegalArgumentException.class, () -> properties.get(AmforeasProperties.DB_DRIVER, null));
        assertThrows(IllegalArgumentException.class, () -> properties.get(AmforeasProperties.DB_DRIVER, "invalid"));
    }

    @Test
    public void testGetAliases () {
        assertEquals(AmforeasProperties.of(javaProperties).getAliases().size(), 1);

        javaProperties.setProperty("amforeas.alias.list", "alias1, alias2");
        javaProperties.setProperty("amforeas.alias2.jdbc.driver", "H2_MEM");
        assertEquals(AmforeasProperties.of(javaProperties).getAliases().size(), 2);
    }

    @Test
    public void testSystemOverride () {
        AmforeasProperties properties = AmforeasProperties.of(javaProperties);
        assertEquals(properties.get(AmforeasProperties.SERVER_PORT), "8080");

        /* Since the system override happens during the load process we have to provide the mock before the load */

        when(system.get(anyString())).thenReturn(Optional.empty());
        properties = new AmforeasProperties();
        properties.setSystem(system);
        properties.load(javaProperties);
        assertEquals(properties.get(AmforeasProperties.SERVER_PORT), "8080");

        when(system.get(anyString())).thenReturn(Optional.of("4444"));
        properties = new AmforeasProperties();
        properties.setSystem(system);
        properties.load(javaProperties);
        assertEquals(properties.get(AmforeasProperties.SERVER_PORT), "4444");
    }

    @Test
    public void testACL () {
        javaProperties.setProperty("amforeas.alias1.acl.allow", "all");
        javaProperties.setProperty("amforeas.alias1.acl.rules.users.allow", "all");
        AmforeasProperties properties = AmforeasProperties.of(javaProperties);

        assertEquals(properties.get(AmforeasProperties.DB_ACL_ALLOW_RULE, "alias1"), "all");
        assertEquals(properties.rule(AmforeasProperties.DB_ALIAS_ALLOW_RULE, "alias1", "users"), "all");

        assertThrows(IllegalArgumentException.class, () -> properties.rule(AmforeasProperties.DB_ALIAS_ALLOW_RULE, "wahh", "users"));
        assertThrows(IllegalArgumentException.class, () -> properties.rule(AmforeasProperties.DB_ALIAS_ALLOW_RULE, "alias1", "what"));
    }

    @Test
    public void test_getAliasRules () {
        javaProperties.setProperty("amforeas.alias1.acl.allow", "all");
        javaProperties.setProperty("amforeas.alias1.acl.rules.users.allow", "all");
        AmforeasProperties properties = AmforeasProperties.of(javaProperties);

        assertTrue(properties.getAliasRules("invalid").isEmpty());
        assertEquals(properties.getAliasRules("alias1").size(), 2);

        /* check that we have a rule for the users table */
        assertEquals(properties.getAliasRules("alias1")
            .stream()
            .filter(e -> e.getResource().isPresent() && e.getResource().get().equals("users"))
            .collect(Collectors.toList())
            .size(), 1);
    }

}
