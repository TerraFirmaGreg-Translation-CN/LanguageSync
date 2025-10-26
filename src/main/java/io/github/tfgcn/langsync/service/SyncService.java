package io.github.tfgcn.langsync.service;

import com.google.gson.reflect.TypeToken;
import io.github.tfgcn.langsync.Constants;
import io.github.tfgcn.langsync.I18n;
import io.github.tfgcn.langsync.service.model.*;
import io.github.tfgcn.langsync.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.tfgcn.langsync.Constants.*;

/**
 * desc: 上传原文服务
 *
 * @author yanmaoyuan
 */
@Slf4j
public class SyncService {

    private String workDir;

    private FileScanRequest request;

    private List<String> existFiles;
    private Map<String, Map<String, String>> existFilesMap;
    private Map<String, Map<String, String>> existModMap;

    private Pattern regex = Pattern.compile(Constants.MOD_ID_REGEX);

    private final FileScanService fileScanService;

    public SyncService(String destLang) {
        this.existFiles = Collections.emptyList();
        this.existFilesMap = Collections.emptyMap();
        this.existModMap = Collections.emptyMap();

        this.fileScanService = new FileScanService();

        request = new FileScanRequest();
        request.setWorkspace(Constants.DEFAULT_WORKSPACE);
        request.setSourceFilePattern(Constants.SOURCE_PATTERN);
        request.setTranslationFilePattern(Constants.TRANSLATION_PATTERN);
        request.setSrcLang(Constants.SRC_LANG);
        request.setDestLang(destLang);
        request.setIgnores(Arrays.asList(Constants.IGNORES));
    }

    /**
     * 设置工作目录
     *
     * @param workspace 工作目录
     */
    public void setWorkspace(String workspace) throws IOException {
        File workspaceFolder = new File(workspace);
        if (!workspaceFolder.exists()) {
            log.warn("folder not found: {}", workspace);
            throw new IOException(Constants.MSG_FOLDER_NOT_FOUND + ":" + workspace);
        }
        if (!workspaceFolder.isDirectory()) {
            log.warn("not a directory: {}", workspace);
            throw new IOException(Constants.MSG_FOLDER_INVALID);
        }
        this.workDir = workspaceFolder.getCanonicalPath().replace("\\", SEPARATOR);
        log.info("set workdir to:{}", workDir);
        this.request.setWorkspace(this.workDir);
    }

    public List<?> scanExistFiles() throws IOException {
        log.info("Scanning exist language files...");
        List<String> exists = fileScanService.scanExistFiles(request);
        exists.sort(Comparator.naturalOrder());
        this.existFiles = exists;
        this.existFilesMap = new HashMap<>();
        this.existModMap = new HashMap<>();

        Type mapType = new TypeToken<Map<String, String>>() {}.getType();

        for (String item : exists) {
            Matcher matcher = regex.matcher(item);
            if (matcher.matches()) {
                String mod = matcher.group(1);
                String lang = matcher.group(2);
                log.info("mod:{}, lang:{}, file:{}", mod, lang, item);

                // read it to cache
                try {
                    Map<String, String> map = JsonUtils.readFile(getAbsoluteFile(item), mapType);
                    existFilesMap.put(item, map);

                    Map<String, String> modMap = existModMap.computeIfAbsent(mod, k -> new HashMap<>());
                    modMap.putAll(map);
                } catch (Exception ex) {
                    log.error("read file failed: {}", item, ex);
                    throw ex;
                }
            }
        }
        return existFiles;
    }

    /**
     * 获取待上传的文件列表
     */
    public List<FileScanResult> getSourceFiles() {

        List<FileScanResult> fileList = new ArrayList<>(100);

        Set<String> distinct = new HashSet<>();
        try {
            List<String> exists = fileScanService.scanExistFiles(request);
            log.info("exist files: {}", exists);
            List<FileScanResult> results = fileScanService.scanAndMapFiles(request);
            for (FileScanResult item : results) {
                if (!distinct.contains(item.getTranslationFilePath())) {
                    distinct.add(item.getTranslationFilePath());
                    fileList.add(item);
                } else {
                    log.debug("Duplicated file:{}", item.getTranslationFilePath());
                }
            }
        } catch (Exception ex) {
            log.error("扫描文件失败, request:{}", request, ex);
        }
        // 按照源文件路径进行排序
        fileList.sort(Comparator.comparing(FileScanResult::getSourceFilePath));
        return fileList;
    }

    /**
     * 上传原文
     */
    public void syncTranslationTo() throws IOException {
        // 扫描远程服务器上已有的文件
        scanExistFiles();

        log.info("Scanning language files");
        List<FileScanResult> fileList = getSourceFiles();

        log.info("Found files: {}", fileList.size());
        for (FileScanResult item : fileList) {

            Matcher matcher = regex.matcher(item.getTranslationFilePath());
            if (matcher.matches()) {
                String mod = matcher.group(1);
                String lang = matcher.group(2);
                log.info("mod:{}, lang:{}, file:{}", mod, lang, item);

                File file = getAbsoluteFile(item.getSourceFilePath());
                String translationFileFolder = item.getTranslationFileFolder();

                // TODO
                Map<String, String> existFile = existFilesMap.get(item.getTranslationFilePath());
                if (existFile == null) {
                    createFile(mod, translationFileFolder, file);
                } else {
                    modifyFile(existFile, mod, translationFileFolder, file);
                }
            }
        }
    }

    public void createFile(String mod, String translationFileFolder, File sourceFile) throws IOException {
        log.info("[NEW] {}/{}", translationFileFolder, sourceFile.getName());
        Map<String, String> sourceMap = JsonUtils.readFile(sourceFile, Map.class);

        // maybe there are some translated contents
        Map<String, String> existContents = existModMap.get(mod);
        if (existContents != null) {
            for (Map.Entry<String, String> entry : sourceMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (existContents.containsKey(key)) {
                    String existValue = existContents.get(key);
                    log.info("[exist] {}/{}:{}", mod, key, existValue);
                    sourceMap.put(key, existValue);
                }
            }
        }

        File targetFile = getAbsoluteFile(translationFileFolder + Constants.SEPARATOR + sourceFile.getName());
        FileUtils.createParentDirectories(targetFile);
        JsonUtils.writeFile(targetFile, sourceMap);
    }

    public void modifyFile(Map<String, String> existFile, String mod, String translationFileFolder, File file) throws IOException {
//        try (FileInputStream fis = new FileInputStream(file)) {
//            String md5 = DigestUtils.md5Hex(fis);
//            if (md5.equals(existFile.getHash())) {
//                log.info("[Not modified] {}/{}", translationFileFolder, file.getName());
//                return;
//            }
//        }

        // TODO
        log.info("[Updated] {}/{}", translationFileFolder, file.getName());
    }

    /**
     * 上传原始文件
     * 这个接口是给GUI用的，返回结果用于界面展示。
     * @param scannedFile
     * @return
     * @throws IOException
     */
    public String uploadSourceFile(FileScanResult scannedFile) throws IOException {
        File file = getAbsoluteFile(scannedFile.getSourceFilePath());
        String remoteFolder = scannedFile.getTranslationFileFolder();

        /*******
        // 生成上传 paratranz 的最终文件名。可用于比较远程文件是否已存在
        FilesDto remoteFile = existFilesMap.get(scannedFile.getTranslationFilePath());
        if (remoteFile == null) {
            uploadFile(remoteFolder, file);
            return I18n.getString("label.completed.newFile");
        } else {
            try (FileInputStream fis = new FileInputStream(file)) {
                String md5 = DigestUtils.md5Hex(fis);
                if (md5.equals(remoteFile.getHash())) {
                    return I18n.getString("label.skipped.notModified");
                }
            }

            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(),
                    RequestBody.create(Constants.MULTIPART_FORM_DATA, file));

            Response<FileUploadRespDto> updateResp = filesApi.updateFile(projectId, remoteFile.getId(), filePart).execute();
            if (updateResp.isSuccessful()) {
                log.info("update success: {}", updateResp.body());
            }

            return I18n.getString("label.completed.updated");
        }
         ***/
        return I18n.getString("label.completed.updated");
    }

    /**
     * 递归更新嵌套的 JSON 结构
     */
    public static void updateNestedStructure(Map<String, Object> targetMap, Map<String, String> translatedDict, String parentKey) {
        for (Map.Entry<String, Object> entry : targetMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String currentKey = getCurrentKey(parentKey, key);

            if (value instanceof String) {
                // 如果是字符串值，直接查找对应的翻译
                String translatedValue = translatedDict.get(currentKey);
                if (translatedValue != null) {
                    targetMap.put(key, translatedValue);
                }
                // 如果没有翻译，保持原值不变
            } else if (value instanceof Map) {
                // 如果是嵌套的 Map，递归处理
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                updateNestedStructure(nestedMap, translatedDict, currentKey);
            } else if (value instanceof List) {
                // 如果是数组，处理每个元素
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) value;
                updateListStructure(list, translatedDict, currentKey);
            }
            // 其他类型（Number, Boolean等）保持不变
        }
    }

    /**
     * 更新数组结构
     */
    private static void updateListStructure(List<Object> list, Map<String, String> translatedDict, String parentKey) {
        for (int i = 0; i < list.size(); i++) {
            Object element = list.get(i);
            String currentKey = getCurrentKey(parentKey, i);

            if (element instanceof Map) {
                // 数组元素是对象，递归处理
                @SuppressWarnings("unchecked")
                Map<String, Object> elementMap = (Map<String, Object>) element;
                updateNestedStructure(elementMap, translatedDict, currentKey);
            } else if (element instanceof List) {
                // 嵌套数组，递归处理
                @SuppressWarnings("unchecked")
                List<Object> nestedList = (List<Object>) element;
                updateListStructure(nestedList, translatedDict, currentKey);
            } else if (element instanceof String) {
                // 数组元素是字符串，查找对应的翻译
                String translatedValue = translatedDict.get(currentKey);
                if (translatedValue != null) {
                    list.set(i, translatedValue);
                }
            }
        }
    }

    /**
     * Flatting nested json structure
     */
    public static void flattenNestedStructure(Map<String, Object> sourceMap, Map<String, String> toSave, String parentKey) {
        for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String currentKey = getCurrentKey(parentKey, key);

            if (value instanceof String) {
                toSave.put(currentKey, (String) value);
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                flattenNestedStructure(nestedMap, toSave, currentKey);
            } else if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) value;
                flattenListStructure(list, toSave, currentKey);
            }
        }
    }

    /**
     * 打平数组结构
     */
    private static void flattenListStructure(List<Object> list, Map<String, String> toSaveMap, String parentKey) {
        for (int i = 0; i < list.size(); i++) {
            Object element = list.get(i);
            String currentKey = getCurrentKey(parentKey, i);

            if (element instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> elementMap = (Map<String, Object>) element;
                flattenNestedStructure(elementMap, toSaveMap, currentKey);
            } else if (element instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> nestedList = (List<Object>) element;
                flattenListStructure(nestedList, toSaveMap, currentKey);
            } else if (element instanceof String) {
                String translatedValue = toSaveMap.get(currentKey);
                if (translatedValue != null) {
                    list.set(i, translatedValue);
                }
            }
        }
    }

    private static String getCurrentKey(String parentKey, String key) {
        return parentKey == null || parentKey.isEmpty() ? key : parentKey + "->" + key;
    }

    private static String getCurrentKey(String parentKey, int index) {
        return parentKey == null || parentKey.isEmpty() ? index + "" : parentKey + "->" + index;
    }

    public File getAbsoluteFile(String relativePath) {
        return new File(workDir + SEPARATOR + relativePath);
    }

    public String getAbsolutePath(String relativePath) {
        return workDir + SEPARATOR + relativePath;
    }
}
