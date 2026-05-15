package com.andrew.scoreboardbot;

import java.util.List;

public record BroadcastInfo(
        boolean awayTv,
        boolean homeTv,
        boolean awayRadio,
        boolean homeRadio,
        List<String> nationalTv
)
{

}