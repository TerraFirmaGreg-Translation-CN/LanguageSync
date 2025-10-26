package io.github.tfgcn.langsync;

import com.google.gson.reflect.TypeToken;
import io.github.tfgcn.langsync.cmd.*;
import io.github.tfgcn.langsync.service.FileScanService;
import io.github.tfgcn.langsync.service.SyncService;
import io.github.tfgcn.langsync.service.model.FileScanRequest;
import io.github.tfgcn.langsync.service.model.FileScanResult;
import io.github.tfgcn.langsync.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import picocli.CommandLine;

@Slf4j
public class Main {

    public static void main(String[] args) throws IOException {
        Type mapType = new TypeToken<Map<String, String>>() {}.getType();

        SyncService app = new SyncService("ru_ru");
        app.setWorkspace(Constants.DEFAULT_WORKSPACE);
        app.syncTranslationTo();
    }

    private static void cmd(String[] args) {
        CommandLine commandLine = new CommandLine(new MainCommand())
                .addSubcommand(new UploadSourceCommand());
        commandLine.setExecutionStrategy(new CommandLine.RunLast());
        System.exit(commandLine.execute(args));
    }
}
