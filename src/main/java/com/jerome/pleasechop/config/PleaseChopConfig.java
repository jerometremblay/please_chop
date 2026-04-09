package com.jerome.pleasechop.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class PleaseChopConfig {
    public static final Common COMMON;
    public static final ModConfigSpec COMMON_SPEC;

    static {
        Pair<Common, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = specPair.getLeft();
        COMMON_SPEC = specPair.getRight();
    }

    private PleaseChopConfig() {
    }

    public static boolean debugChatEnabled() {
        return COMMON.debugChat.get();
    }

    public static boolean debugRenderEnabled() {
        return COMMON.debugRender.get();
    }

    public static void setDebugChatEnabled(boolean enabled) {
        COMMON.debugChat.set(enabled);
    }

    public static void setDebugRenderEnabled(boolean enabled) {
        COMMON.debugRender.set(enabled);
    }

    public static void save() {
        COMMON_SPEC.save();
    }

    public static final class Common {
        private final ModConfigSpec.BooleanValue debugChat;
        private final ModConfigSpec.BooleanValue debugRender;

        private Common(ModConfigSpec.Builder builder) {
            builder.comment("Settings for Please Chop workstation debugging.")
                    .translation("pleasechop.config.category.debug")
                    .push("debug");
            debugChat = builder
                    .comment("Enable workstation debug chat output.")
                    .translation("pleasechop.config.debug.chat")
                    .define("chat", false);
            debugRender = builder
                    .comment("Enable workstation debug render overlays.")
                    .translation("pleasechop.config.debug.render")
                    .define("render", false);
            builder.pop();
        }
    }
}
