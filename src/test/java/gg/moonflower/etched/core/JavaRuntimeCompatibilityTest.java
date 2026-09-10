package gg.moonflower.etched.core;

import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JavaRuntimeCompatibilityTest {

    private static final int CLASS_MAGIC = 0xCAFEBABE;
    private static final int JAVA_17_CLASS_VERSION = 61;

    @Test
    void compilesMinecraftMixinsForJava17() throws IOException {
        String resource = "/gg/moonflower/etched/core/mixin/client/ClientLivingEntityMixin.class";
        InputStream stream = JavaRuntimeCompatibilityTest.class.getResourceAsStream(resource);
        assertNotNull(stream, "Missing compiled mixin class");

        try (DataInputStream input = new DataInputStream(stream)) {
            assertEquals(CLASS_MAGIC, input.readInt());
            input.readUnsignedShort();
            assertEquals(JAVA_17_CLASS_VERSION, input.readUnsignedShort());
        }
    }
}
