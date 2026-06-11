package appeng.parts.automation;

import appeng.util.render.AERenderProperty;

public final class PartModelData {
    private PartModelData() {
    }

    public static final AERenderProperty<StatusIndicatorState> STATUS_INDICATOR = new AERenderProperty<>(
            "status_indicator");
    public static final AERenderProperty<PlaneConnections> CONNECTIONS = new AERenderProperty<>("plane_connections");
    public static final AERenderProperty<Long> P2P_FREQUENCY = new AERenderProperty<>("p2p_frequency");
    public static final AERenderProperty<Boolean> LEVEL_EMITTER_ON = new AERenderProperty<>("level_emitter_on");
    public static final AERenderProperty<Boolean> CABLE_ANCHOR_SHORT = new AERenderProperty<>("cable_anchor_short");
    public static final AERenderProperty<Boolean> MONITOR_LOCKED = new AERenderProperty<>("monitor_locked");
    /**
     * The spin of a reporting part around its outward-facing axis. Was {@code AEModelData.SPIN}.
     */
    public static final AERenderProperty<Byte> SPIN = new AERenderProperty<>("spin");

    public enum StatusIndicatorState {
        ACTIVE,
        POWERED,
        UNPOWERED
    }
}
