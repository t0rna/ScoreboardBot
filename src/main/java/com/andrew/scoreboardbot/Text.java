package com.andrew.scoreboardbot;

public enum Text
{

    NATIONAL("ɴᴀᴛɪᴏɴᴀʟ ʟᴇᴀɢᴜᴇ"),
    AMERICAN("ᴀᴍᴇʀɪᴄᴀɴ ʟᴇᴀɢᴜᴇ"),
    INTERLEAGUE("ɪɴᴛᴇʀʟᴇᴀɢᴜᴇ"),
    CACTUS("ᴄᴀᴄᴛᴜꜱ ʟᴇᴀɢᴜᴇ"),
    GRAPEFRUIT("ɢʀᴀᴘᴇꜰʀᴜɪᴛ ʟᴇᴀɢᴜᴇ"),
    EXHIBITION("ᴇxʜɪʙɪᴛɪᴏɴ"),
    NL_WC(" \uD835\uDC0D\uD835\uDC0B\uD835\uDC16\uD835\uDC02"),
    AL_WC("\uD835\uDC00\uD835\uDC0B\uD835\uDC16\uD835\uDC02"),
    NLDS("\uD835\uDC0D\uD835\uDC0B\uD835\uDC03\uD835\uDC12"),
    ALDS("\uD835\uDC00\uD835\uDC0B\uD835\uDC03\uD835\uDC12"),
    NLCS("\uD835\uDC0D\uD835\uDC0B\uD835\uDC02\uD835\uDC12"),
    ALCS("\uD835\uDC00\uD835\uDC0B\uD835\uDC02\uD835\uDC12"),
    WS("\uD835\uDC16\uD835\uDC0E\uD835\uDC11\uD835\uDC0B\uD835\uDC03 \uD835\uDC12\uD835\uDC04\uD835\uDC11\uD835\uDC08\uD835\uDC04\uD835\uDC12"),
    GAME("\uD835\uDC06\uD835\uDC00\uD835\uDC0C\uD835\uDC04"),
    ONE("\uD835\uDFCF"),
    TWO("\uD835\uDFD0"),
    THREE("\uD835\uDFD1"),
    FOUR("\uD835\uDFD2"),
    FIVE("\uD835\uDFD3"),
    SIX("\uD835\uDFD4"),
    SEVEN("\uD835\uDFD5");

    private String text;

    Text(String text)
    {
        this.text = text;
    }

    public String getText() { return text; }

}