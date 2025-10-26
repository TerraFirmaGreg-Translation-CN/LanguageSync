package io.github.tfgcn.langsync.service.model;

import lombok.Data;

import java.util.List;

/**
 * 文件扫描规则
 */
@Data
public class FileScanRule {
    private Boolean enabled;
    private String sourcePattern;
    private String translationPattern;
    private String srcLang;
    private String destLang;
    private List<String> ignores;
    
    public FileScanRule() {}
    
    public FileScanRule(FileScanRule other) {
        this.enabled = other.enabled;
        this.sourcePattern = other.sourcePattern;
        this.translationPattern = other.translationPattern;
        this.srcLang = other.srcLang;
        this.destLang = other.destLang;
        this.ignores = other.ignores;
    }
}
