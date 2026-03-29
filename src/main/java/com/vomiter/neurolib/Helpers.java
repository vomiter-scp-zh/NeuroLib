package com.vomiter.neurolib;

import net.minecraft.resources.Identifier;

public class Helpers {
    public static Identifier id(String namespace, String path){
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    public static Identifier id(String path){
        return id(NeuroLib.MOD_ID, path);
    }
}
