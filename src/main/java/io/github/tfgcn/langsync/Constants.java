package io.github.tfgcn.langsync;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public final class Constants {

    private Constants() {
    }

    public static final String MOD_ID_REGEX = "^.*?Tools-Modern/LanguageMerger/LanguageFiles/([^/]+)/([^/]+)/.+$";

    public static final String SOURCE_PATTERN = "Tools-Modern/LanguageMerger/LanguageFiles/**/en_us/**.json";
    public static final String TRANSLATION_PATTERN = "%original_path_pre%/%language%/%original_path%/%original_file_name%";
    public static final String SRC_LANG = "en_us";
    public static final String DEST_LANG = "ru_ru";
    public static final String[] IGNORES = {"tfg/**/ore_veins.json"};

    public static final String DEFAULT_WORKSPACE = "..";

    // Environments
    public static final String ENV_WORKSPACE = "WORKSPACE";

    public static final String SEPARATOR = "/";

    public static final String MSG_FOLDER_NOT_FOUND = "找不到目录";
    public static final String MSG_FOLDER_INVALID = "非法目录";
}