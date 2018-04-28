package org.mao.tinyserver.config;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class ServerConfigBuilderTest {

    @Test
    public void build() {
        System.out.println(new ServerConfigBuilder("/Users/garymao/NustoreFiles/Nutstore/github/TinyServer/src/test/resources/ServerConfig.xml").build());
    }
}