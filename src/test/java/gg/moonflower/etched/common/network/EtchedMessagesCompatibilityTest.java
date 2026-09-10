package gg.moonflower.etched.common.network;

import gg.moonflower.etched.common.menu.RadioMenu;
import gg.moonflower.etched.common.network.play.*;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EtchedMessagesCompatibilityTest {

    private static final String RADIO_URL = "https://radio.example/live";

    @Test
    void preservesEtched304ProtocolAndPacketIds() {
        assertEquals("3", EtchedLegacyProtocol.VERSION);
        assertContract(EtchedLegacyProtocol.CLIENTBOUND_INVALID_ETCH_URL, 0,
                ClientboundInvalidEtchUrlPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        assertContract(EtchedLegacyProtocol.CLIENTBOUND_PLAY_ENTITY_MUSIC, 1,
                ClientboundPlayEntityMusicPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        assertContract(EtchedLegacyProtocol.CLIENTBOUND_PLAY_MUSIC, 2,
                ClientboundPlayMusicPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        assertContract(EtchedLegacyProtocol.CLIENTBOUND_SET_URL, 3,
                ClientboundSetUrlPacket.class, NetworkDirection.PLAY_TO_CLIENT);
        assertContract(EtchedLegacyProtocol.SERVERBOUND_SET_URL, 4,
                ServerboundSetUrlPacket.class, NetworkDirection.PLAY_TO_SERVER);
        assertContract(EtchedLegacyProtocol.SERVERBOUND_EDIT_MUSIC_LABEL, 5,
                ServerboundEditMusicLabelPacket.class, NetworkDirection.PLAY_TO_SERVER);
        assertContract(EtchedLegacyProtocol.SET_ALBUM_JUKEBOX_TRACK, 6,
                SetAlbumJukeboxTrackPacket.class, null);
    }

    @Test
    void preservesClientboundRadioUrlEncoding() throws Exception {
        assertLegacyUrlEncoding(new ClientboundSetUrlPacket(RADIO_URL));
    }

    @Test
    void preservesServerboundRadioUrlEncoding() throws Exception {
        assertLegacyUrlEncoding(new ServerboundSetUrlPacket(RADIO_URL));
    }

    @Test
    void preservesLegacyRadioMenuSetUrlDescriptor() throws Exception {
        assertEquals(void.class, RadioMenu.class.getMethod("setUrl", String.class).getReturnType());
    }

    @Test
    void decodesLegacyRadioUrlBytes() {
        byte[] encoded = expectedLegacyUrlBytes();
        FriendlyByteBuf clientboundBuffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(encoded));
        FriendlyByteBuf serverboundBuffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(encoded));

        try {
            ClientboundSetUrlPacket clientbound = new ClientboundSetUrlPacket(clientboundBuffer);
            ServerboundSetUrlPacket serverbound = new ServerboundSetUrlPacket(serverboundBuffer);

            assertEquals(RADIO_URL, clientbound.url());
            assertEquals(RADIO_URL, serverbound.url());
        } finally {
            clientboundBuffer.release();
            serverboundBuffer.release();
        }
    }

    private static void assertLegacyUrlEncoding(EtchedPacket packet) throws Exception {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.writePacketData(buffer);
            assertArrayEquals(expectedLegacyUrlBytes(), ByteBufUtil.getBytes(buffer));
        } finally {
            buffer.release();
        }
    }

    private static void assertContract(EtchedLegacyProtocol.PacketContract<?> contract, int id,
                                       Class<? extends EtchedPacket> type, NetworkDirection direction) {
        assertEquals(id, contract.id());
        assertEquals(type, contract.type());
        assertEquals(direction, contract.direction());
    }

    private static byte[] expectedLegacyUrlBytes() {
        byte[] url = RADIO_URL.getBytes(StandardCharsets.UTF_8);
        byte[] encoded = new byte[url.length + 1];
        encoded[0] = (byte) url.length;
        System.arraycopy(url, 0, encoded, 1, url.length);
        return encoded;
    }
}
