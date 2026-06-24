package net.pickle.rv.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;

public final class Message {

    private Message() {
    }

    public static MutableComponent prefix() {
        return Component.literal("[")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal("R").withStyle(ChatFormatting.GOLD))
                .append(Component.literal("V").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static MutableComponent info(String message) {
        return prefix()
                .append(Component.literal(message).withStyle(ChatFormatting.YELLOW));
    }

    public static MutableComponent warning(String message) {
        return prefix()
                .append(Component.literal(message).withStyle(ChatFormatting.GOLD));
    }

    public static MutableComponent error(String message) {
        return prefix()
                .append(Component.literal(message).withStyle(ChatFormatting.RED));
    }

    public static MutableComponent copyButton(String label, String copyText) {
        return prefix()
                .append(Component.literal(label).setStyle(
                        Style.EMPTY
                                .withColor(ChatFormatting.AQUA)
                                .withUnderlined(true)
                                //.withClickEvent(new ClickEvent.RunCommand(copyText))
                                .withClickEvent(new ClickEvent.CopyToClipboard(copyText))
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy validated waypoints.")))
                ));
    }
}