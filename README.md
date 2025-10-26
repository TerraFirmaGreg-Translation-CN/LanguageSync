# Language Sync Tool

The main objective is:

- Add entries from en_us to the directory of a specified language (e.g. `ru_ru`).
- Only new entries will be appended, while existing ones will remain intact.

## Configuration

### Environments

| 名称                   | 描述                                                                             |
|:---------------------|:-------------------------------------------------------------------------------|
| WORKSPACE            | Working directory, where to scan the original file, and store the target files |

### Example

```json
{
  "workspace" : "..",
  "rules": [
    {
      "enabled": true,
      "sourcePattern": "Tools-Modern/LanguageMerger/LanguageFiles/**/en_us/*.json",
      "translationPattern": "%original_path_pre%/%language%/%original_path%/%original_file_name%",
      "srcLang": "en_us",
      "destLang": "zh_cn",
      "ignores": [
        "tfg/en_us/ore_veins.json"
      ]
    },
    {
      "sourcePattern": "Modpack-Modern/kubejs/assets/tfc/patchouli_books/field_guide/en_us/**.json",
      "translationPattern": "%original_path_pre%/%language%/%original_path%/%original_file_name%",
      "srcLang": "en_us",
      "destLang": "zh_cn",
      "ignores": [
        "**/tfg_ores/*_index.json"
      ]
    },
    {
      "enabled": false,
      "sourcePattern": "Tools-Modern/LanguageMerger/LanguageFiles/tfg/en_us/Quests/*.json",
      "translationPattern": "Tools-Modern/LanguageMerger/LanguageFiles/tfg/zh_cn/Quests/%original_file_name%",
      "srcLang": "en_us",
      "destLang": "zh_cn",
      "ignores": []
    }
  ]
}
```

## 文件扫描映射

参数说明:

| 参数                 | 描述                                                                               |
|:-------------------|:---------------------------------------------------------------------------------|
| enabled            | 是否启用该规则。                                                                         |
| workspace          | 工作目录，用于扫描待翻译的源文件，以及存储译文。程序只会扫描位于工作目录(workspace)下的文件和目录。                          |
| sourcePattern      | 源文件匹配规则，用于识别要翻译的文件。支持[Glob](https://en.wikipedia.org/wiki/Glob_(programming))语法。 |
| translationPattern | 映射译文规则，根据源文件路径生成译文路径。详见下文。                                                       |
| srcLang            | 源语言，用于识别源文目录结构。                                                                  |
| destLang           | 目标语言，用于生成译文文件。                                                                   |
| ignores            | 忽略列表，用于忽略某些文件。                                                                   |

```json
{
  "workspace": "/data/tfgcn",
  "rules": [
    {
      "enabled": true,
      "sourcePattern" : "Modpack-Modern/kubejs/assets/tfc/patchouli_books/field_guide/en_us/**.json",
      "translationPattern" : "%original_path_pre%/%language%/%original_path%/%original_file_name%",
      "srcLang" : "en_us",
      "destLang" : "zh_cn",
      "ignores" : [ "**/tfg_ores/*_index.json" ]
    }
  ]
}
```

### sourcePattern

首先，程序执行时需要确认翻译文件范围。根据 `sourcePattern` 确定原文，随后才能上传到 `paratrans` 项目中。

**1、单文件扫描**

若扫描目录没有复杂结构，只需要识别指定语言文件，例如：

```
test/ae2/en_us.json
test/create/en_us.json
```

扫描语法如下：

| Pattern               | 说明                             | 结果                                              |
|:----------------------|:-------------------------------|-------------------------------------------------|
| `test/**/en_us.json`  | 识别 test/**/ 目录下的所有en_us.json文件 | test/ae2/en_us.json<br/> test/create/en_us.json |

**2、原文分散在多文件、多目录中**

以下面的目录结构为例：

```
test/en_us/blocks.json
test/en_us/items.json
test/en_us/Quests/chapter1.json
test/en_us/Quests/chapter2/chapter2.json
test/en_us/Quests/chapter3/chapter3/chapter3.json
```

不同扫描语法对应的结果如下：

| Pattern                 | 说明                                      | 结果                                                                                                                                                                                                    |
|:------------------------|:----------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `test/en_us/*.json`     | 不包含子目录下的文件。                             | test/en_us/blocks.json<br/> test/en_us/items.json                                                                                                                                                     |
| `test/en_us/**.json`    | 所有文件，含子目录下的文件。                          | test/en_us/blocks.json<br/> test/en_us/items.json<br/> test/en_us/Quests/chapter1.json<br/> test/en_us/Quests/chapter2/chapter2.json<br/> test/en_us/Quests/chapter3/chapter3/chapter3.json           |
| `test/en_us/**/*.json`  | 只包含子目录下的文件，但不含 `test/en_us/` 目录下的文件。    | test/en_us/Quests/chapter1.json<br/> test/en_us/Quests/chapter2/chapter2.json<br/> test/en_us/Quests/chapter3/chapter3/chapter3.json                                                                  |

**模组整合包，语言目录上级还有其他目录**

对于模组整合包，通常目录下首先会有多个mod目录，然后才是语言目录。以下面结构为例：

```
test/ae2/en_us/Quests/chapter.json
test/ae2/en_us/lang.json
test/tfg/en_us/Quests/chapter.json
test/tfg/en_us/lang.json
```

| Pattern                   | 说明                                           | 结果                                                                                                                                  |
|:--------------------------|:---------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| `test/**/en_us/*.json`    | 不包含子目录下的文件。                                  | test/ae2/en_us/lang.json<br/> test/tfg/en_us/lang.json                                                                              |
| `test/**/en_us/**.json`   | 所有文件，含子目录下的文件。                               | test/ae2/en_us/Quests/chapter.json<br/> test/ae2/en_us/lang.json<br/> test/tfg/en_us/Quests/chapter.json<br/> test/tfc/en_us.json   |
| `test/**/en_us/**/*.json` | 含子目录下的文件，但不包含 `test/**/en_us/` 目录下的文件。       | test/ae2/en_us/Quests/chapter.json<br/> test/tfg/en_us/Quests/chapter.json                                                          |

### translationPattern

再确认了翻译文件范围后，下一步是确定译文路径。

本应用支持4个占位符：

| 占位符                    | 说明                   |
|:-----------------------|:---------------------|
| `%language%`           | 目标语言，例如：`zh_cn`      |
| `%original_file_name%` | 原文文件名，例如：`lang.json` |
| `%original_path_pre%`  | 根于源语言分割路径，取语言前面的部分。  |
| `%original_path%`      | 根于源语言分割路径，取语言后面的部分。  |

**例1：动态目录**

下面的文件路径来自 TerraFirmaGreg-Modern 整合包的帕秋莉手册文件。

原文件路径:

```
Modpack-Modern/kubejs/assets/tfc/patchouli_books/field_guide/en_us/categories/beneath.json
Modpack-Modern/kubejs/assets/tfc/patchouli_books/field_guide/en_us/entries/beneath/biomes.json
Modpack-Modern/kubejs/assets/tfc/patchouli_books/field_guide/en_us/entries/firmalife/irrigation.json
Modpack-Modern/kubejs/assets/tfc/patchouli_books/field_guide/en_us/entries/tfg_ores/ore_basic.json
```

规则:

```json
{
  "sourcePattern" : "Modpack-Modern/kubejs/assets/**/en_us/**/*.json",
  "translationPattern" : "%original_path_pre%/%language%/%original_path%/%original_file_name%",
  "srcLang" : "en_us",
  "destLang" : "zh_cn"
}
```

根据 srcLang = `en_us` 找到的位置，将路径分割成3个部分：

| `%original_path_pre%`                                          | `%original_path%`    | `%original_file_name%` |
|:---------------------------------------------------------------|:---------------------|:-----------------------|
| `Modpack-Modern/kubejs/assets/tfc/patchouli_books/field_guide` | `categories`         | `beneath.json`         |
| `Modpack-Modern/kubejs/assets/tfc/patchouli_books/field_guide` | `entries/beneath`    | `biomes.json`          |
| `Modpack-Modern/kubejs/assets/tfc/patchouli_books/field_guide` | `entries/firmalife`  | `irrigation.json`      |
| `Modpack-Modern/kubejs/assets/tfc/patchouli_books/field_guide` | `entries/tfg_ores`   | `ore_basic.json`       |

由于 destLang = `zh_cn`，`%language%` 替换为 `zh_cn`，得到结果：

- `Modpack-Modern/kubejs/assets/tfc/patchouli_books/field_guide`/`zh_cn`/`categories`/`beneath.json`
- `Modpack-Modern/kubejs/assets/tfc/patchouli_books/field_guide`/`zh_cn`/`entries/beneath`/`biomes.json`
- `Modpack-Modern/kubejs/assets/tfc/patchouli_books/field_guide`/`zh_cn`/`entries/firmalife`/`irrigation.json`
- `Modpack-Modern/kubejs/assets/tfc/patchouli_books/field_guide`/`zh_cn`/`entries/tfg_ores`/`ore_basic.json`

细心的朋友可能发现了，此示例中的 `%original_path_pre%` 总是一样的，`%original_path%` 和 `%original_file_name%` 会变化，因此映射规则也可以写成。

```json
{
  "sourcePattern" : "Modpack-Modern/kubejs/assets/tfc/patchouli_books/field_guide/en_us/**/*.json",
  "translationPattern" : "Modpack-Modern/kubejs/assets/tfc/patchouli_books/field_guide/%language%/%original_path%/%original_file_name%",
  "srcLang" : "en_us",
  "destLang" : "zh_cn"
}
```

**例2：只修改文件名**

如果只是想要修改文件名，例如从 en_us.json 改为 zh_cn.json，用 %original_file_name% 肯定不适合。可以参考规则如下：

原文件路径：

```
kubejs/assets/create/lang/en_us.json
kubejs/assets/gtcet/lang/en_us.json
kubejs/assets/tfc/lang/en_us.json
```

规则:

```json
{
  "sourcePattern" : "kubejs/assets/**/lang/en_us.json",
  "translationPattern" : "%original_path_pre%/%language%.json",
  "srcLang" : "en_us",
  "destLang" : "zh_cn"
}
```

根据 srcLang = `en_us` 找到的位置，将路径分割成3个部分：

| `%original_path_pre%`       | `%original_path%` | `%original_file_name%` |
|:----------------------------|:------------------|:-----------------------|
| `kubejs/assets/create/lang` | 无                 | `en_us.json`           |
| `kubejs/assets/gtcet/lang`  | 无                 | `en_us.json`           |
| `kubejs/assets/tfc/lang`    | 无                 | `en_us.json`           |

生成映射路径：

- `kubejs/assets/create/lang`/`zh_cn`.json
- `kubejs/assets/gtcet/lang`/`zh_cn`.json
- `kubejs/assets/tfc/lang`/`zh_cn`.json