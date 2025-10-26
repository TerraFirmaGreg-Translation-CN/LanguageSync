package io.github.tfgcn.langsync.cmd;

import io.github.tfgcn.langsync.service.SyncService;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.concurrent.Callable;

/**
 * desc: 上传原文
 *
 * @author yanmaoyuan
 */
@Slf4j
@CommandLine.Command(name = "upload-source", mixinStandardHelpOptions = true,
        description ="Upload source file to paratranz.")
public class UploadSourceCommand extends BaseCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {

        String destLang = "ru_ru";

        SyncService app = new SyncService(destLang);
        app.setWorkspace(workspace);

        // 执行上传
        app.syncTranslationTo();

        log.info("Done.");
        return 0;
    }

}
