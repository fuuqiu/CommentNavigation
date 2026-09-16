import type { CodeLang } from './code';
import type { Lang } from './index';

/**
 * All landing-page copy, both languages, in one file.
 *
 * Every claim here is traceable to the repository at
 * https://github.com/fuuqiu/CommentNavigation @ main — README.md, plugin.xml and
 * LICENSE.txt. Nothing about pricing, IDE versions, telemetry or the Marketplace
 * listing may be invented here; if the plugin changes, this file changes with it.
 *
 * Inline `backticks` in these strings are rendered as <code> at build time
 * (see inlineCode() in ./inline.ts).
 */

export interface Step {
  title: string;
  body: string;
}

export interface OutlineNode {
  level: number;
  label: string;
  selected?: boolean;
}

export interface CodeSample {
  label: string;
  lang: CodeLang;
  code: string;
  notes: string[];
}

export interface HomeCopy {
  meta: { title: string; description: string };
  hero: {
    badges: string[];
    h1: string;
    lede: string;
    ctaMarketplace: string;
    ctaGithub: string;
    ctaSyntax: string;
    ctaNote: string;
  };
  mock: {
    window: string;
    tab: string;
    toolWindow: string;
    code: string;
    activeLine: number;
    outline: OutlineNode[];
    alt: string;
  };
  steps: { title: string; items: Step[]; why: string };
  features: { title: string; lede: string; items: Step[]; notes: string[] };
  syntax: {
    title: string;
    lede: string;
    sql: CodeSample;
    http: CodeSample;
    tableCaption: string;
    tableHead: [string, string];
    table: [string, string][];
    rulesTitle: string;
    rules: string[];
  };
  install: {
    title: string;
    lede: string;
    marketplacePending: Step;
    marketplaceLive: Step;
    steps: Step[];
    compat: string;
    examplesLead: string;
    exampleSql: string;
    exampleHttp: string;
  };
  license: {
    title: string;
    body: string[];
    linksTitle: string;
    links: { source: string; issues: string; license: string; docs: string };
    contact: string;
  };
}

const EN_MOCK = `-- # Roles and permissions audit
-- Ordinary notes stay out of the heading outline.

-- ## Role entities
-- ### System templates
SELECT 'system role' AS example;

-- ### Merchant roles
SELECT 'merchant role' AS example;

-- ## Grant details
/*
 * ### Page permissions
 * #### PRO pages
 */
SELECT 'page permission' AS example;

-- # Before and after`;

const ZH_MOCK = `-- # 角色与权限核对
-- 普通说明不进入标题大纲。

-- ## 角色本体
-- ### 系统模板
SELECT 'system role' AS example;

-- ### 商户角色
SELECT 'merchant role' AS example;

-- ## 授权明细
/*
 * ### 页面权限
 * #### PRO 页面
 */
SELECT 'page permission' AS example;

-- # 结果对照`;

const EN_SQL = `-- # Roles and permissions audit
-- ## Role entities
-- ### System templates
SELECT 'system role';

-- ### Merchant roles
SELECT 'merchant role';

-- ## Grant details
/*
 * ### Page permissions
 * #### PRO pages
 */
SELECT 'page permission';`;

const EN_HTTP = `// # Freezer queries
// ## Normal cases
# ### Response field check

### List freezers
GET {{host}}/freezers

// # Replenishment
### Replenishment detail
GET {{host}}/replenishments/example`;

const ZH_SQL = `-- # 角色与权限核对
-- ## 角色本体
-- ### 系统模板
SELECT 'system role';

-- ### 商户角色
SELECT 'merchant role';

-- ## 授权明细
/*
 * ### 页面权限
 * #### PRO 页面
 */
SELECT 'page permission';`;

const ZH_HTTP = `// # 货柜查询
// ## 正常场景
# ### 响应字段核对

### 批量查询货柜
GET {{host}}/freezers

// # 补货单
### 补货单详情
GET {{host}}/replenishments/example`;

export const HOME: Record<Lang, HomeCopy> = {
  en: {
    meta: {
      title: 'Code Comment Navigator — SQL & HTTP comment outline for IntelliJ IDEA',
      description:
        'Free MIT-licensed IntelliJ IDEA plugin that turns the Markdown headings in your SQL, HTTP and REST comments into a collapsible outline you can click to jump.',
    },
    hero: {
      badges: ['Free & open source', 'MIT License', 'IntelliJ IDEA 2026.2'],
      h1: 'A table of contents for your SQL and HTTP files',
      lede:
        'Code Comment Navigator reads the Markdown headings you already write in comments — `-- # Heading` in SQL, `// # Heading` in HTTP — and shows them as a collapsible Comment Outline on the right side of IntelliJ IDEA. Click a heading to jump to that comment line. It works in project files and in Scratches, where iteration scripts grow longest.',
      ctaMarketplace: 'Install from JetBrains Marketplace',
      ctaGithub: 'Get it on GitHub',
      ctaSyntax: 'See the heading syntax',
      ctaNote: 'Version 1.1.0 · MIT License · built for IntelliJ IDEA 2026.2',
    },
    mock: {
      window: 'Scratches and Consoles',
      tab: 'iteration.sql',
      toolWindow: 'Comment Outline',
      code: EN_MOCK,
      activeLine: 8,
      outline: [
        { level: 1, label: 'Roles and permissions audit' },
        { level: 2, label: 'Role entities' },
        { level: 3, label: 'System templates' },
        { level: 3, label: 'Merchant roles', selected: true },
        { level: 2, label: 'Grant details' },
        { level: 3, label: 'Page permissions' },
        { level: 4, label: 'PRO pages' },
        { level: 1, label: 'Before and after' },
      ],
      alt:
        'The illustration above is a mock of an IntelliJ IDEA window: a SQL file on the left whose comment headings (-- # Roles and permissions audit, -- ## Role entities, -- ### Merchant roles, and so on) appear on the right in a Comment Outline tool window as a four-level tree. "Merchant roles" is selected in the tree, and the matching comment line is highlighted in the editor.',
    },
    steps: {
      title: 'How it works',
      items: [
        {
          title: 'Write headings in comments',
          body:
            'Mark sections with `#` to `######` inside an ordinary comment line — `-- # Heading` in SQL, `// # Heading` in HTTP. A space after the hashes is required. Nothing else about the file changes.',
        },
        {
          title: 'Open Comment Outline',
          body:
            'Click the Comment Outline tab on the right edge, or go to View → Tool Windows → Comment Outline. The editor context menu and the Edit menu also carry the Comment Structure action (Shift+Cmd+F12 in the default keymap).',
        },
        {
          title: 'Click to jump, type to search',
          body:
            'Click a heading — or select it with the up and down keys and press Enter — to jump to that comment line. Typing in the tree searches headings directly.',
        },
      ],
      why:
        'Iteration scripts in Scratches keep growing: a few hundred lines of SQL with no structure and no quick way back to the section you were reading. The outline gives that file a table of contents without changing how you write it.',
    },
    features: {
      title: 'Features',
      lede: 'Everything the outline does, and nothing it does not.',
      items: [
        {
          title: 'Six heading levels for SQL, HTTP and REST',
          body:
            '`.sql`, `.http` and `.rest` files. Markdown `#` through `######` become a tree. A skipped level attaches to the nearest shallower heading, and same-level headings keep file order.',
        },
        {
          title: 'Click or Enter to jump, type to search',
          body:
            'Click a heading, or move with the up and down keys and press Enter, to jump to the comment line. Type in the tree to search headings — no dialog to open first.',
        },
        {
          title: 'Follows the editor, refreshes without saving',
          body:
            'The panel follows whichever editor is selected and rebuilds the outline about 250 ms after an edit. The file does not have to be saved.',
        },
        {
          title: 'Expand, collapse, keep your place',
          body:
            'Expand or collapse any node, plus expand-all and collapse-all. Folding state is kept while you edit the same file, and resets to expanded when you switch files.',
        },
        {
          title: 'HTTP `###` separators, with a plain-comment fallback',
          body:
            '`### Request name` separators from the HTTP Client become level-2 nodes, and empty `###` lines are skipped. A file with no headings and no request names at all falls back to a flat list of its ordinary comments.',
        },
        {
          title: 'The Java and Kotlin popup is still there',
          body:
            'The existing comment navigation popup for Java and Kotlin is retained and opens from the same Comment Structure action (Shift+Cmd+F12).',
        },
      ],
      notes: [
        'Works in Scratch files as well as project files — ever-growing scratch scripts are the reason this plugin exists.',
        'No dependency on the SQL/Database or HTTP Client language plugins: parsing uses the platform Document API.',
      ],
    },
    syntax: {
      title: 'Heading syntax',
      lede:
        'Headings are ordinary comments. The database console and the HTTP Client still see plain comments, so a file stays runnable exactly as before.',
      sql: {
        label: 'SQL',
        lang: 'sql',
        code: EN_SQL,
        notes: [
          '`--` line comments and `/* … */` block comments are both read.',
          'Each heading has to sit on its own comment line.',
          'Trailing comments after a statement, strings and quoted identifiers are never mistaken for headings.',
        ],
      },
      http: {
        label: 'HTTP',
        lang: 'http',
        code: EN_HTTP,
        notes: [
          '`// # Heading` is the recommended form; `# # Heading` — a hash comment plus a Markdown marker — works too.',
          '`### Request name` is always a level-2 node, so do not use it for level 3; write `// ### Heading` when you need an explicit level.',
          'Empty `###` separators are skipped, and comments inside request pre/post scripts are ignored.',
        ],
      },
      tableCaption: 'HTTP heading levels',
      tableHead: ['Written as', 'Outline level'],
      table: [
        ['`// # Heading` or `# # Heading`', 'Level 1'],
        ['`// ## Heading`', 'Level 2'],
        ['`// ### Heading`', 'Level 3'],
        ['`### Request name`', 'Level 2 — the HTTP Client request separator'],
      ],
      rulesTitle: 'Rules that apply to both',
      rules: [
        'A space is required after the hash marker.',
        'Levels 1 to 6.',
        'A skipped level attaches to the nearest shallower heading.',
        'Same-level headings keep file order.',
        'Ordinary comments do not pollute an outline that already has explicit headings.',
        'A file with no headings and no request names at all falls back to a flat list of ordinary comments.',
      ],
    },
    install: {
      title: 'Install',
      lede: `Two minutes, one command, and an IDE restart when IntelliJ IDEA asks for one.`,
      marketplacePending: {
        title: 'Marketplace listing is being set up',
        body:
          'The JetBrains Marketplace listing for this plugin is not live yet, and there are no GitHub Releases either. Until it is published, build the installable ZIP yourself — the steps below take one command.',
      },
      marketplaceLive: {
        title: 'Install from JetBrains Marketplace',
        body:
          'Open the Marketplace listing and install the plugin into IntelliJ IDEA 2026.2, or search for Code Comment Navigator in Settings → Plugins → Marketplace.',
      },
      steps: [
        {
          title: 'Build from source',
          body:
            'Clone the repository and run `./gradlew test buildPlugin` with JDK 25. The installable package is written to `build/distributions/comment-navigation-1.1.0.zip`.',
        },
        {
          title: 'Install Plugin from Disk',
          body:
            'In the IDE: Settings → Plugins → gear menu → Install Plugin from Disk…, choose the ZIP, and follow the IDE prompts to finish.',
        },
        {
          title: 'Open a SQL or HTTP file',
          body:
            'Open a `.sql`, `.http` or `.rest` file, then click the Comment Outline tab on the right edge, or go to View → Tool Windows → Comment Outline.',
        },
      ],
      compat:
        'This version is built for and verified on IntelliJ IDEA 2026.2 (build 262.*). Earlier IDE versions are not in the compatibility range. Building from source needs JDK 25.',
      examplesLead: 'Want to see the outline before installing? The repository ships two example files:',
      exampleSql: 'SQL example',
      exampleHttp: 'HTTP example',
    },
    license: {
      title: 'License and contact',
      body: [
        'Code Comment Navigator is released under the MIT License, copyright 2024–2026 Fuuqiu (Tinyue). It is free for personal and commercial use — no purchase, no subscription, no activation key.',
        'You may modify it, redistribute it and sell it, as long as the copyright notice and the license notice are kept. The software is provided as is, without warranty. There is no fixed update schedule and no promised support response time.',
      ],
      linksTitle: 'Links',
      links: {
        source: 'Source code on GitHub',
        issues: 'Report an issue',
        license: 'Full MIT License',
        docs: 'README and examples',
      },
      contact: 'Questions, bugs and feature requests — open an issue, or write to',
    },
  },

  zh: {
    meta: {
      title: 'Code Comment Navigator —— IntelliJ IDEA 的 SQL / HTTP 注释大纲',
      description:
        '免费开源的 IntelliJ IDEA 插件：把 SQL、HTTP、REST 注释里的 Markdown 标题变成右侧可折叠的注释大纲，点击标题即可跳转，Scratches 里的长脚本同样适用。',
    },
    hero: {
      badges: ['免费开源', 'MIT 许可证', 'IntelliJ IDEA 2026.2'],
      h1: '给 SQL 和 HTTP 文件一份目录',
      lede:
        'Code Comment Navigator 读取你本来就写在注释里的 Markdown 标题——SQL 里的 `-- # 标题`、HTTP 里的 `// # 标题`——在 IntelliJ IDEA 右侧生成可折叠的 Comment Outline，点击标题就跳到对应注释行。项目文件和 Scratches 里越写越长的迭代脚本都适用。',
      ctaMarketplace: '从 JetBrains Marketplace 安装',
      ctaGithub: '在 GitHub 上获取',
      ctaSyntax: '查看标题写法',
      ctaNote: '版本 1.1.0 · MIT 许可证 · 适配 IntelliJ IDEA 2026.2',
    },
    mock: {
      window: 'Scratches and Consoles',
      tab: 'iteration.sql',
      toolWindow: 'Comment Outline',
      code: ZH_MOCK,
      activeLine: 8,
      outline: [
        { level: 1, label: '角色与权限核对' },
        { level: 2, label: '角色本体' },
        { level: 3, label: '系统模板' },
        { level: 3, label: '商户角色', selected: true },
        { level: 2, label: '授权明细' },
        { level: 3, label: '页面权限' },
        { level: 4, label: 'PRO 页面' },
        { level: 1, label: '结果对照' },
      ],
      alt:
        '上方是 IntelliJ IDEA 窗口的示意图：左侧编辑器里是一个 SQL 文件，注释里的标题（-- # 角色与权限核对、-- ## 角色本体、-- ### 商户角色 等）在右侧 Comment Outline 工具窗口中变成四层树；树里选中的是「商户角色」，编辑器中对应的注释行同时高亮。',
    },
    steps: {
      title: '怎么用',
      items: [
        {
          title: '在注释里写标题',
          body:
            '在普通注释行里用 `#` 到 `######` 标记层级：SQL 写 `-- # 标题`，HTTP 写 `// # 标题`。井号后必须留一个空格，文件的其他部分什么都不用改。',
        },
        {
          title: '打开 Comment Outline',
          body:
            '点右侧的 Comment Outline 标签，或走 View → Tool Windows → Comment Outline。编辑器右键菜单和 Edit 菜单里同样有 Comment Structure 动作（默认键位 Shift+Cmd+F12）。',
        },
        {
          title: '点击跳转，输入搜索',
          body:
            '点击标题，或用上下键选中后按 Enter，跳到对应注释行。直接在树里输入就能搜索标题。',
        },
      ],
      why:
        'Scratches 里的迭代脚本会一路变长：几百行 SQL，没有结构，也很难回到刚才在看的那一段。注释大纲不改变你的书写习惯，只是把这份文件变成一份目录。',
    },
    features: {
      title: '功能',
      lede: '大纲能做的事，以及它不做的事。',
      items: [
        {
          title: 'SQL / HTTP / REST 六级标题',
          body:
            '支持 `.sql`、`.http`、`.rest` 文件，Markdown `#` 到 `######` 直接变成树。跳级时挂在前一个层级更浅的标题下，同级标题保持文件顺序。',
        },
        {
          title: '点击或回车跳转，输入即搜索',
          body:
            '点击标题，或用上下键选中后按 Enter，跳到对应注释行。在树里直接输入就能搜索标题，不用先开弹窗。',
        },
        {
          title: '跟随编辑器，不必保存',
          body: '面板跟随当前选中的编辑器，修改后约 250 ms 自动重建大纲，不需要先保存文件。',
        },
        {
          title: '展开、折叠、保留状态',
          body:
            '可逐个节点展开或折叠，也有全部展开、全部折叠。同一文件编辑时保留折叠状态，切换文件后恢复为展开。',
        },
        {
          title: '认识 HTTP 的 `###`，也有普通注释兜底',
          body:
            'HTTP Client 的 `### 请求名称` 分隔符固定作为二级节点，空的 `###` 会跳过。整个文件没有任何标题和请求名称时，回退成普通注释的平级列表。',
        },
        {
          title: 'Java / Kotlin 弹窗保留',
          body:
            '原有的 Java / Kotlin 注释导航弹窗保留，仍然通过同一个 Comment Structure 动作（Shift+Cmd+F12）打开。',
        },
      ],
      notes: [
        '项目文件和 Scratches 都支持——不断变长的 scratch 脚本正是这个插件的由来。',
        '解析使用平台 Document API，不依赖 SQL / Database 或 HTTP Client 语言插件。',
      ],
    },
    syntax: {
      title: '标题写法',
      lede:
        '标题就是普通注释。数据库控制台和 HTTP Client 看到的仍然是注释，文件照样能直接执行。',
      sql: {
        label: 'SQL',
        lang: 'sql',
        code: ZH_SQL,
        notes: [
          '支持 `--` 行注释和 `/* … */` 块注释。',
          '标题必须独占一整行注释。',
          '不会把 SQL 语句行尾注释、字符串或引用标识符里的内容误认成标题。',
        ],
      },
      http: {
        label: 'HTTP',
        lang: 'http',
        code: ZH_HTTP,
        notes: [
          '推荐用 `// # 标题`；`# # 标题`（`#` 注释后再加 Markdown 标记）同样支持。',
          '`### 请求名称` 固定是二级节点，因此不要用它表达三级标题；需要明确层级时写 `// ### 标题`。',
          '空的 `###` 不进入目录；请求前置和响应处理脚本里的注释也不进入目录。',
        ],
      },
      tableCaption: 'HTTP 标题层级对照',
      tableHead: ['写法', '大纲层级'],
      table: [
        ['`// # 标题` 或 `# # 标题`', '一级'],
        ['`// ## 标题`', '二级'],
        ['`// ### 标题`', '三级'],
        ['`### 请求名称`', '二级 —— HTTP Client 原有的请求分隔符'],
      ],
      rulesTitle: '两种语言通用的规则',
      rules: [
        '标题标记后必须留一个空格。',
        '支持 1–6 级。',
        '跳级时挂在前一个层级更浅的标题下。',
        '同级标题保持文件顺序。',
        '普通说明不会混入已有显式标题的大纲。',
        '整个文件没有任何标题或请求名称时，回退到普通注释的平级列表。',
      ],
    },
    install: {
      title: '安装',
      lede: '两分钟、一条命令，IDE 提示时重启一次即可。',
      marketplacePending: {
        title: 'Marketplace 条目建设中',
        body:
          'JetBrains Marketplace 的条目尚未上线，也还没有 GitHub Release。在它发布之前，请按下面的步骤自行构建安装包——只要一条命令。',
      },
      marketplaceLive: {
        title: '从 JetBrains Marketplace 安装',
        body:
          '打开 Marketplace 条目安装到 IntelliJ IDEA 2026.2，或在 Settings → Plugins → Marketplace 里搜索 Code Comment Navigator。',
      },
      steps: [
        {
          title: '从源码构建',
          body:
            '克隆仓库，用 JDK 25 执行 `./gradlew test buildPlugin`。安装包输出在 `build/distributions/comment-navigation-1.1.0.zip`。',
        },
        {
          title: 'Install Plugin from Disk',
          body:
            '在 IDE 里打开 Settings → Plugins → 齿轮菜单 → Install Plugin from Disk…，选择该 ZIP，按 IDE 提示完成安装。',
        },
        {
          title: '打开 SQL 或 HTTP 文件',
          body:
            '打开 `.sql`、`.http` 或 `.rest` 文件，点右侧的 Comment Outline 标签，或走 View → Tool Windows → Comment Outline。',
        },
      ],
      compat:
        '本版本的构建与验证范围是 IntelliJ IDEA 2026.2（262.*），更早的 IDE 版本未纳入兼容范围。从源码构建需要 JDK 25。',
      examplesLead: '想先看看效果？仓库里带了两个示例文件：',
      exampleSql: 'SQL 示例',
      exampleHttp: 'HTTP 示例',
    },
    license: {
      title: '许可与联系',
      body: [
        'Code Comment Navigator 采用 MIT 许可证，版权归 Fuuqiu (Tinyue) 所有（2024–2026）。个人和企业均可免费使用，包括商业用途，无需购买、订阅或激活码。',
        '允许修改、再分发以及销售，须保留版权声明与许可声明。软件按现状提供，不提供担保，也不承诺固定的更新周期或支持响应时间。',
      ],
      linksTitle: '相关链接',
      links: {
        source: 'GitHub 源代码',
        issues: '问题反馈',
        license: 'MIT 许可证全文',
        docs: 'README 与示例',
      },
      contact: '有问题、Bug 或功能建议：提一个 issue，或发邮件到',
    },
  },
};
