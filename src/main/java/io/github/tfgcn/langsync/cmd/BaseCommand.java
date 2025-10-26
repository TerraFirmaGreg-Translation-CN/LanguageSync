package io.github.tfgcn.langsync.cmd;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.IOException;

/**
 * @author yanmaoyuan
 */
@Slf4j
class BaseCommand {

    @CommandLine.Option(names = {"-w", "--workspace"})
    protected String workspace;
}
