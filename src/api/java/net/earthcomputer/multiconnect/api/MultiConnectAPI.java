package net.earthcomputer.multiconnect.api;

import net.minecraft.SharedConstants;

import java.util.Collections;
import java.util.List;

/**
 * The MultiConnect API
 */
public class MultiConnectAPI {

    static MultiConnectAPI INSTANCE = new MultiConnectAPI();

    /**
     * Returns the singleton instance of this API
     */
    public static MultiConnectAPI instance() {
        return INSTANCE;
    }

    /**
     * Gets the protocol version of the server currently connected to,
     * or the current game version if not connected to a server
     */
    public int getProtocolVersion() {
        return SharedConstants.getGameVersion().getProtocolVersion();
    }

    /**
     * Gets a supported {@link IProtocol} object by its protocol version, or <tt>null</tt> if the protocol is not supported
     */
    public IProtocol byProtocolVersion(int version) {
        return version == SharedConstants.getGameVersion().getProtocolVersion() ? CurrentVersionProtocol.INSTANCE : null;
    }

    /**
     * Returns a list of supported protocols, from newest to oldest
     */
    public List<IProtocol> getSupportedProtocols() {
        return Collections.singletonList(CurrentVersionProtocol.INSTANCE);
    }

    private static class CurrentVersionProtocol implements IProtocol {
        public static CurrentVersionProtocol INSTANCE = new CurrentVersionProtocol();

        @Override
        public int getValue() {
            return SharedConstants.getGameVersion().getProtocolVersion();
        }

        @Override
        public String getName() {
            return SharedConstants.getGameVersion().getName();
        }

        @Override
        public boolean isMajorRelease() {
            return true;
        }

        @Override
        public IProtocol getMajorRelease() {
            return this;
        }

        @Override
        public List<IProtocol> getMinorReleases() {
            return Collections.singletonList(this);
        }
    }

}
