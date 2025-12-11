package com.tonic.packets.types;

import com.google.gson.annotations.Expose;
import lombok.Data;

@Data
public class PacketEntry
{
    @Expose
    private String field;
    @Expose
    private int id;
    @Expose
    private int length;
}