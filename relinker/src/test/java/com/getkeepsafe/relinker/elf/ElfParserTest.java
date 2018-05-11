package com.getkeepsafe.relinker.elf;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class ElfParserTest {
    ElfParser elfParser;

    @After
    public void tearDown() throws Exception {
        if (elfParser != null)
            elfParser.close();
    }

    @Test
    public void testParseDependenciesDoesNotThrowIllegalArgException() throws IOException {
        elfParser = new ElfParser(new File(getClass().getClassLoader().getResource("libdl.so").getFile()));
        elfParser.parseNeededDependencies();
    }
}